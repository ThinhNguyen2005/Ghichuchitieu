package com.notepay.ui.feature.addtransaction

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.ui.component.BottomSheetGlass
import com.notepay.ui.component.GradientBottomActionBar
import com.notepay.ui.component.LiquidButton

/**
 * BottomSheet chứa lưới chọn và thêm danh mục tùy biến cho luồng thêm giao dịch.
 */
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
    BottomSheetGlass(
        visible = true,
        onDismissRequest = onDismiss,
        modifier = modifier,
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
        GradientBottomActionBar(
            contentPadding = PaddingValues(top = 8.dp, bottom = 0.dp),
        ) {
            LiquidButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                androidx.compose.material3.Text(stringResource(R.string.action_close))
            }
        }
    }
}
