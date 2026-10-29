package com.notepay.feature.home.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
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
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = AppTheme.shapes.corner12,
        color = AppTheme.colors.surface.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, AppTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.AccountBalanceWallet,
                contentDescription = null,
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = stringResource(R.string.home_select_wallet),
                tint = AppTheme.colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Thanh chào mừng người dùng phía trên cùng */
@Composable
private fun HomeGreetingBar(
    greeting: String,
    streakDays: Int,
    authUser: AuthUser?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = greeting,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (streakDays > 0) {
                Surface(
                    shape = AppTheme.shapes.corner12,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Text(
                        text = stringResource(R.string.home_streak_format, streakDays),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        if (authUser != null) {
            val initial = (authUser.displayName?.firstOrNull() ?: authUser.email?.firstOrNull())
                ?.uppercaseChar()
                ?.toString()
            UserAvatar(
                photoUrl = authUser.photoUrl,
                initial = initial,
            )
        }
    }
}

/** Avatar người dùng: ảnh nếu có, ngược lại chữ cái đầu, cuối cùng là icon người. */
@Composable
private fun UserAvatar(
    photoUrl: String?,
    initial: String?,
    modifier: Modifier = Modifier,
) {
    val avatarModifier = modifier
        .size(AvatarSize)
        .clip(CircleShape)

    if (!photoUrl.isNullOrBlank()) {
        AsyncImage(
            model = photoUrl,
            contentDescription = stringResource(R.string.auth_avatar_description),
            contentScale = ContentScale.Crop,
            modifier = avatarModifier
        )
    } else {
        Box(
            modifier = avatarModifier.background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (initial != null) {
                Text(
                    text = initial,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
