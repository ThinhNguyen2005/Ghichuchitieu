package com.notepay.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.Whatshot
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
 * avatar, wallet selector + streak badge + customize button row, and the luminous iOS
 * balance card.
 */
@Composable
internal fun HomeHeroBlock(
    streakDays: Int,
    walletLabel: String,
    balanceLabel: String,
    balance: Money,
    income: Money,
    expense: Money,
    isBalanceHidden: Boolean,
    onToggleBalanceHidden: () -> Unit,
    onOpenWalletPicker: () -> Unit,
    onOpenCustomization: () -> Unit,
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
            authUser = LocalAuthUser.current,
            onAvatarClick = onAvatarClick,
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding),
        )

        Spacer(modifier = Modifier.height(10.dp))

        HomeUtilityRow(
            walletLabel = walletLabel,
            streakDays = streakDays,
            onOpenWalletPicker = onOpenWalletPicker,
            onOpenCustomization = onOpenCustomization,
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

@Composable
private fun HomeGreetingBar(
    greeting: String,
    authUser: AuthUser?,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HomeUtilityRow(
    walletLabel: String,
    streakDays: Int,
    onOpenWalletPicker: () -> Unit,
    onOpenCustomization: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isLargeFont = LocalDensity.current.fontScale >= 1.35f
    if (isLargeFont) {
        FlowRow(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            WalletSelectorChip(
                label = walletLabel,
                onClick = onOpenWalletPicker,
            )
            if (streakDays > 0) {
                StreakBadge(streakDays = streakDays)
            }
            CustomizeHomeButton(onClick = onOpenCustomization)
        }
    } else {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WalletSelectorChip(
                label = walletLabel,
                onClick = onOpenWalletPicker,
                modifier = Modifier.weight(1f, fill = false),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (streakDays > 0) {
                    StreakBadge(streakDays = streakDays)
                }
                CustomizeHomeButton(onClick = onOpenCustomization)
            }
        }
    }
}

@Composable
private fun WalletSelectorChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectWalletLabel = stringResource(R.string.home_select_wallet)
    val pillShape = AppTheme.shapes.capsule
    val surfaceColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.90f else 0.85f)

    Surface(
        onClick = onClick,
        shape = pillShape,
        color = surfaceColor,
        contentColor = AppTheme.colors.textPrimary,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = modifier
            .defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget)
            .iosCardShadow(shape = pillShape, elevated = false)
            .semantics(mergeDescendants = true) {
                onClick(label = selectWalletLabel, action = null)
            },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun CustomizeHomeButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val performHaptic = rememberNotePayHaptic()
    val buttonShape = AppTheme.shapes.circle
    val surfaceColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.90f else 0.85f)

    Surface(
        onClick = {
            performHaptic(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        shape = buttonShape,
        color = surfaceColor,
        contentColor = AppTheme.colors.textPrimary,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
        modifier = modifier
            .size(AppTheme.dimensions.minTouchTarget)
            .iosCardShadow(shape = buttonShape, elevated = false),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.Wallpaper,
                contentDescription = stringResource(R.string.home_customize_action),
                tint = AppTheme.colors.textPrimary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** Pill badge "🔥 N ngày" for active daily logging streak. */
@Composable
private fun StreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    val pillShape = AppTheme.shapes.capsule
    val surfaceColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.90f else 0.85f)

    Surface(
        modifier = modifier
            .iosCardShadow(shape = pillShape, elevated = false)
            .semantics(mergeDescendants = true) {},
        shape = pillShape,
        color = surfaceColor,
        contentColor = AppTheme.colors.textPrimary,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Whatshot,
                contentDescription = null,
                tint = AppTheme.colors.warning,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
            )
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
