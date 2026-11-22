package com.notepay.feature.stats.main.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.Transaction
import com.notepay.domain.money.Money
import com.notepay.feature.category.mapper.CategoryAvatar
import com.notepay.feature.stats.main.CategoryBreakdownItem
import com.notepay.feature.stats.main.DynamicDailyBudgetData
import com.notepay.feature.stats.main.formatOrMask
import com.notepay.feature.stats.main.tabularNums
import com.notepay.feature.transaction.component.TransactionItem
import java.util.Locale
import kotlin.math.abs

private const val INITIAL_VISIBLE_TRANSACTIONS = 5
private const val PAGE_VISIBLE_TRANSACTIONS = 10

/**
 * Gop CategoryRow va danh sach Transaction con vao chung 1 Unified Accordion Card,
 * giup phan cap thi giac ro rang (Gestalt Common Region) va gioi han so luong render.
 */
@Composable
internal fun CategoryBreakdownAccordionCard(
    item: CategoryBreakdownItem,
    isSelected: Boolean,
    showAmounts: Boolean,
    transactions: List<Transaction>,
    onToggle: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val reducedMotion = rememberNotePayReducedMotion()
    val categoryColor = Color(item.category.colorArgb)
    val containerColor by animateColorAsState(
        targetValue = if (isSelected) {
            AppTheme.colors.surface
        } else {
            AppTheme.colors.surfaceSunken
        },
        label = "category-card-bg",
    )

    val categoryName = item.category.localizedName()
    val amountLabel = item.amount.formatOrMask(showAmounts)
    val formattedPercent = formatSharePercent(item.percentage)
    val percentageLabel = stringResource(
        R.string.stats_category_percentage_cd,
        formatSharePercentValue(item.percentage),
    )
    // Mô tả cho screen reader lấy nguyên văn từ resource (dấu phân cách nằm trong chuỗi để
    // bản địa hoá đúng), không nối chuỗi thủ công bằng ", ".
    val rowDescription = if (isSelected) {
        stringResource(
            R.string.stats_category_row_open_cd,
            categoryName,
            amountLabel,
            percentageLabel,
            stringResource(R.string.stats_category_selected_cd),
        )
    } else {
        stringResource(R.string.stats_category_row_cd, categoryName, amountLabel, percentageLabel)
    }

    val expandStateDescription = stringResource(
        if (isSelected) R.string.stats_expand_state_open else R.string.stats_expand_state_closed,
    )

    var visibleCount by remember(item.category.id, isSelected) {
        mutableIntStateOf(INITIAL_VISIBLE_TRANSACTIONS)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(
            width = if (isSelected) 1.5.dp else 1.dp,
            color = if (isSelected) {
                categoryColor
            } else {
                AppTheme.colors.hairline
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header của Accordion (chính là Row danh mục)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onToggle)
                    .padding(16.dp)
                    .clearAndSetSemantics {
                        role = Role.Button
                        contentDescription = rowDescription
                        stateDescription = expandStateDescription
                    },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CategoryAvatar(
                    category = item.category,
                    size = 40.dp,
                    iconSize = 18.dp,
                )
                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = categoryName,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = item.amount.formatOrMask(showAmounts),
                            style = MaterialTheme.typography.titleMedium.tabularNums,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        LinearProgressIndicator(
                            progress = { item.percentage.coerceIn(0f, 1f) },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                            color = categoryColor,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            strokeCap = StrokeCap.Round,
                        )

                        Text(
                            text = formattedPercent,
                            style = MaterialTheme.typography.labelSmall.tabularNums,
                            color = AppTheme.colors.textSecondary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.widthIn(min = 48.dp),
                            textAlign = TextAlign.End,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (isSelected) {
                        Icons.Rounded.ExpandLess
                    } else {
                        Icons.Rounded.ExpandMore
                    },
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }

            // Nội dung mở rộng bên trong cùng Card
            AnimatedVisibility(
                visible = isSelected,
                enter = if (reducedMotion) {
                    EnterTransition.None
                } else {
                    fadeIn(tween(180)) + expandVertically(tween(220))
                },
                exit = if (reducedMotion) {
                    ExitTransition.None
                } else {
                    fadeOut(tween(120)) + shrinkVertically(tween(180))
                },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AppTheme.colors.surfaceSunken.copy(alpha = 0.55f))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        modifier = Modifier.padding(bottom = 4.dp),
                    )

                    Text(
                        text = stringResource(
                            R.string.stats_transaction_history_format,
                            categoryName,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )

                    if (transactions.isEmpty()) {
                        Text(
                            text = stringResource(R.string.stats_empty),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(12.dp),
                        )
                    } else {
                        val displayedTransactions = remember(transactions, visibleCount) {
                            transactions.take(visibleCount)
                        }
                        displayedTransactions.forEach { transaction ->
                            key(transaction.id) {
                                TransactionItem(
                                    transaction = transaction,
                                    // Hàng con cũng phải obey nút ẩn số tiền, nếu không mở
                                    // accordion ra là số liệu rò rỉ lại toàn bộ.
                                    showAmounts = showAmounts,
                                    onClick = { onTransactionClick(transaction.id) },
                                )
                            }
                        }

                        if (transactions.size > visibleCount) {
                            val remaining = transactions.size - visibleCount
                            TextButton(
                                onClick = { visibleCount += PAGE_VISIBLE_TRANSACTIONS },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp),
                            ) {
                                Text(
                                    text = pluralStringResource(R.plurals.stats_show_more_transactions, remaining, remaining),
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun BudgetProgressBar(
    spent: Money,
    limit: Money,
    percentage: Float,
    showAmounts: Boolean,
    dailyBudget: DynamicDailyBudgetData?,
) {
    val safePercentage = if (percentage.isFinite()) percentage.coerceAtLeast(0f) else 0f
    val visualProgress = safePercentage.coerceIn(0f, 1f)
    val remainingInCents = limit.amountInCents - spent.amountInCents

    val statusColor = when {
        safePercentage >= 1f -> MaterialTheme.colorScheme.error
        safePercentage >= 0.8f -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    val statusText = when {
        remainingInCents < 0L ->
            stringResource(
                R.string.stats_budget_exceeded_format,
                Money(abs(remainingInCents)).formatOrMask(showAmounts),
            )
        safePercentage >= 0.8f ->
            stringResource(
                R.string.stats_budget_remaining_format,
                Money(remainingInCents.coerceAtLeast(0L)).formatOrMask(showAmounts),
            )
        else -> stringResource(R.string.stats_budget_in_limit)
    }

    val budgetUsedContentDescription = stringResource(
        R.string.stats_budget_used_cd,
        (safePercentage * 100).toInt(),
    )

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
                    color = statusColor.copy(alpha = 0.12f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (safePercentage >= 0.8f) {
                                Icons.Rounded.Warning
                            } else {
                                Icons.Rounded.Wallet
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wallet_field_budget),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.textPrimary,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = statusColor.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = String.format(Locale.US, "%.0f%%", safePercentage * 100f),
                        style = MaterialTheme.typography.labelLarge.tabularNums,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }

            LinearProgressIndicator(
                progress = { visualProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .semantics {
                        contentDescription = budgetUsedContentDescription
                    },
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = stringResource(
                        R.string.stats_spent_format,
                        spent.formatOrMask(showAmounts),
                    ),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(
                        R.string.stats_limit_format,
                        limit.formatOrMask(showAmounts),
                    ),
                    style = MaterialTheme.typography.bodySmall.tabularNums,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // Hạn mức ngày gộp thành 1 chip footer gọn thay vì thẻ progress riêng trên Carousel,
            // tránh trùng lặp thị giác với thanh ngân sách tháng ở trên.
            if (dailyBudget != null) {
                val dailyAccent = if (dailyBudget.isExceeded) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = dailyAccent.copy(alpha = 0.08f),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Today,
                            contentDescription = null,
                            tint = dailyAccent,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            text = stringResource(
                                R.string.stats_daily_limit_format,
                                dailyBudget.dailyBudget.formatOrMask(showAmounts),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            text = if (dailyBudget.isExceeded) {
                                stringResource(
                                    R.string.stats_daily_spent_format,
                                    dailyBudget.spentToday.formatOrMask(showAmounts),
                                )
                            } else {
                                stringResource(
                                    R.string.stats_daily_remaining_format,
                                    dailyBudget.remainingToday.formatOrMask(showAmounts),
                                )
                            },
                            style = MaterialTheme.typography.labelMedium.tabularNums,
                            fontWeight = FontWeight.Bold,
                            color = dailyAccent,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
