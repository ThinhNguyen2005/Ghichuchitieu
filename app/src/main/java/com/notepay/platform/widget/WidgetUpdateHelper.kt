package com.notepay.platform.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.notepay.MainActivity
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.ui.util.MoneyFormatter
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

object WidgetUpdateHelper {

    private val widgetScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun updateQuickActionWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetManager == null || appWidgetIds.isEmpty()) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            action = WidgetConstants.ACTION_QUICK_ADD
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(WidgetConstants.EXTRA_NAVIGATE_TO, "add-transaction")
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        for (widgetId in appWidgetIds) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_action).apply {
                setOnClickPendingIntent(R.id.widget_quick_action_root, pendingIntent)
            }
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }

    fun updateSpentGlanceWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager?,
        appWidgetIds: IntArray,
    ) {
        if (appWidgetManager == null || appWidgetIds.isEmpty()) return

        widgetScope.launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    WidgetEntryPoint::class.java,
                )

                val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val year = today.year
                val month = today.month.number

                val activeWallet = entryPoint.walletRepository().observeActive().firstOrNull()
                    ?: entryPoint.walletRepository().observeAll().firstOrNull()?.firstOrNull()

                val summary = entryPoint.getMonthlySummaryUseCase()(year, month, activeWallet?.id).firstOrNull()
                val totalExpense = summary?.totalExpense ?: Money.ZERO

                val walletBudget = activeWallet?.budgetLimit
                val globalBudget = entryPoint.budgetSettingsStore().settings.firstOrNull()?.monthlyBudgetCents?.let {
                    if (it > 0L) Money(it) else null
                }
                val effectiveBudget = if (walletBudget != null && walletBudget.amountInCents > 0L) {
                    walletBudget
                } else {
                    globalBudget
                }

                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    1,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val quickAddIntent = Intent(context, MainActivity::class.java).apply {
                    action = WidgetConstants.ACTION_QUICK_ADD
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra(WidgetConstants.EXTRA_NAVIGATE_TO, "add-transaction")
                }
                val quickAddPendingIntent = PendingIntent.getActivity(
                    context,
                    2,
                    quickAddIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val titleText = context.getString(R.string.widget_spent_glance_title, month.toString())
                val amountText = MoneyFormatter.format(totalExpense)
                val walletText = activeWallet?.name ?: context.getString(R.string.widget_spent_glance_default_wallet)

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_spent_glance).apply {
                        setOnClickPendingIntent(R.id.widget_glance_root, mainPendingIntent)
                        setOnClickPendingIntent(R.id.btn_glance_quick_add, quickAddPendingIntent)

                        setTextViewText(R.id.tv_glance_title, titleText)
                        setTextViewText(R.id.tv_glance_amount, amountText)
                        setTextViewText(R.id.tv_glance_wallet, walletText)

                        if (effectiveBudget != null && effectiveBudget.amountInCents > 0L) {
                            val spentCents = totalExpense.amountInCents
                            val limitCents = effectiveBudget.amountInCents
                            val progressPercent = ((spentCents.toDouble() / limitCents.toDouble()) * 100)
                                .toInt()
                                .coerceIn(0, 100)

                            setProgressBar(R.id.glance_progress_bar, 100, progressPercent, false)
                            setTextViewText(
                                R.id.tv_glance_budget,
                                context.getString(
                                    R.string.widget_spent_glance_budget_progress,
                                    MoneyFormatter.formatCompact(totalExpense),
                                    MoneyFormatter.formatCompact(effectiveBudget),
                                    progressPercent,
                                ),
                            )

                            if (spentCents > limitCents) {
                                val overspent = Money(spentCents - limitCents)
                                setTextViewText(
                                    R.id.tv_glance_remaining,
                                    context.getString(
                                        R.string.widget_spent_glance_overspent,
                                        MoneyFormatter.formatCompact(overspent),
                                    ),
                                )
                            } else {
                                val remaining = Money(limitCents - spentCents)
                                setTextViewText(
                                    R.id.tv_glance_remaining,
                                    context.getString(
                                        R.string.widget_spent_glance_remaining,
                                        MoneyFormatter.formatCompact(remaining),
                                    ),
                                )
                            }
                        } else {
                            setProgressBar(R.id.glance_progress_bar, 100, 0, false)
                            setTextViewText(
                                R.id.tv_glance_budget,
                                context.getString(R.string.widget_spent_glance_no_budget),
                            )
                            setTextViewText(R.id.tv_glance_remaining, "")
                        }
                    }
                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                // Keep widget alive if DB or resource fetch fails
            }
        }
    }

    fun notifyWidgetsDataChanged(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context) ?: return

        val spentGlanceIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, SpentGlanceWidgetReceiver::class.java),
        )
        if (spentGlanceIds.isNotEmpty()) {
            updateSpentGlanceWidgets(context, appWidgetManager, spentGlanceIds)
        }

        val quickActionIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, QuickActionWidgetReceiver::class.java),
        )
        if (quickActionIds.isNotEmpty()) {
            updateQuickActionWidgets(context, appWidgetManager, quickActionIds)
        }
    }
}
