package com.notepay.platform.widget

import android.content.Intent
import com.google.common.truth.Truth.assertThat
import com.notepay.ui.navigation.Route
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class WidgetTest {

    @Test
    fun widgetConstants_matchExpectedNavigationRoute() {
        assertThat(WidgetConstants.EXTRA_NAVIGATE_TO).isEqualTo("navigate_to")
        assertThat(WidgetConstants.ACTION_QUICK_ADD).isEqualTo("com.notepay.action.QUICK_ADD")
        assertThat(Route.AddTransaction.path).isEqualTo("add-transaction")
    }

    @Test
    fun updateQuickActionWidgets_handlesNullManagerGracefully() {
        val context = RuntimeEnvironment.getApplication()
        WidgetUpdateHelper.updateQuickActionWidgets(context, null, intArrayOf(101))
    }

    @Test
    fun updateSpentGlanceWidgets_handlesNullManagerGracefully() {
        val context = RuntimeEnvironment.getApplication()
        WidgetUpdateHelper.updateSpentGlanceWidgets(context, null, intArrayOf(101))
    }

    @Test
    fun quickActionWidgetReceiver_receivesUpdate_doesNotCrash() {
        val context = RuntimeEnvironment.getApplication()
        val receiver = QuickActionWidgetReceiver()
        val intent = Intent("android.appwidget.action.APPWIDGET_UPDATE")

        receiver.onReceive(context, intent)
    }

    @Test
    fun spentGlanceWidgetReceiver_receivesCustomRefresh_doesNotCrash() {
        val context = RuntimeEnvironment.getApplication()
        val receiver = SpentGlanceWidgetReceiver()
        val intent = Intent(WidgetConstants.ACTION_REFRESH_WIDGETS)

        receiver.onReceive(context, intent)
    }
}
