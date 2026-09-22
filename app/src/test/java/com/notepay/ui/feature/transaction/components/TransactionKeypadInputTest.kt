package com.notepay.ui.feature.transaction.components

import com.google.common.truth.Truth.assertThat
import com.notepay.ui.feature.transaction.AmountParser
import org.junit.Test

class TransactionKeypadInputTest {

    @Test
    fun `typing digits appends up to MAX_DIGITS`() {
        var current = ""
        for (i in 1..AmountParser.MAX_DIGITS) {
            current = handleKeyInput(current, KeypadKey.Number(1))
        }
        assertThat(current.length).isEqualTo(AmountParser.MAX_DIGITS)
        assertThat(current).isEqualTo("1".repeat(AmountParser.MAX_DIGITS))

        // Gõ thêm số thứ 13: Bị chặn hoàn toàn, giữ nguyên chuỗi
        val blocked = handleKeyInput(current, KeypadKey.Number(9))
        assertThat(blocked).isEqualTo(current)
        assertThat(blocked.length).isEqualTo(AmountParser.MAX_DIGITS)
    }

    @Test
    fun `dot three zeros respects MAX_DIGITS limit`() {
        // Có 9 chữ số, bấm .000 -> thành 12 chữ số (được phép)
        val nineDigits = "123456789"
        val twelveDigits = handleKeyInput(nineDigits, KeypadKey.DotThreeZeros)
        assertThat(twelveDigits).isEqualTo("123456789000")
        assertThat(twelveDigits.length).isEqualTo(12)

        // Có 10 chữ số, bấm .000 -> 13 chữ số > 12 -> Bị chặn, giữ nguyên 10 chữ số
        val tenDigits = "1234567890"
        val stillTenDigits = handleKeyInput(tenDigits, KeypadKey.DotThreeZeros)
        assertThat(stillTenDigits).isEqualTo(tenDigits)
        assertThat(stillTenDigits.length).isEqualTo(10)
    }

    @Test
    fun `backspace removes last digit`() {
        val input = "500000"
        val result = handleKeyInput(input, KeypadKey.Backspace)
        assertThat(result).isEqualTo("50000")
    }
}
