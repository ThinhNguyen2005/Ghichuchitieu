package com.notepay.ui

import androidx.compose.ui.text.AnnotatedString
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class VietnamCurrencyVisualTransformationTest {

    private val transformation = VietnamCurrencyVisualTransformation()

    @Test
    fun testEmptyInput() {
        val input = AnnotatedString("")
        val result = transformation.filter(input)
        assertEquals("", result.text.text)
        
        // Offset mapping check
        assertEquals(0, result.offsetMapping.originalToTransformed(0))
        assertEquals(0, result.offsetMapping.transformedToOriginal(0))
    }

    @Test
    fun testSingleDigit() {
        val input = AnnotatedString("5")
        val result = transformation.filter(input)
        assertEquals("5 đ", result.text.text)
        
        // originalToTransformed: len = 1
        // "5" (0) -> "5" (0)
        // cursor after "5" (1) -> cursor after "5" (1) before suffix " đ"
        val mapping = result.offsetMapping
        assertEquals(0, mapping.originalToTransformed(0))
        assertEquals(1, mapping.originalToTransformed(1))
        
        // transformedToOriginal: "5 đ" len = 3
        assertEquals(0, mapping.transformedToOriginal(0)) // "5"
        assertEquals(1, mapping.transformedToOriginal(1)) // " "
        assertEquals(1, mapping.transformedToOriginal(2)) // "đ"
        assertEquals(1, mapping.transformedToOriginal(3)) // end of "5 đ"
    }

    @Test
    fun testFourDigits() {
        val input = AnnotatedString("5000")
        val result = transformation.filter(input)
        assertEquals("5.000 đ", result.text.text)
        
        // original:   5 0 0 0  (len = 4)
        // original:   0 1 2 3
        // transformed: 5 . 0 0 0   đ  (len = 7, suffixStart = 5)
        // transformed: 0 1 2 3 4 5 6
        val mapping = result.offsetMapping
        
        // originalToTransformed
        assertEquals(0, mapping.originalToTransformed(0)) // 5 -> 0
        assertEquals(2, mapping.originalToTransformed(1)) // first 0 -> 2
        assertEquals(3, mapping.originalToTransformed(2)) // second 0 -> 3
        assertEquals(4, mapping.originalToTransformed(3)) // third 0 -> 4
        assertEquals(5, mapping.originalToTransformed(4)) // end -> 5 (before suffix)
        
        // transformedToOriginal
        assertEquals(0, mapping.transformedToOriginal(0)) // '5' -> 0
        assertEquals(1, mapping.transformedToOriginal(1)) // '.' -> 1
        assertEquals(1, mapping.transformedToOriginal(2)) // '0' -> 1
        assertEquals(2, mapping.transformedToOriginal(3)) // '0' -> 2
        assertEquals(3, mapping.transformedToOriginal(4)) // '0' -> 3
        assertEquals(4, mapping.transformedToOriginal(5)) // ' ' -> 4
        assertEquals(4, mapping.transformedToOriginal(6)) // 'đ' -> 4
        assertEquals(4, mapping.transformedToOriginal(7)) // end -> 4
    }

    @Test
    fun testSevenDigits() {
        val input = AnnotatedString("5000000")
        val result = transformation.filter(input)
        assertEquals("5.000.000 đ", result.text.text)
        
        // original:   5 0 0 0 0 0 0 (len = 7)
        // original:   0 1 2 3 4 5 6
        // transformed: 5 . 0 0 0 . 0 0 0   đ (len = 11, suffixStart = 9)
        // transformed: 0 1 2 3 4 5 6 7 8 9 10
        val mapping = result.offsetMapping
        
        assertEquals(0, mapping.originalToTransformed(0)) // 5 -> 0
        assertEquals(2, mapping.originalToTransformed(1)) // 0 -> 2
        assertEquals(3, mapping.originalToTransformed(2)) // 0 -> 3
        assertEquals(4, mapping.originalToTransformed(3)) // 0 -> 4
        assertEquals(6, mapping.originalToTransformed(4)) // 0 -> 6
        assertEquals(7, mapping.originalToTransformed(5)) // 0 -> 7
        assertEquals(8, mapping.originalToTransformed(6)) // 0 -> 8
        assertEquals(9, mapping.originalToTransformed(7)) // end -> 9
        
        assertEquals(0, mapping.transformedToOriginal(0))  // '5'
        assertEquals(1, mapping.transformedToOriginal(1))  // '.'
        assertEquals(1, mapping.transformedToOriginal(2))  // '0'
        assertEquals(2, mapping.transformedToOriginal(3))  // '0'
        assertEquals(3, mapping.transformedToOriginal(4))  // '0'
        assertEquals(4, mapping.transformedToOriginal(5))  // '.'
        assertEquals(4, mapping.transformedToOriginal(6))  // '0'
        assertEquals(5, mapping.transformedToOriginal(7))  // '0'
        assertEquals(6, mapping.transformedToOriginal(8))  // '0'
        assertEquals(7, mapping.transformedToOriginal(9))  // ' '
        assertEquals(7, mapping.transformedToOriginal(10)) // 'đ'
        assertEquals(7, mapping.transformedToOriginal(11)) // end
    }

    @Test
    fun debugRegex() {
        val text = """
            (TPBank): 14/06/26;06:25
            TK: xxxx5539020
            PS:-30.000VND
            SD: 410.054VND
            SD KHA DUNG: 410.054VND
            ND: NAP TIEN VI MOMO - 0945553902
            - 133366724699
            SO GD: 661TTMB261662918
        """.trimIndent()
        
        val regex = Regex(
            """(?:GD|Giao dịch|PS)\s*:?\s*([+-])\s*([0-9.,\s]+)\s*(?:VND|đ)""",
            RegexOption.IGNORE_CASE
        )
        val match = regex.find(text)
        System.out.println("DEBUG_REGEXP MATCH FOUND: ${match != null}")
        if (match != null) {
            System.out.println("DEBUG_REGEXP Group 1: ${match.groupValues[1]}")
            System.out.println("DEBUG_REGEXP Group 2: [${match.groupValues[2]}]")
        }
    }
}
