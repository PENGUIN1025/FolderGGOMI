package com.aggro1025.jeopda.ui.folder

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.data.UnlockRules
import com.aggro1025.jeopda.ui.Scr
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/**
 * 시계형 시간 고르기. 바늘 손잡이를 돌리면 5분 단위로 걸리며 진동하고, 한 바퀴가 12시간(최대값)이다.
 * 12시 방향을 넘어 0으로 튀거나 12시간에서 넘어가지 않게 막는다.
 */
@Composable
fun DialPicker(
    minutes: Int,
    onChange: (Int) -> Unit,
    size: Dp = 230.dp,
) {
    val view = LocalView.current
    val current by rememberUpdatedState(minutes)
    val change by rememberUpdatedState(onChange)
    var showWheel by remember { mutableStateOf(false) }

    /** 가운데(시간 글씨 부분)를 누르면 1분 단위 창, 바깥을 누르거나 돌리면 시계판 */
    fun inCenter(offset: Offset, center: Offset, radius: Float): Boolean {
        val dx = offset.x - center.x
        val dy = offset.y - center.y
        return dx * dx + dy * dy < (radius * 0.55f) * (radius * 0.55f)
    }

    fun update(offset: Offset, center: Offset) {
        val angle = Math.toDegrees(atan2((offset.x - center.x).toDouble(), (center.y - offset.y).toDouble()))
            .let { if (it < 0) it + 360 else it }
        var m = (angle / 360.0 * UnlockRules.MAX_MINUTES).toInt()
        m = (m + UnlockRules.MINUTE_STEP / 2) / UnlockRules.MINUTE_STEP * UnlockRules.MINUTE_STEP
        // 12시 방향을 넘는 순간 반대쪽 끝으로 튀지 않게
        val prev = current
        if (prev > UnlockRules.MAX_MINUTES * 3 / 4 && m < UnlockRules.MAX_MINUTES / 4) m = UnlockRules.MAX_MINUTES
        if (prev < UnlockRules.MAX_MINUTES / 4 && m > UnlockRules.MAX_MINUTES * 3 / 4) m = UnlockRules.MIN_MINUTES
        m = m.coerceIn(UnlockRules.MIN_MINUTES, UnlockRules.MAX_MINUTES)
        if (m != prev) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            change(m)
        }
    }

    Box(
        Modifier
            .size(size)
            .pointerInput(Unit) {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = this.size.width / 2f
                var fromCenter = false
                detectDragGestures(onDragStart = {
                    fromCenter = inCenter(it, c, r)
                    if (!fromCenter) update(it, c)
                }) { change, _ -> if (!fromCenter) update(change.position, c) }
            }
            .pointerInput(Unit) {
                val c = Offset(this.size.width / 2f, this.size.height / 2f)
                val r = this.size.width / 2f
                detectTapGestures { if (inCenter(it, c, r)) showWheel = true else update(it, c) }
            },
        contentAlignment = Alignment.Center,
    ) {
        DialFace(minutes, Modifier.size(size))
        if (showWheel) {
            WheelTimeDialog(
                minutes,
                onDismiss = { showWheel = false },
                onConfirm = {
                    showWheel = false
                    change(it)
                },
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(durationText(minutes), style = scrText(26.sp, weight = FontWeight.Bold))
            Text("눌러서 1분 단위로", style = scrText(11.sp, Scr.accent))
            Text(
                SimpleDateFormat("a h:mm", Locale.KOREA).format(Date(System.currentTimeMillis() + minutes * 60_000L)) + "까지",
                style = scrText(13.sp, Scr.dim),
            )
        }
    }
}

@Composable
private fun DialFace(minutes: Int, modifier: Modifier) {
    Canvas(modifier) {
        val r = this.size.minDimension / 2f
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val ring = r * 0.80f
        val stroke = r * 0.10f
        drawCircle(Color(0x33000000), radius = r * 0.92f, center = c)
        drawCircle(Color(0x26FFFFFF), radius = ring, center = c, style = Stroke(stroke))
        val sweep = minutes / UnlockRules.MAX_MINUTES.toFloat() * 360f
        drawArc(
            brush = Brush.sweepGradient(listOf(Scr.accentDeep, Scr.accent, Scr.accentDeep), c),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - ring, c.y - ring),
            size = Size(ring * 2, ring * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        // 1시간 눈금 12개(굵게) + 30분 눈금
        for (i in 0 until 24) {
            val a = Math.toRadians(i * 15.0 - 90.0)
            val major = i % 2 == 0
            val r1 = r * (if (major) 0.92f else 0.93f)
            val r2 = r * (if (major) 0.99f else 0.96f)
            drawLine(
                if (major) Color(0xB3FFFFFF) else Color(0x4DFFFFFF),
                Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                strokeWidth = if (major) 3f else 2f,
            )
        }
        val a = Math.toRadians(sweep - 90.0)
        val knob = Offset(c.x + ring * cos(a).toFloat(), c.y + ring * sin(a).toFloat())
        drawCircle(Color.White, radius = stroke * 0.95f, center = knob)
        drawCircle(Scr.accentDeep, radius = stroke * 0.45f, center = knob)
    }
}
