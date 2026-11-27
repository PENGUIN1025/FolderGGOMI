package com.aggro1025.jeopda.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.ui.folder.FolderPhone
import com.aggro1025.jeopda.ui.folder.SoftLabels
import com.aggro1025.jeopda.ui.folder.scrText
import kotlin.math.cos
import kotlin.math.sin

/** 시안: 해제 시간 고르기 두 가지 (시계형, 숫자 드래그형). 앱에는 들어가지 않는 테스트용 그림 */

@Composable
private fun ReasonStub() {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("왜 스마트폰이 필요한가요?", style = scrText(14.sp, Scr.dim), modifier = Modifier.weight(1f))
            Text("0/6", style = scrText(13.sp, Scr.dim, FontWeight.Bold))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0x59000000))
                .border(1.dp, Scr.dim.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(10.dp),
        ) { Text("6글자 이상 적어 주세요", style = scrText(15.sp, Scr.faint)) }
    }
}

// ---------------- 시계형 ----------------

@Composable
fun MockDialPicker(minutes: Int) {
    FolderPhone("스마트폰 모드 해제 · 무료", SoftLabels("해제", "", "취소"), {}) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text("스마트폰 모드를 얼마 동안 쓸까요?", style = scrText(14.sp, Scr.dim))
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Dial(minutes, Modifier.fillMaxHeight(0.96f).aspectRatio(1f, matchHeightConstraintsFirst = true))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "${minutes / 60}시간 ${minutes % 60}분",
                        style = scrText(26.sp, weight = FontWeight.Bold),
                    )
                    Text("오후 4:40까지", style = scrText(13.sp, Scr.dim))
                }
            }
            Text(
                "바늘을 돌려서 정해요 · 한 바퀴 = 12시간",
                style = scrText(11.sp, Scr.faint),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
            )
            ReasonStub()
        }
    }
}

@Composable
private fun Dial(minutes: Int, modifier: Modifier) {
    Canvas(modifier) {
        val r = size.minDimension / 2f
        val c = Offset(size.width / 2f, size.height / 2f)
        val ring = r * 0.80f
        val stroke = r * 0.10f
        // 바탕 원
        drawCircle(Color(0x33000000), radius = r * 0.92f, center = c)
        drawCircle(Color(0x26FFFFFF), radius = ring, center = c, style = Stroke(stroke))
        // 채워진 호 (12시 방향부터 시계방향)
        val sweep = minutes / 720f * 360f
        drawArc(
            brush = Brush.sweepGradient(listOf(Scr.accentDeep, Scr.accent, Scr.accentDeep), c),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(c.x - ring, c.y - ring),
            size = Size(ring * 2, ring * 2),
            style = Stroke(stroke, cap = StrokeCap.Round),
        )
        // 시간 눈금 12개 + 30분 눈금
        for (i in 0 until 24) {
            val a = Math.toRadians(i * 15.0 - 90.0)
            val major = i % 2 == 0
            val r1 = r * (if (major) 0.92f else 0.93f)
            val r2 = r * (if (major) 0.98f else 0.96f)
            drawLine(
                if (major) Color(0xB3FFFFFF) else Color(0x4DFFFFFF),
                Offset(c.x + r1 * cos(a).toFloat(), c.y + r1 * sin(a).toFloat()),
                Offset(c.x + r2 * cos(a).toFloat(), c.y + r2 * sin(a).toFloat()),
                strokeWidth = if (major) 3f else 2f,
            )
        }
        // 바늘 끝 손잡이
        val a = Math.toRadians(sweep - 90.0)
        val knob = Offset(c.x + ring * cos(a).toFloat(), c.y + ring * sin(a).toFloat())
        drawCircle(Color.White, radius = stroke * 0.9f, center = knob)
        drawCircle(Scr.accentDeep, radius = stroke * 0.45f, center = knob)
    }
}

// ---------------- 숫자 드래그형 ----------------

@Composable
fun MockWheelPicker(hours: Int, minutes: Int) {
    FolderPhone("스마트폰 모드 해제 · 무료", SoftLabels("해제", "", "취소"), {}) {
        Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 6.dp)) {
            Text("스마트폰 모드를 얼마 동안 쓸까요? (최대 12시간)", style = scrText(14.sp, Scr.dim))
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0x40000000)),
                contentAlignment = Alignment.Center,
            ) {
                // 가운데 선택 띠
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0x335AB0FF))
                        .border(1.dp, Color(0x805AB0FF), RoundedCornerShape(10.dp)),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Wheel((0..12).toList(), hours, "시간")
                    Spacer(Modifier.width(18.dp))
                    Wheel((0..55 step 5).toList(), minutes, "분")
                }
            }
            Text(
                "숫자를 위아래로 밀어서 정해요 · 오후 4:40까지",
                style = scrText(11.sp, Scr.faint),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            )
            ReasonStub()
        }
    }
}

@Composable
private fun Wheel(values: List<Int>, selected: Int, unit: String) {
    val idx = values.indexOf(selected)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.width(64.dp)) {
            for (d in -2..2) {
                val v = values.getOrNull(idx + d)
                val dist = kotlin.math.abs(d)
                Text(
                    v?.toString() ?: "",
                    style = scrText(
                        if (dist == 0) 34.sp else if (dist == 1) 24.sp else 18.sp,
                        Scr.text.copy(alpha = if (dist == 0) 1f else if (dist == 1) 0.45f else 0.2f),
                        if (dist == 0) FontWeight.Bold else FontWeight.Normal,
                    ),
                    modifier = Modifier.height(48.dp).padding(top = if (dist == 0) 2.dp else 10.dp),
                )
            }
        }
        Text(unit, style = scrText(16.sp, Scr.dim), modifier = Modifier.padding(start = 6.dp))
    }
}
