package com.notepay.feature.assets.main.sheet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.component.effect.BottomSheetGlass
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.navigation.rememberNotePayHaptic
import com.notepay.core.navigation.shakeOnError
import com.notepay.feature.assets.main.TransferFormState
import com.notepay.feature.assets.main.TransferValidation
import com.notepay.feature.assets.main.WalletAssetItem
import com.notepay.feature.assets.main.validateTransfer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TransferBottomSheet(
    form: TransferFormState,
    wallets: List<WalletAssetItem>,
    onDismiss: () -> Unit,
    onFromWalletChange: (Long) -> Unit,
    onToWalletChange: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
) {
    val performHaptic = rememberNotePayHaptic()
    var amountTouched by remember { mutableStateOf(false) }
    var amountErrorTrigger by remember { mutableIntStateOf(0) }
    var submitErrorTrigger by remember { mutableIntStateOf(0) }

    // Một nguồn xác thực duy nhất cho cả nút xác nhận lẫn lỗi từng trường: nút không thể bật
    // khi dữ liệu sai, và lỗi không thể hiện khi dữ liệu đã hợp lệ.
    val invalid = remember(form.fromWalletId, form.toWalletId, form.amountInput, wallets) {
        validateTransfer(
            fromWalletId = form.fromWalletId,
            toWalletId = form.toWalletId,
            amountInput = form.amountInput,
            wallets = wallets,
        )
    } as? TransferValidation.Invalid
    val targetWallets = remember(wallets, form.fromWalletId) {
        wallets.filter { it.wallet.id != form.fromWalletId }
    }
    val walletErrorText = when {
        invalid?.walletError == null -> null
        wallets.size < 2 -> stringResource(R.string.assets_transfer_need_two_wallets)
        else -> stringResource(R.string.feedback_transfer_same_wallet)
    }
    val isAmountInvalidWhileTyping = invalid?.amountError != null && form.amountInput.isNotBlank()
    val isAmountError = invalid?.amountError != null && (form.amountInput.isNotBlank() || amountTouched)

    LaunchedEffect(isAmountInvalidWhileTyping) {
        if (isAmountInvalidWhileTyping) {
            amountErrorTrigger++
        }
    }
    LaunchedEffect(form.submitError) {
        if (form.submitError != null) {
            submitErrorTrigger++
        }
    }

    BottomSheetGlass(
        visible = true,
        onDismissRequest = onDismiss,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Bàn phím số che mất nút xác nhận trên máy nhỏ: cho sheet cuộn được và nhường
                // chỗ cho IME thay vì đẩy nút ra ngoài tầm với.
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = AppTheme.dimensions.spaceExtraLarge, vertical = AppTheme.dimensions.spaceSmall)
                .padding(bottom = AppTheme.dimensions.spaceMassive),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.assets_transfer_dialog_title),
                    style = AppTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = AppTheme.colors.textPrimary,
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

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            Text(
                text = stringResource(R.string.assets_transfer_from_wallet),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            WalletChoiceRow(
                wallets = wallets,
                selectedId = form.fromWalletId,
                enabled = !form.isSubmitting,
                onSelect = onFromWalletChange,
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            Text(
                text = stringResource(R.string.assets_transfer_to_wallet),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            WalletChoiceRow(
                // Ví nguồn không thể đồng thời là ví đích nên không mời chọn lại nó.
                wallets = targetWallets,
                selectedId = form.toWalletId,
                enabled = !form.isSubmitting,
                onSelect = onToWalletChange,
            )

            AnimatedVisibility(visible = walletErrorText != null) {
                Column {
                    Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                    Text(
                        text = walletErrorText.orEmpty(),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // Amount input
            Text(
                text = stringResource(R.string.assets_transfer_amount),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            OutlinedTextField(
                value = form.amountInput,
                onValueChange = {
                    amountTouched = true
                    onAmountChange(it)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .shakeOnError(amountErrorTrigger),
                enabled = !form.isSubmitting,
                shape = AppTheme.shapes.corner16,
                placeholder = { Text(stringResource(R.string.ui_0), color = AppTheme.colors.textMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                // Chưa gõ gì thì chưa có gì sai: chỉ báo lỗi khi đã chạm hoặc có ký tự mà
                // không đổi được thành số tiền hợp lệ.
                isError = isAmountError,
                supportingText = {
                    AnimatedVisibility(visible = isAmountError) {
                        Text(
                            text = stringResource(R.string.feedback_transfer_invalid_amount),
                            style = AppTheme.typography.labelMedium,
                        )
                    }
                },
                visualTransformation = remember { VietnamCurrencyVisualTransformation() },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.ink,
                    unfocusedBorderColor = AppTheme.colors.border,
                    cursorColor = AppTheme.colors.ink,
                ),
            )

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

            // Note input
            Text(
                text = stringResource(R.string.assets_transfer_note),
                style = AppTheme.typography.labelMedium,
                color = AppTheme.colors.textSecondary,
            )
            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
            OutlinedTextField(
                value = form.note,
                onValueChange = onNoteChange,
                modifier = Modifier.fillMaxWidth(),
                enabled = !form.isSubmitting,
                shape = AppTheme.shapes.corner16,
                placeholder = { Text(stringResource(R.string.assets_transfer_note_hint), color = AppTheme.colors.textMuted) },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AppTheme.colors.ink,
                    unfocusedBorderColor = AppTheme.colors.border,
                    cursorColor = AppTheme.colors.ink,
                ),
            )

            AnimatedVisibility(visible = form.submitError != null) {
                Column(modifier = Modifier.shakeOnError(submitErrorTrigger)) {
                    Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceMedium))
                    Text(
                        text = stringResource(R.string.feedback_transfer_failed),
                        style = AppTheme.typography.labelMedium,
                        color = AppTheme.colors.error,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceExtraLarge))

            Button(
                onClick = {
                    performHaptic(HapticFeedbackType.TextHandleMove)
                    onConfirm()
                },
                // Bật nút khi và chỉ khi validateTransfer đã trả về Valid, nên dữ liệu gửi đi
                // chính là dữ liệu đã qua kiểm tra.
                enabled = invalid == null && !form.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 56.dp),
                shape = AppTheme.shapes.capsule,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppTheme.colors.ink,
                    contentColor = AppTheme.colors.onInk,
                    disabledContainerColor = AppTheme.colors.surfaceSunken,
                    disabledContentColor = AppTheme.colors.textMuted,
                ),
            ) {
                if (form.isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AppTheme.colors.onInk,
                    )
                } else {
                    Text(
                        text = stringResource(R.string.assets_transfer_confirm_btn),
                        style = AppTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

/**
 * Hàng chọn ví: mỗi lựa chọn hiện cả tên và số dư để chọn đúng ví ngay trong sheet,
 * không phải nhớ số dư từ màn hình phía sau.
 */
@Composable
private fun WalletChoiceRow(
    wallets: List<WalletAssetItem>,
    selectedId: Long?,
    enabled: Boolean,
    onSelect: (Long) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall),
        contentPadding = PaddingValues(vertical = 2.dp),
    ) {
        items(items = wallets, key = { it.wallet.id }) { item ->
            val isSelected = item.wallet.id == selectedId
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(item.wallet.id) },
                enabled = enabled,
                label = {
                    Column {
                        Text(
                            text = item.wallet.name,
                            style = AppTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = MoneyFormatter.format(item.balance),
                            style = AppTheme.typography.labelMedium,
                            color = if (isSelected) AppTheme.colors.onInkMuted else AppTheme.colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                shape = AppTheme.shapes.capsule,
                modifier = Modifier.defaultMinSize(minHeight = AppTheme.dimensions.minTouchTarget),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppTheme.colors.ink,
                    selectedLabelColor = AppTheme.colors.onInk,
                    containerColor = AppTheme.colors.surfaceSunken,
                    labelColor = AppTheme.colors.textSecondary,
                    disabledContainerColor = AppTheme.colors.surfaceSunken,
                    disabledLabelColor = AppTheme.colors.textMuted,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = AppTheme.colors.separator,
                    selectedBorderColor = AppTheme.colors.ink,
                    enabled = enabled,
                    selected = isSelected,
                ),
            )
        }
    }
}
