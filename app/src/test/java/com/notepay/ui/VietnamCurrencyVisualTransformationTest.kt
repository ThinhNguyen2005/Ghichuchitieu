package com.notepay.ui

import androidx.compose.ui.text.AnnotatedString
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import org.junit.Assert.assertEquals
import org.junit.Test

class VietnamCurrencyVisualTransformationTest {

    private val transformation = VietnamCurrencyVisualTransformation()

    @Test
    fun `formats digit strings into VND display with dots and suffix`() {
        listOf(
            "" to "",
            "5" to "5 đ",
            "5000" to "5.000 đ",
            "5000000" to "5.000.000 đ",
        ).forEach { (raw, expected) ->
            assertEquals(expected, transformation.filter(AnnotatedString(raw)).text.text)
        }
    }

    @Test
    fun `maps cursor offsets bidirectionally across separators and suffix`() {
        val single = transformation.filter(AnnotatedString("5")).offsetMapping
        assertEquals(0, single.originalToTransformed(0))
        assertEquals(1, single.originalToTransformed(1))
        assertEquals(1, single.transformedToOriginal(2))
        assertEquals(1, single.transformedToOriginal(3))

        val seven = transformation.filter(AnnotatedString("5000000")).offsetMapping
        // "5000000" -> "5.000.000 đ"
        listOf(0 to 0, 1 to 2, 3 to 4, 4 to 6, 7 to 9).forEach { (orig, trans) ->
            assertEquals(trans, seven.originalToTransformed(orig))
        }
        listOf(0 to 0, 1 to 1, 2 to 1, 5 to 4, 9 to 7, 11 to 7).forEach { (trans, orig) ->
            assertEquals(orig, seven.transformedToOriginal(trans))
        }

        val empty = transformation.filter(AnnotatedString("")).offsetMapping
        assertEquals(0, empty.originalToTransformed(0))
        assertEquals(0, empty.transformedToOriginal(0))
    }
}
