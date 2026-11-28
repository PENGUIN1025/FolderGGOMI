package com.aggro1025.jeopda

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.PhoneNumberUtils
import android.telephony.TelephonyManager
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.aggro1025.jeopda.ads.UnlockAds
import com.aggro1025.jeopda.apps.AppGuard
import com.aggro1025.jeopda.billing.RefillBilling
import com.aggro1025.jeopda.data.UnlockRules
import com.aggro1025.jeopda.apps.AppPolicy
import com.aggro1025.jeopda.apps.Apps
import com.aggro1025.jeopda.apps.BuiltIn
import com.aggro1025.jeopda.data.AppPrefs
import com.aggro1025.jeopda.data.Contact
import com.aggro1025.jeopda.data.ContactsRepo
import com.aggro1025.jeopda.session.NotificationBlocker
import com.aggro1025.jeopda.session.SmartSession
import com.aggro1025.jeopda.session.UnlockMethod
import com.aggro1025.jeopda.ui.JeopdaTheme
import com.aggro1025.jeopda.ui.Lcd
import com.aggro1025.jeopda.ui.folder.FolderActions
import com.aggro1025.jeopda.ui.folder.FolderApp
import com.aggro1025.jeopda.ui.folder.FolderKey
import com.aggro1025.jeopda.ui.folder.KeyRouter
import com.aggro1025.jeopda.ui.folder.MenuEntry
import com.aggro1025.jeopda.ui.folder.Screen
import com.aggro1025.jeopda.ui.smart.RelayWaiting
import com.aggro1025.jeopda.ui.smart.SmartDrawer
import com.aggro1025.jeopda.ui.smart.SetupPending
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 홈(런처) 화면.
 * - 폴더폰 모드: 폴더폰 화면을 그린다.
 * - 스마트폰 모드: 원래 홈 앱(예: One UI 홈)을 바로 띄워 준다 (질문 8번 c안).
 *   접다가 계속 기본 홈 앱이라서, 시간이 끝나면 홈 버튼에서 폴더폰 화면이 나온다.
 */
class MainActivity : ComponentActivity() {

    private enum class Mode { SETUP, FOLDER, RELAY, DRAWER }

    private val prefs by lazy { AppPrefs(this) }
    private val router = KeyRouter()
    private var mode by mutableStateOf(Mode.FOLDER)
    private var screen by mutableStateOf<Screen>(Screen.Home)
    private var menu by mutableStateOf<List<MenuEntry>>(emptyList())
    private var contacts by mutableStateOf<List<Contact>>(emptyList())
    private var contactsPermitted by mutableStateOf(false)
    private var notifBlockOn by mutableStateOf(true)
    private var appGuardOn by mutableStateOf(true)
    private var freeTick by mutableStateOf(0)
    /** 폴더폰 디자인 테마 (설정에서 바꾸면 돌아올 때 다시 읽음) */
    private var theme by mutableStateOf(com.aggro1025.jeopda.ui.theme.FolderTheme.CLASSIC)
    private val relayTimes = ArrayDeque<Long>()

    /** 전화 걸기 권한을 묻는 동안 걸려던 번호 */
    private var pendingCall: String? = null
    private var contactsAskedOnce = false

    private val callPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val number = pendingCall ?: return@registerForActivityResult
        pendingCall = null
        // 거절해도 전화는 쓸 수 있게: 기본 전화 앱에 번호를 채워서 연다
        if (granted) placeCall(number) else Apps.dial(this, number)
    }

    private val contactsPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        contactsAskedOnce = true
        contactsPermitted = granted
        if (granted) reloadContacts()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReturnExtra(intent)
        handleBlockedExtra(intent)

        // 뒤로: 하위 화면이면 홈으로, 홈이면 아무것도 안 함 (런처는 닫히지 않는다)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mode == Mode.FOLDER && screen != Screen.Home) {
                    screen = when (val sc = screen) {
                        is Screen.UnlockInput -> if (sc.step == 1) sc.copy(step = 0) else Screen.UnlockMethod()
                        is Screen.UnlockWait, is Screen.UnlockAd, is Screen.Blocked -> Screen.Home
                        is Screen.Contacts -> if (sc.keys.isNotEmpty()) Screen.Contacts(sc.keys.dropLast(1)) else Screen.Home
                        is Screen.Dialer -> if (sc.number.isNotEmpty()) Screen.Dialer(sc.number.dropLast(1)) else Screen.Home
                        else -> Screen.Home
                    }
                }
            }
        })

        setContent {
            JeopdaTheme {
                LaunchedEffect(mode) { applySystemBars(mode == Mode.FOLDER) }
                when (mode) {
                    Mode.FOLDER -> androidx.compose.runtime.CompositionLocalProvider(
                        com.aggro1025.jeopda.ui.theme.LocalFolderTheme provides theme,
                    ) {
                        FolderApp(
                            screen = screen,
                            onScreen = { screen = it },
                            menu = menu,
                            actions = actions,
                            router = router,
                            devUnlock = BuildConfig.DEBUG,
                            contacts = contacts,
                        )
                    }
                    Mode.SETUP -> SetupPending(onOpen = { actions.openSettings() })
                    // 보통은 바로 원래 홈 화면이 덮어서 보이지 않음. 원래 홈 앱이 안 열리면 잠시 뒤 버튼이 나온다
                    Mode.RELAY -> RelayWaiting(
                        onOpen = { relayTimes.clear(); enterSmart() },
                        onDrawer = { mode = Mode.DRAWER },
                        onReturn = { returnToFolder() },
                    )
                    Mode.DRAWER -> SmartDrawer(onReturn = { returnToFolder() })
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleReturnExtra(intent)
        // 홈 버튼을 누르면 폴더폰 대기 화면으로
        screen = Screen.Home
        handleBlockedExtra(intent)
    }

    /** 다른 앱 막기로 돌아온 경우: 어떤 앱이 막혔는지 보여준다 */
    private fun handleBlockedExtra(intent: Intent?) {
        val pkg = intent?.getStringExtra(EXTRA_BLOCKED_PACKAGE) ?: return
        intent.removeExtra(EXTRA_BLOCKED_PACKAGE)
        if (SmartSession.isSmart(this)) return
        screen = Screen.Blocked(Apps.entry(this, pkg)?.label ?: pkg)
    }

    override fun onResume() {
        super.onResume()
        SmartSession.sync(this)
        if (!prefs.setupDone) {
            // 최초 설정 전: 처음 한 번만 설정 화면을 자동으로 연다 (뒤로 가기로 갇히지 않게)
            if (mode != Mode.SETUP) startActivity(Intent(this, SettingsActivity::class.java))
            mode = Mode.SETUP
            return
        }
        if (SmartSession.isSmart(this)) {
            enterSmart()
        } else {
            mode = Mode.FOLDER
            menu = buildMenu()
            reloadContacts()
            notifBlockOn = NotificationBlocker.isEnabled(this)
            appGuardOn = AppGuard.isReady(this)
            // 결제는 끝났는데 앱이 꺼져서 못 채운 경우 마저 처리
            theme = com.aggro1025.jeopda.ui.theme.FolderTheme.current(prefs)
            RefillBilling.restorePending(
                this,
                onRefilled = { freeTick++ },
                onOwnedChanged = { theme = com.aggro1025.jeopda.ui.theme.FolderTheme.current(prefs) },
            )
            NotificationBlocker.sweepNow()
        }
    }

    /** 연락처를 백그라운드에서 다시 읽는다 (권한이 있을 때만) */
    private fun reloadContacts() {
        contactsPermitted = ContactsRepo.hasPermission(this)
        if (!contactsPermitted) return
        lifecycleScope.launch {
            contacts = withContext(Dispatchers.IO) { runCatching { ContactsRepo.load(this@MainActivity) }.getOrDefault(emptyList()) }
        }
    }

    private fun isEmergency(number: String): Boolean {
        val d = number.filter { it.isDigit() }
        if (d in KR_EMERGENCY) return true
        return try {
            if (Build.VERSION.SDK_INT >= 29) {
                getSystemService(TelephonyManager::class.java)?.isEmergencyNumber(d) == true
            } else {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.isEmergencyNumber(d)
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun placeCall(number: String) {
        try {
            startActivity(Intent(Intent.ACTION_CALL, Uri.fromParts("tel", number, null)))
        } catch (e: SecurityException) {
            Apps.dial(this, number)
        } catch (e: ActivityNotFoundException) {
            Apps.dial(this, number)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (mode == Mode.FOLDER) {
            val key = FolderKey.fromKeyCode(keyCode)
            if (key != null && router.dispatch(key)) return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleReturnExtra(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_RETURN_TO_FOLDER, false) == true) {
            SmartSession.end(this)
            intent.removeExtra(EXTRA_RETURN_TO_FOLDER)
            screen = Screen.Home
        }
    }

    private fun returnToFolder() {
        SmartSession.end(this)
        screen = Screen.Home
        mode = Mode.FOLDER
        menu = buildMenu()
    }

    private fun enterSmart() {
        if (relayToSmartHome()) mode = Mode.RELAY else mode = Mode.DRAWER
    }

    /** 원래 홈 앱을 띄운다. 실패하거나 짧은 시간에 반복되면(원래 홈 앱이 바로 닫히는 경우) 대체 화면을 쓴다 */
    private fun relayToSmartHome(): Boolean {
        val now = SystemClock.elapsedRealtime()
        relayTimes.addLast(now)
        while (relayTimes.isNotEmpty() && relayTimes.first() < now - 3000) relayTimes.removeFirst()
        if (relayTimes.size > 4) return false

        val target = smartHomeTarget() ?: return false
        return try {
            startActivity(
                Intent(Intent.ACTION_MAIN)
                    .addCategory(Intent.CATEGORY_HOME)
                    .setComponent(target)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            // 처음 상태로 새로 열기: 앱 서랍이 열린 채로 남는 문제 대응 (설정에서 끌 수 있음)
                            (if (prefs.smartHomeFresh) Intent.FLAG_ACTIVITY_CLEAR_TASK else 0),
                    ),
            )
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
            true
        } catch (e: ActivityNotFoundException) {
            false
        } catch (e: SecurityException) {
            false
        }
    }

    private fun smartHomeTarget(): ComponentName? {
        val candidates = Apps.otherHomeApps(this).map { it.first }
        val saved = prefs.smartHomeComponent?.let { ComponentName.unflattenFromString(it) }
        return saved?.takeIf { it in candidates }
            ?: candidates.firstOrNull { it.packageName == SAMSUNG_HOME }
            ?: candidates.firstOrNull()
    }

    private fun buildMenu(): List<MenuEntry> {
        fun icon(pkg: String?) = pkg?.let { Apps.entry(this, it)?.icon }?.toBitmap(144, 144)?.asImageBitmap()
        val builtIns = BuiltIn.entries.map { MenuEntry(it.slot, it.label, icon = icon(Apps.builtInPackage(this, it))) }
        val user = AppPrefs.USER_SLOTS.map { slot ->
            val app = prefs.slotPackage(slot)?.let { Apps.entry(this, it) }
            if (app != null) {
                MenuEntry(slot, app.label, icon = app.icon?.toBitmap(144, 144)?.asImageBitmap())
            } else {
                MenuEntry(slot, "비어 있음", empty = true)
            }
        }
        return builtIns + user
    }

    /** 폴더폰 모드에서는 화면 안에 자체 상태 표시줄이 있으므로 시스템 상태 표시줄을 숨긴다 (위에서 쓸어내리면 잠깐 보임) */
    private fun applySystemBars(folder: Boolean) {
        val c = WindowCompat.getInsetsController(window, window.decorView)
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (folder) c.hide(WindowInsetsCompat.Type.statusBars()) else c.show(WindowInsetsCompat.Type.statusBars())
    }

    private val actions = object : FolderActions {
        override fun openMenuSlot(slot: Int) {
            val builtIn = BuiltIn.entries.firstOrNull { it.slot == slot }
            // 전화·연락처는 폴더폰 화면 안에서
            if (builtIn == BuiltIn.PHONE) {
                screen = Screen.Dialer()
                return
            }
            if (builtIn == BuiltIn.CONTACTS) {
                screen = Screen.Contacts()
                return
            }
            if (builtIn != null) {
                Apps.launchBuiltIn(this@MainActivity, builtIn)
                return
            }
            val pkg = prefs.slotPackage(slot)
            when {
                pkg == null || Apps.entry(this@MainActivity, pkg) == null ->
                    Toast.makeText(this@MainActivity, "${slot}번은 비어 있어요. 스마트폰 모드의 설정에서 정할 수 있어요", Toast.LENGTH_SHORT).show()
                // 이전 버전에서 넣어 둔 앱이 지금 규칙으로 막히는 경우
                AppPolicy.menuBlockReason(this@MainActivity, pkg) != null ->
                    Toast.makeText(this@MainActivity, AppPolicy.BLOCKED_MESSAGE, Toast.LENGTH_LONG).show()
                else -> Apps.launchPackage(this@MainActivity, pkg)
            }
        }

        override fun call(number: String) {
            val n = number.filter { it.isDigit() || it in "+*#" }
            if (n.isEmpty()) return
            when {
                // 긴급번호는 안드로이드가 일반 앱의 바로 걸기를 막는다: 기본 전화 앱에 채워서 연다
                isEmergency(n) -> Apps.dial(this@MainActivity, n)
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.CALL_PHONE) ==
                    PackageManager.PERMISSION_GRANTED -> placeCall(n)
                else -> {
                    pendingCall = n
                    callPermission.launch(Manifest.permission.CALL_PHONE)
                }
            }
        }

        override fun sms(number: String) {
            try {
                startActivity(Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", number, null)))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this@MainActivity, "문자 앱을 찾을 수 없어요", Toast.LENGTH_SHORT).show()
            }
        }

        override fun hasContactsPermission(): Boolean = contactsPermitted

        override fun requestContactsPermission() {
            val canAsk = !contactsAskedOnce ||
                shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)
            if (canAsk) {
                contactsPermission.launch(Manifest.permission.READ_CONTACTS)
            } else {
                // "다시 묻지 않음" 상태: 앱 설정 화면에서 직접 켜도록 안내
                Toast.makeText(this@MainActivity, "설정 > 권한에서 연락처를 허용해 주세요", Toast.LENGTH_LONG).show()
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)),
                )
            }
        }

        override fun openSettings() {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        override fun freeRemaining(): Int {
            freeTick // 결제로 다시 채워지면 화면이 새로 그려지게
            return SmartSession.freeRemainingToday(this@MainActivity)
        }

        override fun buyRefill() {
            RefillBilling.buy(this@MainActivity) { result ->
                val msg = when (result) {
                    RefillBilling.Result.REFILLED -> "오늘 무료 해제가 ${UnlockRules.FREE_PER_DAY}회로 초기화됐어요"
                    RefillBilling.Result.PENDING -> "결제가 처리되는 중이에요. 끝나면 무료 해제가 초기화돼요"
                    RefillBilling.Result.UNAVAILABLE -> "지금은 결제를 할 수 없어요. 플레이스토어에서 받은 접다에서만 결제돼요"
                    RefillBilling.Result.ERROR -> "결제를 마치지 못했어요. 다시 시도해 주세요"
                    RefillBilling.Result.CANCELLED, RefillBilling.Result.THEME_OWNED -> null
                }
                if (result == RefillBilling.Result.REFILLED) freeTick++
                msg?.let { Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show() }
            }
        }

        override fun isNotificationBlockOn(): Boolean = notifBlockOn

        override fun isAppGuardOn(): Boolean = appGuardOn

        override fun playUnlockAds(onProgress: (Int) -> Unit, onResult: (UnlockAds.Result) -> Unit) =
            UnlockAds.play(this@MainActivity, onProgress, onResult)

        override fun unlock(minutes: Int, reason: String, method: UnlockMethod) {
            SmartSession.start(this@MainActivity, minutes, reason, method)
            screen = Screen.Home
            enterSmart()
        }
    }

    companion object {
        const val EXTRA_RETURN_TO_FOLDER = "return_to_folder"
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package"
        private const val SAMSUNG_HOME = "com.sec.android.app.launcher"

        /** 한국 긴급·신고 번호 (기기 판별이 실패해도 막히지 않게) */
        private val KR_EMERGENCY = setOf("112", "119", "113", "117", "118", "122", "125", "111", "911")
    }
}
