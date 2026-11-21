package com.notepay.feature.utilities

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Paid
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.core.designsystem.adaptive.LocalContentGutter
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.usesNavigationRail
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.navigation.NotePayStaggeredEntrance
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.rememberNotePayReducedMotion
import com.notepay.domain.model.AuthUser
import com.notepay.feature.auth.main.AuthViewModel
import com.notepay.feature.auth.main.component.sheet.AccountDetailsBottomSheet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UtilitiesScreen(
    onNavigateToBillSplit: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    modifier: Modifier = Modifier,
    onNavigateToSubscription: () -> Unit,
    onNavigateToCurrencySettings: () -> Unit,
    onNavigateToCategoryManagement: () -> Unit,
    onNavigateToAppearanceLanguage: () -> Unit,
    onNavigateToAiSettings: () -> Unit,
    onNavigateToBackupRestore: () -> Unit,
    onNavigateToNotificationSettings: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToDebtManagement: () -> Unit,
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val authUiState by authViewModel.uiState.collectAsStateWithLifecycle()
    var showAccountDetails by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Tablet: lề bù để danh sách một cột không vượt quá 840dp. Bằng 0dp trên điện thoại.
    val contentGutter = LocalContentGutter.current
    val bottomClearance = if (LocalWindowLayout.current.usesNavigationRail) 24.dp else maxOf(
        120.dp,
        104.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
    )

    LaunchedEffect(authViewModel, onFeedback) {
        authViewModel.feedback.collect { feedback -> onFeedback(feedback) }
    }

    val signedInUser = authUiState.user
    if (showAccountDetails && signedInUser != null) {
        AccountDetailsBottomSheet(
            user = signedInUser,
            sheetState = sheetState,
            onDismissRequest = { showAccountDetails = false },
            onSignOut = {
                showAccountDetails = false
                authViewModel.signOut(context)
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp + contentGutter,
                end = 16.dp + contentGutter,
                top = 16.dp,
                bottom = bottomClearance,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "utilities-header") {
                NotePayStaggeredEntrance(index = 0) {
                    Column {
                        Spacer(modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars))
                        Text(
                            text = stringResource(R.string.nav_more),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.padding(vertical = 4.dp),
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        UserProfileHeaderCard(
                            user = authUiState.user,
                            isLoading = authUiState.isLoading,
                            onCardClick = {
                                if (authUiState.user != null) {
                                    showAccountDetails = true
                                } else {
                                    authViewModel.signInWithGoogle(context)
                                }
                            },
                        )
                    }
                }
            }

            // Section 1: CÀI ĐẶT SỔ
            item(key = "utilities-ledger") {
                NotePayStaggeredEntrance(index = 1) {
                    Column {
                        SectionHeader(title = stringResource(R.string.utilities_section_ledger))
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppTheme.shapes.corner20,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                UtilityRowItem(
                                    icon = Icons.AutoMirrored.Outlined.CallSplit,
                                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    title = stringResource(R.string.bill_split_title),
                                    subtitle = stringResource(R.string.utilities_bill_split_subtitle),
                                    onClick = onNavigateToBillSplit,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.Payments,
                                    iconContainerColor = AppTheme.colors.success.copy(alpha = 0.14f),
                                    iconTint = AppTheme.colors.success,
                                    title = stringResource(R.string.debt_nav_title),
                                    subtitle = stringResource(R.string.debt_utilities_subtitle),
                                    onClick = onNavigateToDebtManagement,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.NotificationsActive,
                                    iconContainerColor = AppTheme.colors.warning.copy(alpha = 0.14f),
                                    iconTint = AppTheme.colors.warning,
                                    title = stringResource(R.string.utilities_reminder_title),
                                    subtitle = stringResource(R.string.utilities_subscriptions_subtitle),
                                    onClick = onNavigateToSubscription,
                                )
                                ItemDivider()
                                val currencyCfg = MoneyFormatter.config
                                UtilityRowItem(
                                    icon = Icons.Rounded.Paid,
                                    iconContainerColor = AppTheme.colors.clay.copy(alpha = 0.16f),
                                    iconTint = AppTheme.colors.clay,
                                    title = stringResource(R.string.utilities_currency_title),
                                    subtitle = "${currencyCfg.currencyCode} (${MoneyFormatter.symbolForCode(currencyCfg.currencyCode)})",
                                    onClick = onNavigateToCurrencySettings,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.Category,
                                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    title = stringResource(R.string.utilities_categories_title),
                                    subtitle = stringResource(R.string.utilities_categories_subtitle),
                                    onClick = onNavigateToCategoryManagement,
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: CÀI ĐẶT ỨNG DỤNG
            item(key = "utilities-app") {
                NotePayStaggeredEntrance(index = 2) {
                    Column {
                        SectionHeader(title = stringResource(R.string.utilities_section_app))
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppTheme.shapes.corner20,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                UtilityRowItem(
                                    icon = Icons.Rounded.Palette,
                                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    title = stringResource(R.string.utilities_appearance_language_title),
                                    subtitle = stringResource(R.string.utilities_appearance_language_subtitle),
                                    onClick = onNavigateToAppearanceLanguage,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.AutoAwesome,
                                    iconContainerColor = AppTheme.colors.warning.copy(alpha = 0.14f),
                                    iconTint = AppTheme.colors.warning,
                                    title = stringResource(R.string.ai_settings_title),
                                    subtitle = stringResource(R.string.utilities_ai_engine_subtitle),
                                    onClick = onNavigateToAiSettings,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.Backup,
                                    iconContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    iconTint = MaterialTheme.colorScheme.primary,
                                    title = stringResource(R.string.utilities_backup_restore_title),
                                    subtitle = stringResource(R.string.utilities_backup_restore_subtitle),
                                    onClick = onNavigateToBackupRestore,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.NotificationsActive,
                                    iconContainerColor = AppTheme.colors.clay.copy(alpha = 0.16f),
                                    iconTint = AppTheme.colors.clay,
                                    title = stringResource(R.string.settings_notification_section_title),
                                    subtitle = stringResource(R.string.settings_notification_subtitle),
                                    onClick = onNavigateToNotificationSettings,
                                )
                                ItemDivider()
                                UtilityRowItem(
                                    icon = Icons.Rounded.Info,
                                    iconContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    title = stringResource(R.string.utilities_app_info_title),
                                    subtitle = stringResource(R.string.utilities_app_info_subtitle),
                                    onClick = onNavigateToAbout,
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
private fun UserProfileHeaderCard(
    user: AuthUser?,
    isLoading: Boolean,
    onCardClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val performHaptic = rememberNotePayHaptic()
    val reducedMotion = rememberNotePayReducedMotion()
    val signingInDescription = stringResource(R.string.auth_signing_in)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(AppTheme.shapes.corner24)
            .semantics(mergeDescendants = true) {
                if (isLoading) {
                    stateDescription = signingInDescription
                }
            }
            .clickable(
                enabled = !isLoading,
                role = Role.Button,
                onClick = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onCardClick()
                },
            ),
        shape = AppTheme.shapes.corner24,
        colors = CardDefaults.cardColors(
            containerColor = if (user != null) {
                MaterialTheme.colorScheme.surfaceContainerHigh
            } else {
                MaterialTheme.colorScheme.surfaceContainer
            },
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        AnimatedContent(
            targetState = user to isLoading,
            transitionSpec = {
                if (reducedMotion) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(tween(180)) togetherWith fadeOut(tween(140))
                }
            },
            label = "utilities-profile-state",
        ) { (currentUser, loading) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (currentUser != null) {
                    if (!currentUser.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = currentUser.photoUrl,
                            contentDescription = stringResource(R.string.auth_avatar_description),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        val initial = currentUser.displayName?.firstOrNull()?.uppercaseChar()?.toString()
                            ?: currentUser.email?.firstOrNull()?.uppercaseChar()?.toString()
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (initial != null) {
                                Text(
                                    text = initial,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(30.dp),
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
                        contentAlignment = Alignment.Center,
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(32.dp),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    val title = when {
                        currentUser != null -> currentUser.displayName?.takeIf { it.isNotBlank() }
                            ?: currentUser.email
                            ?: stringResource(R.string.auth_account_name_fallback)
                        loading -> signingInDescription
                        else -> stringResource(R.string.auth_card_unauthenticated_title)
                    }

                    val subtitle = if (currentUser != null) {
                        currentUser.email ?: stringResource(R.string.auth_card_authenticated_subtitle)
                    } else {
                        stringResource(R.string.auth_card_unauthenticated_subtitle)
                    }

                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                if (currentUser != null) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_google_logo),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(20.dp),
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
        modifier = Modifier.padding(start = 4.dp),
    )
}

@Composable
private fun UtilityRowItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(AppTheme.shapes.corner12)
                    .background(iconContainerColor),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(22.dp),
                )
            }
        },
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier.size(14.dp),
            )
        },
    )
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}
