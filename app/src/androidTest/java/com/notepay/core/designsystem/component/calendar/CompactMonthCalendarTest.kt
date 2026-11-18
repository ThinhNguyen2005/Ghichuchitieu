package com.notepay.core.designsystem.component.calendar

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.notepay.R
import com.notepay.core.designsystem.theme.NotePayTheme
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Kiểm tra phần không thể test bằng JUnit thuần của lịch gọn: hợp đồng accessibility và
 * phản hồi khi chạm. Phép chiếu dữ liệu đã có [MonthGridTest].
 */
@RunWith(AndroidJUnit4::class)
class CompactMonthCalendarTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun dayLabel(day: Int) = context.getString(R.string.calendar_day_cd, day)

    private fun activityDayLabel(day: Int) = context.getString(R.string.calendar_day_activity_cd, day)

    private fun show(
        grid: MonthGrid,
        selectedDate: LocalDate? = null,
        onDayClick: (LocalDate) -> Unit = {},
    ) {
        composeTestRule.setContent {
            NotePayTheme(darkTheme = false) {
                CompactMonthCalendar(
                    grid = grid,
                    selectedDate = selectedDate,
                    onDayClick = onDayClick,
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onGoToToday = {},
                )
            }
        }
    }

    @Test
    fun selectedDayExposesSelectedStateToAccessibility() {
        show(grid = buildMonthGrid(2026, 10, emptyMap()), selectedDate = LocalDate(2026, 10, 5))

        composeTestRule.onNodeWithContentDescription(dayLabel(5)).assertIsSelected()
    }

    @Test
    fun tappingDayReportsThatDate() {
        var clicked: LocalDate? = null
        show(grid = buildMonthGrid(2026, 10, emptyMap()), onDayClick = { clicked = it })

        composeTestRule.onNodeWithContentDescription(dayLabel(12)).performClick()

        composeTestRule.runOnIdle { assertEquals(LocalDate(2026, 10, 12), clicked) }
    }

    @Test
    fun dayWithTransactionsUsesTheActivityLabel() {
        val transaction = Transaction(
            amount = Money(50_000_00),
            type = TransactionType.EXPENSE,
            category = Category.FOOD,
            note = "com trua",
            occurredAt = LocalDate(2026, 10, 26).atStartOfDayIn(TimeZone.UTC),
            walletId = 1L,
        )

        show(grid = buildMonthGrid(2026, 10, mapOf(LocalDate(2026, 10, 26) to listOf(transaction))))

        composeTestRule.onNodeWithContentDescription(activityDayLabel(26)).assertExists()
    }

    @Test
    fun dayOutsideCurrentMonthIsNotClickable() {
        // Ngày 28 xuất hiện hai lần: ô đệm 28/9 mở đầu lưới rồi tới 28/10. Thứ tự trong cây
        // semantics là thứ tự đọc nên ô đầu tiên chính là ngày của tháng trước.
        show(grid = buildMonthGrid(2026, 10, emptyMap()))

        val sameNumberCells = composeTestRule.onAllNodesWithContentDescription(dayLabel(28))
        sameNumberCells[0].assertHasNoClickAction()
        sameNumberCells[1].assertHasClickAction()
    }
}
