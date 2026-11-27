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

enum class StepId { HOME, NOTIF_ACCESS, POST_NOTIF, EXACT_ALARM, CONTACTS, CALL }

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
}

private fun t(size: TextUnit, color: Color = Scr.text, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size, color = color, fontWeight = weight)

private val Card = Color(0x1FFFFFFF)
private val CardLine = Color(0x26FFFFFF)

@Composable
fun SettingsDesign(ui: SettingsUi, cb: SettingsCallbacks = NoCallbacks) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Scr.wallTop, Scr.wallBottom))),
    ) {
        Column(
            Modifier
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // 머리말
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(Lcd.body),
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
                    Text("정보를 가져가지 않아요", style = t(15.sp, weight = FontWeight.Bold))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "접다가 요청하는 권한은 이 폰을 폴더폰처럼 쓰기 위한 것뿐이에요. " +
                            "연락처, 앱 목록, 알림 내용은 폰 안에서만 쓰고 저장하거나 밖으로 보내지 않아요.",
                        style = t(13.sp, Scr.text.copy(alpha = 0.9f)),
                    )
                }
            }

            if (ui.homeLost) {
                Banner(
                    "접다가 기본 홈 앱이 아니에요",
                    "지금은 폴더폰 화면이 뜨지 않아요. 눌러서 다시 홈 앱으로 지정하세요",
                ) { cb.onStep(StepId.HOME) }
            }

            if (ui.smartEndText != null) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("스마트폰 모드", style = t(15.sp, weight = FontWeight.Bold))
                    Text(ui.smartEndText, style = t(13.sp, Scr.dim))
                    PrimaryButton("폴더폰으로 돌아가기", enabled = true, onClick = cb::onReturnToFolder)
                    Text("지금 돌아가면 남은 시간은 사라져요", style = t(12.sp, Scr.dim))
                }
            }

            // 설정 단계
            val doneCount = ui.steps.count { it.done }
            Section("폴더폰 준비 ${doneCount}/${ui.steps.size}")
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card),
            ) {
                ui.steps.forEachIndexed { i, s ->
                    StepRow(i + 1, s) { cb.onStep(s.id) }
                    if (i < ui.steps.lastIndex) Divider()
                }
            }
            if (!ui.setupDone) {
                val ready = ui.steps.filter { it.required }.all { it.done }
                PrimaryButton(if (ready) "폴더폰 모드 시작" else "필수 설정을 먼저 마쳐 주세요", enabled = ready, onClick = cb::onStart)
            }

            // 메뉴
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

            // 알림 허용 앱
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

            // 기타
            Section("기타")
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card)) {
                LinkRow("스마트폰 모드에서 쓸 홈 화면", ui.smartHomeLabel, cb::onSmartHome)
                Divider()
                LinkRow("개인정보처리방침", "", cb::onPrivacy)
                Divider()
                LinkRow("오픈소스 라이선스", "", cb::onLicenses)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
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
    version = "0.2.0",
    modeText = "처음 설정 중",
    setupDone = false,
    smartEndText = null,
    homeLost = false,
    steps = listOf(
        SetupStep(StepId.HOME, "기본 홈 앱", "접다를 홈 앱으로 지정하면 폴더폰 화면이 켜져요", done = true, required = true),
        SetupStep(StepId.NOTIF_ACCESS, "알림 접근", "폴더폰 모드에서 필요한 알림만 남겨요. 알림 내용은 저장하거나 보내지 않아요", done = false, required = true),
        SetupStep(StepId.POST_NOTIF, "알림 표시", "스마트폰 모드 남은 시간과 종료 5분 전에 알려요", done = true, required = false),
        SetupStep(StepId.EXACT_ALARM, "알람 및 리마인더", "종료 5분 전 알림을 정확한 시각에 보내요", done = false, required = false),
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
