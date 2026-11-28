package com.notepay.core.designsystem.component.action

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notepay.R
import com.notepay.core.designsystem.theme.NotePayTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CreateNewBottomSheetFlowTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun allQuickAddCardsRenderWithAccessibleHeadingAndButtonRoles() {
        val title = context.getString(R.string.quick_add_title)
        val cardTitles = listOf(
            context.getString(R.string.quick_add_expense_title),
            context.getString(R.string.quick_add_transfer_title),
            context.getString(R.string.quick_add_scan_receipt_title),
            context.getString(R.string.quick_add_text_entry_title),
            context.getString(R.string.quick_add_debt_title),
            context.getString(R.string.bill_split_title),
            context.getString(R.string.quick_add_subscription_title),
        )

        composeRule.setContent {
            NotePayTheme(darkTheme = false) {
                CreateNewBottomSheet(
                    onDismissRequest = {},
                    onAction = {},
                )
            }
        }

        composeRule.onNodeWithText(title)
            .assertIsDisplayed()
            .assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        cardTitles.forEach { cardTitle ->
            composeRule.onNodeWithText(cardTitle)
                .performScrollTo()
                .assertIsDisplayed()
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .assertHasClickAction()
                .assertHeightIsAtLeast(64.dp)
        }
    }

    @Test
    fun selectingExpenseClosesSheetBeforeFiringActionAndPreventsDoubleTap() {
        val expenseTitle = context.getString(R.string.quick_add_expense_title)
        var showSheet by mutableStateOf(true)
        val dispatchedActions = mutableListOf<QuickAddAction>()

        composeRule.setContent {
            NotePayTheme(darkTheme = false) {
                if (showSheet) {
                    CreateNewBottomSheet(
                        onDismissRequest = { showSheet = false },
                        onAction = { action -> dispatchedActions.add(action) },
                    )
                }
            }
        }

        composeRule.onNodeWithText(expenseTitle).assertIsDisplayed()

        composeRule.mainClock.autoAdvance = false
        composeRule.onNodeWithText(expenseTitle).performClick()
        composeRule.mainClock.advanceTimeBy(16)
        assertTrue("onAction must wait until sheet hides", dispatchedActions.isEmpty())

        composeRule.onNodeWithText(expenseTitle).performClick()
        composeRule.mainClock.autoAdvance = true

        composeRule.waitUntil(timeoutMillis = 3_000) { dispatchedActions.isNotEmpty() }
        composeRule.onNodeWithText(expenseTitle).assertIsNotDisplayed()
        composeRule.runOnIdle {
            assertEquals(listOf(QuickAddAction.Transaction), dispatchedActions)
            showSheet = false
        }
        composeRule.onNodeWithText(expenseTitle).assertDoesNotExist()
    }

    @Test
    fun quickAddCardsRemainAccessibleAndClickableAtMaxFontScale() {
        val expenseTitle = context.getString(R.string.quick_add_expense_title)
        val cardTitles = listOf(
            expenseTitle,
            context.getString(R.string.quick_add_transfer_title),
            context.getString(R.string.quick_add_scan_receipt_title),
            context.getString(R.string.quick_add_text_entry_title),
            context.getString(R.string.quick_add_debt_title),
            context.getString(R.string.bill_split_title),
            context.getString(R.string.quick_add_subscription_title),
        )
        var showSheet by mutableStateOf(true)
        var selectedAction: QuickAddAction? = null

        composeRule.setContent {
            val baseDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = baseDensity.density, fontScale = 2.0f),
            ) {
                NotePayTheme(darkTheme = false) {
                    if (showSheet) {
                        CreateNewBottomSheet(
                            onDismissRequest = { showSheet = false },
                            onAction = { action ->
                                selectedAction = action
                                showSheet = false
                            },
                        )
                    }
                }
            }
        }

        cardTitles.forEach { cardTitle ->
            composeRule.onNodeWithText(cardTitle)
                .performScrollTo()
                .assertIsDisplayed()
        }

        composeRule.onNodeWithText(expenseTitle)
            .performScrollTo()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 3_000) { selectedAction != null }
        composeRule.runOnIdle {
            assertEquals(QuickAddAction.Transaction, selectedAction)
        }
    }
}
