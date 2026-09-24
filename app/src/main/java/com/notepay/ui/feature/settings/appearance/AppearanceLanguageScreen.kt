package com.notepay.ui.feature.settings.appearance

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
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Translate
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.ThemeManager

private data class ThemeColorOption(
    val id: String,
    val nameRes: Int,
    val previewColor: Color,
)

private val themeColorOptions = listOf(
    ThemeColorOption("ios", R.string.appearance_color_ios, Color(0xFF1C1C1E)),
    ThemeColorOption("dynamic", R.string.appearance_color_dynamic, Color(0xFF6750A4)),
    ThemeColorOption("ocean", R.string.appearance_color_ocean, Color(0xFF007AFF)),
    ThemeColorOption("emerald", R.string.appearance_color_emerald, Color(0xFF34C759)),
    ThemeColorOption("amber", R.string.appearance_color_amber, Color(0xFFFF9500)),
    ThemeColorOption("rose", R.string.appearance_color_rose, Color(0xFFFF2D55)),
)

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
    viewModel: AppearanceLanguageViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.appearance_language_title),
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
                SectionHeader(
                    icon = Icons.Rounded.LightMode,
                    title = stringResource(R.string.appearance_section_theme),
                )
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
                                val isSelected = ThemeManager.themeMode == mode
                                SegmentedButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setThemeMode(context, mode) },
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
                                            "light" -> stringResource(R.string.appearance_theme_light)
                                            "dark" -> stringResource(R.string.appearance_theme_dark)
                                            else -> stringResource(R.string.appearance_theme_system)
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
                SectionHeader(
                    icon = Icons.Rounded.Palette,
                    title = stringResource(R.string.appearance_section_color),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(themeColorOptions) { option ->
                            val isSelected = ThemeManager.currentThemeColor == option.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .clickable { viewModel.setThemeColor(context, option.id) }
                                    .padding(4.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(option.previewColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                                Text(
                                    text = stringResource(option.nameRes),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Hiệu ứng thị giác (Liquid Glass)
            item {
                SectionHeader(
                    icon = Icons.Rounded.WaterDrop,
                    title = stringResource(R.string.appearance_section_glass),
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.corner20,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 56.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.appearance_liquid_glass_title),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(R.string.appearance_liquid_glass_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = uiState.liquidGlassEnabled,
                            onCheckedChange = { viewModel.setLiquidGlassEnabled(it) },
                            enabled = uiState.isLiquidGlassSupported,
                        )
                    }
                }
            }

            // Section 4: Ngôn ngữ ứng dụng (Language)
            item {
                SectionHeader(
                    icon = Icons.Rounded.Translate,
                    title = stringResource(R.string.appearance_section_language),
                )
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
                                    .clickable { viewModel.setLanguage(context, option.code) }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = option.flag,
                                    style = MaterialTheme.typography.titleLarge,
                                    modifier = Modifier.padding(end = 14.dp),
                                )
                                Text(
                                    text = stringResource(option.nameRes),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { viewModel.setLanguage(context, option.code) },
                                )
                            }
                            if (index < languageOptions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(start = 56.dp),
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
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
private fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 4.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
