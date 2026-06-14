package com.notepay.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.notepay.domain.model.Money
import com.notepay.ui.util.MoneyFormatter

@Composable
fun KpiRow(
    income: Money,
    expense: Money,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        KpiCard(
            modifier = Modifier.weight(1f),
            label = "Thu nhập",
            value = MoneyFormatter.format(income),
            color = Color(0xFF2E7D32),
        )
        KpiCard(
            modifier = Modifier.weight(1f),
            label = "Chi tiêu",
            value = MoneyFormatter.format(expense),
            color = Color(0xFFC62828),
        )
    }
}

@Composable
private fun KpiCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                color = color,
            )
        }
    }
}
