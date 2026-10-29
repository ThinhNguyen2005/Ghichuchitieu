package com.notepay.feature.onboarding.steps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.notepay.R
import com.notepay.feature.onboarding.OnboardingUiState
import com.notepay.feature.onboarding.components.ChoiceRow
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.theme.NotePayTheme

@Composable
fun Step1CurrencyScreen(
    uiState: OnboardingUiState,
    onSelectCurrency: (String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(
            start = AppTheme.dimensions.spaceLarge,
            end = AppTheme.dimensions.spaceLarge,
            top = AppTheme.dimensions.spaceLarge,
            bottom = AppTheme.dimensions.spaceExtraLarge
        ),
        verticalArrangement = Arrangement.spacedBy(AppTheme.dimensions.spaceSmall)
    ) {
        item {
            Column {
                Text(
                    text = stringResource(R.string.onboarding_currency_title),
                    style = MaterialTheme.typography.headlineLarge,
                    color = AppTheme.colors.textPrimary
                )
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceSmall))
                Text(
                    text = stringResource(R.string.onboarding_currency_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = AppTheme.colors.textSecondary
                )
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))
                Text(
                    text = stringResource(R.string.onboarding_currency_section_currency),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted
                )
            }
        }

        items(OnboardingUiState.CURRENCIES, key = { it }) { code ->
            ChoiceRow(
                label = code,
                selected = uiState.currencyCode == code,
                onClick = { onSelectCurrency(code) }
            )
        }

        item {
            Column {
                Spacer(modifier = Modifier.height(AppTheme.dimensions.spaceLarge))
                Text(
                    text = stringResource(R.string.onboarding_currency_section_language),
                    style = MaterialTheme.typography.labelSmall,
                    color = AppTheme.colors.textMuted
                )
            }
        }

        items(OnboardingUiState.LANGUAGES, key = { it.key }) { option ->
            ChoiceRow(
                label = stringResource(option.labelRes),
                selected = uiState.languageCode == option.key,
                onClick = { onSelectLanguage(option.key) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun Step1CurrencyPreview() {
    NotePayTheme {
        Step1CurrencyScreen(
            uiState = OnboardingUiState(),
            onSelectCurrency = {},
            onSelectLanguage = {}
        )
    }
}
