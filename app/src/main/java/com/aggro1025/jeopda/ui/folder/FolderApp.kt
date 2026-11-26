package com.aggro1025.jeopda.ui.folder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.aggro1025.jeopda.ui.JeopdaTheme
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.aggro1025.jeopda.data.UnlockRules
import com.aggro1025.jeopda.data.UnlockRules.ReasonCheck
import com.aggro1025.jeopda.session.UnlockMethod
import com.aggro1025.jeopda.ui.Lcd
import com.aggro1025.jeopda.ui.LcdText
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 화면의 버튼부와 물리 키 입력을 지금 보이는 화면으로 보낸다 */
class KeyRouter {
    var handler: ((FolderKey) -> Unit)? = null
    fun dispatch(key: FolderKey): Boolean = handler?.let { it(key); true } ?: false
}

data class MenuEntry(val slot: Int, val label: String, val empty: Boolean = false)

interface FolderActions {
    fun openMenuSlot(slot: Int)
    fun dial(number: String)
    fun openSettings()
    fun isFreeAvailable(): Boolean
    fun unlock(minutes: Int, reason: String, method: UnlockMethod)
}

private data class MethodOption(val method: UnlockMethod, val title: String, val note: String, val enabled: Boolean)

private val QUICK_REASONS = listOf("업무 연락 확인", "길찾기", "결제·송금", "사진 정리")

@Composable
fun FolderApp(
    screen: Screen,
    onScreen: (Screen) -> Unit,
    menu: List<MenuEntry>,
    actions: FolderActions,
    router: KeyRouter,
    devUnlock: Boolean,
) {
    when (screen) {
        Screen.Home -> {
            val onKey: (FolderKey) -> Unit = { k ->
                when {
                    k == FolderKey.SOFT_LEFT -> onScreen(Screen.UnlockMethod())
                    k == FolderKey.OK -> onScreen(Screen.Menu())
                    k == FolderKey.SOFT_RIGHT -> actions.openSettings()
                    k == FolderKey.CALL -> actions.dial("")
                    k.char != null -> actions.dial(k.char.toString())
                }
            }
            Bind(router, onKey)
            FolderPhone(SoftLabels("해제", "메뉴", "설정"), onKey) { HomeDisplay(actions.isFreeAvailable()) }
        }

        is Screen.Menu -> {
            val sel = screen.selected
            val onKey: (FolderKey) -> Unit = { k ->
                val d = k.digit
                when {
                    d != null && d in 1..9 -> actions.openMenuSlot(d)
                    k == FolderKey.OK || k == FolderKey.SOFT_LEFT -> actions.openMenuSlot(sel)
                    k == FolderKey.SOFT_RIGHT || k == FolderKey.END -> onScreen(Screen.Home)
                    k == FolderKey.UP -> onScreen(Screen.Menu(if (sel > 3) sel - 3 else sel + 6))
                    k == FolderKey.DOWN -> onScreen(Screen.Menu(if (sel < 7) sel + 3 else sel - 6))
                    k == FolderKey.LEFT -> onScreen(Screen.Menu(if (sel == 1) 9 else sel - 1))
                    k == FolderKey.RIGHT -> onScreen(Screen.Menu(if (sel == 9) 1 else sel + 1))
                    k == FolderKey.CALL -> actions.dial("")
                }
            }
            Bind(router, onKey)
            FolderPhone(SoftLabels("", "열기", "뒤로"), onKey) {
                MenuDisplay(menu, sel, onTap = { actions.openMenuSlot(it) })
            }
        }

        is Screen.UnlockMethod -> {
            val free = actions.isFreeAvailable()
            val options = buildList {
                add(MethodOption(UnlockMethod.FREE, "하루 1회 무료", if (free) "오늘 남아 있어요" else "오늘은 이미 썼어요", free))
                add(MethodOption(UnlockMethod.AD, "광고 2개 보기", "준비 중", false))
                add(MethodOption(UnlockMethod.PAY, "500원 결제", "준비 중", false))
                if (devUnlock) add(MethodOption(UnlockMethod.DEV, "개발용 해제", "디버그 빌드에만 보여요", true))
            }
            val sel = screen.selected.coerceIn(options.indices)
            val choose: (Int) -> Unit = { i ->
                val o = options.getOrNull(i)
                if (o != null && o.enabled) onScreen(Screen.UnlockInput(o.method))
                else if (o != null) onScreen(Screen.UnlockMethod(i))
            }
            val onKey: (FolderKey) -> Unit = { k ->
                val d = k.digit
                when {
                    d != null && d in 1..options.size -> choose(d - 1)
                    k == FolderKey.OK || k == FolderKey.SOFT_LEFT -> choose(sel)
                    k == FolderKey.UP -> onScreen(Screen.UnlockMethod((sel - 1 + options.size) % options.size))
                    k == FolderKey.DOWN -> onScreen(Screen.UnlockMethod((sel + 1) % options.size))
                    k == FolderKey.SOFT_RIGHT || k == FolderKey.END -> onScreen(Screen.Home)
                }
            }
            Bind(router, onKey)
            FolderPhone(SoftLabels("", "선택", "뒤로"), onKey) {
                Column(Modifier.fillMaxSize().padding(14.dp)) {
                    Title("해제 방법")
                    Spacer(Modifier.height(10.dp))
                    options.forEachIndexed { i, o ->
                        val selected = i == sel
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (selected) Lcd.text else Lcd.screen)
                                .clickable { choose(i) }
                                .padding(horizontal = 10.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val fg = when {
                                selected -> Lcd.screen
                                o.enabled -> Lcd.text
                                else -> Lcd.dim
                            }
                            Text("${i + 1}", style = LcdText.copy(color = fg, fontSize = 18.sp, fontWeight = FontWeight.Bold))
                            Spacer(Modifier.padding(start = 10.dp))
                            Column {
                                Text(o.title, style = LcdText.copy(color = fg, fontSize = 17.sp))
                                Text(o.note, style = LcdText.copy(color = fg.copy(alpha = 0.8f), fontSize = 12.sp))
                            }
                        }
                    }
                }
            }
        }

        is Screen.UnlockInput -> UnlockInputScreen(screen.method, onScreen, actions, router)
    }
}

/** 지금 화면의 키 처리 함수를 물리 키 입력에도 연결 */
@Composable
private fun Bind(router: KeyRouter, onKey: (FolderKey) -> Unit) {
    SideEffect { router.handler = onKey }
}

// ---------------- 홈 ----------------

@Composable
private fun HomeDisplay(freeAvailable: Boolean) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000L - now % 1000L)
        }
    }
    val battery = rememberBattery()
    val date = remember(now / 60_000) { Date(now) }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("접다", style = LcdText.copy(fontSize = 13.sp, color = Lcd.dim), modifier = Modifier.weight(1f))
            Text(
                (if (battery.charging) "⚡" else "") + "${battery.percent}%",
                style = LcdText.copy(fontSize = 13.sp),
            )
            Spacer(Modifier.padding(start = 6.dp))
            BatteryIcon(battery.percent)
        }
        Spacer(Modifier.weight(1f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                SimpleDateFormat("a", Locale.KOREA).format(date),
                style = LcdText.copy(fontSize = 18.sp),
            )
            Text(
                SimpleDateFormat("h:mm", Locale.KOREA).format(date),
                style = LcdText.copy(fontSize = 64.sp, fontWeight = FontWeight.Bold),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                SimpleDateFormat("M월 d일 EEEE", Locale.KOREA).format(date),
                style = LcdText.copy(fontSize = 17.sp),
            )
        }
        Spacer(Modifier.weight(1f))
        Text(
            if (freeAvailable) "오늘 무료 해제 1회 남음" else "오늘 무료 해제를 썼어요",
            style = LcdText.copy(fontSize = 12.sp, color = Lcd.dim),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
    }
}

private data class Battery(val percent: Int, val charging: Boolean)

@Composable
private fun rememberBattery(): Battery {
    val context = LocalContext.current
    var state by remember { mutableStateOf(Battery(0, false)) }
    if (LocalInspectionMode.current) return Battery(87, false) // Android Studio 미리보기용
    DisposableEffect(context) {
        fun read(i: Intent?): Battery? {
            i ?: return null
            val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            val status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            if (level < 0 || scale <= 0) return null
            val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            return Battery(level * 100 / scale, charging)
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, i: Intent) {
                read(i)?.let { state = it }
            }
        }
        val sticky = ContextCompat.registerReceiver(
            context, receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        read(sticky)?.let { state = it }
        onDispose { context.unregisterReceiver(receiver) }
    }
    return state
}

@Composable
private fun BatteryIcon(percent: Int) {
    val bars = ((percent + 19) / 25).coerceIn(0, 4)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .border(1.5.dp, Lcd.text, RoundedCornerShape(2.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(1.5.dp),
        ) {
            repeat(4) { i ->
                Box(
                    Modifier
                        .padding(vertical = 0.dp)
                        .background(if (i < bars) Lcd.text else Lcd.screen)
                        .height(8.dp)
                        .padding(horizontal = 2.dp),
                )
            }
        }
        Box(Modifier.background(Lcd.text).height(5.dp).padding(horizontal = 1.dp))
    }
}

// ---------------- 메뉴 ----------------

@Composable
private fun MenuDisplay(menu: List<MenuEntry>, selected: Int, onTap: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(10.dp)) {
        Title("메뉴", Modifier.padding(start = 4.dp, bottom = 6.dp))
        (0 until 3).forEach { r ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                (1..3).forEach { c ->
                    val slot = r * 3 + c
                    val entry = menu.firstOrNull { it.slot == slot }
                    val isSel = slot == selected
                    Column(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .padding(3.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Lcd.text else Lcd.text.copy(alpha = 0.06f))
                            .clickable { onTap(slot) }
                            .padding(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        val fg = if (isSel) Lcd.screen else if (entry?.empty != false) Lcd.dim else Lcd.text
                        Text("$slot", style = LcdText.copy(color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold))
                        Text(
                            entry?.label ?: "",
                            style = LcdText.copy(color = fg, fontSize = 13.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}

// ---------------- 시간·사유 입력 ----------------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnlockInputScreen(
    method: UnlockMethod,
    onScreen: (Screen) -> Unit,
    actions: FolderActions,
    router: KeyRouter,
) {
    var minutes by rememberSaveable { mutableIntStateOf(30) }
    var reason by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    fun setMinutes(v: Int) {
        minutes = v.coerceIn(UnlockRules.MIN_MINUTES, UnlockRules.MAX_MINUTES)
        error = null
    }

    fun submit() {
        if (method == UnlockMethod.FREE && !actions.isFreeAvailable()) {
            error = "오늘 무료 해제는 이미 썼어요"
            return
        }
        if (!UnlockRules.isValidDuration(minutes)) {
            error = "시간은 ${UnlockRules.MIN_MINUTES}분~${UnlockRules.MAX_MINUTES / 60}시간 사이로 정해요"
            return
        }
        when (val c = UnlockRules.checkReason(reason)) {
            is ReasonCheck.TooShort -> error = "사유를 ${UnlockRules.MIN_REASON_CHARS}글자 이상 적어주세요 (지금 ${c.count}글자)"
            ReasonCheck.Repetitive -> error = "같은 글자만 반복할 수는 없어요"
            ReasonCheck.Ok -> actions.unlock(minutes, reason, method)
        }
    }

    val cancel = { onScreen(Screen.UnlockMethod()) }
    val onKey: (FolderKey) -> Unit = { k ->
        when (k) {
            FolderKey.UP -> setMinutes(minutes + 60)
            FolderKey.DOWN -> setMinutes(minutes - 60)
            FolderKey.RIGHT -> setMinutes(minutes + UnlockRules.MINUTE_STEP)
            FolderKey.LEFT -> setMinutes(minutes - UnlockRules.MINUTE_STEP)
            FolderKey.SOFT_LEFT, FolderKey.OK -> submit()
            FolderKey.SOFT_RIGHT, FolderKey.END -> cancel()
            else -> Unit
        }
    }
    Bind(router, onKey)

    FolderPhone(SoftLabels("해제", "", "취소"), onKey) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(14.dp),
        ) {
            Title("스마트폰 모드 (${method.label})")
            Spacer(Modifier.height(12.dp))

            Label("얼마나 쓸까요? (최대 ${UnlockRules.MAX_MINUTES / 60}시간)")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Stepper("시간", minutes / 60, { setMinutes(minutes - 60) }, { setMinutes(minutes + 60) })
                Spacer(Modifier.padding(start = 12.dp))
                Stepper("분", minutes % 60, { setMinutes(minutes - UnlockRules.MINUTE_STEP) }, { setMinutes(minutes + UnlockRules.MINUTE_STEP) })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15 to "15분", 30 to "30분", 60 to "1시간", 120 to "2시간").forEach { (m, t) ->
                    Chip(t, selected = minutes == m) { setMinutes(m) }
                }
            }
            Text("▲▼ 시간 · ◀▶ 분", style = LcdText.copy(fontSize = 11.sp, color = Lcd.dim), modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(14.dp))
            val count = UnlockRules.reasonCharCount(reason)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("왜 필요한가요?", Modifier.weight(1f))
                Text(
                    "$count/${UnlockRules.MIN_REASON_CHARS}",
                    style = LcdText.copy(fontSize = 13.sp, color = if (count >= UnlockRules.MIN_REASON_CHARS) Lcd.text else Lcd.dim),
                )
            }
            BasicTextField(
                value = reason,
                onValueChange = { reason = it.take(200); error = null },
                textStyle = LcdText.copy(fontSize = 16.sp),
                cursorBrush = SolidColor(Lcd.text),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .border(1.dp, Lcd.dim, RoundedCornerShape(6.dp))
                    .padding(10.dp),
                minLines = 2,
                decorationBox = { inner ->
                    Box {
                        if (reason.isEmpty()) Text("10글자 이상 적어주세요", style = LcdText.copy(fontSize = 15.sp, color = Lcd.dim))
                        inner()
                    }
                },
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                QUICK_REASONS.forEach { q ->
                    Chip(q) {
                        reason = if (reason.isBlank()) "$q " else "${reason.trimEnd()} $q "
                        error = null
                    }
                }
            }

            error?.let {
                Text(it, style = LcdText.copy(fontSize = 13.sp, color = Lcd.warn), modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button("취소", Modifier.weight(1f), primary = false) { cancel() }
                Button("해제하기", Modifier.weight(1f), primary = true) { submit() }
            }
        }
    }
}

// ---------------- 공통 LCD 부품 ----------------

@Composable
private fun Title(text: String, modifier: Modifier = Modifier) =
    Text(text, style = LcdText.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold), modifier = modifier)

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) =
    Text(text, style = LcdText.copy(fontSize = 14.sp), modifier = modifier)

@Composable
private fun Stepper(unit: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        SmallKey("−", onMinus)
        Text(
            "%2d".format(value),
            style = LcdText.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        SmallKey("+", onPlus)
        Text(unit, style = LcdText.copy(fontSize = 15.sp), modifier = Modifier.padding(start = 4.dp))
    }
}

@Composable
private fun SmallKey(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .border(1.dp, Lcd.text, RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 2.dp),
    ) { Text(text, style = LcdText.copy(fontSize = 20.sp)) }
}

@Composable
private fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = LcdText.copy(fontSize = 13.sp, color = if (selected) Lcd.screen else Lcd.text),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Lcd.text else Lcd.screen)
            .border(1.dp, Lcd.text.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun Button(text: String, modifier: Modifier, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (primary) Lcd.text else Lcd.screen)
            .border(1.dp, Lcd.text, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = LcdText.copy(fontSize = 16.sp, color = if (primary) Lcd.screen else Lcd.text))
    }
}

// ---------------- Android Studio 미리보기 ----------------
// 파일을 열고 오른쪽 위 "Split" 또는 "Design"을 누르면 기기 없이 화면을 볼 수 있다.

internal object PreviewActions : FolderActions {
    override fun openMenuSlot(slot: Int) = Unit
    override fun dial(number: String) = Unit
    override fun openSettings() = Unit
    override fun isFreeAvailable() = true
    override fun unlock(minutes: Int, reason: String, method: UnlockMethod) = Unit
}

internal val previewMenu = listOf(
    MenuEntry(1, "전화"), MenuEntry(2, "문자"), MenuEntry(3, "연락처"),
    MenuEntry(4, "알람"), MenuEntry(5, "카메라"), MenuEntry(6, "카카오톡"),
    MenuEntry(7, "네이버 지도"), MenuEntry(8, "비어 있음", empty = true), MenuEntry(9, "비어 있음", empty = true),
)

@Composable
internal fun PreviewScreen(screen: Screen) {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        JeopdaTheme {
            FolderApp(screen, {}, previewMenu, PreviewActions, KeyRouter(), devUnlock = true)
        }
    }
}

@Preview(name = "1. 홈", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun HomePreview() = PreviewScreen(Screen.Home)

@Preview(name = "2. 메뉴", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun MenuPreview() = PreviewScreen(Screen.Menu(selected = 6))

@Preview(name = "3. 해제 방법", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun UnlockMethodPreview() = PreviewScreen(Screen.UnlockMethod())

@Preview(name = "4. 시간·사유 입력", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun UnlockInputPreview() = PreviewScreen(Screen.UnlockInput(UnlockMethod.FREE))
