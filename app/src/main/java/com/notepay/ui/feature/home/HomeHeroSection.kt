package com.notepay.ui.feature.home

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
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.domain.model.AuthUser
import com.notepay.domain.money.Money
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.LocalAuthUser
import com.notepay.ui.util.rememberUserGreeting

// ── Design tokens của khối hero (bội số của 4) ───────────────────────────────
// `internal` vì HomeScreen và carousel ví dùng chung lề ngang này.
internal val ScreenHorizontalPadding = 16.dp
private val AvatarSize = 36.dp

/**
 * Khối hero dùng chung cho bố cục 1 cột (Compact/Medium) và cột trái của bố cục 2 cột
 * (Expanded): lời chào + số dư hero + carousel ví.
 */
@Composable
internal fun HomeHeroBlock(
    streakDays: Int,
    balanceLabel: String,
    balance: Money,
    income: Money,
    expense: Money,
    backgroundUri: String?,
    isSpecificWallet: Boolean,
    onChangeBackground: (() -> Unit)?,
    wallets: List<WalletSummary>,
    selectedWalletId: Long?,
    totalBalanceAll: Money,
    onSelectWallet: (Long?) -> Unit,
    onAddWallet: () -> Unit,
    onChangeWalletBackground: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        HomeGreetingBar(
            greeting = rememberUserGreeting(),
            streakDays = streakDays,
            authUser = LocalAuthUser.current,
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
        )

        Spacer(modifier = Modifier.height(16.dp))

        HeroBalanceCard(
            label = balanceLabel,
            balance = balance,
            income = income,
            expense = expense,
            backgroundUri = backgroundUri,
            isSpecificWallet = isSpecificWallet,
            onChangeBackground = onChangeBackground,
            modifier = Modifier.padding(horizontal = ScreenHorizontalPadding)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Carousel Thẻ Ví Nhỏ Gọn (Thẻ "Tất cả ví" + từng ví + nút Thêm)
        WalletsCompactCarousel(
            wallets = wallets,
            selectedWalletId = selectedWalletId,
            totalBalanceAll = totalBalanceAll,
            onSelectWallet = onSelectWallet,
            onAddWallet = onAddWallet,
            onChangeBackground = onChangeWalletBackground
        )
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
                    shape = AppTheme.shapes.corner8,
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
