package com.notepay.feature.home.component

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.notepay.R
import com.notepay.core.designsystem.component.effect.NotePayModalBottomSheet
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.feature.home.HomeBackgroundPreset
import java.io.File

private val builtInBackgroundPresets = listOf(
    HomeBackgroundPreset.WARM_SILK,
    HomeBackgroundPreset.OCEAN_MIST,
    HomeBackgroundPreset.SAGE_MORNING,
    HomeBackgroundPreset.SUNSET_BLOOM,
    HomeBackgroundPreset.AURORA_LAVENDER,
    HomeBackgroundPreset.MINIMAL_CANVAS,
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun HomeCustomizationSheet(
    activePreset: HomeBackgroundPreset,
    customBackgroundPath: String?,
    backgroundBlurEnabled: Boolean,
    isSmartTipsDismissed: Boolean,
    onSelectPreset: (HomeBackgroundPreset) -> Unit,
    onPickCustomPhoto: (Uri) -> Unit,
    onClearCustomPhoto: () -> Unit,
    onToggleBlur: (Boolean) -> Unit,
    onToggleSmartTips: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            onPickCustomPhoto(uri)
        }
    }
    val customFile = remember(customBackgroundPath) {
        customBackgroundPath?.let(::File)?.takeIf { it.exists() && it.canRead() }
    }
    val hasCustomPhoto = activePreset == HomeBackgroundPreset.CUSTOM && customFile != null

    NotePayModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.home_customize_title),
                    style = AppTheme.typography.displaySerif,
                    color = AppTheme.colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .semantics { heading() },
                )
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(AppTheme.dimensions.minTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = AppTheme.colors.textSecondary,
                    )
                }
            }

            // Section 1: Background customization
            Text(
                text = stringResource(R.string.home_bg_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                itemVerticalAlignment = Alignment.CenterVertically,
            ) {
                if (hasCustomPhoto) {
                    AsyncImage(
                        model = customFile,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AppTheme.shapes.corner12),
                    )
                }

                Button(
                    onClick = {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = AppTheme.shapes.capsule,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                    modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PhotoLibrary,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(
                            if (hasCustomPhoto) R.string.home_bg_change_photo else R.string.home_bg_pick_photo
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (hasCustomPhoto) {
                    OutlinedButton(
                        onClick = onClearCustomPhoto,
                        shape = AppTheme.shapes.capsule,
                        border = BorderStroke(1.dp, AppTheme.colors.border),
                        modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.home_bg_remove_photo),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            if (hasCustomPhoto) {
                CustomizationSwitchRow(
                    title = stringResource(R.string.home_bg_blur_title),
                    description = stringResource(R.string.home_bg_blur_desc),
                    checked = backgroundBlurEnabled,
                    onCheckedChange = onToggleBlur,
                )
            }

            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(builtInBackgroundPresets, key = { it.id }) { preset ->
                    PresetChipItem(
                        preset = preset,
                        isSelected = activePreset == preset,
                        onClick = { onSelectPreset(preset) },
                    )
                }
            }

            HorizontalDivider(thickness = 0.5.dp, color = AppTheme.colors.hairline)

            // Section 2: Widgets / Smart Tips toggle
            Text(
                text = stringResource(R.string.home_widgets_section_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = AppTheme.colors.textPrimary,
                modifier = Modifier.semantics { heading() },
            )

            CustomizationSwitchRow(
                title = stringResource(R.string.home_show_smart_tips_title),
                description = stringResource(R.string.home_show_smart_tips_desc),
                checked = !isSmartTipsDismissed,
                onCheckedChange = onToggleSmartTips,
            )
        }
    }
}

@Composable
private fun PresetChipItem(
    preset: HomeBackgroundPreset,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val label = stringResource(presetTitleRes(preset))
    val borderColor = if (isSelected) AppTheme.colors.primary else AppTheme.colors.hairline
    val borderWidth = if (isSelected) 2.dp else 1.dp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .defaultMinSize(minWidth = 68.dp, minHeight = AppTheme.dimensions.minTouchTarget)
            .clip(AppTheme.shapes.corner16)
            .clickable(
                role = Role.RadioButton,
                onClickLabel = label,
                onClick = onClick,
            )
            .semantics { selected = isSelected }
            .padding(4.dp),
    ) {
        Surface(
            modifier = Modifier.size(60.dp),
            shape = AppTheme.shapes.corner16,
            border = BorderStroke(borderWidth, borderColor),
        ) {
            Box(contentAlignment = Alignment.Center) {
                HomePresetSwatch(
                    preset = preset,
                    modifier = Modifier.matchParentSize(),
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(AppTheme.shapes.circle)
                            .background(AppTheme.colors.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CustomizationSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        color = AppTheme.colors.surfaceSunken,
        border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 56.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.textPrimary,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = AppTheme.colors.textSecondary,
                )
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}

private fun presetTitleRes(preset: HomeBackgroundPreset): Int = when (preset) {
    HomeBackgroundPreset.WARM_SILK, HomeBackgroundPreset.CUSTOM -> R.string.home_bg_preset_warm_silk
    HomeBackgroundPreset.OCEAN_MIST -> R.string.home_bg_preset_ocean_mist
    HomeBackgroundPreset.SAGE_MORNING -> R.string.home_bg_preset_sage_morning
    HomeBackgroundPreset.SUNSET_BLOOM -> R.string.home_bg_preset_sunset_bloom
    HomeBackgroundPreset.AURORA_LAVENDER -> R.string.home_bg_preset_aurora_lavender
    HomeBackgroundPreset.MINIMAL_CANVAS -> R.string.home_bg_preset_minimal_canvas
}
