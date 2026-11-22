package com.notepay.feature.assets.main.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.component.bar.GradientBottomActionBar
import com.notepay.core.designsystem.component.bar.GradientTopAppBar
import com.notepay.core.designsystem.component.button.LiquidButton
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletScreen(
    onSaved: suspend (UiFeedback) -> Unit,
    onBack: () -> Unit,
    onFeedback: suspend (UiFeedback) -> Boolean,
    viewModel: AddWalletViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showArchiveConfirm by remember { mutableStateOf(false) }
    var showUnarchiveConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.feedback.collect { feedback ->
            if (feedback.type == FeedbackType.Success) {
                onSaved(feedback)
            } else {
                onFeedback(feedback)
            }
        }
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = {
                    Text(
                        if (state.isEditMode) {
                            stringResource(R.string.action_edit_wallet)
                        } else {
                            stringResource(R.string.home_add_new_wallet)
                        },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.wallet_back),
                        )
                    }
                },
            )
        },
        bottomBar = {
            GradientBottomActionBar {
                LiquidButton(
                    onClick = { viewModel.save() },
                    enabled = state.canSave,
                    surfaceColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 52.dp),
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = stringResource(R.string.action_saving),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            text = if (state.isEditMode) {
                                stringResource(R.string.action_save_changes)
                            } else {
                                stringResource(R.string.home_create_wallet)
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            WalletBasicInfoSection(
                state = state,
                currencyTransformation = currencyTransformation,
                onNatureChanged = viewModel::onNatureChanged,
                onWalletTypeChanged = viewModel::onWalletTypeChanged,
                onPurposeChanged = viewModel::onPurposeChanged,
                onTargetAmountChanged = viewModel::onTargetAmountChanged,
                onTargetDateChanged = viewModel::onTargetDateChanged,
                onIncludeInTotalBalanceChanged = viewModel::onIncludeInTotalBalanceChanged,
                onNameChanged = viewModel::onNameChanged,
                onCurrencyChanged = viewModel::onCurrencyChanged,
                onInitialBalanceChanged = viewModel::onInitialBalanceChanged,
                onCreditLimitChanged = viewModel::onCreditLimitChanged,
            )

            BudgetAlertSection(
                hasBudgetLimit = state.hasBudgetLimit,
                onHasBudgetLimitChanged = viewModel::onHasBudgetLimitChanged,
                budgetPeriod = state.budgetPeriod,
                onBudgetPeriodChanged = viewModel::onBudgetPeriodChanged,
                budgetLimitInput = state.budgetLimitInput,
                onBudgetLimitChanged = viewModel::onBudgetLimitChanged,
                currencyTransformation = currencyTransformation,
            )

            BankQrSection(
                banks = state.banks,
                selectedBankBin = state.bankBin,
                accountNumber = state.accountNumber,
                accountName = state.accountName,
                onBankSelected = viewModel::onBankSelected,
                onAccountNumberChanged = viewModel::onAccountNumberChanged,
                onAccountNameChanged = viewModel::onAccountNameChanged,
            )

            WalletIconPicker(
                selectedIconKey = state.iconKey,
                onIconSelected = viewModel::onIconChanged,
            )

            WalletColorPicker(
                selectedColorKey = state.colorKey,
                usedColorKeys = state.usedColorKeys,
                isAutoColorAssigned = state.isAutoColorAssigned,
                onColorSelected = viewModel::onColorChanged,
            )

            if (state.isEditMode) {
                WalletActionButtonsSection(
                    canDeleteDirectly = state.canDeleteDirectly,
                    canArchive = state.canArchive,
                    isArchived = state.isArchived,
                    onDeleteClick = { showDeleteConfirm = true },
                    onArchiveClick = { showArchiveConfirm = true },
                    onUnarchiveClick = { showUnarchiveConfirm = true },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    WalletActionDialogs(
        walletName = state.name,
        transactionCount = state.transactionCount,
        showDeleteConfirm = showDeleteConfirm,
        showArchiveConfirm = showArchiveConfirm,
        showUnarchiveConfirm = showUnarchiveConfirm,
        onDismissDelete = { showDeleteConfirm = false },
        onConfirmDelete = {
            showDeleteConfirm = false
            viewModel.deleteWallet()
        },
        onDismissArchive = { showArchiveConfirm = false },
        onConfirmArchive = {
            showArchiveConfirm = false
            viewModel.archiveWallet()
        },
        onDismissUnarchive = { showUnarchiveConfirm = false },
        onConfirmUnarchive = {
            showUnarchiveConfirm = false
            viewModel.unarchiveWallet()
        },
    )
}
