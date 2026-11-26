package com.aggro1025.jeopda

import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.aggro1025.jeopda.apps.Apps
import com.aggro1025.jeopda.apps.BuiltIn
import com.aggro1025.jeopda.data.AppPrefs
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
import com.aggro1025.jeopda.ui.smart.SmartDrawer
import com.aggro1025.jeopda.ui.smart.SetupPending

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
    private val relayTimes = ArrayDeque<Long>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleReturnExtra(intent)

        // 뒤로: 하위 화면이면 홈으로, 홈이면 아무것도 안 함 (런처는 닫히지 않는다)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mode == Mode.FOLDER && screen != Screen.Home) {
                    screen = if (screen is Screen.UnlockInput) Screen.UnlockMethod() else Screen.Home
                }
            }
        })

        setContent {
            JeopdaTheme {
                when (mode) {
                    Mode.FOLDER -> FolderApp(
                        screen = screen,
                        onScreen = { screen = it },
                        menu = menu,
                        actions = actions,
                        router = router,
                        devUnlock = BuildConfig.DEBUG,
                    )
                    Mode.SETUP -> SetupPending(onOpen = { actions.openSettings() })
                    Mode.RELAY -> Box(Modifier.fillMaxSize().background(Lcd.body))
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
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
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
        val builtIns = BuiltIn.entries.map { MenuEntry(it.slot, it.label) }
        val user = AppPrefs.USER_SLOTS.map { slot ->
            val app = prefs.slotPackage(slot)?.let { Apps.entry(this, it) }
            if (app != null) MenuEntry(slot, app.label) else MenuEntry(slot, "비어 있음", empty = true)
        }
        return builtIns + user
    }

    private val actions = object : FolderActions {
        override fun openMenuSlot(slot: Int) {
            val builtIn = BuiltIn.entries.firstOrNull { it.slot == slot }
            if (builtIn != null) {
                Apps.launchBuiltIn(this@MainActivity, builtIn)
                return
            }
            val pkg = prefs.slotPackage(slot)
            if (pkg == null || Apps.entry(this@MainActivity, pkg) == null) {
                Toast.makeText(this@MainActivity, "${slot}번은 비어 있어요. 스마트폰 모드의 설정에서 정할 수 있어요", Toast.LENGTH_SHORT).show()
            } else {
                Apps.launchPackage(this@MainActivity, pkg)
            }
        }

        override fun dial(number: String) = Apps.dial(this@MainActivity, number)

        override fun openSettings() {
            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
        }

        override fun isFreeAvailable(): Boolean = SmartSession.isFreeAvailableToday(this@MainActivity)

        override fun unlock(minutes: Int, reason: String, method: UnlockMethod) {
            SmartSession.start(this@MainActivity, minutes, reason, method)
            screen = Screen.Home
            enterSmart()
        }
    }

    companion object {
        const val EXTRA_RETURN_TO_FOLDER = "return_to_folder"
        private const val SAMSUNG_HOME = "com.sec.android.app.launcher"
    }
}
