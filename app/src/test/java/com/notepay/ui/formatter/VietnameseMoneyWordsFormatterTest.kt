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
        )

        cases.forEach { (amount, expected) ->
            assertThat(VietnameseMoneyWordsFormatter.format(amount, zeroWord = "Không"))
                .isEqualTo(expected)
        }
    }
}
