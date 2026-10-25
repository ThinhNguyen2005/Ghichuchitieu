package com.notepay.ui.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper
import java.io.File

/** Carousel thẻ ví nhỏ gọn (Compact Wallet Cards) */
@Composable
internal fun WalletsCompactCarousel(
    wallets: List<WalletSummary>,
    selectedWalletId: Long?,
    modifier: Modifier = Modifier,
    totalBalanceAll: Money,
    onSelectWallet: (Long?) -> Unit,
    onAddWallet: () -> Unit,
    onChangeBackground: ((Long) -> Unit)? = null,

) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = ScreenHorizontalPadding),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thẻ đầu tiên: Tất cả ví
        item(key = "wallet_all") {
            val isSelected = selectedWalletId == null
            CompactWalletCard(
                name = stringResource(R.string.home_all_wallets),
                balance = totalBalanceAll,
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppTheme.colors.primary,
                isSelected = isSelected,
                onClick = { onSelectWallet(null) }
            )
        }

        // Từng thẻ ví riêng biệt
        items(
            items = wallets,
            key = { "wallet_${it.wallet.id}" }
        ) { summary ->
            val isSelected = summary.wallet.id == selectedWalletId
            val icon = WalletUiHelper.getIcon(summary.wallet.iconKey)
            val color = WalletUiHelper.getColor(summary.wallet.colorKey)

            CompactWalletCard(
                name = summary.wallet.name,
                balance = summary.balance,
                icon = icon,
                iconTint = color,
                isSelected = isSelected,
                backgroundUri = summary.backgroundUri,
                onClick = { onSelectWallet(summary.wallet.id) },
                onLongClick = onChangeBackground?.let { cb -> { cb(summary.wallet.id) } }
            )
        }

        // Thẻ nút Thêm ví (+)
        item(key = "wallet_add_button") {
            Surface(
                modifier = Modifier
                    .size(width = 88.dp, height = 92.dp)
                    .clip(AppTheme.shapes.corner16)
                    .clickable(onClick = onAddWallet),
                shape = AppTheme.shapes.corner16,
                color = AppTheme.colors.surface.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, AppTheme.colors.border.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(AppTheme.colors.primary.copy(alpha = 0.12f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.home_add_wallet),
                            tint = AppTheme.colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.home_add_wallet),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

/** Từng thẻ ví nhỏ gọn 156dp x 92dp */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactWalletCard(
    name: String,
    balance: Money,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundUri: String? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val hasCustomBg = !backgroundUri.isNullOrBlank()
    // Nhãn trợ năng cho thao tác nhấn giữ (đổi ảnh nền) để TalkBack đọc được ý nghĩa.
    val longClickLabel = if (onLongClick != null) {
        stringResource(R.string.action_change_card_background)
    } else {
        null
    }
    val borderColor = if (isSelected) AppTheme.colors.primary else AppTheme.colors.border.copy(alpha = 0.5f)
    val borderWidth = if (isSelected) 1.5.dp else 1.dp
    val containerColor = if (isSelected) {
        AppTheme.colors.surfaceRaised
    } else {
        AppTheme.colors.surface
    }

    Surface(
        modifier = modifier
            .size(width = 156.dp, height = 92.dp)
            .clip(AppTheme.shapes.corner16)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = longClickLabel
            ),
        shape = AppTheme.shapes.corner16,
        color = if (hasCustomBg) Color.Transparent else containerColor,
        border = BorderStroke(borderWidth, borderColor),
        tonalElevation = if (isSelected) 3.dp else 1.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (hasCustomBg) {
                val imageModel = remember(backgroundUri) {
                    if (backgroundUri.startsWith("/")) File(backgroundUri) else backgroundUri
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Black.copy(alpha = 0.45f),
                                    Color.Black.copy(alpha = 0.75f)
                                )
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .background(
                                if (hasCustomBg) Color.White.copy(alpha = 0.2f) else iconTint.copy(alpha = 0.15f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (hasCustomBg) Color.White else iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium.copy(
                            shadow = if (hasCustomBg) Shadow(Color.Black.copy(alpha = 0.8f), Offset(0f, 1f), 3f) else null
                        ),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Text(
                    text = MoneyFormatter.format(balance),
                    style = MaterialTheme.typography.titleMedium.copy(
                        shadow = if (hasCustomBg) Shadow(Color.Black.copy(alpha = 0.85f), Offset(0f, 1.5f), 4f) else null
                    ),
                    fontWeight = FontWeight.Bold,
                    color = if (hasCustomBg) Color.White else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CompactWalletCardPreview() {
    NotePayTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CompactWalletCard(
                name = "Tiền mặt",
                balance = Money(1_500_000_00),
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppTheme.colors.primary,
                isSelected = true,
                onClick = {}
            )
            CompactWalletCard(
                name = "Ngân hàng",
                balance = Money(12_000_000_00),
                icon = Icons.Rounded.AccountBalance,
                iconTint = AppTheme.colors.primary,
                isSelected = false,
                onClick = {}
            )
        }
    }
}
