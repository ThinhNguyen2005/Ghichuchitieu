package com.notepay.feature.stats.main.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.button.LiquidButton
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.domain.analytics.AdvisorAvailability
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.ForecastConfidence
import com.notepay.domain.analytics.SpendingPrediction
import com.notepay.domain.analytics.StatsAdviceType
import com.notepay.domain.money.Money
import com.notepay.feature.stats.main.AiAdviceItem
import com.notepay.feature.stats.main.DetectedSubscription
import com.notepay.feature.stats.main.LocalAdvisorStatus
import com.notepay.feature.stats.main.LocalAdvisorUiState
import com.notepay.feature.stats.main.SectionHeading
import com.notepay.feature.stats.main.StatsUiState
import com.notepay.feature.stats.main.formatOrMask
import com.notepay.feature.stats.main.resolve
import com.notepay.feature.stats.main.tabularNums

@Composable
internal fun SpendingPredictionCard(
    prediction: SpendingPrediction,
    showAmounts: Boolean,
    onConfigureBudget: () -> Unit,
) {
    val probability = prediction.overBudgetProbability

    val confidenceLabel = when (prediction.confidence) {
        ForecastConfidence.LOW -> stringResource(R.string.stats_confidence_low)
        ForecastConfidence.MEDIUM -> stringResource(R.string.stats_confidence_medium)
        ForecastConfidence.HIGH -> stringResource(R.string.stats_confidence_high)
    }

    val riskContainerColor = when {
        probability == null -> MaterialTheme.colorScheme.secondaryContainer
        probability >= 0.70 -> MaterialTheme.colorScheme.errorContainer
        probability >= 0.35 -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.primaryContainer
    }
    val riskOnContainerColor = when {
        probability == null -> MaterialTheme.colorScheme.onSecondaryContainer
        probability >= 0.70 -> MaterialTheme.colorScheme.onErrorContainer
        probability >= 0.35 -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onPrimaryContainer
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .iosCardShadow(shape = AppTheme.shapes.corner24, elevated = false),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
        ),
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    shape = CircleShape,
                    color = riskContainerColor,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AutoGraph,
                            contentDescription = null,
                            tint = riskOnContainerColor,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.stats_forecast_end_month),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = stringResource(R.string.stats_processed_on_device),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = riskContainerColor,
                ) {
                    Text(
                        text = stringResource(R.string.stats_confidence_format, confidenceLabel),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = riskOnContainerColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
                    )
                }
            }

            Text(
                text = Money(prediction.predictedMonthTotalInCents).formatOrMask(showAmounts),
                style = AppTheme.typography.headlineMedium.tabularNums,
                fontWeight = FontWeight.ExtraBold,
                color = AppTheme.colors.textPrimary,
            )

            Text(
                text = stringResource(
                    R.string.stats_expected_range_format,
                    Money(prediction.lowerBoundInCents).formatOrMask(showAmounts),
                    Money(prediction.upperBoundInCents).formatOrMask(showAmounts),
                ),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = AppTheme.colors.textSecondary,
            )

            HorizontalDivider(
                color = AppTheme.colors.hairline,
            )

            Text(
                text = probability?.let {
                    pluralStringResource(
                        R.plurals.stats_probability_format,
                        prediction.observedDays,
                        (it * 100).toInt(),
                        prediction.observedDays,
                    )
                } ?: pluralStringResource(
                    R.plurals.stats_probability_no_limit_format,
                    prediction.observedDays,
                    prediction.observedDays,
                ),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
                fontWeight = FontWeight.SemiBold,
            )

            if (probability == null) {
                OutlinedButton(
                    onClick = onConfigureBudget,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = AppTheme.dimensions.minTouchTarget),
                    shape = AppTheme.shapes.corner12,
                ) {
                    Text(stringResource(R.string.stats_action_set_budget))
                }
            }
        }
    }
}

@Composable
internal fun AiInsightsCarousel(
    state: StatsUiState,
    showAmounts: Boolean,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (DetectedSubscription) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
    onSelectLocalModel: () -> Unit,
) {
    val performHaptic = rememberNotePayHaptic()
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    // Tinh cardWidth truc tiep tu WindowInfo.containerSize, tranh dung LocalConfiguration.screenWidthDp
    val windowWidthDp = with(density) { windowInfo.containerSize.width.toDp() }
    val cardWidth = remember(windowWidthDp) {
        (windowWidthDp - 48.dp).coerceIn(272.dp, 328.dp)
    }

    // Chi dem LocalAdvisor la 1 insight khi da READY (co loi khuyen thuc su)
    val itemCount = (if (state.localAdvisor.status == LocalAdvisorStatus.READY) 1 else 0) +
        (if (state.dynamicDailyBudget != null) 1 else 0) +
        state.detectedSubscriptions.size +
        state.aiAdvices.size

    val playHaptic = {
        performHaptic(HapticFeedbackType.TextHandleMove)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val adviceCount = itemCount.coerceAtLeast(1)
        SectionHeading(
            title = stringResource(R.string.stats_section_advice),
            description = pluralStringResource(R.plurals.stats_advice_count_format, adviceCount, adviceCount),
        )

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "local-advisor") {
                LocalAdvisorCard(
                    advisor = state.localAdvisor,
                    onGenerate = onGenerateLocalAdvice,
                    onSelectModel = onSelectLocalModel,
                    modifier = Modifier.width(cardWidth),
                )
            }

            items(
                items = state.detectedSubscriptions,
                key = { sub -> "sub-${sub.name}-${sub.amount.amountInCents}" },
            ) { subscription ->
                SubscriptionProposalCard(
                    sub = subscription,
                    showAmounts = showAmounts,
                    onAdd = {
                        playHaptic()
                        onAddSubscription(subscription)
                    },
                    modifier = Modifier.width(cardWidth),
                )
            }

            items(
                items = state.aiAdvices,
                key = { advice -> "advice-${advice.id}" },
            ) { advice ->
                AiAdviceCard(
                    advice = advice,
                    onFeedback = { score ->
                        playHaptic()
                        onAdviceFeedback(advice.id, score)
                    },
                    modifier = Modifier.width(cardWidth),
                )
            }
        }
    }
}

@Composable
private fun LocalAdvisorCard(
    advisor: LocalAdvisorUiState,
    onGenerate: () -> Unit,
    onSelectModel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = advisor.result
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val isExpandable = advisor.status == LocalAdvisorStatus.READY
    val expandStateDescription = stringResource(
        if (isExpanded) R.string.stats_expand_state_open else R.string.stats_expand_state_closed,
    )

    val idleDescription = when (advisor.availability) {
        AdvisorAvailability.GEMINI_NANO ->
            stringResource(R.string.stats_advisor_gemini_description)
        AdvisorAvailability.CLOUD_GEMINI ->
            stringResource(R.string.stats_advisor_cloud_ready_format, "Gemini 2.0 Flash")
        AdvisorAvailability.STATISTICAL_ONLY ->
            stringResource(R.string.stats_advisor_statistical_description)
        AdvisorAvailability.CHECKING ->
            stringResource(R.string.stats_advisor_checking_description)
    }

    InsightCard(
        modifier = modifier
            .semantics {
                if (isExpandable) stateDescription = expandStateDescription
            }
            .clickable(
                enabled = isExpandable,
                role = Role.Button,
                onClick = { isExpanded = !isExpanded },
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = result?.title ?: stringResource(R.string.stats_advisor_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(R.string.stats_advisor_privacy),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when (advisor.status) {
            LocalAdvisorStatus.RUNNING -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            strokeWidth = 3.dp,
                        )
                        Text(
                            text = stringResource(R.string.stats_analyzing),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }

            LocalAdvisorStatus.NOT_REQUESTED -> {
                Text(
                    text = idleDescription,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )

                // Chi 1 Primary CTA duy nhat (LiquidButton), nut Settings la Secondary IconButton
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    LiquidButton(
                        onClick = onGenerate,
                        enabled = advisor.availability != AdvisorAvailability.CHECKING,
                        modifier = Modifier.weight(1f),
                        tint = MaterialTheme.colorScheme.primary,
                    ) {
                        Icon(
                            Icons.Rounded.Lightbulb,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(
                                if (advisor.availability == AdvisorAvailability.STATISTICAL_ONLY) {
                                    R.string.stats_action_local_analysis
                                } else {
                                    R.string.stats_action_analyze
                                },
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }

                    if (advisor.availability != AdvisorAvailability.GEMINI_NANO) {
                        FilledTonalIconButton(
                            onClick = onSelectModel,
                            modifier = Modifier.size(48.dp),
                            shape = AppTheme.shapes.corner12,
                        ) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.stats_action_ai_settings),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            LocalAdvisorStatus.READY -> {
                Text(
                    text = cleanAiMarkdown(result?.content.orEmpty()),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (isExpanded) Int.MAX_VALUE else 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = !isExpanded),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when (result?.provider) {
                                AdvisorProvider.GEMINI_NANO -> stringResource(R.string.stats_provider_gemini)
                                AdvisorProvider.CLOUD_GEMINI -> stringResource(R.string.stats_provider_cloud)
                                AdvisorProvider.STATISTICAL_FALLBACK, null -> stringResource(R.string.stats_provider_statistics)
                            },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        result?.providerMessage?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    if (result?.provider != AdvisorProvider.GEMINI_NANO) {
                        IconButton(onClick = onSelectModel) {
                            Icon(
                                Icons.Rounded.Settings,
                                contentDescription = stringResource(R.string.stats_cd_open_ai_settings),
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    TextButton(
                        onClick = onGenerate,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.stats_refresh))
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAdviceCard(
    advice: AiAdviceItem,
    onFeedback: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(advice.id) { mutableStateOf(false) }
    val expandStateDescription = stringResource(
        if (isExpanded) R.string.stats_expand_state_open else R.string.stats_expand_state_closed,
    )
    // type là enum domain nên when tất định; nhánh "info" trước đây là code chết vì
    // StatsAdviceType chỉ có WARNING/SUCCESS.
    val accentColor = when (advice.type) {
        StatsAdviceType.WARNING -> MaterialTheme.colorScheme.error
        StatsAdviceType.SUCCESS -> MaterialTheme.colorScheme.primary
    }

    InsightCard(
        modifier = modifier
            .semantics { stateDescription = expandStateDescription }
            .clickable(role = Role.Button) { isExpanded = !isExpanded },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = if (advice.type == StatsAdviceType.WARNING) {
                    Icons.Rounded.Warning
                } else {
                    Icons.Rounded.Info
                },
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = advice.title.resolve(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }

        Text(
            text = cleanAiMarkdown(advice.content.resolve()),
            style = MaterialTheme.typography.bodySmall,
            maxLines = if (isExpanded) Int.MAX_VALUE else 4,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = !isExpanded),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (advice.feedback == 1 || advice.feedback == -1) {
                    stringResource(R.string.stats_feedback_thanks)
                } else {
                    stringResource(R.string.stats_feedback_question)
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )

            IconButton(
                onClick = { onFeedback(1) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ThumbUp,
                    contentDescription = stringResource(R.string.stats_feedback_helpful),
                    tint = if (advice.feedback == 1) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(
                onClick = { onFeedback(-1) },
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    Icons.Rounded.ThumbDown,
                    contentDescription = stringResource(R.string.stats_feedback_not_helpful),
                    tint = if (advice.feedback == -1) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun SubscriptionProposalCard(
    sub: DetectedSubscription,
    showAmounts: Boolean,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    InsightCard(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.stats_subscription_expense),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.stats_subscription_detected_history),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = sub.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(
                    R.string.stats_subscription_monthly_format,
                    sub.amount.formatOrMask(showAmounts),
                ),
                style = MaterialTheme.typography.bodySmall.tabularNums,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FilledTonalButton(
            onClick = onAdd,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp),
            shape = AppTheme.shapes.corner12,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.stats_add_to_subscriptions),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier
            .heightIn(min = 220.dp)
            .iosCardShadow(shape = AppTheme.shapes.corner20, elevated = false)
            .animateContentSize(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = AppTheme.colors.surface,
        ),
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

private val AI_MARKDOWN_BOLD_REGEX = Regex("\\*\\*")
private val AI_MARKDOWN_BULLET_REGEX = Regex("(?m)^\\s*\\*\\s+")

private fun cleanAiMarkdown(text: String?): String {
    if (text.isNullOrBlank()) return ""
    return text
        .replace(AI_MARKDOWN_BOLD_REGEX, "")
        .replace(AI_MARKDOWN_BULLET_REGEX, "• ")
        .trim()
}
