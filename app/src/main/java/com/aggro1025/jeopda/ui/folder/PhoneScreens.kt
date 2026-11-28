package com.aggro1025.jeopda.ui.folder

import android.telephony.PhoneNumberUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.data.Contact
import com.aggro1025.jeopda.data.ContactSearch
import com.aggro1025.jeopda.ui.Scr

private const val MAX_DIAL_LENGTH = 20

// ---------------- 전화 (다이얼러) ----------------

@Composable
internal fun DialerScreen(
    number: String,
    contacts: List<Contact>,
    onScreen: (Screen) -> Unit,
    actions: FolderActions,
    router: KeyRouter,
) {
    val onKey: (FolderKey) -> Unit = { k ->
        val ch = k.char
        when {
            ch != null -> if (number.length < MAX_DIAL_LENGTH) onScreen(Screen.Dialer(number + ch))
            k == FolderKey.CALL || k == FolderKey.OK -> if (number.isNotEmpty()) actions.call(number)
            k == FolderKey.SOFT_LEFT -> onScreen(Screen.Contacts())
            k == FolderKey.SOFT_RIGHT -> if (number.isEmpty()) onScreen(Screen.Home) else onScreen(Screen.Dialer(number.dropLast(1)))
            k == FolderKey.END -> onScreen(Screen.Home)
        }
    }
    BindKeys(router, onKey)
    val match = remember(number, contacts) { ContactSearch.findByNumber(contacts, number) }
    FolderPhone(
        "전화",
        SoftLabels("연락처", if (number.isEmpty()) "" else "통화", if (number.isEmpty()) "뒤로" else "지우기"),
        onKey,
    ) {
        DialerDisplay(number, match) { if (number.isNotEmpty()) actions.call(number) }
    }
}

@Composable
private fun DialerDisplay(number: String, match: Contact?, onCall: () -> Unit) {
    val shown = remember(number) {
        if (number.isEmpty()) "" else PhoneNumberUtils.formatNumber(number, "KR") ?: number
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Spacer(Modifier.weight(1f))
        Text(
            shown.ifEmpty { "번호를 누르세요" },
            style = if (shown.isEmpty()) {
                scrText(20.sp, Scr.faint)
            } else {
                scrText(if (shown.length > 13) 32.sp else 40.sp, weight = FontWeight.Light)
            },
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Visible,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        if (match != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x40000000))
                    .clickable(onClick = onCall)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Avatar(match.name)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(match.name, style = scrText(16.sp, weight = FontWeight.Bold))
                    Text("연락처에 있는 번호", style = scrText(12.sp, Scr.dim))
                }
                Icon(Icons.Filled.Call, null, tint = Color(0xFF3DDC84), modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.weight(1.2f))
        Text(
            "통화 키를 누르면 바로 걸려요",
            style = scrText(12.sp, Scr.faint),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

// ---------------- 연락처 ----------------

@Composable
internal fun ContactsScreen(
    screen: Screen.Contacts,
    contacts: List<Contact>,
    onScreen: (Screen) -> Unit,
    actions: FolderActions,
    router: KeyRouter,
) {
    val permitted = actions.hasContactsPermission()
    val list = remember(contacts, screen.keys) { ContactSearch.filter(contacts, screen.keys) }
    val sel = screen.selected.coerceIn(0, (list.size - 1).coerceAtLeast(0))
    val current = list.getOrNull(sel)

    val onKey: (FolderKey) -> Unit = { k ->
        val d = k.digit
        when {
            !permitted -> when (k) {
                FolderKey.OK, FolderKey.SOFT_LEFT -> actions.requestContactsPermission()
                FolderKey.SOFT_RIGHT, FolderKey.END -> onScreen(Screen.Home)
                else -> Unit
            }
            d != null -> onScreen(Screen.Contacts(screen.keys + k.char, 0))
            k == FolderKey.UP && list.isNotEmpty() -> onScreen(screen.copy(selected = (sel - 1 + list.size) % list.size))
            k == FolderKey.DOWN && list.isNotEmpty() -> onScreen(screen.copy(selected = (sel + 1) % list.size))
            k == FolderKey.OK || k == FolderKey.CALL -> current?.let { actions.call(it.number) }
            k == FolderKey.SOFT_LEFT -> current?.let { actions.sms(it.number) }
            k == FolderKey.SOFT_RIGHT ->
                if (screen.keys.isNotEmpty()) onScreen(Screen.Contacts(screen.keys.dropLast(1), 0)) else onScreen(Screen.Home)
            k == FolderKey.END -> onScreen(Screen.Home)
        }
    }
    BindKeys(router, onKey)

    if (!permitted) {
        FolderPhone("연락처", SoftLabels("", "허용", "뒤로"), onKey) {
            Column(
                Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "연락처를 폴더폰 화면에 보여주기 위해\n연락처 권한이 필요해요",
                    style = scrText(15.sp),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "연락처는 기기 안에서만 쓰고 저장하거나 보내지 않아요",
                    style = scrText(12.sp, Scr.dim),
                    textAlign = TextAlign.Center,
                )
            }
        }
        return
    }

    FolderPhone(
        if (screen.keys.isEmpty()) "연락처 (${contacts.size})" else "연락처",
        SoftLabels(if (current != null) "문자" else "", if (current != null) "통화" else "", if (screen.keys.isEmpty()) "뒤로" else "지우기"),
        onKey,
    ) {
        ContactsDisplay(list, sel, screen.keys, empty = contacts.isEmpty()) { i ->
            if (i == sel) list.getOrNull(i)?.let { actions.call(it.number) } else onScreen(screen.copy(selected = i))
        }
    }
}

@Composable
private fun ContactsDisplay(list: List<Contact>, selected: Int, keys: String, empty: Boolean, onTap: (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(selected, keys) {
        val visible = state.layoutInfo.visibleItemsInfo.map { it.index }
        if (visible.isEmpty() || selected < visible.first() + 1 || selected > visible.last() - 1) {
            state.scrollToItem((selected - 2).coerceAtLeast(0))
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x40000000))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("찾기", style = scrText(12.sp, Scr.dim))
            Spacer(Modifier.width(8.dp))
            if (keys.isEmpty()) {
                Text("숫자키로 초성을 눌러요 (4 → ㄱ ㅋ)", style = scrText(12.sp, Scr.faint))
            } else {
                Text(ContactSearch.describe(keys), style = scrText(14.sp, Scr.accent, FontWeight.Bold))
                Text("  ($keys)", style = scrText(12.sp, Scr.faint))
            }
        }
        Spacer(Modifier.height(4.dp))
        when {
            empty -> CenterNote("연락처가 없어요")
            list.isEmpty() -> CenterNote("찾는 이름이 없어요")
            else -> LazyColumn(state = state, modifier = Modifier.fillMaxSize()) {
                itemsIndexed(list, key = { _, c -> c.name + "|" + c.number }) { i, c ->
                    ContactRow(c, i == selected) { onTap(i) }
                }
            }
        }
    }
}

@Composable
private fun CenterNote(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = scrText(14.sp, Scr.dim))
    }
}

@Composable
private fun ContactRow(c: Contact, selected: Boolean, onClick: () -> Unit) {
    val base = Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp))
    val bar = if (selected) base.background(Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))).inkFrame(isCatTheme(), 8.dp, strong = true) else base
    Row(
        bar.clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Avatar(c.name, small = true)
        Spacer(Modifier.width(10.dp))
        Text(
            c.name,
            style = scrText(16.sp, weight = if (selected) FontWeight.Bold else FontWeight.Normal),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(c.number, style = scrText(12.sp, if (selected) Scr.text else Scr.dim), maxLines = 1)
    }
}

@Composable
private fun Avatar(name: String, small: Boolean = false) {
    val colors = listOf(
        Color(0xFF5AB0FF), Color(0xFF7CE38B), Color(0xFFFFB74D), Color(0xFFBA68C8), Color(0xFFFF8A80),
    )
    Box(
        Modifier
            .size(if (small) 28.dp else 36.dp)
            .clip(CircleShape)
            .background(colors[(name.hashCode() and 0x7fffffff) % colors.size].copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center,
    ) {
        Text(name.take(1), style = scrText(if (small) 13.sp else 16.sp, weight = FontWeight.Bold))
    }
}

// ---------------- 미리보기용 데이터 (예시 이름·번호) ----------------

internal val previewContacts = listOf(
    Contact("강민수", "010-2481-5530"),
    Contact("김지은", "010-7719-0042"),
    Contact("김하늘", "010-3356-8120"),
    Contact("박서준", "010-9902-1187"),
    Contact("아빠", "010-5521-3398"),
    Contact("엄마", "010-1234-5678"),
    Contact("이도윤", "010-6640-2259"),
    Contact("최유진", "010-4418-7730"),
)
