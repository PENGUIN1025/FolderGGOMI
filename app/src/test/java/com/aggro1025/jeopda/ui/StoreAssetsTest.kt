package com.aggro1025.jeopda.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aggro1025.jeopda.R
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
 * 구글 플레이 스토어 등록 이미지 (모두 접다 화면을 직접 그린 것).
 * ./gradlew testDebugUnitTest 실행 후 app/build/store/
 * - 휴대전화 스크린샷 1080x2160 (1:2, 구글 플레이 최대 비율)
 * - 앱 아이콘 512x512
 * - 그래픽 이미지 1024x500
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class StoreAssetsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun save(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) { JeopdaTheme { content() } }
        }
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(500)
        val view = compose.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bmp))
        val dir = File("build/store").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // ---- 휴대전화 스크린샷 (출시 빌드처럼 개발용 해제는 숨김) ----
    @Test @Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
    fun shot1Home() = save("phone_1_home") { PreviewScreen(Screen.Home, devUnlock = false) }

    @Test @Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
    fun shot2Menu() = save("phone_2_menu") { PreviewScreen(Screen.Menu(selected = 1), devUnlock = false) }

    @Test @Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
    fun shot3Contacts() = save("phone_3_contacts") { PreviewScreen(Screen.Contacts(keys = "4", selected = 1), devUnlock = false) }

    @Test @Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
    fun shot4Dialer() = save("phone_4_dialer") { PreviewScreen(Screen.Dialer("01012345678"), devUnlock = false) }

    @Test @Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
    fun shot5UnlockMethod() = save("phone_5_unlock_method") { PreviewScreen(Screen.UnlockMethod(), devUnlock = false) }

    @Test @Config(sdk = [35], qualifiers = "w360dp-h720dp-xxhdpi")
    fun shot6UnlockInput() = save("phone_6_unlock_input") { PreviewScreen(Screen.UnlockInput(UnlockMethod.FREE), devUnlock = false) }

    // ---- 앱 아이콘 512x512 (구글 플레이가 모서리를 둥글게 잘라 보여줌) ----
    @Test @Config(sdk = [35], qualifiers = "w512dp-h512dp-mdpi")
    fun icon() = save("icon_512") {
        Box(Modifier.fillMaxSize().background(Lcd.body), contentAlignment = Alignment.Center) {
            Image(
                painterResource(R.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(512.dp).graphicsLayer(scaleX = 1.35f, scaleY = 1.35f),
            )
        }
    }

    // ---- 그래픽 이미지 1024x500 ----
    @Test @Config(sdk = [35], qualifiers = "w1024dp-h500dp-mdpi")
    fun featureGraphic() = save("feature_1024x500") {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(Scr.wallTop, Scr.wallBottom))),
        ) {
            Row(Modifier.fillMaxSize().padding(start = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.width(520.dp)) {
                    Box(
                        Modifier.size(96.dp).clip(RoundedCornerShape(24.dp)).background(Lcd.body),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painterResource(R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(96.dp).graphicsLayer(scaleX = 1.35f, scaleY = 1.35f),
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text("접다", style = TextStyle(fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Scr.text))
                    Spacer(Modifier.height(8.dp))
                    Text("스마트폰을 폴더폰처럼", style = TextStyle(fontSize = 32.sp, color = Scr.text))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "홈 화면은 단순하게, 스마트폰은 꼭 필요할 때만",
                        style = TextStyle(fontSize = 20.sp, color = Scr.dim),
                    )
                }
                // 오른쪽: 실제 홈 화면을 축소해서
                Box(Modifier.size(width = 360.dp, height = 500.dp), contentAlignment = Alignment.Center) {
                    Box(
                        Modifier
                            .requiredSize(360.dp, 640.dp)
                            .graphicsLayer(scaleX = 0.72f, scaleY = 0.72f)
                            .clip(RoundedCornerShape(28.dp)),
                    ) { PreviewScreen(Screen.Home, devUnlock = false) }
                }
            }
        }
    }
}
