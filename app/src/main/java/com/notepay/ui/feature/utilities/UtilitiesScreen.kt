package com.notepay.ui.feature.utilities

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notepay.R
import com.notepay.ui.theme.AppTheme

@Composable
fun UtilitiesScreen(
    onNavigateToBillSplit: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToCurrencySettings: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToAppearanceLanguage: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToAppSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 120.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                Text(
                    text = stringResource(R.string.nav_more),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            // Mascot Header Card: "Chọn mèo của bạn"
            item {
                MascotBannerCard()
            }

            // Section 1: CÀI ĐẶT SỔ
            item {
                SectionHeader(title = stringResource(R.string.utilities_section_ledger))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        UtilityRowItem(
                            icon = Icons.AutoMirrored.Outlined.CallSplit,
                            iconTint = Color(0xFF007AFF),
                            title = stringResource(R.string.bill_split_title),
                            subtitle = stringResource(R.string.utilities_bill_split_subtitle),
                            onClick = onNavigateToBillSplit
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.NotificationsActive,
                            iconTint = Color(0xFFFF9500),
                            title = stringResource(R.string.utilities_reminder_title),
                            subtitle = stringResource(R.string.utilities_subscriptions_subtitle),
                            onClick = onNavigateToSubscription
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.AccountBalanceWallet,
                            iconTint = Color(0xFF34C759),
                            title = stringResource(R.string.utilities_wallet_manage_title),
                            subtitle = stringResource(R.string.utilities_wallet_manage_subtitle),
                            onClick = onNavigateToAssets
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.Paid,
                            iconTint = Color(0xFF5856D6),
                            title = stringResource(R.string.utilities_currency_title),
                            subtitle = stringResource(R.string.utilities_currency_subtitle),
                            onClick = onNavigateToCurrencySettings
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.Category,
                            iconTint = Color(0xFFFF2D55),
                            title = stringResource(R.string.utilities_categories_title),
                            subtitle = stringResource(R.string.utilities_categories_subtitle),
                            onClick = onNavigateToCategoryManagement
                        )
                    }
                }
            }

            // Section 2: CÀI ĐẶT ỨNG DỤNG
            item {
                SectionHeader(title = stringResource(R.string.utilities_section_app))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        UtilityRowItem(
                            icon = Icons.Rounded.Palette,
                            iconTint = Color(0xFFAF52DE),
                            title = stringResource(R.string.utilities_appearance_language_title),
                            subtitle = stringResource(R.string.utilities_appearance_language_subtitle),
                            onClick = onNavigateToAppearanceLanguage
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.AutoAwesome,
                            iconTint = Color(0xFFFF9500),
                            title = stringResource(R.string.settings_ai_engine_title),
                            subtitle = stringResource(R.string.utilities_ai_engine_subtitle),
                            onClick = onNavigateToAiSettings
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.Backup,
                            iconTint = Color(0xFF32ADE6),
                            title = stringResource(R.string.utilities_backup_restore_title),
                            subtitle = stringResource(R.string.utilities_backup_restore_subtitle),
                            onClick = onNavigateToBackupRestore
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.Info,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = stringResource(R.string.utilities_app_info_title),
                            subtitle = stringResource(R.string.utilities_app_info_subtitle),
                            onClick = onNavigateToAppSettings
                        )
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}

@Composable
private fun MascotBannerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Pets,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.utilities_mascot_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.utilities_mascot_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun UtilityRowItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outlineVariant,
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    )
}
