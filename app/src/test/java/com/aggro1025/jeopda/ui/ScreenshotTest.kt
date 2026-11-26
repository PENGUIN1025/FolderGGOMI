package com.aggro1025.jeopda.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
    @Test fun menu() = shoot("2_menu", Screen.Menu(selected = 6))
    @Test fun unlockMethod() = shoot("3_unlock_method", Screen.UnlockMethod())
    @Test fun unlockInput() = shoot("4_unlock_input", Screen.UnlockInput(UnlockMethod.FREE))

    // 시안: 폴더폰 화면 안의 전화·연락처
    @Test fun mockDialer() = shootContent("5_mock_dialer") { com.aggro1025.jeopda.ui.folder.MockDialer() }
    @Test fun mockContacts() = shootContent("6_mock_contacts") { com.aggro1025.jeopda.ui.folder.MockContacts() }
    @Test fun mockContactsFiltered() = shootContent("7_mock_contacts_filtered") { com.aggro1025.jeopda.ui.folder.MockContactsFiltered() }
}

