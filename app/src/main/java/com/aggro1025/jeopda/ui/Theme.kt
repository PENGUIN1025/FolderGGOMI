package com.aggro1025.jeopda.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.aggro1025.jeopda.R

/** 임시 디자인: 검은 LCD 화면 + 밝은 연두 글자, 짙은 회색 본체 */
object Lcd {
    val body = Color(0xFF15171A)
    val bodyEdge = Color(0xFF24272C)
    val key = Color(0xFF2B2F35)
    val keyPressed = Color(0xFF3A3F47)
    val keyText = Color(0xFFE6E8EB)
    val keySub = Color(0xFF8A9099)
    val call = Color(0xFF2E7D4F)
    val end = Color(0xFFB0413E)

    val screen = Color(0xFF0B110D)
    val text = Color(0xFFB6F2C4)
    val dim = Color(0xFF5E8A6A)
    val warn = Color(0xFFF2D0A0)
}

/** 화면(디스플레이) 팔레트: 2000년대 컬러 폴더폰 느낌의 어두운 남색 배경화면 + 흰 글자 */
object Scr {
    val wallTop = Color(0xFF1C2E52)
    val wallBottom = Color(0xFF060A13)
    val text = Color(0xFFF4F7FB)
    val dim = Color(0xFF9FB0C8)
    val faint = Color(0xFF5C6B82)
    val accent = Color(0xFF5AB0FF)
    val accentDeep = Color(0xFF2A6FD6)
    val warn = Color(0xFFFFC98A)
    val panel = Color(0x33FFFFFF)
    val statusBg = Color(0x66000000)
}

val Galmuri = FontFamily(
    Font(R.font.galmuri11, FontWeight.Normal),
    Font(R.font.galmuri11_bold, FontWeight.Bold),
)

val LcdText = TextStyle(fontFamily = Galmuri, color = Lcd.text)

@Composable
fun JeopdaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Lcd.text,
            onPrimary = Lcd.screen,
            background = Lcd.body,
            surface = Lcd.body,
            onBackground = Lcd.keyText,
            onSurface = Lcd.keyText,
        ),
        typography = Typography(),
        content = content,
    )
}
