package com.aggro1025.jeopda.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import com.aggro1025.jeopda.data.AppPrefs

/**
 * 폴더폰 디자인 테마. 화면과 버튼부를 한 세트로 바꾼다.
 * 유료 테마는 한 번 사면 계속 쓰는 인앱 상품(소비하지 않음)이고, 같은 구글 계정이면 다시 설치해도 복원된다.
 */
enum class FolderTheme(val id: String, val label: String, val productId: String?, val fallbackPrice: String?, val note: String) {
    CLASSIC("classic", "기본", null, null, "2000년대 컬러 폴더폰"),
    CLOUD_CAT("cloud_cat", "구름 고양이", "theme_cloud_cat", "₩4,900", "흰 종이에 펜으로 그린 폴더폰. 달빛 하늘과 구름 위 고양이가 움직여요");

    val paid: Boolean get() = productId != null

    companion object {
        fun of(id: String?): FolderTheme = entries.firstOrNull { it.id == id } ?: CLASSIC

        /** 지금 적용할 테마: 고른 테마가 유료인데 산 기록이 없으면 기본 */
        fun current(prefs: AppPrefs): FolderTheme {
            val t = of(prefs.themeId)
            return if (!t.paid || t.productId in prefs.ownedProducts) t else CLASSIC
        }

        fun owned(prefs: AppPrefs, t: FolderTheme) = !t.paid || t.productId in prefs.ownedProducts
    }
}

val LocalFolderTheme = staticCompositionLocalOf { FolderTheme.CLASSIC }

/** 테마와 그 테마의 화면 색을 함께 적용 */
@androidx.compose.runtime.Composable
fun ProvideFolderTheme(theme: FolderTheme, content: @androidx.compose.runtime.Composable () -> Unit) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalFolderTheme provides theme,
        com.aggro1025.jeopda.ui.LocalScr provides if (theme == FolderTheme.CLOUD_CAT) com.aggro1025.jeopda.ui.CloudCatScr else com.aggro1025.jeopda.ui.ClassicScr,
        content = content,
    )
}
