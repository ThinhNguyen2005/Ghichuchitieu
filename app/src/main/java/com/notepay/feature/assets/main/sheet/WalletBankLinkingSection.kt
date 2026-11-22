package com.notepay.feature.assets.main.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.domain.model.VietQrBank

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BankQrSection(
    banks: List<VietQrBank>,
    selectedBankBin: String?,
    accountNumber: String,
    accountName: String,
    onBankSelected: (VietQrBank) -> Unit,
    onAccountNumberChanged: (String) -> Unit,
    onAccountNameChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedBank = remember(selectedBankBin, banks) {
        banks.find { it.bin == selectedBankBin }
    }
    val hasBankInfo = !selectedBankBin.isNullOrBlank() || accountNumber.isNotBlank() || accountName.isNotBlank()
    val isBankMissing = hasBankInfo && selectedBank == null

    val dimensions = AppTheme.dimensions
    val shapes = AppTheme.shapes

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(dimensions.paddingMedium),
            verticalArrangement = Arrangement.spacedBy(dimensions.spaceSmall + dimensions.spaceExtraSmall),
        ) {
            Text(
                text = stringResource(R.string.wallet_field_vietqr),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
            ) {
                OutlinedTextField(
                    value = selectedBank?.let { "${it.shortName} - ${it.name}" }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.billsplit_receiving_bank)) },
                    placeholder = { Text(stringResource(R.string.billsplit_choose_bank)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    isError = isBankMissing,
                    supportingText = if (isBankMissing) {
                        {
                            Text(
                                text = stringResource(R.string.wallet_vietqr_bank_error),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    } else null,
                    shape = shapes.corner12,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    banks.forEach { bank ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${bank.shortName} - ${bank.name}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            },
                            onClick = {
                                onBankSelected(bank)
                                expanded = false
                            },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = accountNumber,
                onValueChange = onAccountNumberChanged,
                label = { Text(stringResource(R.string.billsplit_account_number_label)) },
                placeholder = { Text(stringResource(R.string.billsplit_account_number_placeholder)) },
                singleLine = true,
                shape = shapes.corner12,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next,
                ),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = accountName,
                onValueChange = onAccountNameChanged,
                label = { Text(stringResource(R.string.transfer_account_name)) },
                placeholder = { Text(stringResource(R.string.billsplit_account_name_placeholder)) },
                singleLine = true,
                shape = shapes.corner12,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
