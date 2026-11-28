package com.aggro1025.jeopda.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.aggro1025.jeopda.session.UnlockMethod
import com.aggro1025.jeopda.ui.folder.PreviewScreen
import com.aggro1025.jeopda.ui.folder.Screen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * 폴더폰 화면을 이미지로 저장한다 (S23 Ultra와 비슷한 화면 크기).
 * ./gradlew testDebugUnitTest 실행 후 app/build/screenshots/ 에 PNG가 생긴다.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class ScreenshotTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun shoot(name: String, screen: Screen) = shootContent(name) { PreviewScreen(screen) }

    private fun shootContent(name: String, content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalInspectionMode provides true) {
                JeopdaTheme { content() }
            }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val dir = File("build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    @Test fun home() = shoot("1_home", Screen.Home)

    // 유료 테마: 구름 고양이
    private fun shootCat(name: String, screen: Screen) = shootContent(name) {
        androidx.compose.runtime.CompositionLocalProvider(
            com.aggro1025.jeopda.ui.theme.LocalFolderTheme provides com.aggro1025.jeopda.ui.theme.FolderTheme.CLOUD_CAT,
        ) { PreviewScreen(screen) }
    }
    @Test fun catHome() = shootCat("20_cat_home", Screen.Home)
    @Test fun themeScreen() = shootContent("25_theme_screen") {
        val T = com.aggro1025.jeopda.ui.theme.FolderTheme.entries
        com.aggro1025.jeopda.ui.settings.ThemeScreen(
            items = listOf(
                com.aggro1025.jeopda.ui.settings.ThemeItem(T[0], owned = true, applied = false, price = null),
                com.aggro1025.jeopda.ui.settings.ThemeItem(T[1], owned = false, applied = false, price = "₩4,900"),
            ).let { l -> l.map { it } },
            busy = false, onBack = {}, onApply = {}, onBuy = {}, onRestore = {},
        )
    }
    @Test fun catMenu() = shootCat("21_cat_menu", Screen.Menu(selected = 6))
    @Test fun catUnlockMethod() = shootCat("22_cat_unlock_method", Screen.UnlockMethod())
    @Test fun catDialer() = shootCat("23_cat_dialer", Screen.Dialer("01012345678"))
    @Test @Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
    fun catHomeSmall() = shootCat("24_cat_home_small", Screen.Home)
    @Test fun menu() = shoot("2_menu", Screen.Menu(selected = 6))
    @Test fun unlockMethod() = shoot("3_unlock_method", Screen.UnlockMethod())
    @Test fun unlockInput() = shoot("4_unlock_input", Screen.UnlockInput(UnlockMethod.FREE))
    @Test fun unlockInputReason() = shoot("4c_unlock_reason", Screen.UnlockInput(UnlockMethod.FREE, minutes = 90, step = 1))

    // 폴더폰 화면 안의 전화·연락처
    @Test fun dialer() = shoot("5_dialer", Screen.Dialer("01012345678"))
    @Test fun contacts() = shoot("6_contacts", Screen.Contacts(selected = 5))
    @Test fun contactsFiltered() = shoot("7_contacts_filtered", Screen.Contacts(keys = "4", selected = 1))

    // 세로가 짧은 폰 (메뉴 이름이 잘리지 않는지)
    @Test @Config(sdk = [35], qualifiers = "w360dp-h640dp-xxhdpi")
    fun menuSmallScreen() = shoot("8_menu_small_screen", Screen.Menu(selected = 6))

    @Test fun unlockWait() = shoot("10_unlock_wait", Screen.UnlockWait(30, "회사 메일 확인해야 해요", adFailed = true))
    @Test fun wheelPanel() = shootContent("14_wheel_time") {
        androidx.compose.foundation.layout.Box(
            androidx.compose.ui.Modifier.fillMaxSize().background(Lcd.body),
            contentAlignment = androidx.compose.ui.Alignment.Center,
        ) { com.aggro1025.jeopda.ui.folder.WheelTimePanel(127, {}, {}) }
    }
    @Test fun blocked() = shoot("13_blocked", Screen.Blocked("인스타그램"))
    @Test fun unlockAd() = shoot("12_unlock_ad", Screen.UnlockAd(30, "회사 메일 확인해야 해요"))

    @Test fun history() = shootContent("11_history") {
        val now = System.currentTimeMillis()
        com.aggro1025.jeopda.ui.settings.HistoryScreen(com.aggro1025.jeopda.ui.settings.previewHistory(now), now, onBack = {})
    }

    // 새 설정 화면
    @Test fun settingsDesign() = shootContent("9_settings_design") {
        com.aggro1025.jeopda.ui.settings.SettingsDesign(com.aggro1025.jeopda.ui.settings.previewSettingsFirstRun)
    }
    @Test fun settingsDesignPage2() = shootContent("9b_settings_apps") {
        com.aggro1025.jeopda.ui.settings.SettingsDesign(com.aggro1025.jeopda.ui.settings.previewSettingsFirstRun, page = 1)
    }


    // 해제 입력 전체 모습 (스크롤 없이 보이게 세로로 긴 화면)
    @Test @Config(sdk = [35], qualifiers = "w384dp-h1180dp-xxhdpi")
    fun unlockInputFull() = shoot("4b_unlock_input_full", Screen.UnlockInput(UnlockMethod.FREE))
}
