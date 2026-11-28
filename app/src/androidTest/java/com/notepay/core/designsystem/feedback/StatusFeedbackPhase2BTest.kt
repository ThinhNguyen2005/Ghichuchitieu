package com.notepay.core.designsystem.feedback

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notepay.R
import com.notepay.core.designsystem.theme.NotePayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StatusFeedbackPhase2BTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun feedbackTypesExposeDistinctStatusIcons() {
        assertEquals(Icons.Rounded.WarningAmber, FeedbackType.Warning.feedbackIcon())
        assertEquals(Icons.Rounded.CloudOff, FeedbackType.Offline.feedbackIcon())
        assertEquals(Icons.Rounded.Sync, FeedbackType.Pending.feedbackIcon())
        assertEquals(Icons.Rounded.ErrorOutline, FeedbackType.Error.feedbackIcon())
        assertNotEquals(FeedbackType.Warning.feedbackIcon(), FeedbackType.Offline.feedbackIcon())
    }

    @Test
    fun snackbarWarningAndOfflineInvokeRetryWithMinimumTouchTarget() {
        val retryLabel = context.getString(R.string.action_retry)
        val offlineMessage = "Đang ngoại tuyến, dữ liệu sẽ tự đồng bộ"
        var retryClicks = 0

        composeRule.showFeedbackHost(
            UiFeedback(
                message = offlineMessage,
                actionLabel = retryLabel,
                type = FeedbackType.Offline,
                duration = FeedbackDuration.Indefinite,
                onAction = { retryClicks++ },
            ),
        )

        composeRule.onNodeWithText(offlineMessage).assertIsDisplayed()
        composeRule.onNodeWithText(retryLabel)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, retryClicks) }
    }

    @Test
    fun statusBannerPendingAndOfflineRenderWithPoliteLiveRegion() {
        val pendingMsg = "Đang đồng bộ hóa 2 thay đổi chờ mạng"
        val offlineMsg = "Không có kết nối mạng, hiển thị tỷ giá gần nhất"
        val retryLabel = context.getString(R.string.action_retry)

        composeRule.setContent {
            NotePayTheme(darkTheme = false) {
                NotePayStatusBanner(
                    message = pendingMsg,
                    type = FeedbackType.Pending,
                )
                NotePayStatusBanner(
                    message = offlineMsg,
                    type = FeedbackType.Offline,
                    actionLabel = retryLabel,
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText(pendingMsg).assertIsDisplayed()
        composeRule.onNodeWithText(offlineMsg).assertIsDisplayed()
        composeRule.onNodeWithText(retryLabel)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
        composeRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite),
        ).fetchSemanticsNodes().also { assertTrue(it.size >= 2) }
    }

    @Test
    fun statusBannerErrorRetryMeetsTouchTargetAtFontScale100And200WithoutOverflow() {
        assertErrorBannerNoOverflow(fontScale = 1.0f)
    }

    @Test
    fun statusBannerErrorRetryMeetsTouchTargetAtFontScale200WithoutOverflow() {
        assertErrorBannerNoOverflow(fontScale = 2.0f)
    }

    private fun assertErrorBannerNoOverflow(fontScale: Float) {
        val errorMsg = "Không thể kết nối máy chủ tỷ giá, vui lòng thử lại sau"
        val retryLabel = context.getString(R.string.action_retry)
        var retried = 0

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            val scaledDensity = Density(density = baseDensity.density, fontScale = fontScale)
            CompositionLocalProvider(LocalDensity provides scaledDensity) {
                NotePayTheme(darkTheme = false) {
                    NotePayStatusBanner(
                        message = errorMsg,
                        type = FeedbackType.Error,
                        actionLabel = retryLabel,
                        onAction = { retried++ },
                        liveRegionMode = LiveRegionMode.Assertive,
                    )
                }
            }
        }

        val messageNode = composeRule.onNodeWithText(errorMsg).assertIsDisplayed()
        composeRule.onNode(
            SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive),
        ).assertIsDisplayed()
        composeRule.onNodeWithText(retryLabel)
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        composeRule.runOnIdle { assertEquals(1, retried) }

        val layoutResults = mutableListOf<TextLayoutResult>()
        val semanticsNode = messageNode.fetchSemanticsNode()
        composeRule.runOnIdle {
            semanticsNode.config[SemanticsActions.GetTextLayoutResult].action?.invoke(layoutResults)
        }
        assertTrue(layoutResults.isNotEmpty())
        assertFalse(
            "Unexpected text overflow at fontScale=$fontScale",
            layoutResults.first().hasVisualOverflow,
        )
    }

    private fun ComposeContentTestRule.showFeedbackHost(feedback: UiFeedback) {
        setContent {
            val state = remember { SnackbarHostState() }
            NotePayTheme(darkTheme = false) {
                FeedbackSnackbarHost(state)
            }
            LaunchedEffect(state, feedback) {
                state.showFeedback(feedback)
            }
        }
    }
}
