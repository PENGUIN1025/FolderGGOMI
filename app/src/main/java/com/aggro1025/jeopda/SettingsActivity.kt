package com.aggro1025.jeopda

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.aggro1025.jeopda.apps.AppEntry
import com.aggro1025.jeopda.apps.AppGuard
import com.aggro1025.jeopda.apps.AppPolicy
import com.aggro1025.jeopda.apps.Apps
import com.aggro1025.jeopda.apps.BuiltIn
import com.aggro1025.jeopda.data.AppPrefs
import com.aggro1025.jeopda.session.NotificationBlocker
import com.aggro1025.jeopda.session.SmartSession
import com.aggro1025.jeopda.ui.JeopdaTheme
import com.aggro1025.jeopda.ui.settings.HistoryScreen
import com.aggro1025.jeopda.ui.settings.NotifApp
import com.aggro1025.jeopda.ui.settings.SettingsCallbacks
import com.aggro1025.jeopda.ui.settings.SettingsDesign
import com.aggro1025.jeopda.ui.settings.SettingsUi
import com.aggro1025.jeopda.ui.settings.SetupStep
import com.aggro1025.jeopda.ui.settings.SlotItem
import com.aggro1025.jeopda.ui.settings.StepId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 설정·상태 화면. 앱 서랍의 "접다" 아이콘과 폴더폰 홈의 오른쪽 소프트키로 연다.
 * 최초 실행 때는 단계별 권한 안내 화면 역할도 한다 (요구사항 3장 "권한 안내").
 * 메뉴 앱·알림 허용 앱 편집은 최초 설정 중이거나 스마트폰 모드일 때만 가능하다 (요구사항 5장).
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JeopdaTheme { SettingsScreen(onStartFolder = ::startFolderMode, onReturn = ::returnToFolder) } }
    }

    private fun startFolderMode() {
        AppPrefs(this).setupDone = true
        NotificationBlocker.sweepNow()
        goHome()
    }

    private fun returnToFolder() {
        SmartSession.end(this)
        goHome()
    }

    private fun goHome() {
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }
}

const val PRIVACY_URL = "https://penguin1025.github.io/jeopda-privacy/"

private enum class PickerMode { MENU, NOTIF }

@Composable
private fun SettingsScreen(onStartFolder: () -> Unit, onReturn: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { AppPrefs(context) }

    // 다른 화면(권한 설정 등)에서 돌아올 때마다 상태를 다시 읽는다
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            tick++
        }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    var picker by remember { mutableStateOf<Pair<PickerMode, Int>?>(null) }
    var showLicenses by remember { mutableStateOf(false) }
    var showHomeChoice by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val ui = remember(tick) { buildUi(context, prefs) }

    val cb = object : SettingsCallbacks {
        override fun onStep(id: StepId) {
            when (id) {
                StepId.HOME -> requestHomeRole(context, roleLauncher::launch)
                StepId.NOTIF_ACCESS -> openNotificationAccess(context)
                StepId.USAGE_ACCESS -> context.startSafely(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, Uri.parse("package:${context.packageName}")),
                    fallback = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS),
                )
                StepId.OVERLAY -> context.startSafely(
                    Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")),
                )
                StepId.POST_NOTIF -> if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                StepId.EXACT_ALARM -> if (Build.VERSION.SDK_INT >= 31) {
                    context.startSafely(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                }
                StepId.CONTACTS -> permLauncher.launch(Manifest.permission.READ_CONTACTS)
                StepId.CALL -> permLauncher.launch(Manifest.permission.CALL_PHONE)
            }
        }

        override fun onStart() = onStartFolder()
        override fun onReturnToFolder() = onReturn()
        override fun onSlot(slot: Int) {
            picker = PickerMode.MENU to slot
        }

        override fun onClearSlot(slot: Int) {
            prefs.setSlotPackage(slot, null)
            tick++
        }

        override fun onAddNotifApp() {
            picker = PickerMode.NOTIF to 0
        }

        override fun onRemoveNotifApp(pkg: String) {
            prefs.allowedNotifApps = prefs.allowedNotifApps - pkg
            tick++
        }

        override fun onSmartHome() {
            showHomeChoice = true
        }

        override fun onPrivacy() = context.startSafely(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL)))
        override fun onLicenses() {
            showLicenses = true
        }

        override fun onHistory() {
            showHistory = true
        }

        override fun onToggleSmartHomeFresh() {
            prefs.smartHomeFresh = !prefs.smartHomeFresh
            tick++
        }
    }

    if (showHistory) {
        BackHandler { showHistory = false }
        HistoryScreen(prefs.reasons(), System.currentTimeMillis(), onBack = { showHistory = false })
        return
    }
    SettingsDesign(ui, cb)

    picker?.let { (mode, slot) ->
        AppPickerDialog(
            mode = mode,
            exclude = if (mode == PickerMode.NOTIF) prefs.allowedNotifApps + context.packageName else setOf(context.packageName),
            onDismiss = { picker = null },
            onPick = { pkg ->
                if (mode == PickerMode.MENU) {
                    prefs.setSlotPackage(slot, pkg)
                } else if (prefs.allowedNotifApps.size < AppPrefs.MAX_ALLOWED_NOTIF_APPS) {
                    prefs.allowedNotifApps = prefs.allowedNotifApps + pkg
                }
                picker = null
                tick++
            },
        )
    }
    if (showLicenses) LicensesDialog(onDismiss = { showLicenses = false })
    if (showHomeChoice) {
        HomeChoiceDialog(
            current = prefs.smartHomeComponent,
            onDismiss = { showHomeChoice = false },
            onPick = {
                prefs.smartHomeComponent = it
                showHomeChoice = false
                tick++
            },
        )
    }
}

private fun granted(context: Context, perm: String) =
    ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED

private fun buildUi(context: Context, prefs: AppPrefs): SettingsUi {
    val setupDone = prefs.setupDone
    val smart = SmartSession.isSmart(context)
    val isHome = Apps.isDefaultHome(context)
    val steps = buildList {
        add(SetupStep(StepId.HOME, "기본 홈 앱", "접다를 홈 앱으로 지정하면 폴더폰 화면이 켜져요", isHome, required = true))
        add(
            SetupStep(
                StepId.NOTIF_ACCESS, "알림 접근",
                "폴더폰 모드에서 필요한 알림만 남겨요. 알림 내용은 저장하거나 보내지 않아요",
                NotificationBlocker.isEnabled(context), required = true,
            ),
        )
        add(
            SetupStep(
                StepId.USAGE_ACCESS, "사용 기록 접근",
                "폴더폰 모드에서 최근 앱 화면 등으로 다른 앱을 열면 막아요. 지금 맨 앞 앱만 확인하고 기록은 저장하지 않아요",
                AppGuard.hasUsageAccess(context), required = true,
            ),
        )
        add(
            SetupStep(
                StepId.OVERLAY, "다른 앱 위에 표시",
                "막힌 앱 대신 폴더폰 화면을 띄우는 데 필요해요",
                AppGuard.canDrawOverlays(context), required = true,
            ),
        )
        if (Build.VERSION.SDK_INT >= 33) {
            add(
                SetupStep(
                    StepId.POST_NOTIF, "알림 표시", "스마트폰 모드 남은 시간과 종료 30분 전·10분 전에 알려요",
                    granted(context, Manifest.permission.POST_NOTIFICATIONS), required = false,
                ),
            )
        }
        if (Build.VERSION.SDK_INT >= 31) {
            add(
                SetupStep(
                    StepId.EXACT_ALARM, "알람 및 리마인더", "종료 전 알림을 정확한 시각에 보내요",
                    SmartSession.canScheduleExact(context), required = false,
                ),
            )
        }
        add(
            SetupStep(
                StepId.CONTACTS, "연락처", "폴더폰 화면에 연락처를 보여줘요. 기기 밖으로 보내지 않아요",
                granted(context, Manifest.permission.READ_CONTACTS), required = false,
            ),
        )
        add(
            SetupStep(
                StepId.CALL, "전화 걸기", "번호를 누르면 바로 전화를 걸어요",
                granted(context, Manifest.permission.CALL_PHONE), required = false,
            ),
        )
    }
    fun icon(pkg: String?) = pkg?.let { Apps.entry(context, it)?.icon }?.toBitmap(108, 108)?.asImageBitmap()
    val slots = BuiltIn.entries.map { SlotItem(it.slot, it.label, icon(Apps.builtInPackage(context, it))) } +
        AppPrefs.USER_SLOTS.map { slot ->
            val app = prefs.slotPackage(slot)?.let { Apps.entry(context, it) }
            if (app != null) SlotItem(slot, app.label, icon(app.packageName)) else SlotItem(slot, "비어 있음", empty = true)
        }
    val homes = Apps.otherHomeApps(context)
    val homeLabel = homes.firstOrNull { it.first.flattenToString() == prefs.smartHomeComponent }?.second
        ?: homes.firstOrNull()?.second ?: "접다 앱 목록"
    return SettingsUi(
        version = BuildConfig.VERSION_NAME,
        modeText = when {
            !setupDone -> "처음 설정 중"
            smart -> "스마트폰 모드"
            else -> "폴더폰 모드"
        },
        setupDone = setupDone,
        smartEndText = if (smart) "${SmartSession.formatTime(SmartSession.endAt(context))}에 끝나요" else null,
        homeLost = setupDone && !isHome,
        steps = steps,
        slots = slots,
        allowedNotifApps = prefs.allowedNotifApps.map { NotifApp(it, Apps.entry(context, it)?.label ?: it) },
        editable = !setupDone || smart,
        smartHomeLabel = homeLabel,
        smartHomeFresh = prefs.smartHomeFresh,
    )
}

private fun requestHomeRole(context: Context, launch: (Intent) -> Unit) {
    if (Build.VERSION.SDK_INT >= 29) {
        val rm = context.getSystemService(RoleManager::class.java)
        if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME) && !rm.isRoleHeld(RoleManager.ROLE_HOME)) {
            try {
                launch(rm.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            } catch (e: ActivityNotFoundException) {
                // 아래 설정 화면으로
            }
        }
    }
    context.startSafely(Intent(Settings.ACTION_HOME_SETTINGS))
}

/** 알림 접근 설정: 가능하면 접다 항목 화면으로 바로 */
private fun openNotificationAccess(context: Context) {
    if (Build.VERSION.SDK_INT >= 30) {
        val detail = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
            .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, NotificationBlocker.component(context).flattenToString())
        try {
            context.startActivity(detail.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (e: ActivityNotFoundException) {
            // 목록 화면으로
        }
    }
    context.startSafely(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
}

private fun Context.startSafely(intent: Intent, fallback: Intent = Intent(Settings.ACTION_SETTINGS)) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        try {
            startActivity(fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e2: ActivityNotFoundException) {
            startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}

private data class PickerRow(val app: AppEntry, val blockReason: String?)

@Composable
private fun AppPickerDialog(mode: PickerMode, exclude: Set<String>, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var rows by remember { mutableStateOf<List<PickerRow>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        rows = withContext(Dispatchers.IO) {
            val browsers = AppPolicy.browserPackages(context)
            Apps.launchableApps(context)
                .filter { it.packageName !in exclude }
                .map { app ->
                    val reason = if (mode == PickerMode.MENU) AppPolicy.menuBlockReason(context, app.packageName, browsers) else null
                    PickerRow(app, reason)
                }
                // 넣을 수 있는 앱을 위로
                .sortedBy { it.blockReason != null }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        title = { Text(if (mode == PickerMode.MENU) "메뉴에 넣을 앱" else "알림을 받을 앱") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("검색") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = rows
                if (list == null) {
                    Text("불러오는 중…", Modifier.padding(16.dp))
                } else {
                    val filtered = list.filter { query.isBlank() || it.app.label.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.app.packageName }) { row ->
                            val blocked = row.blockReason != null
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (blocked) {
                                            Toast.makeText(context, AppPolicy.BLOCKED_MESSAGE, Toast.LENGTH_LONG).show()
                                        } else {
                                            onPick(row.app.packageName)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                row.app.icon?.let {
                                    val bmp = remember(row.app.packageName) { it.toBitmap(96, 96).asImageBitmap() }
                                    Image(bmp, null, Modifier.size(32.dp), alpha = if (blocked) 0.35f else 1f)
                                }
                                Column(Modifier.padding(start = 12.dp)) {
                                    val dim = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                    Text(row.app.label, color = if (blocked) dim else MaterialTheme.colorScheme.onSurface)
                                    if (blocked) Text("넣을 수 없음 · ${row.blockReason}", fontSize = 12.sp, color = dim)
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun HomeChoiceDialog(current: String?, onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    val homes = remember { Apps.otherHomeApps(context) }
    val selected = current ?: homes.firstOrNull()?.first?.flattenToString()
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        title = { Text("스마트폰 모드에서 쓸 홈 화면") },
        text = {
            Column {
                if (homes.isEmpty()) Text("다른 홈 앱이 없어서, 스마트폰 모드에서는 접다의 단순한 앱 목록을 보여줘요.")
                homes.forEach { (comp, label) ->
                    val key = comp.flattenToString()
                    Row(
                        Modifier.fillMaxWidth().clickable { onPick(key) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == key, onClick = { onPick(key) })
                        Text(label)
                    }
                }
            }
        },
    )
}

/** 앱에 포함된 오픈소스 라이브러리와 라이선스 고지 */
private val OPEN_SOURCE = listOf(
    "Android Jetpack (AndroidX: Core, Activity, Lifecycle, Compose UI·Foundation·Material3)" to "Apache License 2.0",
    "Material Icons (Compose material-icons-core, 아이콘 모양 일부 포함)" to "Apache License 2.0",
    "Kotlin 표준 라이브러리, kotlinx.coroutines" to "Apache License 2.0",
    "Google Mobile Ads SDK, User Messaging Platform (광고·개인정보 동의)" to
        "Google 독점 소프트웨어 · Google API 서비스 약관과 AdMob 정책 적용 (일부 오픈소스 부품은 각 라이선스를 따름)",
)

@Composable
private fun LicensesDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val apache = remember {
        runCatching { context.assets.open("apache-2.0.txt").bufferedReader().use { it.readText() } }.getOrDefault("")
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        title = { Text("오픈소스 라이선스") },
        text = {
            Column(Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())) {
                OPEN_SOURCE.forEach { (name, license) ->
                    Text(name, fontSize = 14.sp)
                    Text(license, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(8.dp))
                }
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Text(apache, fontSize = 11.sp)
            }
        },
    )
}
