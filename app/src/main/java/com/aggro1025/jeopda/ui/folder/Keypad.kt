package com.aggro1025.jeopda.ui.folder

import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class FolderKey {
    SOFT_LEFT, SOFT_RIGHT, UP, DOWN, LEFT, RIGHT, OK, CALL, END,
    D0, D1, D2, D3, D4, D5, D6, D7, D8, D9, STAR, HASH;

    val digit: Int? get() = if (ordinal in D0.ordinal..D9.ordinal) ordinal - D0.ordinal else null
    val char: Char? get() = digit?.let { '0' + it } ?: when (this) { STAR -> '*'; HASH -> '#'; else -> null }

    companion object {
        fun ofDigit(d: Int): FolderKey = entries[D0.ordinal + d]

        /** 블루투스 키보드 등 물리 키 입력 */
        fun fromKeyCode(code: Int): FolderKey? = when (code) {
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> ofDigit(code - KeyEvent.KEYCODE_0)
            in KeyEvent.KEYCODE_NUMPAD_0..KeyEvent.KEYCODE_NUMPAD_9 -> ofDigit(code - KeyEvent.KEYCODE_NUMPAD_0)
            KeyEvent.KEYCODE_STAR -> STAR
            KeyEvent.KEYCODE_POUND -> HASH
            KeyEvent.KEYCODE_DPAD_UP -> UP
            KeyEvent.KEYCODE_DPAD_DOWN -> DOWN
            KeyEvent.KEYCODE_DPAD_LEFT -> LEFT
            KeyEvent.KEYCODE_DPAD_RIGHT -> RIGHT
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> OK
            KeyEvent.KEYCODE_CALL -> CALL
            KeyEvent.KEYCODE_ENDCALL -> END
            KeyEvent.KEYCODE_SOFT_LEFT -> SOFT_LEFT
            KeyEvent.KEYCODE_SOFT_RIGHT -> SOFT_RIGHT
            else -> null
        }
    }
}

/** 키패드 팔레트: 광택 있는 검은 통판 + 백라이트 글자 */
private object Kp {
    val plateTop = Color(0xFF1E2228)
    val plateBottom = Color(0xFF0B0D10)
    val rim = Color(0xFF353A42)
    val divider = Color(0xFF2A2F37)
    val glow = Color(0xFFEAF4FF)
    val sub = Color(0xFF8CC8FF)
    val soft = Color(0xFF4DA8FF)
    val call = Color(0xFF3DDC84)
    val end = Color(0xFFFF4D4D)
    val pressed = Color(0x1FBFE0FF)
}

/** 천지인 배치 (장식용. 실제 한글 입력은 시스템 키보드) */
private val JAMO = mapOf(
    1 to "ㅣ", 2 to "ㆍ", 3 to "ㅡ",
    4 to "ㄱㅋ", 5 to "ㄴㄹ", 6 to "ㄷㅌ",
    7 to "ㅂㅍ", 8 to "ㅅㅎ", 9 to "ㅈㅊ",
    0 to "ㅇㅁ",
)

private val Backlit = Shadow(Color(0x995AB0FF), Offset.Zero, 10f)

/** 화면 아래쪽 폴더폰 버튼부: 검은 통판 위에 소프트키·방향키·통화/종료 + 숫자 12키 */
@Composable
fun Keypad(onKey: (FolderKey) -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .padding(start = 10.dp, end = 10.dp, bottom = 6.dp),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(Kp.plateTop, Kp.plateBottom)))
                .border(1.dp, Kp.rim, RoundedCornerShape(22.dp)),
        ) {
            NavSection(onKey, Modifier.fillMaxWidth().weight(0.36f))
            HLine()
            NumberGrid(onKey, Modifier.fillMaxWidth().weight(0.64f))
        }
    }
}

// ---------------- 네비게이션부 ----------------

@Composable
private fun NavSection(onKey: (FolderKey) -> Unit, modifier: Modifier) {
    Row(modifier) {
        Column(Modifier.weight(1f).fillMaxHeight()) {
            KeyArea(FolderKey.SOFT_LEFT, onKey, Modifier.weight(1f)) { pressed -> SoftBar(pressed) }
            KeyArea(FolderKey.CALL, onKey, Modifier.weight(1f)) { pressed ->
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "통화",
                    tint = if (pressed) Kp.glow else Kp.call,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
        Box(Modifier.weight(1.2f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            DPad(onKey)
        }
        Column(Modifier.weight(1f).fillMaxHeight()) {
            KeyArea(FolderKey.SOFT_RIGHT, onKey, Modifier.weight(1f)) { pressed -> SoftBar(pressed) }
            KeyArea(FolderKey.END, onKey, Modifier.weight(1f)) { pressed ->
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "종료",
                    tint = if (pressed) Kp.glow else Kp.end,
                    modifier = Modifier.size(28.dp).rotate(135f),
                )
            }
        }
    }
}

/** 빛나는 짧은 파란 막대 */
@Composable
private fun SoftBar(pressed: Boolean) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .width(46.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Kp.soft.copy(alpha = if (pressed) 0.45f else 0.22f)),
        )
        Box(
            Modifier
                .width(34.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (pressed) Kp.glow else Kp.soft),
        )
    }
}

/** 가운데 확인 키를 감싼 고리 모양 방향키 */
@Composable
private fun DPad(onKey: (FolderKey) -> Unit) {
    Box(
        Modifier
            .fillMaxHeight(0.92f)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(Brush.radialGradient(listOf(Color(0xFF262B33), Color(0xFF15181D))))
            .border(1.5.dp, Kp.rim, CircleShape),
    ) {
        Column(Modifier.fillMaxSize()) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Spacer1()
                KeyArea(FolderKey.UP, onKey, Modifier.weight(1f)) { Chevron(0f, it) }
                Spacer1()
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                KeyArea(FolderKey.LEFT, onKey, Modifier.weight(1f)) { Chevron(270f, it) }
                Spacer1()
                KeyArea(FolderKey.RIGHT, onKey, Modifier.weight(1f)) { Chevron(90f, it) }
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Spacer1()
                KeyArea(FolderKey.DOWN, onKey, Modifier.weight(1f)) { Chevron(180f, it) }
                Spacer1()
            }
        }
        // 가운데 확인 키 (고리 안쪽 원)
        KeyArea(
            FolderKey.OK,
            onKey,
            Modifier
                .align(Alignment.Center)
                .fillMaxSize(0.42f)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFF2E343D), Color(0xFF1A1E24))))
                .border(1.dp, Kp.rim, CircleShape),
            shape = CircleShape,
        ) { pressed ->
            Text("확인", style = keyText(13.sp, if (pressed) Kp.glow else Kp.glow.copy(alpha = 0.85f), FontWeight.Medium))
        }
    }
}

@Composable
private fun RowScope.Spacer1() = Box(Modifier.weight(1f))

/** 작은 삼각형 화살표. 누르면 파랗게 빛남 */
@Composable
private fun Chevron(rotation: Float, pressed: Boolean) {
    val color = if (pressed) Kp.soft else Color(0xFF8A929E)
    Canvas(Modifier.size(10.dp).rotate(rotation)) {
        val p = Path().apply {
            moveTo(size.width / 2, 0f)
            lineTo(size.width, size.height * 0.8f)
            lineTo(0f, size.height * 0.8f)
            close()
        }
        drawPath(p, color)
    }
}

// ---------------- 숫자부 ----------------

@Composable
private fun NumberGrid(onKey: (FolderKey) -> Unit, modifier: Modifier) {
    val rows = listOf(
        listOf(FolderKey.D1, FolderKey.D2, FolderKey.D3),
        listOf(FolderKey.D4, FolderKey.D5, FolderKey.D6),
        listOf(FolderKey.D7, FolderKey.D8, FolderKey.D9),
        listOf(FolderKey.STAR, FolderKey.D0, FolderKey.HASH),
    )
    Column(modifier) {
        rows.forEachIndexed { r, row ->
            Row(Modifier.fillMaxWidth().weight(1f)) {
                row.forEachIndexed { c, k ->
                    val lineColor = Kp.divider
                    KeyArea(
                        k,
                        onKey,
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .drawBehind {
                                val w = 1.dp.toPx()
                                // 칸 사이 가는 선 (오른쪽·아래)
                                if (c < 2) drawRect(lineColor, Offset(size.width - w, size.height * 0.12f), androidx.compose.ui.geometry.Size(w, size.height * 0.76f))
                                if (r < 3) drawRect(lineColor, Offset(size.width * 0.08f, size.height - w), androidx.compose.ui.geometry.Size(size.width * 0.84f, w))
                            },
                    ) { pressed -> NumberLabel(k, pressed) }
                }
            }
        }
    }
}

@Composable
private fun NumberLabel(k: FolderKey, pressed: Boolean) {
    val main = if (pressed) Color.White else Kp.glow.copy(alpha = 0.92f)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            k.char.toString(),
            style = keyText(if (k.digit != null) 25.sp else 24.sp, main, FontWeight.Medium),
        )
        val sub = k.digit?.let { JAMO[it] }
        if (sub != null) {
            Text(
                sub,
                style = keyText(12.sp, if (pressed) Kp.glow else Kp.sub, FontWeight.Normal),
                modifier = Modifier.padding(start = 6.dp),
            )
        }
    }
}

private fun keyText(size: androidx.compose.ui.unit.TextUnit, color: Color, weight: FontWeight) =
    TextStyle(fontSize = size, color = color, fontWeight = weight, shadow = Backlit)

// ---------------- 공통: 누르는 영역 ----------------

@Composable
private fun KeyArea(
    key: FolderKey,
    onKey: (FolderKey) -> Unit,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(14.dp),
    content: @Composable BoxScope.(pressed: Boolean) -> Unit,
) {
    val view = LocalView.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (pressed) Kp.pressed else Color.Transparent)
            .clickable(interactionSource = source, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onKey(key)
            },
        contentAlignment = Alignment.Center,
    ) { content(pressed) }
}

@Composable
private fun HLine() = Box(
    Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .height(1.dp)
        .background(Kp.divider),
)
