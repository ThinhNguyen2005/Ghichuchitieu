package com.notepay.feature.home.component

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
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
import androidx.compose.ui.layout.ContentScale
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
import com.notepay.domain.model.AuthUser
import com.notepay.domain.money.Money
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.LocalAuthUser
import com.notepay.core.designsystem.util.rememberUserGreeting
import kotlin.time.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime

// ── Design tokens của khối hero (bội số của 4) ───────────────────────────────
// `internal` vì HomeScreen và feed giao dịch dùng chung lề ngang này.
internal val ScreenHorizontalPadding = 16.dp
private val AvatarSize = 36.dp

/**
 * Khối hero dùng chung cho bố cục 1 cột (Compact/Medium) và cột trái của bố cục 2 cột
 * (Expanded): nền trang trí phía trên, lời chào, bộ chọn ví trên thanh trên cùng và thẻ
 * số dư ink.
 *
 * Chọn / đổi ví nằm ở thanh trên cùng thay vì một dãy thẻ ví riêng, để trang chính gọn
 * và tập trung vào con số tổng.
 */
@Composable
internal fun HomeHeroBlock(
    streakDays: Int,
    walletLabel: String,
    balanceLabel: String,
    balance: Money,
    income: Money,
    expense: Money,
    onOpenWalletPicker: () -> Unit,
    topInset: Dp = 0.dp,
    modifier: Modifier = Modifier,
    onAvatarClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier.fillMaxWidth()) {
        HomeTopBackdrop(modifier = Modifier.matchParentSize())

        Column(modifier = Modifier.fillMaxWidth()) {
            // Nền trang trí phải phủ cả vùng status bar nên inset được bù bằng khoảng đệm
            // bên trong, không dùng statusBarsPadding() ở ngoài.
            if (topInset > 0.dp) {
                Spacer(modifier = Modifier.height(topInset))
            }

            HomeGreetingBar(
                greeting = rememberUserGreeting(),
                streakDays = streakDays,
                authUser = LocalAuthUser.current,
                onAvatarClick = onAvatarClick,
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
            )

            Spacer(modifier = Modifier.height(8.dp))

            WalletSelectorChip(
                label = walletLabel,
                onClick = onOpenWalletPicker,
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
            )

            Spacer(modifier = Modifier.height(12.dp))

            HeroBalanceCard(
                label = balanceLabel,
                balance = balance,
                income = income,
                expense = expense,
                modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/** Nút chọn ví trên thanh trên cùng: mở bottom sheet chọn ví hoặc quay về "Tất cả ví". */
@Composable
private fun WalletSelectorChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectWalletLabel = stringResource(R.string.home_select_wallet)
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingIcon = {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
        },
        shape = AppTheme.shapes.corner12,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = AppTheme.colors.surface.copy(alpha = 0.72f),
            labelColor = AppTheme.colors.textPrimary,
            leadingIconContentColor = AppTheme.colors.textSecondary,
            trailingIconContentColor = AppTheme.colors.textSecondary,
        ),
        border = BorderStroke(1.dp, AppTheme.colors.hairline),
        modifier = modifier
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {
                onClick(label = selectWalletLabel, action = null)
            },
    )
}

/**
 * Thanh chào mừng phía trên cùng: dòng ngày nhỏ kiểu eyebrow, lời chào serif lớn xếp chồng
 * bên trái; huy hiệu chuỗi ngày và avatar nằm bên phải.
 */
@Composable
private fun HomeGreetingBar(
    greeting: String,
    streakDays: Int,
    authUser: AuthUser?,
    onAvatarClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = rememberHomeDateLabel(),
                style = AppTheme.typography.eyebrow,
                color = AppTheme.colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = greeting,
                style = AppTheme.typography.displaySerif,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (streakDays > 0) {
            StreakBadge(streakDays = streakDays)
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

/** Huy hiệu pill "🔥 N ngày" cho chuỗi ngày ghi chép liên tiếp. */
@Composable
private fun StreakBadge(
    streakDays: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.chip,
        color = AppTheme.colors.surfaceSunken,
        contentColor = AppTheme.colors.textSecondary,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Whatshot,
                contentDescription = null,
                tint = AppTheme.colors.warning,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = pluralStringResource(R.plurals.home_streak_days, streakDays, streakDays),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

/** Nhãn ngày hiện tại dạng "Thứ 7, 31 Th10", ghép từ các tài nguyên bản địa hoá. */
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

/** Avatar người dùng: ảnh nếu có, ngược lại chữ cái đầu, cuối cùng là icon người. */
@Composable
private fun UserAvatar(
    photoUrl: String?,
    initial: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val avatarModifier = Modifier
        .size(AvatarSize)
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
