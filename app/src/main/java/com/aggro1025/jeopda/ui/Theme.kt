package com.aggro1025.jeopda.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

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

/** 화면(디스플레이) 팔레트. 테마마다 다르다 */
@androidx.compose.runtime.Immutable
data class ScrPalette(
    val wallTop: Color,
    val wallBottom: Color,
    val text: Color,
    val dim: Color,
    val faint: Color,
    val accent: Color,
    val accentDeep: Color,
    val warn: Color,
    val panel: Color,
    val statusBg: Color,
)

/** 기본: 2000년대 컬러 폴더폰 느낌의 어두운 남색 배경화면 + 흰 글자 */
val ClassicScr = ScrPalette(
    wallTop = Color(0xFF1C2E52),
    wallBottom = Color(0xFF060A13),
    text = Color(0xFFF4F7FB),
    dim = Color(0xFF9FB0C8),
    faint = Color(0xFF5C6B82),
    accent = Color(0xFF5AB0FF),
    accentDeep = Color(0xFF2A6FD6),
    warn = Color(0xFFFFC98A),
    panel = Color(0x33FFFFFF),
    statusBg = Color(0x66000000),
)

/** 구름 고양이: 짙은 밤하늘 + 종이색 글자, 강조는 목도리·소프트키와 같은 빨강 계열 */
val CloudCatScr = ScrPalette(
    wallTop = Color(0xFF3F3388),
    wallBottom = Color(0xFF2B2463),
    text = Color(0xFFFFFDF8),
    dim = Color(0xFFF0ECFF),
    faint = Color(0xFFCFC7F0),
    accent = Color(0xFFF08A76),
    accentDeep = Color(0xFFC8473A),
    warn = Color(0xFFF7E08A),
    panel = Color(0x26FFFFFF),
    statusBg = Color(0x00000000),
)

val LocalScr = androidx.compose.runtime.staticCompositionLocalOf { ClassicScr }

/** 폴더폰 화면 색: 지금 테마의 팔레트를 읽는다 */
object Scr {
    val wallTop: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.wallTop
    val wallBottom: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.wallBottom
    val text: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.text
    val dim: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.dim
    val faint: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.faint
    val accent: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.accent
    val accentDeep: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.accentDeep
    val warn: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.warn
    val panel: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.panel
    val statusBg: Color @Composable @androidx.compose.runtime.ReadOnlyComposable get() = LocalScr.current.statusBg
}

/** 설정 화면처럼 테마와 상관없는 곳에서 쓰는 기본 색 */
val ScrFixed = ClassicScr

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
