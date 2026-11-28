package com.notepay.core.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.notepay.core.designsystem.component.effect.RollingNumberTicker
import com.notepay.core.designsystem.theme.NotePayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MotionPhase2BTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun forwardTransitionsHonorReducedMotionFlag() {
        assertEquals(
            EnterTransition.None,
            notePayForwardEnterTransition(Route.Home.path, Route.AddTransaction.path, reducedMotion = true, tabDistancePx = 80),
        )
        assertEquals(
            ExitTransition.None,
            notePayForwardExitTransition(Route.Home.path, Route.AddTransaction.path, reducedMotion = true, tabDistancePx = 80),
        )
        assertNotEquals(
            EnterTransition.None,
            notePayForwardEnterTransition(Route.Home.path, Route.AddTransaction.path, reducedMotion = false, tabDistancePx = 80),
        )
        assertNotEquals(
            ExitTransition.None,
            notePayForwardExitTransition(Route.Home.path, Route.Stats.path, reducedMotion = false, tabDistancePx = 80),
        )
    }

    @Test
    fun sharedAnimatedContentAndVisibilityToggleCleanly() {
        var isExpanded by mutableStateOf(false)
        var step by mutableIntStateOf(0)

        composeRule.setContent {
            NotePayTheme(darkTheme = false) {
                SharedMotionHarness(
                    step = step,
                    isExpanded = isExpanded,
                    onNextStep = { step++ },
                    onToggleExpanded = { isExpanded = !isExpanded },
                )
            }
        }

        composeRule.onNodeWithText("Step 0").assertIsDisplayed()
        composeRule.onNodeWithText("Next Step").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Step 1").assertIsDisplayed()

        composeRule.onNodeWithText("Hidden Detail").assertDoesNotExist()
        composeRule.onNodeWithText("Toggle Detail").assertHeightIsAtLeast(48.dp).performClick()
        composeRule.onNodeWithText("Hidden Detail").assertIsDisplayed()
    }

    @Test
    fun rollingNumberTickerAndPressScaleUpdateCleanly() {
        var amountText by mutableStateOf("100.000đ")
        var taps by mutableIntStateOf(0)

        composeRule.setContent {
            val interactionSource = remember { MutableInteractionSource() }
            NotePayTheme(darkTheme = false) {
                Column {
                    RollingNumberTicker(text = amountText)
                    Button(
                        onClick = {
                            taps++
                            amountText = "250.000đ"
                        },
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .notePayPressScale(interactionSource)
                            .notePayStaggeredEntrance(index = 0),
                    ) {
                        Text("Update Amount")
                    }
                }
            }
        }

        composeRule.onNodeWithText("Update Amount").assertIsDisplayed().performClick()
        composeRule.runOnIdle { assertEquals(1, taps) }
    }

    @Composable
    private fun SharedMotionHarness(
        step: Int,
        isExpanded: Boolean,
        onNextStep: () -> Unit,
        onToggleExpanded: () -> Unit,
    ) {
        Column {
            NotePayAnimatedContent(targetState = step, label = "motion-test-content") { current ->
                Text("Step $current")
            }
            NotePayAnimatedVisibility(visible = isExpanded, label = "motion-test-visibility") {
                Text("Hidden Detail")
            }
            Button(onClick = onNextStep, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("Next Step")
            }
            Button(onClick = onToggleExpanded, modifier = Modifier.defaultMinSize(minHeight = 48.dp)) {
                Text("Toggle Detail")
            }
        }
    }
}
