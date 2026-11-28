package com.aggro1025.jeopda.ui.settings

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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.data.AppPrefs.ReasonEntry
import com.aggro1025.jeopda.session.UnlockMethod
import com.aggro1025.jeopda.ui.ScrFixed
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 해제 기록: 언제, 무슨 사유로, 몇 분 정했고 실제로 얼마나 썼는지. 폰 안에만 있는 기록 */
@Composable
fun HistoryScreen(entries: List<ReasonEntry>, now: Long, onBack: () -> Unit) {
    val newestFirst = entries.sortedByDescending { it.at }
    val weekAgo = now - 7 * 24 * 60 * 60 * 1000L
    val week = entries.filter { it.at >= weekAgo }
    val weekUsed = week.sumOf { usedMs(it, now) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(ScrFixed.wallTop, ScrFixed.wallBottom))),
    ) {
        Column(Modifier.safeDrawingPadding().padding(horizontal = 18.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "‹ 뒤로",
                    style = t(15.sp, ScrFixed.accent, FontWeight.Bold),
                    modifier = Modifier.clickable(onClick = onBack).padding(vertical = 8.dp, horizontal = 4.dp),
                )
            }
            Text("해제 기록", style = t(22.sp, weight = FontWeight.Bold))
            Text("이 기록은 폰 안에만 있어요", style = t(13.sp, ScrFixed.dim))
            Spacer(Modifier.height(14.dp))

            // 최근 7일 요약
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x335AB0FF))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Stat("최근 7일 해제", "${week.size}번")
                Stat("스마트폰 사용", minutesText(weekUsed))
                Stat("일찍 돌아옴", "${week.count { isEarly(it) }}번")
            }
            Spacer(Modifier.height(14.dp))

            if (newestFirst.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("아직 해제한 적이 없어요", style = t(15.sp, ScrFixed.dim))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(newestFirst, key = { it.at }) { e -> EntryCard(e, now) }
                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun EntryCard(e: ReasonEntry, now: Long) {
    val ongoing = e.endedAt == null && now < e.plannedEnd
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0x1FFFFFFF))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                SimpleDateFormat("M월 d일 (E) a h:mm", Locale.KOREA).format(Date(e.at)),
                style = t(14.sp, weight = FontWeight.Bold),
                modifier = Modifier.weight(1f),
            )
            Text(methodLabel(e.method), style = t(12.sp, ScrFixed.dim))
        }
        Text("“${e.reason}”", style = t(15.sp))
        Row {
            Text("정한 시간 ${durationLabel(e.minutes)}", style = t(13.sp, ScrFixed.dim))
            Spacer(Modifier.width(10.dp))
            val used = when {
                ongoing -> "지금 사용 중"
                isEarly(e) -> "실제 ${minutesText(usedMs(e, now))} · 일찍 돌아옴"
                else -> "끝까지 사용"
            }
            Text(used, style = t(13.sp, if (isEarly(e)) Color(0xFF7CE38B) else ScrFixed.dim, FontWeight.Bold))
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = t(18.sp, weight = FontWeight.Bold))
        Text(label, style = t(12.sp, ScrFixed.dim))
    }
}

private fun t(size: TextUnit, color: Color = ScrFixed.text, weight: FontWeight = FontWeight.Normal) =
    TextStyle(fontSize = size, color = color, fontWeight = weight)

/** 실제로 스마트폰 모드였던 시간 */
internal fun usedMs(e: ReasonEntry, now: Long): Long {
    val end = e.endedAt ?: minOf(now, e.plannedEnd)
    return (end - e.at).coerceAtLeast(0L)
}

/** 정한 시간보다 1분 넘게 일찍 끝났으면 일찍 돌아온 것 */
internal fun isEarly(e: ReasonEntry): Boolean = e.endedAt != null && e.plannedEnd - e.endedAt > 60_000L

private fun minutesText(ms: Long): String = durationLabel((ms / 60_000L).toInt())

private fun durationLabel(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return listOfNotNull(if (h > 0) "${h}시간" else null, if (m > 0 || h == 0) "${m}분" else null).joinToString(" ")
}

private fun methodLabel(method: String): String =
    runCatching { UnlockMethod.valueOf(method).label }.getOrDefault(method)

// ---------------- 미리보기용 예시 ----------------

internal fun previewHistory(now: Long): List<ReasonEntry> {
    val h = 60 * 60 * 1000L
    return listOf(
        ReasonEntry(now - 50 * h, 30, "FREE", "은행 앱으로 송금해야 해요", endedAt = now - 50 * h + 12 * 60_000L),
        ReasonEntry(now - 26 * h, 60, "WAIT", "회사 메일 확인", endedAt = now - 25 * h),
        ReasonEntry(now - 3 * h, 120, "FREE", "지도 보면서 처음 가는 곳 찾기", endedAt = now - 3 * h + 40 * 60_000L),
        ReasonEntry(now - 20 * 60_000L, 30, "WAIT", "사진 정리하고 보내기"),
    )
}
