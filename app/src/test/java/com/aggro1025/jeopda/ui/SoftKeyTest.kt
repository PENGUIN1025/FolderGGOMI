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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.aggro1025.jeopda.ui.folder.FolderApp
import com.aggro1025.jeopda.ui.folder.KeyRouter
import com.aggro1025.jeopda.ui.folder.PreviewActions
import com.aggro1025.jeopda.ui.folder.Screen
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
                JeopdaTheme { FolderApp(screen, { screen = it }, previewMenu, PreviewActions, KeyRouter(), devUnlock = true) }
            }
        }
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
    fun okKeyOpensMenu() {
        launch()
        compose.onNodeWithText("확인").performClick()
        compose.mainClock.advanceTimeBy(100)
        compose.onNodeWithText("열기").assertIsDisplayed()
    }
}
