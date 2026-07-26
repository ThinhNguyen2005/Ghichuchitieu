package com.notepay.feature.autocapture

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.notepay.data.preferences.AutoCaptureSettings
import com.notepay.data.preferences.AutoCaptureSettingsStore
import com.notepay.data.preferences.KnownBankApps
import com.notepay.ui.theme.AppTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AutoCaptureSettingsViewModel @Inject constructor(
    private val settingsStore: AutoCaptureSettingsStore,
) : ViewModel() {
    val settings = settingsStore.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AutoCaptureSettings(),
    )

    fun setAutoCaptureEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setAutoCaptureEnabled(enabled)
        }
    }

    fun setPackageEnabled(packageName: String, enabled: Boolean) {
        viewModelScope.launch {
            settingsStore.setPackageEnabled(packageName, enabled)
        }
    }
}

/** Bản full có tính năng tự động ghi, nên thêm một item vào màn Cài đặt dùng chung. */
fun LazyListScope.autoCaptureSettingsItem() = item { AutoCaptureSettingsSection() }

@Composable
private fun AutoCaptureSettingsSection(
    viewModel: AutoCaptureSettingsViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
        shape = AppTheme.shapes.corner16,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Tự động ghi từ thông báo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = if (settings.autoCaptureEnabled) {
                    "Chỉ xử lý thông báo từ ứng dụng đã được kiểm chứng."
                } else {
                    "Bật để NotePay có thể tự động ghi nhận giao dịch từ thông báo ngân hàng."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val enabled = !settings.autoCaptureEnabled
                        viewModel.setAutoCaptureEnabled(enabled)
                        if (enabled) {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                            )
                        }
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Bật tự động ghi",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Switch(
                    checked = settings.autoCaptureEnabled,
                    onCheckedChange = { enabled ->
                        viewModel.setAutoCaptureEnabled(enabled)
                        if (enabled) {
                            context.startActivity(
                                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                            )
                        }
                    },
                )
            }
            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS),
                    )
                },
                shape = AppTheme.shapes.corner16,
            ) {
                Text("Cấp quyền đọc thông báo")
            }
            HorizontalDivider()
            Text(
                text = "Ứng dụng ngân hàng",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            KnownBankApps.displayApps.forEach { app ->
                val isSupported = KnownBankApps.isSupported(app.packageName)
                val isEnabled = isSupported && settings.enabledPackages.contains(app.packageName)
                val canToggle = isSupported && settings.autoCaptureEnabled
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = canToggle) {
                            viewModel.setPackageEnabled(app.packageName, !isEnabled)
                        }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = if (isSupported) {
                                if (isEnabled) "Đang nhận diện" else "Đã tắt"
                            } else {
                                "Chưa được hỗ trợ"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Switch(
                        checked = isEnabled,
                        enabled = canToggle,
                        onCheckedChange = { enabled ->
                            viewModel.setPackageEnabled(app.packageName, enabled)
                        },
                    )
                }
            }
        }
    }
}
