package com.notepay.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.LocalAuthUser
import com.notepay.core.designsystem.util.rememberUserGreeting
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.domain.model.AuthUser
import com.notepay.domain.money.Money
import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

// `internal` because HomeScreen and recent feed share this horizontal screen margin.
internal val ScreenHorizontalPadding = 16.dp
private val AvatarSize = 40.dp

/**
 * Top hero block for both 1-column and 2-column Home layouts: date eyebrow + greeting +
 * animated streak pill + avatar, and the luminous iOS balance card for the primary wallet.
 */
@Composable
internal fun HomeHeroBlock(
    streakDays: Int,
    balanceLabel: String,
    balance: Money,
    income: Money,
    expense: Money,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: () -> Unit,
    onStreakClick: () -> Unit,
    modifier: Modifier = Modifier,
    topInset: Dp = 0.dp,
    onAvatarClick: (() -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (topInset > 0.dp) {
            Spacer(modifier = Modifier.height(topInset))
        }

        HomeGreetingBar(
            greeting = rememberUserGreeting(),
            streakDays = streakDays,
            authUser = LocalAuthUser.current,
            onStreakClick = onStreakClick,
            onAvatarClick = onAvatarClick,
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
        )

        Spacer(modifier = Modifier.height(14.dp))

        HeroBalanceCard(
            label = balanceLabel,
            balance = balance,
            income = income,
            expense = expense,
            isBalanceHidden = isBalanceHidden,
            onToggleBalanceHidden = onToggleBalanceHidden,
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
        )

        Spacer(modifier = Modifier.height(14.dp))
    }
}

private data class HomeQuickActionSpec(
    val labelRes: Int,
    val icon: ImageVector,
    val accentColor: androidx.compose.ui.graphics.Color,
    val onClick: () -> Unit,
)

/**
 * Quick-access shortcuts right beneath the Home hero balance card for Bill Split, Debt Book,
 * and Subscription Renewals. Stacks vertically at `fontScale >= 1.45f` for readability.
 */
@Composable
internal fun HomeQuickActionsRow(
    onOpenBillSplit: () -> Unit,
    onOpenDebtManagement: () -> Unit,
    onOpenSubscription: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stackVertically = LocalDensity.current.fontScale >= 1.45f
    val actions = listOf(
        HomeQuickActionSpec(
            labelRes = R.string.home_quick_bill_split,
            icon = Icons.AutoMirrored.Rounded.CallSplit,
            accentColor = AppTheme.colors.warning,
            onClick = onOpenBillSplit,
        ),
        HomeQuickActionSpec(
            labelRes = R.string.home_quick_debt_book,
            icon = Icons.AutoMirrored.Rounded.MenuBook,
            accentColor = MaterialTheme.colorScheme.primary,
            onClick = onOpenDebtManagement,
        ),
        HomeQuickActionSpec(
            labelRes = R.string.home_quick_renewal,
            icon = Icons.Rounded.Autorenew,
            accentColor = AppTheme.colors.success,
            onClick = onOpenSubscription,
        ),
    )

    if (stackVertically) {
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            actions.forEach { action ->
                HomeQuickActionTile(
                    action = action,
                    stacked = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            actions.forEach { action ->
                HomeQuickActionTile(
                    action = action,
                    stacked = false,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeQuickActionTile(
    action: HomeQuickActionSpec,
    stacked: Boolean,
    modifier: Modifier = Modifier,
) {
    val performHaptic = rememberNotePayHaptic()
    val tileShape = AppTheme.shapes.corner20
    val surfaceColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.94f else 0.88f)
    val badgeAlpha = if (AppTheme.colors.isLight) 0.12f else 0.22f
    val label = stringResource(action.labelRes)

    Surface(
        onClick = {
            performHaptic(HapticFeedbackType.TextHandleMove)
            action.onClick()
        },
        shape = tileShape,
        color = surfaceColor,
        contentColor = AppTheme.colors.textPrimary,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = modifier
            .defaultMinSize(minHeight = if (stacked) 48.dp else 72.dp)
            .iosCardShadow(shape = tileShape, elevated = false),
    ) {
        if (stacked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(action.accentColor.copy(alpha = badgeAlpha), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = action.accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(action.accentColor.copy(alpha = badgeAlpha), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = action.icon,
                        contentDescription = null,
                        tint = action.accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HomeGreetingBar(
    greeting: String,
    streakDays: Int,
    authUser: AuthUser?,
    onStreakClick: () -> Unit,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rememberHomeDateLabel(),
                style = AppTheme.typography.eyebrow,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = greeting,
                style = AppTheme.typography.displaySerif,
                color = AppTheme.colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (streakDays > 0 || authUser != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (streakDays > 0) {
                    AnimatedStreakBadge(
                        streakDays = streakDays,
                        onClick = onStreakClick,
                    )
                }
                if (authUser != null) {
                    val initial = (authUser.displayName?.firstOrNull() ?: authUser.email?.firstOrNull())
                        ?.uppercaseChar()
                        ?.toString()
                    UserAvatar(
                        photoUrl = authUser.photoUrl,
                        initial = initial,
                        onClick = onAvatarClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberHomeDateLabel(): String {
    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }
    val weekday = when (today.dayOfWeek) {
        DayOfWeek.MONDAY -> stringResource(R.string.day_monday)
        DayOfWeek.TUESDAY -> stringResource(R.string.day_tuesday)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.day_wednesday)
        DayOfWeek.THURSDAY -> stringResource(R.string.day_thursday)
        DayOfWeek.FRIDAY -> stringResource(R.string.day_friday)
        DayOfWeek.SATURDAY -> stringResource(R.string.day_saturday)
        DayOfWeek.SUNDAY -> stringResource(R.string.day_sunday)
    }
    val monthSuffix = stringResource(R.string.date_month_suffix, today.month.number)
    val format = stringResource(R.string.home_greeting_date_format)
    return format.format(weekday, today.day, monthSuffix)
}

@Composable
private fun UserAvatar(
    photoUrl: String?,
    initial: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val avatarModifier = Modifier
        .size(AvatarSize)
        .iosCardShadow(shape = CircleShape, elevated = false)
        .border(1.5.dp, AppTheme.colors.surface, CircleShape)
        .clip(CircleShape)

    val containerModifier = if (onClick != null) {
        modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.auth_details_title),
                onClick = onClick,
            )
    } else {
        modifier
    }

    Box(
        modifier = containerModifier,
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = stringResource(R.string.auth_avatar_description),
                contentScale = ContentScale.Crop,
                modifier = avatarModifier,
            )
        } else {
            Box(
                modifier = avatarModifier.background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (initial != null) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = stringResource(R.string.auth_avatar_description),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}
