package com.notepay.feature.home.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.analytics.SmartTipDomain
import com.notepay.domain.analytics.SmartTipItem
import com.notepay.domain.analytics.SmartTipKind

private data class DomainVisual(
    val icon: ImageVector,
    val labelRes: Int,
    val accent: Color,
)

/**
 * Full-height Smart Financial Insights card with diverse data-driven & rotating tips,
 * domain badge, 48dp next-tip switcher, and persistent dismiss control.
 */
@Composable
internal fun SmartInsightsCard(
    tip: SmartTipItem,
    tipPosition: Int,
    totalTips: Int,
    onNextTip: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val performHaptic = rememberNotePayHaptic()
    val reducedMotion = rememberNotePayReducedMotion()
    val cardShape = AppTheme.shapes.corner20

    val visual = when (tip.domain) {
        SmartTipDomain.SPENDING -> DomainVisual(
            icon = Icons.AutoMirrored.Rounded.TrendingUp,
            labelRes = R.string.home_tip_domain_spending,
            accent = colors.error,
        )
        SmartTipDomain.SAVINGS -> DomainVisual(
            icon = Icons.Rounded.Savings,
            labelRes = R.string.home_tip_domain_savings,
            accent = colors.success,
        )
        SmartTipDomain.BUDGET -> DomainVisual(
            icon = Icons.Rounded.Lightbulb,
            labelRes = R.string.home_tip_domain_budget,
            accent = colors.warning,
        )
        SmartTipDomain.HABIT -> DomainVisual(
            icon = Icons.Rounded.Whatshot,
            labelRes = R.string.home_tip_domain_habit,
            accent = colors.clay,
        )
        SmartTipDomain.SUBSCRIPTION -> DomainVisual(
            icon = Icons.Rounded.CalendarMonth,
            labelRes = R.string.home_tip_domain_subscription,
            accent = colors.primary,
        )
        SmartTipDomain.WALLET -> DomainVisual(
            icon = Icons.Rounded.AccountBalanceWallet,
            labelRes = R.string.home_tip_domain_wallet,
            accent = colors.graphite,
        )
    }

    val (title, description) = resolveTipCopy(tip)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .iosCardShadow(shape = cardShape, elevated = false),
        shape = cardShape,
        color = colors.surface,
        contentColor = colors.textPrimary,
        border = BorderStroke(0.5.dp, colors.hairline),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 6.dp, bottom = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(AppTheme.shapes.circle)
                        .background(visual.accent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = visual.icon,
                        contentDescription = null,
                        tint = visual.accent,
                        modifier = Modifier.size(18.dp),
                    )
                }

                Surface(
                    shape = AppTheme.shapes.capsule,
                    color = visual.accent.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = stringResource(visual.labelRes),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = visual.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                if (totalTips > 1) {
                    Text(
                        text = stringResource(R.string.home_tip_counter_format, tipPosition, totalTips),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                        maxLines = 1,
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (totalTips > 1) {
                    IconButton(
                        onClick = {
                            performHaptic(HapticFeedbackType.TextHandleMove)
                            onNextTip()
                        },
                        modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.home_tip_next_cd),
                            tint = colors.textSecondary,
                            modifier = Modifier.size(19.dp),
                        )
                    }
                }

                IconButton(
                    onClick = {
                        performHaptic(HapticFeedbackType.TextHandleMove)
                        onDismiss()
                    },
                    modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.content_description_close_suggestion),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            AnimatedContent(
                targetState = tip.kind to (title to description),
                transitionSpec = {
                    if (reducedMotion) {
                        EnterTransition.None togetherWith ExitTransition.None
                    } else {
                        fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                    }
                },
                label = "SmartTipContent",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 10.dp),
            ) { (_, copyPair) ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
                        .semantics(mergeDescendants = true) {},
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = copyPair.first,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = copyPair.second,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }
    }
}

@Composable
private fun resolveTipCopy(tip: SmartTipItem): Pair<String, String> = when (tip.kind) {
    SmartTipKind.TOP_SPENDING_CATEGORY -> {
        val categoryLabel = tip.category?.localizedName().orEmpty()
        stringResource(R.string.home_tip_top_category_title, categoryLabel, tip.primaryInt) to
            stringResource(R.string.home_tip_top_category_desc, MoneyFormatter.format(tip.primaryMoney))
    }

    SmartTipKind.SAVINGS_RATE_HIGH ->
        stringResource(R.string.home_tip_savings_high_title, tip.primaryInt) to
            stringResource(R.string.home_tip_savings_high_desc, MoneyFormatter.format(tip.primaryMoney))

    SmartTipKind.SPENDING_EXCEEDS_INCOME ->
        stringResource(R.string.home_tip_deficit_title) to
            stringResource(R.string.home_tip_deficit_desc, MoneyFormatter.format(tip.primaryMoney))

    SmartTipKind.UPCOMING_SUBSCRIPTIONS ->
        pluralStringResource(R.plurals.home_tip_subscriptions_due_title, tip.primaryInt, tip.primaryInt) to
            stringResource(R.string.home_tip_subscriptions_due_desc)

    SmartTipKind.SMALL_EXPENSES_ADD_UP ->
        pluralStringResource(R.plurals.home_tip_small_expenses_title, tip.primaryInt, tip.primaryInt) to
            stringResource(R.string.home_tip_small_expenses_desc, MoneyFormatter.format(tip.primaryMoney))

    SmartTipKind.SET_WALLET_BUDGET ->
        stringResource(R.string.home_tip_set_budget_title) to
            stringResource(R.string.home_tip_set_budget_desc, tip.secondaryText)

    SmartTipKind.STREAK_MOMENTUM ->
        pluralStringResource(R.plurals.home_tip_streak_title, tip.primaryInt, tip.primaryInt) to
            stringResource(R.string.home_tip_streak_desc)

    SmartTipKind.CONCENTRATED_WALLET ->
        stringResource(R.string.home_tip_concentrated_wallet_title, tip.primaryInt) to
            stringResource(R.string.home_tip_concentrated_wallet_desc, tip.secondaryText)

    SmartTipKind.EVERGREEN_RULE_50_30_20 ->
        stringResource(R.string.home_tip_rule_503020_title) to
            stringResource(R.string.home_tip_rule_503020_desc)

    SmartTipKind.EVERGREEN_EMERGENCY_FUND ->
        stringResource(R.string.home_tip_emergency_fund_title) to
            stringResource(R.string.home_tip_emergency_fund_desc)

    SmartTipKind.EVERGREEN_WAIT_24H ->
        stringResource(R.string.home_tip_wait_24h_title) to
            stringResource(R.string.home_tip_wait_24h_desc)

    SmartTipKind.EVERGREEN_PAY_YOURSELF_FIRST ->
        stringResource(R.string.home_tip_pay_yourself_title) to
            stringResource(R.string.home_tip_pay_yourself_desc)

    SmartTipKind.EVERGREEN_AUDIT_SUBSCRIPTIONS ->
        stringResource(R.string.home_tip_audit_subs_title) to
            stringResource(R.string.home_tip_audit_subs_desc)

    SmartTipKind.EVERGREEN_WEEKLY_REVIEW ->
        stringResource(R.string.home_tip_weekly_review_title) to
            stringResource(R.string.home_tip_weekly_review_desc)

    SmartTipKind.EVERGREEN_SEPARATE_WALLETS ->
        stringResource(R.string.home_tip_separate_wallets_title) to
            stringResource(R.string.home_tip_separate_wallets_desc)

    SmartTipKind.EVERGREEN_LOG_IMMEDIATELY ->
        stringResource(R.string.home_tip_log_now_title) to
            stringResource(R.string.home_tip_log_now_desc)
}
