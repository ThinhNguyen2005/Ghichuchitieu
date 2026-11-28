package com.notepay.core.designsystem.feedback

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notepay.R
import com.notepay.core.designsystem.theme.NotePayTheme
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FeedbackSnackbarHostExtendedTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun successWithViewActionInvokesCallbackAndMeetsTouchTarget() {
        val message = "Đã lưu giao dịch mới"
        val viewLabel = context.getString(R.string.action_view_transaction)
        var actionCount = 0
        var feedbackResult: Boolean? = null

        composeRule.showSingleFeedback(
            feedback = UiFeedback(
                message = message,
                actionLabel = viewLabel,
                type = FeedbackType.Success,
                duration = FeedbackDuration.Indefinite,
                onAction = { actionCount++ },
            ),
            onResult = { feedbackResult = it },
        )

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText(viewLabel)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { feedbackResult != null }
        composeRule.runOnIdle {
            assertEquals(true, feedbackResult)
            assertEquals(1, actionCount)
        }
        composeRule.onNodeWithText(message).assertDoesNotExist()
    }

    @Test
    fun queuedSuccessWithUndoKeepsActionClickableBeforeNextSnackbar() {
        val firstMessage = "Đã xóa giao dịch ăn sáng"
        val secondMessage = "Đã cập nhật số dư ví"
        val undoLabel = context.getString(R.string.feedback_undo)
        var undoCount = 0
        var firstResult: Boolean? = null

        composeRule.setContent {
            val state = remember { SnackbarHostState() }
            NotePayTheme(darkTheme = false) { FeedbackSnackbarHost(state) }
            LaunchedEffect(state) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    firstResult = state.showFeedback(
                        UiFeedback(
                            message = firstMessage,
                            actionLabel = undoLabel,
                            type = FeedbackType.Success,
                            duration = FeedbackDuration.Indefinite,
                            onAction = { undoCount++ },
                        ),
                    )
                }
                launch {
                    state.showFeedback(
                        UiFeedback(
                            message = secondMessage,
                            type = FeedbackType.Info,
                            duration = FeedbackDuration.Indefinite,
                        ),
                    )
                }
            }
        }

        composeRule.onNodeWithText(firstMessage).assertIsDisplayed()
        composeRule.onNodeWithText(secondMessage).assertDoesNotExist()
        composeRule.onNodeWithText(undoLabel)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { firstResult != null }
        composeRule.runOnIdle {
            assertEquals(true, firstResult)
            assertEquals(1, undoCount)
        }
        composeRule.onNodeWithText(firstMessage).assertDoesNotExist()
        composeRule.onNodeWithText(secondMessage).assertIsDisplayed()
    }

    @Test
    fun errorWithRetryActionInvokesRetryCallback() {
        val message = "Không thể đồng bộ giao dịch"
        val retryLabel = context.getString(R.string.action_retry_transaction_sync)
        var retryCount = 0
        var feedbackResult: Boolean? = null

        composeRule.showSingleFeedback(
            feedback = UiFeedback(
                message = message,
                actionLabel = retryLabel,
                type = FeedbackType.Error,
                duration = FeedbackDuration.Indefinite,
                onAction = { retryCount++ },
            ),
            onResult = { feedbackResult = it },
        )

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithText(retryLabel)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { feedbackResult != null }
        composeRule.runOnIdle {
            assertEquals(true, feedbackResult)
            assertEquals(1, retryCount)
        }
    }

    @Test
    fun indefinitePendingFeedbackExposesAccessibleDismissButton() {
        assertIndefiniteFeedbackDismissible(
            type = FeedbackType.Pending,
            message = "Đang đồng bộ giao dịch",
        )
    }

    @Test
    fun indefiniteInfoFeedbackExposesAccessibleDismissButton() {
        assertIndefiniteFeedbackDismissible(
            type = FeedbackType.Info,
            message = "Nhắc nhở kiểm tra sao kê cuối ngày",
        )
    }

    @Test
    fun fontScale145PlacesActionOnNewLineWithoutClipping() {
        assertActionOnNewLineAtFontScale(fontScale = 1.45f)
    }

    @Test
    fun fontScale200PlacesActionOnNewLineWithoutClipping() {
        assertActionOnNewLineAtFontScale(fontScale = 2.0f)
    }

    private fun assertIndefiniteFeedbackDismissible(type: FeedbackType, message: String) {
        val closeDescription = context.getString(R.string.action_close)
        var feedbackResult: Boolean? = null

        composeRule.showSingleFeedback(
            feedback = UiFeedback(
                message = message,
                type = type,
                duration = FeedbackDuration.Indefinite,
            ),
            onResult = { feedbackResult = it },
        )

        composeRule.onNodeWithText(message).assertIsDisplayed()
        composeRule.onNodeWithContentDescription(closeDescription)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { feedbackResult != null }
        composeRule.runOnIdle { assertEquals(false, feedbackResult) }
        composeRule.onNodeWithText(message).assertDoesNotExist()
    }

    private fun assertActionOnNewLineAtFontScale(fontScale: Float) {
        val longMessage = "Đã xóa giao dịch ăn trưa tại quán cơm gần công ty"
        val undoLabel = context.getString(R.string.feedback_undo)

        composeRule.showSingleFeedback(
            feedback = UiFeedback(
                message = longMessage,
                actionLabel = undoLabel,
                type = FeedbackType.Success,
                duration = FeedbackDuration.Indefinite,
            ),
            fontScale = fontScale,
        )

        val messageNode = composeRule.onNodeWithText(longMessage).assertIsDisplayed()
        val actionNode = composeRule.onNodeWithText(undoLabel)
            .assertIsDisplayed()
            .assertHeightIsAtLeast(48.dp)

        val messageBounds = messageNode.getUnclippedBoundsInRoot()
        val actionBounds = actionNode.getUnclippedBoundsInRoot()
        assertTrue(
            "Expected positive non-clipped message bounds at fontScale=$fontScale",
            messageBounds.width > 0.dp && messageBounds.height > 0.dp,
        )
        assertTrue(
            "Expected action below message at fontScale=$fontScale, " +
                "actionTop=${actionBounds.top}, messageBottom=${messageBounds.bottom}",
            actionBounds.top >= messageBounds.bottom,
        )

        val layoutResults = mutableListOf<TextLayoutResult>()
        val messageSemantics = messageNode.fetchSemanticsNode()
        assertTrue(
            "Expected GetTextLayoutResult semantics on message node",
            messageSemantics.config.contains(SemanticsActions.GetTextLayoutResult),
        )
        composeRule.runOnIdle {
            messageSemantics.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layoutResults)
        }
        assertTrue("Expected at least one TextLayoutResult", layoutResults.isNotEmpty())
        assertFalse(
            "Expected no visual text overflow at fontScale=$fontScale",
            layoutResults.first().hasVisualOverflow,
        )
    }

    private fun ComposeContentTestRule.showSingleFeedback(
        feedback: UiFeedback,
        fontScale: Float? = null,
        onResult: (Boolean) -> Unit = {},
    ) {
        setContent {
            val baseDensity = LocalDensity.current
            val effectiveDensity = if (fontScale != null) {
                Density(density = baseDensity.density, fontScale = fontScale)
            } else {
                baseDensity
            }
            val state = remember { SnackbarHostState() }
            CompositionLocalProvider(LocalDensity provides effectiveDensity) {
                NotePayTheme(darkTheme = false) {
                    FeedbackSnackbarHost(state)
                }
            }
            LaunchedEffect(state, feedback) {
                onResult(state.showFeedback(feedback))
            }
        }
    }
}
