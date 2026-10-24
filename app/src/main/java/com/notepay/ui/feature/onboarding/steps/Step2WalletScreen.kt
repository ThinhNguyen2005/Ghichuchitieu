package com.notepay.ui.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.notepay.R
import com.notepay.domain.model.Wallet
import com.notepay.ui.feature.onboarding.OnboardingUiState
import com.notepay.ui.feature.onboarding.components.IconChoice
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import com.notepay.ui.util.WalletUiHelper
import androidx.compose.ui.unit.dp

@Composable
fun Step2WalletScreen(
    uiState: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onSelectIcon: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                start = AppTheme.dimensions.spaceLarge,
                end = AppTheme.dimensions.spaceLarge,
                top = AppTheme.dimensions.spaceLarge,
                bottom = AppTheme.dimensions.spaceExtraLarge
            )
    ) {
        Text(
            text = stringResource(R.string.onboarding_wallet_title),
            style = MaterialTheme.typography.headlineLarge,
            color = AppTheme.colors.textPrimary
        )
        Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
        Text(
            text = stringResource(R.string.onboarding_wallet_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = AppTheme.colors.textSecondary
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceExtraLarge))

        OutlinedTextField(
            value = uiState.walletName,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.onboarding_wallet_name_label)) },
            singleLine = true,
            shape = AppTheme.shapes.input,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))

        Text(
            text = stringResource(R.string.onboarding_wallet_icon_label),
            style = MaterialTheme.typography.labelSmall,
            color = AppTheme.colors.textMuted
        )

        Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))

        Row(horizontalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall)) {
            OnboardingUiState.WALLET_ICONS.forEach { option ->
                IconChoice(
                    selected = uiState.walletIconKey == option.key,
                    onClick = { onSelectIcon(option.key) }
                ) {
                    Icon(
                        imageVector = WalletUiHelper.getIcon(option.key),
                        contentDescription = stringResource(option.labelRes),
                        tint = if (uiState.walletIconKey == option.key) {
                            AppTheme.colors.primary
                        } else {
                            AppTheme.colors.textSecondary
                        },
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Step2WalletPreview() {
    NotePayTheme {
        Step2WalletScreen(
            uiState = OnboardingUiState(walletName = Wallet.default().name),
            onNameChange = {},
            onSelectIcon = {}
        )
    }
}
