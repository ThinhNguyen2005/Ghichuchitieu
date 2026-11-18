package com.notepay.feature.home.component

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion

/**
 * Thẻ gợi ý & nhận định tài chính thông minh (Smart Financial Insights).
 */
@Composable
fun SmartInsightsCard(
    streakDays: Int,
    isBudgetExceeded: Boolean,
    budgetSpentPercentage: Float,
    modifier: Modifier = Modifier,
    onDismiss: (() -> Unit)? = null,
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }
    val performHaptic = rememberNotePayHaptic()
    val reducedMotion = rememberNotePayReducedMotion()
    val expandStateDescription = stringResource(
        if (isExpanded) R.string.stats_expand_state_open else R.string.stats_expand_state_closed
    )

    val (iconColor, title, description) = when {
            isBudgetExceeded -> Triple(
                AppTheme.colors.error,
                stringResource(R.string.insight_budget_exceeded_title),
                stringResource(R.string.insight_budget_exceeded_description)
            )
            budgetSpentPercentage >= 0.8f -> Triple(
                AppTheme.colors.warning,
                stringResource(R.string.insight_budget_near_limit_title, (budgetSpentPercentage * 100).toInt()),
                stringResource(R.string.insight_budget_near_limit_description)
            )
            streakDays >= 3 -> Triple(
                AppTheme.colors.clay,
                stringResource(R.string.insight_streak_title, streakDays),
                stringResource(R.string.insight_streak_description)
            )
            else -> Triple(
                AppTheme.colors.primary,
                stringResource(R.string.insight_default_title),
                stringResource(R.string.insight_default_description)
            )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (reducedMotion) Modifier else Modifier.animateContentSize()),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.65f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(iconColor.copy(alpha = 0.15f), AppTheme.shapes.circle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lightbulb,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
                    .semantics { stateDescription = expandStateDescription }
                    .clickable(role = Role.Button) { isExpanded = !isExpanded },
                verticalArrangement = Arrangement.Center,
            ) {
                if (isExpanded) {
                    Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    Text(
                        text = "$title · $description",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            IconButton(
                onClick = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onDismiss?.invoke()
                },
                modifier = Modifier.size(AppTheme.dimensions.minTouchTarget)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.content_description_close_suggestion),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
