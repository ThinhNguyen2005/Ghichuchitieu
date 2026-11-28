package com.notepay.feature.billsplit.main.sheet

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BillSplitCreateLogicTest {

    @Test
    fun `splitEvenlyInVnd supports both includePayer and groupOnly modes`() {
        val includeMe = splitEvenlyInVnd(
            totalCents = 300_000_00L,
            debtorCount = 2,
            includePayer = true,
        )
        assertThat(includeMe).containsExactly("100000", "100000").inOrder()

        val groupOnly = splitEvenlyInVnd(
            totalCents = 300_000_00L,
            debtorCount = 2,
            includePayer = false,
        )
        assertThat(groupOnly).containsExactly("150000", "150000").inOrder()
    }
}
