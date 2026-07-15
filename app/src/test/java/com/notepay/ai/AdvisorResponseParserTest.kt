package com.notepay.ai

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AdvisorResponseParserTest {
    @Test
    fun `parses the constrained Vietnamese response`() {
        val result = AdvisorResponseParser.parse(
            """
                TIÊU ĐỀ: Nhịp chi đang tăng
                NHẬN XÉT: Khoản ăn uống là điểm đáng chú ý nhất.
                HÀNH ĐỘNG: Theo dõi riêng khoản này trong 7 ngày.
            """.trimIndent(),
        )

        assertThat(result?.title).isEqualTo("Nhịp chi đang tăng")
        assertThat(result?.observation).contains("ăn uống")
        assertThat(result?.action).contains("7 ngày")
    }

    @Test
    fun `rejects an unconstrained model response`() {
        assertThat(AdvisorResponseParser.parse("Bạn đang chi tiêu khá nhiều.")).isNull()
    }
    @Test
    fun `accepts response labels with inconsistent Vietnamese accents`() {
        val result = AdvisorResponseParser.parse(
            """
                TIEU DỀ: Can theo doi
                NHAN XẾT: Chi tieu dang tang.
                HANH ĐỘNG: Giam mot khoan nho trong tuan nay.
            """.trimIndent(),
        )

        assertThat(result?.title).isEqualTo("Can theo doi")
        assertThat(result?.observation).isEqualTo("Chi tieu dang tang.")
        assertThat(result?.action).isEqualTo("Giam mot khoan nho trong tuan nay.")
    }
}
