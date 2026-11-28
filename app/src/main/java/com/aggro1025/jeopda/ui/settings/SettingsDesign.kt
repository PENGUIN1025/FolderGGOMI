package com.aggro1025.jeopda.ui.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.R
import com.aggro1025.jeopda.ui.Lcd
import com.aggro1025.jeopda.ui.Scr

/** 설정 화면 (폴더폰 화면과 같은 남색 스타일) */

enum class StepId { HOME, NOTIF_ACCESS, USAGE_ACCESS, OVERLAY, POST_NOTIF, EXACT_ALARM, CONTACTS, CALL }

data class SetupStep(val id: StepId, val title: String, val desc: String, val done: Boolean, val required: Boolean)
data class SlotItem(val slot: Int, val label: String, val icon: ImageBitmap? = null, val empty: Boolean = false)
data class NotifApp(val pkg: String, val label: String)

data class SettingsUi(
    val version: String,
    val modeText: String,
    val setupDone: Boolean,
    /** 스마트폰 모드면 "오후 3:20에 끝나요" 같은 문구, 아니면 null */
    val smartEndText: String?,
    /** 최초 설정 뒤 접다가 기본 홈 앱이 아니게 된 경우 */
    val homeLost: Boolean,
    val steps: List<SetupStep>,
    val slots: List<SlotItem>,
    val allowedNotifApps: List<NotifApp>,
    val editable: Boolean,
    val smartHomeLabel: String,
    /** 홈 버튼 누르면 원래 홈 화면을 처음 상태로 새로 열기 */
    val smartHomeFresh: Boolean = true,
)

interface SettingsCallbacks {
    fun onStep(id: StepId)
    fun onStart()
    fun onReturnToFolder()
    fun onSlot(slot: Int)
    fun onClearSlot(slot: Int)
    fun onAddNotifApp()
    fun onRemoveNotifApp(pkg: String)
    fun onSmartHome()
    fun onPrivacy()
    fun onLicenses()
    fun onHistory()
    fun onToggleSmartHomeFresh()
}

object NoCallbacks : SettingsCallbacks {
    override fun onStep(id: StepId) = Unit
    override fun onStart() = Unit
    override fun onReturnToFolder() = Unit
    override fun onSlot(slot: Int) = Unit
    override fun onClearSlot(slot: Int) = Unit
    override fun onAddNotifApp() = Unit
    override fun onRemoveNotifApp(pkg: String) = Unit
    override fun onSmartHome() = Unit
    override fun onPrivacy() = Unit
    override fun onLicenses() = Unit
    override fun onHistory() = Unit
    override fun onToggleSmartHomeFresh() = Unit
}

private fun t(size: TextUnit, color: Color = Scr.text, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size, color = color, fontWeight = weight)

private val Card = Color(0x1FFFFFFF)
private val CardLine = Color(0x26FFFFFF)

/**
 * 설정 화면. 두 페이지로 나눈다.
 * - 0: 권한 (정보 안내 + 권한 체크리스트)
 * - 1: 앱 설정 (메뉴, 알림 받을 앱, 기타)
 * 처음 설정 때는 1/2 → 2/2 단계로 넘어가고, 그 뒤에는 위쪽 탭으로 오간다.
 */
@Composable
fun SettingsDesign(
    ui: SettingsUi,
    cb: SettingsCallbacks = NoCallbacks,
    page: Int = 0,
    onPage: (Int) -> Unit = {},
) {
    val requiredDone = ui.steps.filter { it.required }.all { it.done }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Scr.wallTop, Scr.wallBottom))),
    ) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            // 머리말 (고정)
            Column(
                Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(androidx.compose.ui.graphics.Color.White),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painterResource(R.drawable.ic_launcher_foreground),
                            null,
                            Modifier.size(44.dp).graphicsLayer(scaleX = 1.35f, scaleY = 1.35f),
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("접다 설정", style = t(22.sp, weight = FontWeight.Bold))
                        Text("버전 ${ui.version} · ${ui.modeText}", style = t(13.sp, Scr.dim))
                    }
                }
                if (ui.homeLost) {
                    Banner(
                        "접다가 기본 홈 앱이 아니에요",
                        "지금은 폴더폰 화면이 뜨지 않아요. 눌러서 다시 홈 앱으로 지정하세요",
                    ) { cb.onStep(StepId.HOME) }
                }
                if (ui.smartEndText != null) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("스마트폰 모드", style = t(15.sp, weight = FontWeight.Bold))
                            Text(ui.smartEndText, style = t(13.sp, Scr.dim))
                        }
                        Text(
                            "폴더폰으로",
                            style = t(14.sp, weight = FontWeight.Bold),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .background(Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep)))
                                .clickable(onClick = cb::onReturnToFolder)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
                if (ui.setupDone) {
                    Tabs(page, onPage)
                } else {
                    StepHeader(page)
                }
            }

            // 페이지 내용 (이 부분만 스크롤)
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (page == 0) PermissionPage(ui, cb) else AppSettingsPage(ui, cb)
                Spacer(Modifier.height(4.dp))
            }

            // 처음 설정 때의 다음·이전 버튼 (고정)
            if (!ui.setupDone) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    if (page == 0) {
                        PrimaryButton(
                            if (requiredDone) "다음: 앱 설정" else "필수 권한을 먼저 켜 주세요",
                            enabled = requiredDone,
                        ) { onPage(1) }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(Modifier.weight(0.4f)) { SecondaryButton("이전") { onPage(0) } }
                            Box(Modifier.weight(0.6f)) { PrimaryButton("폴더폰 모드 시작", enabled = requiredDone, onClick = cb::onStart) }
                        }
                    }
                }
            }
        }
    }
}

/** 처음 설정: 1/2 권한 → 2/2 앱 설정 */
@Composable
private fun StepHeader(page: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        listOf("권한 설정", "앱 설정").forEachIndexed { i, label ->
            val on = i == page
            Box(
                Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (on) Scr.accent else Color(0x33FFFFFF)),
                contentAlignment = Alignment.Center,
            ) { Text("${i + 1}", style = t(13.sp, weight = FontWeight.Bold)) }
            Spacer(Modifier.width(6.dp))
            Text(label, style = t(14.sp, if (on) Scr.text else Scr.dim, if (on) FontWeight.Bold else FontWeight.Normal))
            if (i == 0) {
                Box(Modifier.padding(horizontal = 10.dp).width(24.dp).height(1.dp).background(Color(0x40FFFFFF)))
            }
        }
    }
}

/** 설정이 끝난 뒤: 권한 / 앱 설정 탭 */
@Composable
private fun Tabs(page: Int, onPage: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33000000))
            .padding(4.dp),
    ) {
        listOf("권한", "앱 설정").forEachIndexed { i, label ->
            val on = i == page
            val base = Modifier.weight(1f).clip(RoundedCornerShape(9.dp))
            Box(
                (if (on) base.background(Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))) else base)
                    .clickable { onPage(i) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) { Text(label, style = t(15.sp, if (on) Scr.text else Scr.dim, FontWeight.Bold)) }
        }
    }
}

@Composable
private fun PermissionPage(ui: SettingsUi, cb: SettingsCallbacks) {
    // 안심 문구
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x335AB0FF))
            .border(1.dp, Color(0x665AB0FF), RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(Icons.Filled.Lock, null, tint = Scr.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column {
            Text("연락처·알림·사용 기록은 폰 밖으로 나가지 않아요", style = t(15.sp, weight = FontWeight.Bold))
            Spacer(Modifier.height(2.dp))
            Text(
                "접다가 요청하는 권한은 이 폰을 폴더폰처럼 쓰기 위한 것뿐이에요. " +
                    "연락처, 앱 목록, 알림, 사용 기록, 해제 사유는 폰 안에서만 쓰고 저장하거나 보내지 않아요.",
                style = t(13.sp, Scr.text.copy(alpha = 0.9f)),
            )
        }
    }
    val doneCount = ui.steps.count { it.done }
    Section("폴더폰 준비 ${doneCount}/${ui.steps.size}")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card)) {
        ui.steps.forEachIndexed { i, s ->
            StepRow(i + 1, s) { cb.onStep(s.id) }
            if (i < ui.steps.lastIndex) Divider()
        }
    }
}

@Composable
private fun AppSettingsPage(ui: SettingsUi, cb: SettingsCallbacks) {
    Section("폴더폰 메뉴")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ui.slots.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    SlotCell(item, ui.editable, Modifier.weight(1f), onClick = { cb.onSlot(item.slot) }, onLong = { cb.onClearSlot(item.slot) })
                }
            }
        }
        Text(
            if (ui.editable) "6~9번을 눌러 앱을 고르고, 길게 눌러 비워요. 게임·소셜·영상 앱과 브라우저는 넣을 수 없어요"
            else "폴더폰 모드에서는 보기만 할 수 있어요. 바꾸려면 먼저 해제하세요",
            style = t(12.sp, Scr.dim),
        )
    }

    Section("알림을 받을 앱 ${ui.allowedNotifApps.size}/3")
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("전화, 문자, 알람, 재난문자는 항상 와요. 그 밖의 앱 알림은 폴더폰 모드에서 지워져요", style = t(13.sp, Scr.dim))
        ui.allowedNotifApps.forEach { app ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("· ${app.label}", style = t(15.sp), modifier = Modifier.weight(1f))
                if (ui.editable) {
                    Text(
                        "빼기",
                        style = t(13.sp, Scr.dim),
                        modifier = Modifier.clickable { cb.onRemoveNotifApp(app.pkg) }.padding(6.dp),
                    )
                }
            }
        }
        if (ui.allowedNotifApps.size < 3 && ui.editable) {
            Text(
                "+ 앱 추가",
                style = t(15.sp, Scr.accent, FontWeight.Bold),
                modifier = Modifier.clickable(onClick = cb::onAddNotifApp).padding(vertical = 4.dp),
            )
        }
        if (!ui.editable) Text("폴더폰 모드에서는 보기만 할 수 있어요", style = t(12.sp, Scr.dim))
    }

    Section("기타")
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card)) {
        LinkRow("해제 기록", "사유·사용 시간", cb::onHistory)
        Divider()
        LinkRow("스마트폰 모드에서 쓸 홈 화면", ui.smartHomeLabel, cb::onSmartHome)
        Divider()
        LinkRow(
            "홈 버튼 누르면 원래 홈 첫 화면으로",
            if (ui.smartHomeFresh) "켜짐" else "꺼짐",
            cb::onToggleSmartHomeFresh,
        )
        Divider()
        LinkRow("개인정보처리방침", "", cb::onPrivacy)
        Divider()
        LinkRow("오픈소스 라이선스", "", cb::onLicenses)
    }
}

@Composable
private fun SecondaryButton(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x33FFFFFF))
            .clickable(onClick = onClick)
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = t(16.sp, weight = FontWeight.Bold)) }
}

@Composable
private fun Section(text: String) =
    Text(text, style = t(14.sp, Scr.accent, FontWeight.Bold), modifier = Modifier.padding(start = 4.dp, top = 4.dp))

@Composable
private fun Divider() = Box(Modifier.fillMaxWidth().padding(horizontal = 14.dp).height(1.dp).background(CardLine))

@Composable
private fun StepRow(n: Int, s: SetupStep, onClick: () -> Unit) {
    val row = Modifier.fillMaxWidth()
    Row(
        (if (s.done) row else row.clickable(onClick = onClick)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(if (s.done) Color(0xFF2E9E5B) else Color(0x33FFFFFF)),
            contentAlignment = Alignment.Center,
        ) {
            if (s.done) {
                Icon(Icons.Filled.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text("$n", style = t(13.sp, weight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(s.title, style = t(15.sp, weight = FontWeight.Bold))
                Spacer(Modifier.width(6.dp))
                Text(if (s.required) "필수" else "선택", style = t(11.sp, if (s.required) Scr.warn else Scr.dim))
            }
            Text(s.desc, style = t(12.sp, Scr.dim))
        }
        Spacer(Modifier.width(8.dp))
        if (s.done) {
            Text("완료", style = t(13.sp, Color(0xFF7CE38B), FontWeight.Bold))
        } else {
            Text(
                "켜기",
                style = t(13.sp, weight = FontWeight.Bold),
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep)))
                    .padding(horizontal = 14.dp, vertical = 6.dp),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SlotCell(item: SlotItem, editable: Boolean, modifier: Modifier, onClick: () -> Unit, onLong: () -> Unit) {
    val userSlot = item.slot >= 6
    val base = modifier
        .clip(RoundedCornerShape(10.dp))
        .background(if (userSlot && editable) Color(0x1A5AB0FF) else Color.Transparent)
    Column(
        (if (userSlot && editable) base.combinedClickable(onClick = onClick, onLongClick = onLong) else base)
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
            when {
                item.icon != null -> Image(item.icon, null, Modifier.size(36.dp))
                item.empty -> Box(
                    Modifier.size(36.dp).border(1.5.dp, Scr.faint, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) { Text("+", style = t(18.sp, Scr.faint)) }
                else -> Box(
                    Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0x40FFFFFF)),
                    contentAlignment = Alignment.Center,
                ) { Text(item.label.take(1), style = t(15.sp, weight = FontWeight.Bold)) }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "${item.slot} ${item.label}",
            style = t(12.sp, if (item.empty) Scr.faint else Scr.text),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun LinkRow(title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = t(15.sp), modifier = Modifier.weight(1f))
        if (value.isNotEmpty()) Text(value, style = t(13.sp, Scr.dim))
        Text("  ›", style = t(18.sp, Scr.dim))
    }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
    Box(
        (if (enabled) base.clickable(onClick = onClick) else base)
            .background(
                if (enabled) Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))
                else Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x33FFFFFF))),
            )
            .padding(vertical = 15.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = t(16.sp, if (enabled) Scr.text else Scr.dim, FontWeight.Bold)) }
}

@Composable
private fun Banner(title: String, desc: String, onClick: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x40FFC98A))
            .border(1.dp, Scr.warn, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Text(title, style = t(15.sp, Scr.warn, FontWeight.Bold))
        Text(desc, style = t(13.sp))
    }
}

// ---------------- 미리보기용 예시 데이터 ----------------

internal val previewSettingsFirstRun = SettingsUi(
    version = "0.4.0",
    modeText = "처음 설정 중",
    setupDone = false,
    smartEndText = null,
    homeLost = false,
    steps = listOf(
        SetupStep(StepId.HOME, "기본 홈 앱", "접다를 홈 앱으로 지정하면 폴더폰 화면이 켜져요", done = true, required = true),
        SetupStep(StepId.NOTIF_ACCESS, "알림 접근", "폴더폰 모드에서 필요한 알림만 남겨요. 알림 내용은 저장하거나 보내지 않아요", done = false, required = true),
        SetupStep(StepId.USAGE_ACCESS, "사용 기록 접근", "폴더폰 모드에서 최근 앱 화면 등으로 다른 앱을 열면 막아요. 지금 맨 앞 앱만 확인하고 기록은 저장하지 않아요", done = false, required = true),
        SetupStep(StepId.OVERLAY, "다른 앱 위에 표시", "막힌 앱 대신 폴더폰 화면을 띄우는 데 필요해요", done = false, required = true),
        SetupStep(StepId.POST_NOTIF, "알림 표시", "스마트폰 모드 남은 시간과 종료 30분 전·10분 전에 알려요", done = true, required = false),
        SetupStep(StepId.EXACT_ALARM, "알람 및 리마인더", "종료 전 알림을 정확한 시각에 보내요", done = false, required = false),
        SetupStep(StepId.CONTACTS, "연락처", "폴더폰 화면에 연락처를 보여줘요", done = false, required = false),
        SetupStep(StepId.CALL, "전화 걸기", "번호를 누르면 바로 전화를 걸어요", done = false, required = false),
    ),
    slots = listOf(
        SlotItem(1, "전화"), SlotItem(2, "문자"), SlotItem(3, "연락처"),
        SlotItem(4, "알람"), SlotItem(5, "카메라"), SlotItem(6, "카카오톡"),
        SlotItem(7, "지도"), SlotItem(8, "비어 있음", empty = true), SlotItem(9, "비어 있음", empty = true),
    ),
    allowedNotifApps = listOf(NotifApp("com.kakao.talk", "카카오톡")),
    editable = true,
    smartHomeLabel = "One UI 홈",
)
