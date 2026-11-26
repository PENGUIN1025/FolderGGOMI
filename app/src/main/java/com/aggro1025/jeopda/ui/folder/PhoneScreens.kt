package com.aggro1025.jeopda.ui.folder

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aggro1025.jeopda.ui.Scr

/** 폴더폰 화면 안의 전화·연락처 (시안 단계: 아직 앱 동작에 연결하지 않음) */

data class ContactItem(val name: String, val number: String)

/** 다이얼러: 누른 번호를 크게, 아래에 일치하는 연락처 */
@Composable
fun DialerDisplay(number: String, match: ContactItem?) {
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp)) {
        Spacer(Modifier.weight(1f))
        Text(
            number,
            style = scrText(if (number.length > 11) 34.sp else 40.sp, weight = FontWeight.Light),
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        if (match != null) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x40000000))
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

/** 연락처 목록. 숫자키로 초성 찾기 (4 → ㄱ·ㅋ) */
@Composable
fun ContactsDisplay(contacts: List<ContactItem>, selected: Int, filter: String?) {
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
            if (filter == null) {
                Text("숫자키로 초성을 눌러요 (4 → ㄱ ㅋ)", style = scrText(12.sp, Scr.faint))
            } else {
                Text(filter, style = scrText(14.sp, Scr.accent, FontWeight.Bold))
            }
        }
        Spacer(Modifier.height(4.dp))
        contacts.forEachIndexed { i, c ->
            ContactRow(c, i == selected)
        }
    }
}

@Composable
private fun ContactRow(c: ContactItem, selected: Boolean) {
    val base = Modifier.fillMaxWidth().padding(vertical = 1.dp).clip(RoundedCornerShape(8.dp))
    val bar = if (selected) base.background(Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))) else base
    Row(
        bar.padding(horizontal = 10.dp, vertical = 7.dp),
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
        Text(c.number, style = scrText(12.sp, if (selected) Scr.text else Scr.dim))
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

// ---------------- 시안용 데이터 ----------------

internal val mockContacts = listOf(
    ContactItem("강민수", "010-2481-5530"),
    ContactItem("김지은", "010-7719-0042"),
    ContactItem("김하늘", "010-3356-8120"),
    ContactItem("박서준", "010-9902-1187"),
    ContactItem("아빠", "010-5521-3398"),
    ContactItem("엄마", "010-1234-5678"),
    ContactItem("이도윤", "010-6640-2259"),
)

@Composable
internal fun MockDialer() = FolderPhone("전화", SoftLabels("연락처", "통화", "지우기"), {}) {
    DialerDisplay("010-1234-5678", ContactItem("엄마", "010-1234-5678"))
}

@Composable
internal fun MockContacts() = FolderPhone("연락처 (7)", SoftLabels("문자", "통화", "뒤로"), {}) {
    ContactsDisplay(mockContacts, selected = 5, filter = null)
}

@Composable
internal fun MockContactsFiltered() = FolderPhone("연락처", SoftLabels("문자", "통화", "뒤로"), {}) {
    ContactsDisplay(mockContacts.filter { it.name.startsWith("강") || it.name.startsWith("김") }, selected = 1, filter = "ㄱ ㅋ")
}
