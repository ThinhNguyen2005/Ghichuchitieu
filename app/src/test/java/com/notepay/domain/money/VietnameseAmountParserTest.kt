package com.notepay.domain.money

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class VietnameseAmountParserTest {

    @Test
    fun `parseSingle parses k shorthand`() {
        assertThat(VietnameseAmountParser.parseSingle("50k")).isEqualTo(50_000L)
    }

    @Test
    fun `parseSingle parses uppercase K and tr shorthand`() {
        assertThat(VietnameseAmountParser.parseSingle("50K")).isEqualTo(50_000L)
        assertThat(VietnameseAmountParser.parseSingle("1.5TR")).isEqualTo(1_500_000L)
    }

    @Test
    fun `parseSingle parses ngan and nghin with and without accents`() {
        assertThat(VietnameseAmountParser.parseSingle("500 ngàn")).isEqualTo(500_000L)
        assertThat(VietnameseAmountParser.parseSingle("500 nghìn")).isEqualTo(500_000L)
        assertThat(VietnameseAmountParser.parseSingle("500 ngan")).isEqualTo(500_000L)
        assertThat(VietnameseAmountParser.parseSingle("80 nghin")).isEqualTo(80_000L)
    }

    @Test
    fun `parseSingle parses cu and chai slang`() {
        assertThat(VietnameseAmountParser.parseSingle("2 củ")).isEqualTo(2_000_000L)
        assertThat(VietnameseAmountParser.parseSingle("2 chai")).isEqualTo(2_000_000L)
        assertThat(VietnameseAmountParser.parseSingle("2 cu")).isEqualTo(2_000_000L)
        assertThat(VietnameseAmountParser.parseSingle("2 CỦ")).isEqualTo(2_000_000L)
    }

    @Test
    fun `parseSingle parses xi slang with and without accents`() {
        assertThat(VietnameseAmountParser.parseSingle("3 xị")).isEqualTo(300_000L)
        assertThat(VietnameseAmountParser.parseSingle("3 xi")).isEqualTo(300_000L)
    }

    @Test
    fun `parseSingle parses lit slang with and without accents`() {
        assertThat(VietnameseAmountParser.parseSingle("2 lít")).isEqualTo(200_000L)
        assertThat(VietnameseAmountParser.parseSingle("2 lit")).isEqualTo(200_000L)
    }

    @Test
    fun `parseSingle parses nua trieu and nua cu`() {
        assertThat(VietnameseAmountParser.parseSingle("nửa triệu")).isEqualTo(500_000L)
        assertThat(VietnameseAmountParser.parseSingle("nua trieu")).isEqualTo(500_000L)
        assertThat(VietnameseAmountParser.parseSingle("nửa củ")).isEqualTo(500_000L)
        assertThat(VietnameseAmountParser.parseSingle("nửa xị")).isEqualTo(50_000L)
    }

    @Test
    fun `parseSingle parses ba trieu ruoi with and without accents`() {
        assertThat(VietnameseAmountParser.parseSingle("ba triệu rưỡi")).isEqualTo(3_500_000L)
        assertThat(VietnameseAmountParser.parseSingle("ba trieu ruoi")).isEqualTo(3_500_000L)
    }

    @Test
    fun `parseSingle parses compound million shorthands`() {
        assertThat(VietnameseAmountParser.parseSingle("1tr5")).isEqualTo(1_500_000L)
        assertThat(VietnameseAmountParser.parseSingle("1tr25")).isEqualTo(1_250_000L)
        assertThat(VietnameseAmountParser.parseSingle("2cu5")).isEqualTo(2_500_000L)
    }

    @Test
    fun `parseSingle parses decimal million shorthands with dot and comma`() {
        assertThat(VietnameseAmountParser.parseSingle("1.5tr")).isEqualTo(1_500_000L)
        assertThat(VietnameseAmountParser.parseSingle("1,5tr")).isEqualTo(1_500_000L)
    }

    @Test
    fun `parseSingle parses compound thousand shorthand 80k5`() {
        assertThat(VietnameseAmountParser.parseSingle("80k5")).isEqualTo(80_500L)
        assertThat(VietnameseAmountParser.parseSingle("80k25")).isEqualTo(80_250L)
    }

    @Test
    fun `parseSingle parses slang with ruoi suffix`() {
        assertThat(VietnameseAmountParser.parseSingle("3 củ rưỡi")).isEqualTo(3_500_000L)
        assertThat(VietnameseAmountParser.parseSingle("2 xị rưỡi")).isEqualTo(250_000L)
    }

    @Test
    fun `parseSingle parses spoken Vietnamese number words`() {
        assertThat(VietnameseAmountParser.parseSingle("hai trăm năm mươi nghìn")).isEqualTo(250_000L)
        assertThat(VietnameseAmountParser.parseSingle("hai tram nam muoi nghin")).isEqualTo(250_000L)
        assertThat(VietnameseAmountParser.parseSingle("một triệu hai trăm nghìn")).isEqualTo(1_200_000L)
        assertThat(VietnameseAmountParser.parseSingle("hai trăm linh năm nghìn")).isEqualTo(205_000L)
    }

    @Test
    fun `parseSingle parses grouped VND and d suffix`() {
        assertThat(VietnameseAmountParser.parseSingle("45.000đ")).isEqualTo(45_000L)
        assertThat(VietnameseAmountParser.parseSingle("100,000 VND")).isEqualTo(100_000L)
    }

    @Test
    fun `parseAll preserves accurate source ranges for accented text`() {
        val input = "trả tiền trọ ba triệu rưỡi"
        val matches = VietnameseAmountParser.parseAll(input)
        assertThat(matches).hasSize(1)
        assertThat(matches[0].amountMajorVnd).isEqualTo(3_500_000L)
        assertThat(matches[0].rawText).isEqualTo("ba triệu rưỡi")
    }

    @Test
    fun `parseAll ignores masked date ranges`() {
        val input = "trả tiền trọ tháng 8 ba triệu rưỡi"
        val dateStart = input.indexOf("tháng 8")
        val dateRange = dateStart until (dateStart + "tháng 8".length)
        val matches = VietnameseAmountParser.parseAll(input, maskedRanges = listOf(dateRange))
        assertThat(matches).hasSize(1)
        assertThat(matches[0].amountMajorVnd).isEqualTo(3_500_000L)
    }

    @Test
    fun `parseAll does not treat item counts after spaces as decimals`() {
        val input = "ăn phở 50k 2 tô"
        val matches = VietnameseAmountParser.parseAll(input)
        assertThat(matches).hasSize(1)
        assertThat(matches[0].amountMajorVnd).isEqualTo(50_000L)
    }

    @Test
    fun `parseAll rejects negative numbers`() {
        assertThat(VietnameseAmountParser.parseAll("-50k")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("ăn sáng -2 củ")).isEmpty()
    }

    @Test
    fun `parseAll rejects zero amounts`() {
        assertThat(VietnameseAmountParser.parseAll("0k")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("0đ")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("0 củ")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("không nghìn")).isEmpty()
    }

    @Test
    fun `parseAll returns empty for blank text, no-amount phrases, and garbage`() {
        assertThat(VietnameseAmountParser.parseAll("   ")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("đi ăn bún bò với bạn")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("!!!@@@ ###")).isEmpty()
        assertThat(VietnameseAmountParser.parseAll("Wave Alpha 110 năm 2026")).isEmpty()
    }

    @Test
    fun `CurrencyParserEngine supports Vietnamese slang while preserving negative sign handling`() {
        val cu = CurrencyParserEngine.parseInputText("2 củ") as MoneyParseResult.Success
        assertThat(cu.money).isEqualTo(Money.fromMajorUnit(2_000_000L))

        val half = CurrencyParserEngine.parseInputText("nửa triệu") as MoneyParseResult.Success
        assertThat(half.money).isEqualTo(Money.fromMajorUnit(500_000L))

        val neg = CurrencyParserEngine.parseInputText("-50k") as MoneyParseResult.Success
        assertThat(neg.money).isEqualTo(Money.fromMajorUnit(-50_000L))
    }

    @Test
    fun `parseSingle parses multi-unit digit million and thousand phrases`() {
        assertThat(VietnameseAmountParser.parseSingle("2 triệu 500 nghìn")).isEqualTo(2_500_000L)
        assertThat(VietnameseAmountParser.parseSingle("1 triệu 250k")).isEqualTo(1_250_000L)
    }
}
