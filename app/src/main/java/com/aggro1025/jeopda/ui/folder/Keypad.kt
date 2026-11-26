package com.aggro1025.jeopda.ui.folder

import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.ui.Galmuri
import com.aggro1025.jeopda.ui.Lcd

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

private val SUB_LABELS = mapOf(
    2 to "ABC", 3 to "DEF", 4 to "GHI", 5 to "JKL",
    6 to "MNO", 7 to "PQRS", 8 to "TUV", 9 to "WXYZ",
)

/** 화면 아래쪽 폴더폰 버튼부: 소프트키·방향키·통화/종료 + 숫자 12키 */
@Composable
fun Keypad(onKey: (FolderKey) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Lcd.body)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        // 네비게이션부 (전체 높이의 약 36%)
        Row(Modifier.fillMaxWidth().weight(0.36f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyButton(FolderKey.SOFT_LEFT, onKey, Modifier.weight(1f)) { Bar() }
                KeyButton(FolderKey.CALL, onKey, Modifier.weight(1f), bg = Lcd.call) { KeyLabel("통화") }
            }
            DPad(onKey, Modifier.weight(1.25f).fillMaxHeight())
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                KeyButton(FolderKey.SOFT_RIGHT, onKey, Modifier.weight(1f)) { Bar() }
                KeyButton(FolderKey.END, onKey, Modifier.weight(1f), bg = Lcd.end) { KeyLabel("종료") }
            }
        }
        // 숫자부
        val rows = listOf(
            listOf(FolderKey.D1, FolderKey.D2, FolderKey.D3),
            listOf(FolderKey.D4, FolderKey.D5, FolderKey.D6),
            listOf(FolderKey.D7, FolderKey.D8, FolderKey.D9),
            listOf(FolderKey.STAR, FolderKey.D0, FolderKey.HASH),
        )
        Column(Modifier.fillMaxWidth().weight(0.64f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            rows.forEach { row ->
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { k -> NumberKey(k, onKey) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.NumberKey(k: FolderKey, onKey: (FolderKey) -> Unit) {
    KeyButton(k, onKey, Modifier.weight(1f).fillMaxHeight()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text(
                text = k.char.toString(),
                fontFamily = Galmuri,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = Lcd.keyText,
            )
            val sub = k.digit?.let { SUB_LABELS[it] }
            if (sub != null) {
                Text(
                    text = " $sub",
                    fontFamily = Galmuri,
                    fontSize = 10.sp,
                    color = Lcd.keySub,
                )
            }
        }
    }
}

@Composable
private fun DPad(onKey: (FolderKey) -> Unit, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(Lcd.bodyEdge),
    ) {
        Column(Modifier.fillMaxSize().padding(4.dp)) {
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Spacer1()
                KeyButton(FolderKey.UP, onKey, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Arrow("▲") }
                Spacer1()
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                KeyButton(FolderKey.LEFT, onKey, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Arrow("◀") }
                KeyButton(FolderKey.OK, onKey, Modifier.weight(1f).padding(2.dp), shape = CircleShape, bg = Lcd.keyPressed) {
                    KeyLabel("확인")
                }
                KeyButton(FolderKey.RIGHT, onKey, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Arrow("▶") }
            }
            Row(Modifier.weight(1f).fillMaxWidth()) {
                Spacer1()
                KeyButton(FolderKey.DOWN, onKey, Modifier.weight(1f), shape = RoundedCornerShape(10.dp)) { Arrow("▼") }
                Spacer1()
            }
        }
    }
}

@Composable
private fun RowScope.Spacer1() = Box(Modifier.weight(1f))

@Composable
private fun KeyButton(
    key: FolderKey,
    onKey: (FolderKey) -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
    bg: Color = Lcd.key,
    content: @Composable () -> Unit,
) {
    val view = LocalView.current
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    Box(
        modifier
            .fillMaxSize()
            .clip(shape)
            .background(if (pressed) Lcd.keyPressed else bg)
            .clickable(interactionSource = source, indication = null) {
                view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                onKey(key)
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun KeyLabel(text: String) =
    Text(text, fontFamily = Galmuri, fontSize = 13.sp, color = Lcd.keyText)

@Composable
private fun Arrow(text: String) =
    Text(text, fontSize = 12.sp, color = Lcd.keySub)

/** 소프트키 표시용 짧은 막대 (기능 이름은 화면 맨 아래에 표시) */
@Composable
private fun Bar() = Box(
    Modifier
        .fillMaxWidth(0.4f)
        .padding(vertical = 0.dp)
        .clip(RoundedCornerShape(2.dp))
        .background(Lcd.keySub)
        .padding(vertical = 1.5.dp),
)
