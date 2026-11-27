package com.aggro1025.jeopda.ui.folder

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.ui.Scr

/** 기다리는 동안 보여 주는 문구 (모두 직접 쓴 문구) */
val MINDFUL_LINES = listOf(
    "뇌 건강을 회복하는 중",
    "머리를 쉬게 하는 중",
    "집중력을 되찾는 중",
    "충동이 지나가길 기다리는 중",
    "스크롤 습관을 끊어 내는 중",
    "눈을 쉬게 하는 중",
    "생각을 정리하는 중",
    "지금 이 순간에 머무는 중",
    "진짜 필요한 일만 고르는 중",
    "나를 위한 1분을 쓰는 중",
)

/** 숨쉬기 원: 4초 동안 커지며 "들이쉬고", 4초 동안 작아지며 "내쉬고" */
@Composable
fun BreathingCircle(size: Dp = 170.dp) {
    val inspection = LocalInspectionMode.current
    val t = rememberInfiniteTransition(label = "breath")
    val phase by t.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(8_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val p = if (inspection) 0.35f else phase
    val inhale = p < 0.5f
    // 0→0.5 커짐, 0.5→1 작아짐 (부드럽게)
    val k = if (inhale) p / 0.5f else 1f - (p - 0.5f) / 0.5f
    val eased = (1 - kotlin.math.cos(k * Math.PI).toFloat()) / 2f
    val scale = 0.55f + 0.45f * eased

    Box(Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size)) {
            val r = this.size.minDimension / 2f
            val c = Offset(this.size.width / 2f, this.size.height / 2f)
            drawCircle(Color(0x1FFFFFFF), radius = r, center = c, style = Stroke(1.5.dp.toPx()))
            drawCircle(
                Brush.radialGradient(listOf(Scr.accent.copy(alpha = 0.55f), Scr.accentDeep.copy(alpha = 0.15f)), c, r * scale),
                radius = r * scale,
                center = c,
            )
        }
        Text(if (inhale) "들이쉬고" else "내쉬고", style = scrText(17.sp, weight = FontWeight.Bold))
    }
}
