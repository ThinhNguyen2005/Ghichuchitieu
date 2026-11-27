package com.notepay.feature.settings.appearance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.EmeraldAccentLight
import com.notepay.core.designsystem.theme.EmeraldInk
import com.notepay.core.designsystem.theme.IosBlack
import com.notepay.core.designsystem.theme.IosLightSurfaceContainer
import com.notepay.core.designsystem.theme.IosWhite
import com.notepay.core.designsystem.theme.LavenderAccentLight
import com.notepay.core.designsystem.theme.LavenderInk
import com.notepay.core.designsystem.theme.OceanAccentLight
import com.notepay.core.designsystem.theme.OceanInk
import com.notepay.core.designsystem.theme.Sand
import com.notepay.core.designsystem.theme.SandInk
import com.notepay.core.designsystem.theme.SandOnSand
import com.notepay.feature.home.HomeBackgroundPreset
import com.notepay.feature.home.component.HomeCustomizationSheet
import kotlinx.coroutines.launch

private data class ThemeColorOption(
    val id: String,
    val nameRes: Int,
    val primaryPreview: Color,
    val accentPreview: Color,
    val onPreviewColor: Color,
)

@Composable
private fun rememberThemeColorOptions(): List<ThemeColorOption> {
    val dynamicPrimary = MaterialTheme.colorScheme.primary
    val dynamicContainer = MaterialTheme.colorScheme.primaryContainer
    val dynamicOnPrimary = MaterialTheme.colorScheme.onPrimary
    return remember(dynamicPrimary, dynamicContainer, dynamicOnPrimary) {
        listOf(
            ThemeColorOption("sand", R.string.appearance_color_sand, SandInk, Sand, SandOnSand),
            ThemeColorOption("emerald", R.string.appearance_color_emerald, EmeraldInk, EmeraldAccentLight, IosWhite),
            ThemeColorOption("ocean", R.string.appearance_color_ocean, OceanInk, OceanAccentLight, IosWhite),
            ThemeColorOption("lavender", R.string.appearance_color_lavender, LavenderInk, LavenderAccentLight, IosWhite),
            ThemeColorOption("ios", R.string.appearance_color_ios, IosBlack, IosLightSurfaceContainer, IosWhite),
            ThemeColorOption("dynamic", R.string.theme_dynamic_color, dynamicPrimary, dynamicContainer, dynamicOnPrimary),
        )
    }
}

private data class LanguageOption(
    val code: String,
    val nameRes: Int,
    val flag: String,
)

private val languageOptions = listOf(
    LanguageOption("vi", R.string.appearance_lang_vi, "🇻🇳"),
    LanguageOption("en", R.string.appearance_lang_en, "🇬🇧"),
    LanguageOption("system", R.string.appearance_lang_system, "⚙️"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceLanguageScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AppearanceLanguageViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeColorOptions = rememberThemeColorOptions()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var isHomeCustomizationOpen by rememberSaveable { mutableStateOf(false) }
    val savedPhotoMessage = stringResource(R.string.home_bg_photo_saved)
    val errorPhotoMessage = stringResource(R.string.home_bg_photo_error)

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.utilities_appearance_language_title),
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
        modifier = modifier.fillMaxSize(),
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 16.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Section 1: Chế độ giao diện (Theme Mode)
            item {
                SectionHeader(title = stringResource(R.string.appearance_section_theme))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        val modes = listOf("light", "dark", "system")
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            modes.forEachIndexed { index, mode ->
                                val isSelected = uiState.themeMode == mode
                                SegmentedButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(mode) },
                                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                                    icon = {
                                        Icon(
                                            imageVector = when (mode) {
                                                "light" -> Icons.Rounded.LightMode
                                                "dark" -> Icons.Rounded.DarkMode
                                                else -> Icons.Rounded.BrightnessAuto
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    },
                                ) {
                                    Text(
                                        text = when (mode) {
                                            "light" -> stringResource(R.string.theme_light)
                                            "dark" -> stringResource(R.string.theme_dark)
                                            else -> stringResource(R.string.theme_system)
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 2: Màu sắc chủ đề (Color Presets)
            item {
                SectionHeader(title = stringResource(R.string.settings_theme_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(themeColorOptions, key = { it.id }) { option ->
                                val isSelected = uiState.themeColor == option.id
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .defaultMinSize(minWidth = 56.dp, minHeight = 48.dp)
                                        .clip(AppTheme.shapes.corner12)
                                        .clickable { viewModel.setThemeColor(option.id) }
                                        .padding(4.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .border(
                                                width = if (isSelected) 2.5.dp else 1.dp,
                                                color = if (isSelected) {
                                                    AppTheme.colors.primary
                                                } else {
                                                    AppTheme.colors.hairline
                                                },
                                                shape = CircleShape,
                                            ),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Row(modifier = Modifier.fillMaxSize()) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(option.primaryPreview),
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .background(option.accentPreview),
                                            )
                                        }
                                        if (isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(22.dp)
                                                    .clip(CircleShape)
                                                    .background(option.primaryPreview),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = option.onPreviewColor,
                                                    modifier = Modifier.size(14.dp),
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = stringResource(option.nameRes),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) {
                                            AppTheme.colors.primary
                                        } else {
                                            AppTheme.colors.textSecondary
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 0.5.dp,
                            color = AppTheme.colors.hairline,
                        )
                        ThemeLivePreviewCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                        )
                    }
                }
            }

            // Section 2.5: Tùy chỉnh Trang chủ
            item {
                SectionHeader(title = stringResource(R.string.home_customize_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = AppTheme.colors.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    AppearanceSettingRow(
                        icon = Icons.Rounded.Wallpaper,
                        iconTint = AppTheme.colors.primary,
                        title = stringResource(R.string.home_customize_title),
                        subtitle = stringResource(R.string.appearance_home_customize_desc),
                        modifier = Modifier.clickable { isHomeCustomizationOpen = true },
                        trailing = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Surface(
                                    shape = AppTheme.shapes.capsule,
                                    color = AppTheme.colors.sand,
                                    contentColor = AppTheme.colors.onSand,
                                ) {
                                    Text(
                                        text = stringResource(homePresetLabelRes(uiState.backgroundPreset)),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = AppTheme.colors.textSecondary,
                                )
                            }
                        },
                    )
                }
            }

            // Section 3: Hiệu ứng thị giác (Liquid Glass)
            item {
                SectionHeader(title = stringResource(R.string.appearance_section_glass))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    AppearanceSettingRow(
                        icon = Icons.Rounded.WaterDrop,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.appearance_liquid_glass_title),
                        subtitle = stringResource(R.string.appearance_liquid_glass_desc),
                        trailing = {
                            Switch(
                                checked = uiState.liquidGlassEnabled,
                                onCheckedChange = { viewModel.setLiquidGlassEnabled(it) },
                                enabled = uiState.isLiquidGlassSupported,
                            )
                        },
                    )
                }
            }

            // Section 4: Phản hồi rung (Haptic Feedback)
            item {
                SectionHeader(title = stringResource(R.string.appearance_haptic_feedback_title))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    AppearanceSettingRow(
                        icon = Icons.Rounded.Vibration,
                        iconTint = AppTheme.colors.warning,
                        title = stringResource(R.string.appearance_haptic_feedback_title),
                        subtitle = stringResource(R.string.appearance_haptic_feedback_desc),
                        trailing = {
                            Switch(
                                checked = uiState.hapticFeedbackEnabled,
                                onCheckedChange = { viewModel.setHapticFeedbackEnabled(it) },
                            )
                        },
                    )
                }
            }

            // Section 5: Ngôn ngữ ứng dụng (Language)
            item {
                SectionHeader(title = stringResource(R.string.appearance_section_language))
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        languageOptions.forEachIndexed { index, option ->
                            val isSelected = uiState.appLanguage == option.code
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 56.dp)
                                    .clickable { viewModel.setLanguage(option.code) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = option.flag,
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Text(
                                    text = stringResource(option.nameRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                )
                            }
                            if (index < languageOptions.lastIndex) {
                                ItemDivider()
                            }
                        }
                    }
                }
            }
        }
    }

    if (isHomeCustomizationOpen) {
        HomeCustomizationSheet(
            activePreset = uiState.backgroundPreset,
            customBackgroundPath = uiState.customBackgroundPath,
            backgroundBlurEnabled = uiState.backgroundBlurEnabled,
            isSmartTipsDismissed = uiState.isSmartTipsDismissed,
            onSelectPreset = viewModel::selectBackgroundPreset,
            onPickCustomPhoto = { uri ->
                viewModel.applyCustomBackground(uri) { success ->
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar(
                            if (success) savedPhotoMessage else errorPhotoMessage,
                        )
                    }
                }
            },
            onClearCustomPhoto = viewModel::clearCustomBackground,
            onToggleBlur = viewModel::setBackgroundBlurEnabled,
            onToggleSmartTips = viewModel::setSmartInsightsEnabled,
            onDismiss = { isHomeCustomizationOpen = false },
        )
    }
}

@Composable
private fun ThemeLivePreviewCard(
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = AppTheme.shapes.corner16,
        color = AppTheme.colors.surfaceSunken,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.appearance_theme_preview_label),
                style = MaterialTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = AppTheme.shapes.capsule,
                color = AppTheme.colors.sand,
                contentColor = AppTheme.colors.onSand,
            ) {
                Text(
                    text = stringResource(R.string.appearance_theme_preview_chip),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
            Surface(
                shape = AppTheme.shapes.capsule,
                color = AppTheme.colors.ink,
                contentColor = AppTheme.colors.onInk,
            ) {
                Text(
                    text = stringResource(R.string.appearance_theme_preview_cta),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

private fun homePresetLabelRes(preset: HomeBackgroundPreset): Int = when (preset) {
    HomeBackgroundPreset.WARM_SILK, HomeBackgroundPreset.CUSTOM -> R.string.home_bg_preset_warm_silk
    HomeBackgroundPreset.OCEAN_MIST -> R.string.home_bg_preset_ocean_mist
    HomeBackgroundPreset.SAGE_MORNING -> R.string.home_bg_preset_sage_morning
    HomeBackgroundPreset.SUNSET_BLOOM -> R.string.home_bg_preset_sunset_bloom
    HomeBackgroundPreset.AURORA_LAVENDER -> R.string.home_bg_preset_aurora_lavender
    HomeBackgroundPreset.MINIMAL_CANVAS -> R.string.home_bg_preset_minimal_canvas
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
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 70.dp, end = 16.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun AppearanceSettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp),
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing()
    }
}
