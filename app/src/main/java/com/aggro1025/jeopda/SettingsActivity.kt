package com.aggro1025.jeopda

import android.Manifest
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import com.aggro1025.jeopda.apps.Apps
import com.aggro1025.jeopda.apps.BuiltIn
import com.aggro1025.jeopda.data.AppPrefs
import com.aggro1025.jeopda.session.SmartSession
import com.aggro1025.jeopda.ui.JeopdaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 설정·상태 화면. 앱 서랍의 "접다" 아이콘과 폴더폰 홈의 오른쪽 소프트키로 연다.
 * 메뉴 앱 편집은 최초 설정 중이거나 스마트폰 모드일 때만 가능하다 (요구사항 5장).
 */
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { JeopdaTheme { SettingsScreen(onStartFolder = ::startFolderMode, onReturn = ::returnToFolder) } }
    }

    private fun startFolderMode() {
        AppPrefs(this).setupDone = true
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

    val setupDone = remember(tick) { prefs.setupDone }
    val smart = remember(tick) { SmartSession.isSmart(context) }
    val editable = !setupDone || smart
    val isHome = remember(tick) { Apps.isDefaultHome(context) }
    val notifGranted = remember(tick) {
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    }
    val exactOk = remember(tick) { SmartSession.canScheduleExact(context) }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { tick++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    var pickSlot by remember { mutableStateOf<Int?>(null) }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("접다 설정", fontSize = 24.sp)

            // ---- 상태 ----
            Section("지금 상태")
            when {
                !setupDone -> Body("아직 폴더폰 모드를 켜지 않았어요. 아래 설정을 마치고 시작하세요.")
                smart -> {
                    Body("스마트폰 모드 · ${SmartSession.formatTime(SmartSession.endAt(context))}에 끝나요")
                    Button(onClick = onReturn) { Text("폴더폰으로 돌아가기") }
                    Hint("지금 돌아가면 남은 시간은 사라져요")
                }
                else -> Body("폴더폰 모드")
            }

            // ---- 권한 ----
            Section("필요한 설정")
            PermissionRow(
                title = "기본 홈 앱",
                desc = "접다를 홈 앱으로 지정하면 폴더폰 화면이 켜져요",
                ok = isHome,
            ) { requestHomeRole(context, roleLauncher::launch) }
            if (Build.VERSION.SDK_INT >= 33) {
                PermissionRow(
                    title = "알림 표시",
                    desc = "스마트폰 모드 남은 시간과 종료 5분 전에 알려드려요",
                    ok = notifGranted,
                ) { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }
            }
            if (Build.VERSION.SDK_INT >= 31) {
                PermissionRow(
                    title = "알람 및 리마인더",
                    desc = "종료 5분 전 알림을 정확한 시각에 보내요. 꺼져 있으면 몇 분 늦을 수 있어요",
                    ok = exactOk,
                ) {
                    context.startSafely(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")),
                    )
                }
            }

            // ---- 스마트폰 모드 홈 ----
            Section("스마트폰 모드에서 쓸 홈 화면")
            val homes = remember(tick) { Apps.otherHomeApps(context) }
            if (homes.isEmpty()) {
                Body("다른 홈 앱이 없어서, 스마트폰 모드에서는 접다의 단순한 앱 목록을 보여줘요.")
            } else {
                var selected by remember(tick) { mutableStateOf(prefs.smartHomeComponent ?: homes.first().first.flattenToString()) }
                homes.forEach { (comp, label) ->
                    val key = comp.flattenToString()
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { selected = key; prefs.smartHomeComponent = key },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == key, onClick = { selected = key; prefs.smartHomeComponent = key })
                        Text(label)
                    }
                }
                Hint("스마트폰 모드에서는 이 홈 화면이 그대로 떠요. 시간이 끝나면 홈 버튼에서 폴더폰 화면이 나와요.")
            }

            // ---- 메뉴 ----
            Section("폴더폰 메뉴")
            BuiltIn.entries.forEach { Body("${it.slot}  ${it.label}  (기본 앱)") }
            AppPrefs.USER_SLOTS.forEach { slot ->
                val pkg = remember(tick, pickSlot) { prefs.slotPackage(slot) }
                val label = remember(pkg) { pkg?.let { Apps.entry(context, it)?.label } }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("$slot  ${label ?: "비어 있음"}", Modifier.weight(1f))
                    if (editable) {
                        TextButton(onClick = { pickSlot = slot }) { Text("변경") }
                        if (pkg != null) TextButton(onClick = { prefs.setSlotPackage(slot, null); tick++ }) { Text("비우기") }
                    }
                }
            }
            if (!editable) Hint("폴더폰 모드에서는 보기만 할 수 있어요. 바꾸려면 먼저 해제하세요.")

            // ---- 시작 ----
            if (!setupDone) {
                HorizontalDivider()
                Button(onClick = onStartFolder, enabled = isHome, modifier = Modifier.fillMaxWidth()) {
                    Text("폴더폰 모드 시작")
                }
                if (!isHome) Hint("먼저 접다를 기본 홈 앱으로 지정해 주세요")
            }

            Spacer(Modifier.height(20.dp))
            Hint("접다 ${BuildConfig.VERSION_NAME} · 폰트: Galmuri (© Lee Minseo, SIL Open Font License 1.1)")
        }
    }

    pickSlot?.let { slot ->
        AppPickerDialog(
            onDismiss = { pickSlot = null },
            onPick = { pkg ->
                prefs.setSlotPackage(slot, pkg)
                pickSlot = null
                tick++
            },
        )
    }
}

private fun requestHomeRole(context: android.content.Context, launch: (Intent) -> Unit) {
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

private fun android.content.Context.startSafely(intent: Intent) {
    try {
        startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (e: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

@Composable
private fun AppPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    var apps by remember { mutableStateOf<List<AppEntry>?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { apps = withContext(Dispatchers.IO) { Apps.launchableApps(context) } }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
        title = { Text("메뉴에 넣을 앱") },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    placeholder = { Text("검색") },
                    modifier = Modifier.fillMaxWidth(),
                )
                val list = apps
                if (list == null) {
                    Text("불러오는 중…", Modifier.padding(16.dp))
                } else {
                    val filtered = list.filter { query.isBlank() || it.label.contains(query, ignoreCase = true) }
                    LazyColumn(Modifier.heightIn(max = 420.dp).padding(top = 8.dp)) {
                        items(filtered, key = { it.packageName }) { app ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { onPick(app.packageName) }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                app.icon?.let {
                                    val bmp = remember(app.packageName) { it.toBitmap(96, 96).asImageBitmap() }
                                    Image(bmp, null, Modifier.size(32.dp))
                                }
                                Text(app.label, Modifier.padding(start = 12.dp))
                            }
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun PermissionRow(title: String, desc: String, ok: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 16.sp)
            Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        if (ok) Text("✓ 완료", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp))
        else OutlinedButton(onClick = onClick, modifier = Modifier.padding(start = 8.dp)) { Text("설정") }
    }
}

@Composable
private fun Section(text: String) {
    Spacer(Modifier.height(6.dp))
    Text(text, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun Body(text: String) = Text(text, fontSize = 15.sp)

@Composable
private fun Hint(text: String) =
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
