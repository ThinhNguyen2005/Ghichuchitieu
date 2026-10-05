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
import androidx.compose.material.icons.rounded.Payments
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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.notepay.domain.model.AuthUser
import com.notepay.ui.feature.auth.AccountDetailsBottomSheet
import com.notepay.ui.feature.auth.AuthViewModel
import com.notepay.R
import com.notepay.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    onNavigateToBillSplit: () -> Unit,
    onNavigateToSubscription: () -> Unit,
    onNavigateToAssets: () -> Unit = {},
    onNavigateToCurrencySettings: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToAppearanceLanguage: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDebtManagement: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var showAccountDetails by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showAccountDetails && authUiState.user != null) {
        AccountDetailsBottomSheet(
            user = authUiState.user!!,
            sheetState = sheetState,
            onDismissRequest = { showAccountDetails = false },
            onSignOut = { authViewModel.signOut(context) }
        )
    }

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
            // User Profile Header Card (Thay thế chọn mèo)
            item {
                UserProfileHeaderCard(
                    user = authUiState.user,
                    isLoading = authUiState.isLoading,
                    onCardClick = {
                        if (authUiState.user != null) {
                            showAccountDetails = true
                        } else {
                            authViewModel.signInWithGoogle(context)
                        }
                    }
                )
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
                            icon = Icons.Rounded.Payments,
                            iconTint = Color(0xFF34C759),
                            title = stringResource(R.string.debt_nav_title),
                            subtitle = stringResource(R.string.debt_utilities_subtitle),
                            onClick = onNavigateToDebtManagement
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
                            title = stringResource(R.string.ai_settings_title),
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
                            icon = Icons.Rounded.NotificationsActive,
                            iconTint = Color(0xFFFF9500),
                            title = stringResource(R.string.settings_notification_section_title),
                            subtitle = stringResource(R.string.settings_notification_subtitle),
                            onClick = onNavigateToNotificationSettings
                        )
                        ItemDivider()
                        UtilityRowItem(
                            icon = Icons.Rounded.Info,
                            iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                            title = stringResource(R.string.utilities_app_info_title),
                            subtitle = stringResource(R.string.utilities_app_info_subtitle),
                            onClick = onNavigateToAbout
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
private fun UserProfileHeaderCard(
    user: AuthUser?,
    isLoading: Boolean,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner24)
            .clickable(onClick = onCardClick),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = if (user != null) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            if (user != null) {
                if (!user.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = user.photoUrl,
                        contentDescription = stringResource(R.string.auth_avatar_description),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                    )
                } else {
                    val initial = user.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                        ?: user.email?.firstOrNull()?.uppercaseChar()?.toString()
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (initial != null) {
                            Text(
                                text = initial,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // User Info
            Column(modifier = Modifier.weight(1f)) {
                val title = if (user != null) {
                    user.displayName?.takeIf { it.isNotBlank() }
                        ?: user.email
                        ?: stringResource(R.string.auth_account_name_fallback)
                } else {
                    stringResource(R.string.auth_card_unauthenticated_title)
                }

                val subtitle = if (user != null) {
                    user.email ?: stringResource(R.string.auth_card_authenticated_subtitle)
                } else {
                    stringResource(R.string.auth_card_unauthenticated_subtitle)
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing action / icon
            if (user != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_google_logo),
                    contentDescription = null,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(20.dp)
                )
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
