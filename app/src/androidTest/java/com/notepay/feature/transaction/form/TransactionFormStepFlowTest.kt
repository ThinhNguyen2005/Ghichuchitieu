package com.notepay.feature.transaction.form

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notepay.R
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.domain.model.Category
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.feature.transaction.component.CalcKey
import com.notepay.feature.transaction.form.calculator.CalculatorEngine
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransactionFormStepFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun categorySelectionTransitionsToDetailsAndChangeCategoryReturnsWithDraftAndSelection() {
        val foodLabel = Category.FOOD.localizedName(context)
        val stepCategoryTitle = context.getString(R.string.add_transaction_step_category_title)
        val stepDetailsTitle = context.getString(R.string.add_transaction_step_details_title)
        val changeCategoryLabel = context.getString(R.string.action_change_category)
        val expenseLabel = context.getString(R.string.transaction_type_expense)
        val incomeLabel = context.getString(R.string.transaction_type_income)

        composeTestRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    TransactionFormUiState(
                        isEditMode = false,
                        step = TransactionFormStep.SELECT_CATEGORY,
                        hasLoadedCategories = true,
                        availableCategories = Category.getAll(),
                        walletId = 1L,
                        availableWallets = listOf(
                            Wallet(
                                id = 1L,
                                name = "Tiền mặt",
                                initialBalance = Money.ZERO,
                                iconKey = Wallet.ICON_CASH,
                                colorKey = Wallet.COLOR_PRIMARY,
                                isActive = true,
                            ),
                        ),
                    ),
                )
            }
            val snackbarHostState = remember { SnackbarHostState() }

            NotePayTheme(darkTheme = false) {
                TransactionFormContent(
                    state = uiState,
                    dateLabel = "Hôm nay",
                    onEvent = { event ->
                        uiState = when (event) {
                            is TransactionFormUiEvent.CategorySelected -> uiState.copy(
                                category = event.category,
                                isCategoryExplicitlySelected = true,
                                step = TransactionFormStep.DETAILS,
                            )

                            TransactionFormUiEvent.OpenCategorySelection -> uiState.copy(
                                step = TransactionFormStep.SELECT_CATEGORY,
                            )

                            is TransactionFormUiEvent.TypeChanged -> uiState.copy(
                                type = event.type,
                                isCategoryExplicitlySelected = false,
                                step = TransactionFormStep.SELECT_CATEGORY,
                            )

                            is TransactionFormUiEvent.CalcKeyPressed -> {
                                val nextCalc = when (val key = event.key) {
                                    is CalcKey.Digit -> CalculatorEngine.appendDigit(uiState.calcState, key.value)
                                    else -> uiState.calcState
                                }
                                val major = CalculatorEngine.previewMajorUnits(nextCalc) ?: 0L
                                uiState.copy(
                                    calcState = nextCalc,
                                    amountInput = nextCalc.currentOperand,
                                    amount = if (major > 0L) Money.fromMajorUnit(major) else null,
                                )
                            }

                            else -> uiState
                        }
                    },
                    onBack = {},
                    onPickImage = {},
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        // Starts on category selection step with accessible type tabs and category cell
        composeTestRule.onNodeWithText(stepCategoryTitle).assertExists()
        composeTestRule.onNodeWithText(expenseLabel)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
            .assertIsSelected()
        composeTestRule.onNodeWithText(incomeLabel)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        composeTestRule.onNodeWithContentDescription(foodLabel).assertHasClickAction().performClick()

        // Transitions in-place to Details showing selected category + "Đổi danh mục"
        composeTestRule.onNodeWithText(stepDetailsTitle).assertExists()
        composeTestRule.onNodeWithText(foodLabel).assertExists()
        composeTestRule.onNodeWithText(changeCategoryLabel).assertHasClickAction()

        // Enter amount digit on keypad, then return to grid and confirm selection + draft preservation
        composeTestRule.onNodeWithText("5").performClick()
        composeTestRule.onNodeWithText(changeCategoryLabel).performClick()

        composeTestRule.onNodeWithText(stepCategoryTitle).assertExists()
        composeTestRule.onNodeWithContentDescription(foodLabel).assertIsSelected().performClick()
        composeTestRule.onNodeWithText(stepDetailsTitle).assertExists()
        composeTestRule.onNodeWithText("5").assertExists()
    }

    @Test
    fun searchEmptyShowsClearFilterAndSwitchingTypeResetsGroupFilter() {
        val searchPlaceholder = context.getString(R.string.category_search_placeholder)
        val clearFiltersLabel = context.getString(R.string.category_clear_filters)
        val foodGroupLabel = context.getString(R.string.category_group_food_transport)
        val incomeLabel = context.getString(R.string.transaction_type_income)
        val salaryLabel = Category.SALARY.localizedName(context)

        composeTestRule.setContent {
            var uiState by remember {
                mutableStateOf(
                    TransactionFormUiState(
                        isEditMode = false,
                        step = TransactionFormStep.SELECT_CATEGORY,
                        hasLoadedCategories = true,
                        availableCategories = Category.getAll(),
                    ),
                )
            }
            val snackbarHostState = remember { SnackbarHostState() }

            NotePayTheme(darkTheme = false) {
                TransactionFormContent(
                    state = uiState,
                    dateLabel = "Hôm nay",
                    onEvent = { event ->
                        if (event is TransactionFormUiEvent.TypeChanged) {
                            uiState = uiState.copy(type = event.type, isCategoryExplicitlySelected = false)
                        }
                    },
                    onBack = {},
                    onPickImage = {},
                    snackbarHostState = snackbarHostState,
                )
            }
        }

        // Apply expense group filter, then switch to Income -> filter resets so SALARY is visible
        composeTestRule.onNodeWithText(foodGroupLabel).performClick()
        composeTestRule.onNodeWithText(incomeLabel).performClick()
        composeTestRule.onNodeWithContentDescription(salaryLabel).assertExists()

        // Searching non-existent query shows empty search state with "Xóa bộ lọc"
        composeTestRule.onNodeWithText(searchPlaceholder).performTextInput("zzzzz")
        composeTestRule.onNodeWithText(clearFiltersLabel).assertHasClickAction().performClick()
        composeTestRule.onNodeWithContentDescription(salaryLabel).assertExists()
    }
}
