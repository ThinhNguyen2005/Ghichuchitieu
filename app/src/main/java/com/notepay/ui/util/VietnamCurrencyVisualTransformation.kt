package com.notepay.ui.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Tự động định dạng chuỗi số nhập vào thành tiền tệ Việt Nam có dấu phân cách hàng nghìn
 * và hậu tố " đ". Ví dụ: "5000000" -> "5.000.000 đ".
 */
class VietnamCurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val formatted = StringBuilder()
        val suffix = " đ"
        
        var groupCount = 0
        for (i in originalText.indices.reversed()) {
            if (groupCount > 0 && groupCount % 3 == 0) {
                formatted.insert(0, '.')
            }
            formatted.insert(0, originalText[i])
            groupCount++
        }
        
        formatted.append(suffix)
        val formattedString = formatted.toString()

        val len = originalText.length
        val originalToTransformed = IntArray(len + 1)
        val transformedToOriginal = IntArray(formattedString.length + 1)

        var currentOriginalIdx = 0
        val suffixStartIdx = formattedString.length - suffix.length

        for (i in 0 until formattedString.length) {
            transformedToOriginal[i] = currentOriginalIdx
            if (i < suffixStartIdx) {
                if (formattedString[i] != '.') {
                    originalToTransformed[currentOriginalIdx] = i
                    currentOriginalIdx++
                }
            }
        }
        originalToTransformed[len] = suffixStartIdx
        transformedToOriginal[formattedString.length] = len

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= len) return suffixStartIdx
                return originalToTransformed[offset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                if (offset >= formattedString.length) return len
                return transformedToOriginal[offset]
            }
        }

        return TransformedText(AnnotatedString(formattedString), offsetMapping)
    }
}
