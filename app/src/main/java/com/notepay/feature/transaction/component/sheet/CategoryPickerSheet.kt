package com.notepay.feature.transaction.component.sheet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.domain.model.Category
import com.notepay.feature.transaction.component.CategoryGridPicker

/**
 * ModalBottomSheet chứa lưới chọn và thêm danh mục tùy biến cho luồng thêm giao dịch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerSheet(
    categories: List<Category>,
    selectedCategory: Category?,
    isIncome: Boolean,
    onCategoryChanged: (Category) -> Unit,
    onDismiss: () -> Unit,
    onCreateCategory: (
        displayName: String,
        colorArgb: Long,
        iconId: String,
        isIncome: Boolean,
    ) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppTheme.shapes.sheetTop,
        containerColor = AppTheme.colors.surface,
        contentColor = AppTheme.colors.textPrimary,
        scrimColor = Color.Black.copy(alpha = 0.45f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        ) {
            CategoryGridPicker(
                categories = categories,
                selectedCategory = selectedCategory,
                isIncome = isIncome,
                onCategoryChanged = onCategoryChanged,
                onCreateCategory = onCreateCategory,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            )
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppTheme.colors.hairline,
            )
            OutlinedButton(
                onClick = onDismiss,
                shape = AppTheme.shapes.capsule,
                border = BorderStroke(1.dp, AppTheme.colors.borderStrong),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = AppTheme.colors.surfaceRaised,
                    contentColor = AppTheme.colors.textPrimary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .defaultMinSize(minHeight = 48.dp),
            ) {
                Text(
                    text = stringResource(R.string.action_close),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
