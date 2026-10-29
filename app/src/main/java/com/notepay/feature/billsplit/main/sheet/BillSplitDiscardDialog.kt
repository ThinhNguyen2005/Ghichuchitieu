package com.notepay.ui.feature.billsplit

import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.notepay.R

/**
 * Hộp thoại xác nhận hủy bỏ của phiếu chia hóa đơn: hiện khi người dùng
 * bấm Đóng hoặc nút Back trong lúc đã có người nợ được nhập.
 */
@Composable
internal fun BillSplitDiscardDialog(
    visible: Boolean,
    onStay: () -> Unit,
    onDiscard: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onStay,
        title = { Text(stringResource(R.string.billsplit_discard_title)) },
        text = { Text(stringResource(R.string.billsplit_discard_message)) },
        confirmButton = {
            TextButton(
                onClick = onDiscard,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.billsplit_discard_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onStay,
                modifier = Modifier.heightIn(min = 48.dp),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}
