package com.notepay.ui.component

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiquidControlsTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun toggle_reportsState_and_changesValueWhenEnabled() {
        var checked by mutableStateOf(false)

        composeRule.setContent {
            LiquidToggle(
                checked = checked,
                onCheckedChange = { checked = it },
                contentDescription = "Auto capture",
            )
        }

        composeRule.onNodeWithContentDescription("Auto capture")
            .assertIsOff()
            .performClick()
            .assertIsOn()
    }

    @Test
    fun disabledToggle_isNotInteractive() {
        composeRule.setContent {
            LiquidToggle(
                checked = true,
                onCheckedChange = {},
                enabled = false,
                contentDescription = "Auto capture",
            )
        }

        composeRule.onNodeWithContentDescription("Auto capture")
            .assertIsOn()
            .assertIsNotEnabled()
    }

    @Test
    fun slider_acceptsAccessibilityProgressAction() {
        var value by mutableFloatStateOf(0.25f)

        composeRule.setContent {
            LiquidSlider(
                value = value,
                onValueChange = { value = it },
                contentDescription = "Monthly budget",
            )
        }

        composeRule.onNodeWithContentDescription("Monthly budget")
            .performSemanticsAction(SemanticsActions.SetProgress) { action ->
                action(0.75f)
            }

        composeRule.runOnIdle {
            assertEquals(0.75f, value, 0.001f)
        }
    }

    @Test
    fun disabledSlider_isNotInteractive() {
        composeRule.setContent {
            LiquidSlider(
                value = 0.25f,
                onValueChange = {},
                enabled = false,
                contentDescription = "Monthly budget",
            )
        }

        composeRule.onNodeWithContentDescription("Monthly budget")
            .assertIsNotEnabled()
    }

    @Test
    fun glassSurfaces_renderWithFallbackBackdrop() {
        composeRule.setContent {
            Column {
                LiquidButton(onClick = {}) {
                    Text("Save")
                }
                LiquidGlassPanel {
                    Text("Overview")
                }
            }
        }

        composeRule.onNodeWithText("Save").assertExists()
        composeRule.onNodeWithText("Overview").assertExists()
    }
}