package com.notepay.ui.formatter

object VietnameseMoneyWordsFormatter {
    private val units = listOf("không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín")

    fun format(number: Long, zeroWord: String): String {
        require(number >= 0L) { "Amount must not be negative" }
        if (number == 0L) return zeroWord

        var remainingNumber = number
        val groups = listOf(
            1_000_000_000L to "tỷ",
            1_000_000L to "triệu",
            1_000L to "nghìn",
            1L to "",
        )
        val result = buildList {
            var hasHigherGroup = false
            groups.forEach { (divisor, label) ->
                val value = (remainingNumber / divisor).toInt()
                remainingNumber %= divisor
                if (value > 0) {
                    add(readThreeDigits(value, showZeroHundred = hasHigherGroup))
                    if (label.isNotEmpty()) add(label)
                    hasHigherGroup = true
                }
            }
        }.joinToString(" ").replace(Regex("\\s+"), " ").trim()

        return result.replaceFirstChar { it.uppercase() }
    }

    private fun readThreeDigits(number: Int, showZeroHundred: Boolean): String {
        val hundred = number / 100
        val ten = (number % 100) / 10
        val unit = number % 10
        return buildList {
            if (hundred > 0 || showZeroHundred) {
                add(units[hundred])
                add("trăm")
            }
            when {
                ten == 0 && unit > 0 -> {
                    if (hundred > 0 || showZeroHundred) add("lẻ")
                    add(units[unit])
                }
                ten == 1 -> {
                    add("mười")
                    if (unit > 0) add(if (unit == 5) "lăm" else units[unit])
                }
                ten > 1 -> {
                    add(units[ten])
                    add("mươi")
                    if (unit > 0) {
                        add(
                            when (unit) {
                                1 -> "mốt"
                                4 -> "tư"
                                5 -> "lăm"
                                else -> units[unit]
                            },
                        )
                    }
                }
            }
        }.joinToString(" ")
    }
}
