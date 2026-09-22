package com.notepay.ui.formatter

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VietnameseMoneyWordsFormatterTest {
    @Test
    fun `formats Vietnamese money words at pronunciation boundaries`() {
        val cases = linkedMapOf(
            0L to "Không",
            1L to "Một",
            5L to "Năm",
            10L to "Mười",
            15L to "Mười lăm",
            21L to "Hai mươi mốt",
            24L to "Hai mươi tư",
            25L to "Hai mươi lăm",
            101L to "Một trăm lẻ một",
            105L to "Một trăm lẻ năm",
            110L to "Một trăm mười",
            115L to "Một trăm mười lăm",
            1_000L to "Một nghìn",
            1_001L to "Một nghìn không trăm lẻ một",
            1_000_000L to "Một triệu",
            1_000_000_000L to "Một tỷ",
            10_000_000_000L to "Mười tỷ",
            100_000_000_000L to "Một trăm tỷ",
            999_999_999_999L to "Chín trăm chín mươi chín tỷ chín trăm chín mươi chín triệu chín trăm chín mươi chín nghìn chín trăm chín mươi chín",
        )

        cases.forEach { (amount, expected) ->
            assertThat(VietnameseMoneyWordsFormatter.format(amount, zeroWord = "Không"))
                .isEqualTo(expected)
        }
    }

    @Test
    fun `formats extreme long values coerced to maximum allowed bound safely`() {
        val result = VietnameseMoneyWordsFormatter.format(Long.MAX_VALUE, zeroWord = "Không")
        assertThat(result).isNotEmpty()
        assertThat(result).startsWith("Chín trăm chín mươi chín tỷ")
    }
}
