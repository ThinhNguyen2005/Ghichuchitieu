package com.notepay.feature.home.component

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class StreakTierTest {

    @Test
    fun `streakTierFor maps day thresholds accurately`() {
        assertThat(streakTierFor(1)).isEqualTo(StreakTier.SPARK)
        assertThat(streakTierFor(2)).isEqualTo(StreakTier.SPARK)
        assertThat(streakTierFor(3)).isEqualTo(StreakTier.BLAZE)
        assertThat(streakTierFor(6)).isEqualTo(StreakTier.BLAZE)
        assertThat(streakTierFor(7)).isEqualTo(StreakTier.INFERNO)
        assertThat(streakTierFor(30)).isEqualTo(StreakTier.INFERNO)
    }
}
