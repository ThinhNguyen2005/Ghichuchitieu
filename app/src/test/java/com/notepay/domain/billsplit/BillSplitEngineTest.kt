package com.notepay.domain.billsplit

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Money
import kotlin.time.Clock
import org.junit.Test

class BillSplitEngineTest {

    @Test
    fun `formatMemoCode generates clean ASCII NP prefix memo string`() {
        val memo = BillSplitEngine.formatMemoCode(105L, "Nguyễn Văn Nam")
        assertThat(memo).isEqualTo("NP105 NGUYEN VAN NAM")
    }

    @Test
    fun `MemoCode parse extracts transaction ID and debtor slug`() {
        val parsed = MemoCode.parse("Chuyen tien NP105 NAM trả nợ")
        assertThat(parsed).isNotNull()
        assertThat(parsed?.transactionId).isEqualTo(105L)
        assertThat(parsed?.debtorSlug).isEqualTo("NAM")
    }

    @Test
    fun `matchByMemoCode matches notification text to unpaid split`() {
        val split1 = BillSplit(
            id = 1L,
            transactionId = 105L,
            debtorName = "Nam",
            amount = Money(50_000_00L),
            isPaid = false,
            memoCode = "NP105 NAM",
            paidAt = null,
            createdAt = Clock.System.now()
        )
        val split2 = BillSplit(
            id = 2L,
            transactionId = 106L,
            debtorName = "Hoa",
            amount = Money(100_000_00L),
            isPaid = false,
            memoCode = "NP106 HOA",
            paidAt = null,
            createdAt = Clock.System.now()
        )

        val matched = BillSplitEngine.matchByMemoCode("GD: +50,000 VND. ND: NP105 NAM tra tien", listOf(split1, split2))
        assertThat(matched).isEqualTo(split1)
    }

    @Test
    fun `settleSplit updates status to paid non-destructively`() = run {
        val split = BillSplit(
            id = 1L,
            transactionId = 105L,
            debtorName = "Nam",
            amount = Money(50_000_00L),
            isPaid = false,
            memoCode = "NP105 NAM",
            paidAt = null,
            createdAt = Clock.System.now()
        )

        val settled = BillSplitEngine.settleSplit(split)
        assertThat(settled.isPaid).isTrue()
        assertThat(settled.paidAt).isNotNull()
        assertThat(settled.amount).isEqualTo(Money(50_000_00L))
    }
}
