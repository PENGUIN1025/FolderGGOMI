package com.aggro1025.jeopda.ui

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aggro1025.jeopda.ui.folder.FolderApp
import com.aggro1025.jeopda.ui.folder.KeyRouter
import com.aggro1025.jeopda.ui.folder.PreviewActions
import com.aggro1025.jeopda.ui.folder.Screen
import com.aggro1025.jeopda.ui.folder.previewContacts
import com.aggro1025.jeopda.ui.folder.previewMenu
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** 화면 아래 소프트키 글자를 눌러도 버튼과 같은 동작을 하는지 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], qualifiers = "w384dp-h832dp-xxhdpi")
class SoftKeyTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun launch() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            var screen by remember { mutableStateOf<Screen>(Screen.Home) }
            CompositionLocalProvider(LocalInspectionMode provides true) {
                JeopdaTheme { FolderApp(screen, { screen = it }, previewMenu, PreviewActions, KeyRouter(), devUnlock = true, contacts = previewContacts) }
            }
        }
        compose.mainClock.advanceTimeBy(100)
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.mainClock.advanceTimeBy(100)
    }

    @Test
    fun tapMenuLabelOpensMenu() {
        launch()
        compose.onNodeWithText("메뉴").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("열기").assertIsDisplayed()
        compose.onNodeWithText("카카오톡").assertIsDisplayed()
    }

    @Test
    fun tapUnlockLabelOpensUnlockMethods() {
        launch()
        compose.onNodeWithText("해제").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("하루 1회 무료").assertIsDisplayed()
    }


    @Test
    fun digitOnHomeOpensDialerWithThatDigit() {
        launch()
        tap("0")
        tap("1")
        compose.onNodeWithText("전화").assertIsDisplayed()
        compose.onNodeWithText("01").assertIsDisplayed()
        tap("지우기")
        compose.onNodeWithText("01").assertDoesNotExist()
    }

    @Test
    fun contactsFilterByChoseongKey() {
        launch()
        // 홈에서 통화 키(수화기 아이콘) → 다이얼러
        compose.onNodeWithContentDescription("통화").performClick()
        compose.mainClock.advanceTimeBy(100)
        tap("연락처")
        compose.onNodeWithText("엄마").assertIsDisplayed()
        tap("4") // ㄱㅋ
        compose.onNodeWithText("강민수").assertIsDisplayed()
        compose.onNodeWithText("(4)", substring = true).assertIsDisplayed() // 찾기 칸 (키패드에도 ㄱㅋ 글자가 있어서 숫자로 확인)
        compose.onNodeWithText("박서준").assertDoesNotExist()
    }
}
