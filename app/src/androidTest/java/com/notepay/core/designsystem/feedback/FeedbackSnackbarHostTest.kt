package com.notepay.core.designsystem.feedback

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notepay.core.designsystem.theme.NotePayTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackSnackbarHostTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun queuedStatusKeepsCurrentActionAvailable() {
        var undoCount = 0
        composeRule.setContent {
            val state = remember { SnackbarHostState() }
            NotePayTheme(darkTheme = false) {
                FeedbackSnackbarHost(state)
            }
            LaunchedEffect(state) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    state.showFeedback(
                        UiFeedback(
                            message = "Removed",
                            actionLabel = "Undo",
                            duration = FeedbackDuration.Indefinite,
                            onAction = { undoCount++ },
                        ),
                    )
                }
                launch {
                    state.showFeedback(UiFeedback("Next status"))
                }
            }
        }

        composeRule.onNodeWithText("Removed").assertIsDisplayed()
        composeRule.onNodeWithText("Undo").performClick()
        composeRule.onNodeWithText("Next status").assertIsDisplayed()
        composeRule.runOnIdle { assertEquals(1, undoCount) }
    }
}
