package com.aggro1025.jeopda.ui.folder

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.data.Contact
import com.aggro1025.jeopda.data.UnlockRules
import com.aggro1025.jeopda.data.UnlockRules.ReasonCheck
import com.aggro1025.jeopda.session.UnlockMethod
import com.aggro1025.jeopda.ui.JeopdaTheme
import com.aggro1025.jeopda.ui.Scr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 화면의 버튼부와 물리 키 입력을 지금 보이는 화면으로 보낸다 */
class KeyRouter {
    var handler: ((FolderKey) -> Unit)? = null
    fun dispatch(key: FolderKey): Boolean = handler?.let { it(key); true } ?: false
}

data class MenuEntry(
    val slot: Int,
    val label: String,
    val empty: Boolean = false,
    val icon: ImageBitmap? = null,
)

interface FolderActions {
    fun openMenuSlot(slot: Int)
    /** 바로 전화 걸기 (긴급번호는 기본 전화 앱으로 넘김) */
    fun call(number: String)
    /** 기본 문자 앱의 작성 화면으로 넘김 */
    fun sms(number: String)
    fun hasContactsPermission(): Boolean
    fun requestContactsPermission()
    fun openSettings()
    fun isFreeAvailable(): Boolean
    /** 알림 차단(알림 접근 권한)이 켜져 있는지 */
    fun isNotificationBlockOn(): Boolean
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
    contacts: List<Contact> = emptyList(),
) {
    when (screen) {
        Screen.Home -> {
            val onKey: (FolderKey) -> Unit = { k ->
                when {
                    k == FolderKey.SOFT_LEFT -> onScreen(Screen.UnlockMethod())
                    k == FolderKey.OK -> onScreen(Screen.Menu())
                    k == FolderKey.SOFT_RIGHT -> actions.openSettings()
                    k == FolderKey.CALL -> onScreen(Screen.Dialer())
                    k.char != null -> onScreen(Screen.Dialer(k.char.toString()))
                }
            }
            BindKeys(router, onKey)
            FolderPhone(null, SoftLabels("해제", "메뉴", "설정"), onKey) {
                HomeDisplay(actions.isFreeAvailable(), actions.isNotificationBlockOn(), onFixNotif = { actions.openSettings() })
            }
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
                    k == FolderKey.CALL -> onScreen(Screen.Dialer())
                }
            }
            BindKeys(router, onKey)
            FolderPhone("메뉴", SoftLabels("", "열기", "뒤로"), onKey) {
                MenuDisplay(menu, sel, onTap = { actions.openMenuSlot(it) })
            }
        }

        is Screen.UnlockMethod -> {
            val free = actions.isFreeAvailable()
            val options = buildList {
                add(MethodOption(UnlockMethod.FREE, "하루 1회 무료", if (free) "오늘 남아 있어요" else "오늘은 이미 썼어요", free))
                // 광고(v0.2b)가 나오기 전까지는 광고 자리에 1분 기다리기
                add(MethodOption(UnlockMethod.WAIT, "1분 기다리기", "광고 대신 잠시 기다리면 해제돼요", true))
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
            BindKeys(router, onKey)
            FolderPhone("해제 방법", SoftLabels("", "선택", "뒤로"), onKey) {
                Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 8.dp)) {
                    options.forEachIndexed { i, o ->
                        ListRow(
                            number = i + 1,
                            title = o.title,
                            note = o.note,
                            selected = i == sel,
                            enabled = o.enabled,
                            onClick = { choose(i) },
                        )
                    }
                }
            }
        }

        is Screen.UnlockInput -> UnlockInputScreen(screen.method, onScreen, actions, router)
        is Screen.Dialer -> DialerScreen(screen.number, contacts, onScreen, actions, router)
        is Screen.Contacts -> ContactsScreen(screen, contacts, onScreen, actions, router)
        is Screen.UnlockWait -> UnlockWaitScreen(screen, onScreen, actions, router)
    }
}

/** 지금 화면의 키 처리 함수를 물리 키 입력에도 연결 */
@Composable
internal fun BindKeys(router: KeyRouter, onKey: (FolderKey) -> Unit) {
    SideEffect { router.handler = onKey }
}

// ---------------- 홈 (대기 화면) ----------------

@Composable
private fun HomeDisplay(freeAvailable: Boolean, notifBlockOn: Boolean, onFixNotif: () -> Unit) {
    val now = rememberNow()
    val date = remember(now / 60_000) { Date(now) }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
        // 실제 폴더폰의 "통신사 이름 + 날짜" 자리
        Text("폴더폰 모드", style = scrText(17.sp, weight = FontWeight.Bold))
        Text(
            SimpleDateFormat("M월 d일 (E)", Locale.KOREA).format(date),
            style = scrText(14.sp, Scr.dim),
        )
        Spacer(Modifier.weight(0.8f))
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    SimpleDateFormat("a", Locale.KOREA).format(date),
                    style = scrText(20.sp, Scr.dim),
                    modifier = Modifier.padding(bottom = 14.dp, end = 6.dp),
                )
                Text(
                    SimpleDateFormat("h:mm", Locale.KOREA).format(date),
                    style = scrText(76.sp, weight = FontWeight.Light),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        // 알림 차단이 꺼져 있으면 그 안내를 먼저 (누르면 설정으로)
        val chipBase = Modifier
            .align(Alignment.CenterHorizontally)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x33000000))
        Row(
            (if (notifBlockOn) chipBase else chipBase.clickable(onClick = onFixNotif))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dot = when {
                !notifBlockOn -> Scr.warn
                freeAvailable -> Scr.accent
                else -> Scr.faint
            }
            Box(Modifier.size(7.dp).clip(CircleShape).background(dot))
            Text(
                when {
                    !notifBlockOn -> "  알림 차단이 꺼져 있어요 · 눌러서 켜기"
                    freeAvailable -> "  오늘 무료 해제 1회 남음"
                    else -> "  오늘 무료 해제를 썼어요"
                },
                style = scrText(12.sp, if (notifBlockOn) Scr.dim else Scr.warn),
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

// ---------------- 메뉴 (아이콘 격자) ----------------

@Composable
private fun MenuDisplay(menu: List<MenuEntry>, selected: Int, onTap: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
        (0 until 3).forEach { r ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                (1..3).forEach { c ->
                    val slot = r * 3 + c
                    val entry = menu.firstOrNull { it.slot == slot } ?: MenuEntry(slot, "", empty = true)
                    MenuCell(entry, slot == selected, Modifier.weight(1f).fillMaxHeight()) { onTap(slot) }
                }
            }
        }
    }
}

@Composable
private fun MenuCell(entry: MenuEntry, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    val base = modifier.padding(3.dp).clip(shape)
    val framed = if (selected) {
        base
            .background(Brush.verticalGradient(listOf(Color(0x665AB0FF), Color(0x332A6FD6))))
            .border(2.dp, Scr.accent, shape)
    } else {
        base
    }
    BoxWithConstraints(framed.clickable(onClick = onClick)) {
        // 칸 높이에 맞춰 아이콘 크기를 줄인다 (세로가 짧은 폰에서 이름이 잘리지 않게)
        val iconSize = (maxHeight - 34.dp).coerceIn(24.dp, 46.dp)
        Column(
            Modifier.fillMaxSize().padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            MenuIcon(entry, iconSize)
            Spacer(Modifier.height(4.dp))
            Text(
                entry.label,
                style = scrText(
                    13.sp,
                    color = if (entry.empty) Scr.faint else Scr.text,
                    weight = if (selected) FontWeight.Bold else FontWeight.Normal,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
        // 숫자키 번호
        Text(
            "${entry.slot}",
            style = scrText(11.sp, if (selected) Scr.text else Scr.dim, FontWeight.Bold),
            modifier = Modifier.padding(start = 7.dp, top = 4.dp),
        )
    }
}

/** 실제 앱 아이콘. 읽지 못하면(미리보기, 기본 앱을 못 찾은 경우) 색 사각형 + 기호 */
@Composable
private fun MenuIcon(entry: MenuEntry, size: Dp) {
    when {
        entry.icon != null -> Image(entry.icon, contentDescription = null, modifier = Modifier.size(size))
        entry.empty -> Box(
            Modifier
                .size(size)
                .border(1.5.dp, Scr.faint, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) { Text("+", style = scrText(22.sp, Scr.faint)) }
        else -> {
            val colors = FALLBACK_COLORS[(entry.slot - 1) % FALLBACK_COLORS.size]
            Box(
                Modifier
                    .size(size)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.linearGradient(colors)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Symbols.forSlot(entry.slot),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.56f),
                )
            }
        }
    }
}

private val FALLBACK_COLORS = listOf(
    listOf(Color(0xFF4CD964), Color(0xFF1E9E48)),
    listOf(Color(0xFFFFD54F), Color(0xFFF2A100)),
    listOf(Color(0xFF64B5F6), Color(0xFF1E6FD9)),
    listOf(Color(0xFFFF8A65), Color(0xFFE0452B)),
    listOf(Color(0xFFB0BEC5), Color(0xFF607D8B)),
    listOf(Color(0xFF9575CD), Color(0xFF5E35B1)),
    listOf(Color(0xFF4DD0E1), Color(0xFF00838F)),
)

// ---------------- 목록 행 (해제 방법) ----------------

@Composable
private fun ListRow(
    number: Int,
    title: String,
    note: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    val base = Modifier.fillMaxWidth().padding(vertical = 2.dp).clip(shape)
    val bar = if (selected) base.background(Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))) else base
    Row(
        bar
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fg = if (enabled || selected) Scr.text else Scr.faint
        Box(
            Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (selected) Color(0x40FFFFFF) else Color(0x26FFFFFF)),
            contentAlignment = Alignment.Center,
        ) { Text("$number", style = scrText(13.sp, fg, FontWeight.Bold)) }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, style = scrText(16.sp, fg, if (selected) FontWeight.Bold else FontWeight.Normal))
            Text(note, style = scrText(12.sp, if (selected) Scr.text.copy(alpha = 0.85f) else Scr.faint))
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
            ReasonCheck.Ok ->
                if (method == UnlockMethod.WAIT) {
                    onScreen(Screen.UnlockWait(minutes, reason.trim()))
                } else {
                    actions.unlock(minutes, reason, method)
                }
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
    BindKeys(router, onKey)

    FolderPhone("스마트폰 모드 해제 · ${method.label}", SoftLabels("해제", "", "취소"), onKey) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Label("얼마나 쓸까요? (최대 ${UnlockRules.MAX_MINUTES / 60}시간)")
            Row(
                Modifier
                    .padding(vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x40000000))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                Stepper("시간", minutes / 60, { setMinutes(minutes - 60) }, { setMinutes(minutes + 60) })
                Stepper("분", minutes % 60, { setMinutes(minutes - UnlockRules.MINUTE_STEP) }, { setMinutes(minutes + UnlockRules.MINUTE_STEP) })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15 to "15분", 30 to "30분", 60 to "1시간", 120 to "2시간").forEach { (m, t) ->
                    Chip(t, selected = minutes == m) { setMinutes(m) }
                }
            }
            Text("▲▼ 시간 · ◀▶ 분", style = scrText(11.sp, Scr.faint), modifier = Modifier.padding(top = 4.dp))

            Spacer(Modifier.height(12.dp))
            val count = UnlockRules.reasonCharCount(reason)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Label("왜 필요한가요?", Modifier.weight(1f))
                Text(
                    "$count/${UnlockRules.MIN_REASON_CHARS}",
                    style = scrText(13.sp, if (count >= UnlockRules.MIN_REASON_CHARS) Scr.accent else Scr.dim, FontWeight.Bold),
                )
            }
            BasicTextField(
                value = reason,
                onValueChange = { reason = it.take(200); error = null },
                textStyle = scrText(16.sp).copy(shadow = null),
                cursorBrush = SolidColor(Scr.accent),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0x59000000))
                    .border(1.dp, Scr.dim.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                minLines = 2,
                decorationBox = { inner ->
                    Box {
                        if (reason.isEmpty()) Text("10글자 이상 적어주세요", style = scrText(15.sp, Scr.faint).copy(shadow = null))
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
                Text(it, style = scrText(13.sp, Scr.warn), modifier = Modifier.padding(top = 10.dp))
            }

            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button("취소", Modifier.weight(1f), primary = false) { cancel() }
                Button("해제하기", Modifier.weight(1f), primary = true) { submit() }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

// ---------------- 1분 기다리기 ----------------

/** 90 → "1시간 30분", 30 → "30분", 120 → "2시간" */
internal fun durationText(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return listOfNotNull(if (h > 0) "${h}시간" else null, if (m > 0 || h == 0) "${m}분" else null).joinToString(" ")
}

/** 카운트다운만 보여주고, 끝나면 해제. 취소하거나 화면을 떠나면 폴더폰 모드에 남는다 (4장 공통 규칙 5) */
@Composable
private fun UnlockWaitScreen(
    screen: Screen.UnlockWait,
    onScreen: (Screen) -> Unit,
    actions: FolderActions,
    router: KeyRouter,
) {
    val startedAt = remember(screen) { android.os.SystemClock.elapsedRealtime() }
    var left by remember(screen) { mutableIntStateOf(UnlockRules.WAIT_SECONDS) }
    val inspection = LocalInspectionMode.current
    LaunchedEffect(screen) {
        if (inspection) return@LaunchedEffect
        while (left > 0) {
            kotlinx.coroutines.delay(250)
            val passed = ((android.os.SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
            left = (UnlockRules.WAIT_SECONDS - passed).coerceAtLeast(0)
        }
        actions.unlock(screen.minutes, screen.reason, UnlockMethod.WAIT)
    }
    val cancel = { onScreen(Screen.Home) }
    val onKey: (FolderKey) -> Unit = { k ->
        if (k == FolderKey.SOFT_RIGHT || k == FolderKey.END) cancel()
    }
    BindKeys(router, onKey)
    FolderPhone("잠시 기다려요", SoftLabels("", "", "취소"), onKey) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("$left", style = scrText(88.sp, weight = FontWeight.Light))
            Text("초 뒤에 스마트폰 모드가 켜져요", style = scrText(15.sp, Scr.dim))
            Spacer(Modifier.height(20.dp))
            Text(
                "${durationText(screen.minutes)} · ${screen.reason}",
                style = scrText(13.sp, Scr.faint),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(20.dp))
            Text("취소하면 폴더폰 모드로 남아요", style = scrText(12.sp, Scr.faint))
        }
    }
}

// ---------------- 공통 부품 ----------------

@Composable
private fun Label(text: String, modifier: Modifier = Modifier) =
    Text(text, style = scrText(14.sp, Scr.dim), modifier = modifier)

@Composable
private fun Stepper(unit: String, value: Int, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RoundKey("−", onMinus)
        Text(
            "$value",
            style = scrText(28.sp, weight = FontWeight.Bold),
            modifier = Modifier.padding(start = 8.dp),
        )
        Text(unit, style = scrText(14.sp, Scr.dim), modifier = Modifier.padding(start = 2.dp, end = 8.dp))
        RoundKey("+", onPlus)
    }
}

@Composable
private fun RoundKey(text: String, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Scr.panel)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = scrText(18.sp, weight = FontWeight.Bold)) }
}

@Composable
private fun Chip(text: String, selected: Boolean = false, onClick: () -> Unit) {
    Text(
        text,
        style = scrText(13.sp, weight = if (selected) FontWeight.Bold else FontWeight.Normal),
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))
                else Brush.verticalGradient(listOf(Scr.panel, Scr.panel)),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 5.dp),
    )
}

@Composable
private fun Button(text: String, modifier: Modifier, primary: Boolean, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (primary) Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))
                else Brush.verticalGradient(listOf(Scr.panel, Scr.panel)),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = scrText(16.sp, weight = FontWeight.Bold))
    }
}

// ---------------- Android Studio 미리보기 ----------------
// 파일을 열고 오른쪽 위 "Split" 또는 "Design"을 누르면 기기 없이 화면을 볼 수 있다.

internal object PreviewActions : FolderActions {
    override fun openMenuSlot(slot: Int) = Unit
    override fun call(number: String) = Unit
    override fun sms(number: String) = Unit
    override fun hasContactsPermission() = true
    override fun requestContactsPermission() = Unit
    override fun openSettings() = Unit
    override fun isFreeAvailable() = true
    override fun isNotificationBlockOn() = true
    override fun unlock(minutes: Int, reason: String, method: UnlockMethod) = Unit
}

internal val previewMenu = listOf(
    MenuEntry(1, "전화"), MenuEntry(2, "문자"), MenuEntry(3, "연락처"),
    MenuEntry(4, "알람"), MenuEntry(5, "카메라"), MenuEntry(6, "카카오톡"),
    MenuEntry(7, "네이버 지도"), MenuEntry(8, "비어 있음", empty = true), MenuEntry(9, "비어 있음", empty = true),
)

@Composable
internal fun PreviewScreen(screen: Screen, devUnlock: Boolean = true) {
    CompositionLocalProvider(LocalInspectionMode provides true) {
        JeopdaTheme {
            FolderApp(screen, {}, previewMenu, PreviewActions, KeyRouter(), devUnlock = devUnlock, contacts = previewContacts)
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

@Preview(name = "5. 전화", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun DialerPreview() = PreviewScreen(Screen.Dialer("01012345678"))

@Preview(name = "7. 1분 기다리기", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun WaitPreview() = PreviewScreen(Screen.UnlockWait(30, "회사 메일 확인해야 해요"))

@Preview(name = "6. 연락처", device = "spec:width=384dp,height=832dp,dpi=500", showSystemUi = true)
@Composable
private fun ContactsPreview() = PreviewScreen(Screen.Contacts(selected = 5))
