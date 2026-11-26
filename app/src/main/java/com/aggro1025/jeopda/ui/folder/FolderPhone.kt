package com.aggro1025.jeopda.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.ui.Lcd
import com.aggro1025.jeopda.ui.LcdText

/** 폴더폰 화면(디스플레이)에 뜨는 화면들 */
sealed interface Screen {
    data object Home : Screen
    data class Menu(val selected: Int = 1) : Screen
    data class UnlockMethod(val selected: Int = 0) : Screen
    data class UnlockInput(val method: com.aggro1025.jeopda.session.UnlockMethod) : Screen
}

data class SoftLabels(val left: String = "", val center: String = "", val right: String = "")

/**
 * 전체 화면: 위쪽 약 55%는 디스플레이, 아래쪽 약 45%는 폴더폰 버튼부.
 * 키보드(사유 입력)가 올라오면 버튼부를 숨기고 디스플레이가 남은 공간을 쓴다.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderPhone(
    soft: SoftLabels,
    onKey: (FolderKey) -> Unit,
    display: @Composable () -> Unit,
) {
    val imeVisible = WindowInsets.isImeVisible
    Column(
        Modifier
            .fillMaxSize()
            .background(Lcd.body)
            .safeDrawingPadding(),
    ) {
        Column(
            Modifier
                .weight(if (imeVisible) 1f else 0.55f)
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Lcd.screen),
        ) {
            Box(Modifier.weight(1f).fillMaxWidth()) { display() }
            if (!imeVisible) SoftKeyBar(soft)
        }
        if (!imeVisible) {
            Keypad(onKey = onKey, modifier = Modifier.weight(0.45f))
        }
    }
}

@Composable
private fun SoftKeyBar(soft: SoftLabels) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(Lcd.text.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val style = LcdText.copy(fontSize = 15.sp)
        Text(soft.left, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
        Text(soft.center, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
        Text(soft.right, style = style, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}
