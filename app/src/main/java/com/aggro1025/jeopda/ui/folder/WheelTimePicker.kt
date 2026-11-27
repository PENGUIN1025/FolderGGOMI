package com.aggro1025.jeopda.ui.folder

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.aggro1025.jeopda.data.UnlockRules
import com.aggro1025.jeopda.ui.Scr

private val ITEM_HEIGHT = 46.dp
private const val VISIBLE = 5

/** 시계 가운데를 누르면 뜨는 창: 시간·분 숫자를 위아래로 밀어 1분 단위로 고른다 */
@Composable
fun WheelTimeDialog(minutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        WheelTimePanel(minutes, onDismiss, onConfirm)
    }
}

@Composable
fun WheelTimePanel(minutes: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var h by remember { mutableIntStateOf(minutes / 60) }
    var m by remember { mutableIntStateOf(minutes % 60) }
    val total = (if (h >= 12) 12 * 60 else h * 60 + m).coerceIn(UnlockRules.MIN_MINUTES, UnlockRules.MAX_MINUTES)

    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Scr.wallTop, Scr.wallBottom)))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("시간 직접 정하기", style = scrText(18.sp, weight = FontWeight.Bold))
        Text("숫자를 위아래로 밀어서 1분 단위로 골라요", style = scrText(12.sp, Scr.dim))
        Spacer(Modifier.height(14.dp))
        Box(contentAlignment = Alignment.Center) {
            // 가운데 선택 띠
            Box(
                Modifier
                    .width(240.dp)
                    .height(ITEM_HEIGHT)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0x335AB0FF))
                    .border(1.dp, Color(0x805AB0FF), RoundedCornerShape(10.dp)),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberWheel(0..12, h) { h = it }
                Text("시간", style = scrText(15.sp, Scr.dim), modifier = Modifier.padding(start = 2.dp, end = 18.dp))
                NumberWheel(0..59, m) { m = it }
                Text("분", style = scrText(15.sp, Scr.dim), modifier = Modifier.padding(start = 2.dp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (h >= 12 && m > 0) "최대 12시간이에요" else if (h * 60 + m < UnlockRules.MIN_MINUTES) "최소 ${UnlockRules.MIN_MINUTES}분이에요" else durationText(total),
            style = scrText(14.sp, if (h >= 12 && m > 0 || h * 60 + m < UnlockRules.MIN_MINUTES) Scr.warn else Scr.text),
        )
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PanelButton("취소", primary = false, onClick = onDismiss)
            PanelButton("확인", primary = true) { onConfirm(total) }
        }
    }
}

@Composable
private fun PanelButton(text: String, primary: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .width(110.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (primary) Brush.verticalGradient(listOf(Scr.accent, Scr.accentDeep))
                else Brush.verticalGradient(listOf(Scr.panel, Scr.panel)),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = scrText(15.sp, weight = FontWeight.Bold)) }
}

/** 숫자 바퀴: 밀면 한 칸씩 딱 맞춰 멈추고, 바뀔 때마다 짧게 진동 */
@Composable
private fun NumberWheel(range: IntRange, value: Int, onChange: (Int) -> Unit) {
    val values = range.toList()
    val pad = VISIBLE / 2
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (value - range.first).coerceIn(0, values.lastIndex))
    val itemPx = with(LocalDensity.current) { ITEM_HEIGHT.toPx() }
    val view = LocalView.current
    val selected by remember {
        derivedStateOf {
            val idx = state.firstVisibleItemIndex + if (state.firstVisibleItemScrollOffset > itemPx / 2) 1 else 0
            values[idx.coerceIn(0, values.lastIndex)]
        }
    }
    LaunchedEffect(state) {
        snapshotFlow { selected }.collect {
            if (it != value) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            onChange(it)
        }
    }
    LazyColumn(
        state = state,
        flingBehavior = rememberSnapFlingBehavior(state),
        modifier = Modifier.width(64.dp).height(ITEM_HEIGHT * VISIBLE),
        contentPadding = PaddingValues(vertical = ITEM_HEIGHT * pad),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        itemsIndexed(values) { _, v ->
            val dist = kotlin.math.abs(v - selected)
            Box(Modifier.height(ITEM_HEIGHT).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    "$v",
                    style = scrText(
                        if (dist == 0) 30.sp else if (dist == 1) 22.sp else 17.sp,
                        Scr.text.copy(alpha = if (dist == 0) 1f else if (dist == 1) 0.45f else 0.2f),
                        if (dist == 0) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
            }
        }
    }
}
