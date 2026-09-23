package com.notepay.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotePayMotionTest {
    @Test
    fun rootTabIndexIgnoresQueryArguments() {
        assertEquals(1, rootTabIndexForRoute("stats?showCreate=false"))
        assertEquals(2, rootTabIndexForRoute("assets?tab=0"))
        assertNull(rootTabIndexForRoute("transaction-detail/42"))
    }

    @Test
    fun tabTransitionDirectionFollowsTabOrder() {
        assertEquals(1, tabTransitionDirection(Route.Home.path, Route.Stats.path))
        assertEquals(-1, tabTransitionDirection(Route.Utilities.path, Route.Stats.path))
        assertNull(tabTransitionDirection(Route.Home.path, Route.Home.path))
        assertNull(tabTransitionDirection(Route.Home.path, Route.TransactionDetail(42).path))
    }

    @Test
    fun tabIndicatorOffsetClampsToAvailableTabs() {
        assertEquals(0f, tabIndicatorOffsetPx(-1, 4, 100f), 0f)
        assertEquals(200f, tabIndicatorOffsetPx(2, 4, 100f), 0f)
        assertEquals(300f, tabIndicatorOffsetPx(9, 4, 100f), 0f)
        assertEquals(0f, tabIndicatorOffsetPx(2, 0, 100f), 0f)
    }
}
