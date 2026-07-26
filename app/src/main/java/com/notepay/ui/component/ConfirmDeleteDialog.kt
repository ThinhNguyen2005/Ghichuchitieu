package com.notepay.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Hộp thoại xác nhận xóa dùng chung cho mọi danh sách có hành vi destructive.
 *
 * Trước đây, nhiều màn hình (BillSplit, Subscription) gọi trực tiếp
 * `viewModel.deleteXxx(...)` từ IconButton — chạm nhầm là mất dữ liệu vĩnh viễn.
 *
 * Sử dụng:
 * ```
 * var pendingDelete by remember { mutableStateOf<Subscription?>(null) }
 * pendingDelete?.let { sub ->
 *     ConfirmDeleteDialog(
 *         title = "Xóa nhắc nhở?",
 *         itemName = sub.name,
 *         onConfirm = { viewModel.deleteSubscription(sub.id) },
 *         onDismiss = { pendingDelete = null },
 *     )
 * }
 * ```
 *
 * @param title tiêu đề, ví dụ chuỗi từ `confirm_delete_bill_title`
 * @param itemName tên mục sẽ bị xóa, hiển thị trong message mặc định
 * @param onConfirm callback khi người dùng nhấn nút xác nhận
 * @param onDismiss callback khi người dùng nhấn Hủy hoặc ngoài dialog
 * @param message tuỳ chỉnh mô tả; nếu null sẽ dùng mặc định kèm itemName
 * @param confirmLabel nhãn nút xác nhận (mặc định "Xóa")
 * @param cancelLabel nhãn nút hủy (mặc định "Hủy")
 */
@Composable
fun ConfirmDeleteDialog(
    title: String,
    itemName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    confirmLabel: String = "Xóa",
    cancelLabel: String = "Hủy",
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
            )
        },
        title = { Text(title) },
        text = {
            Text(
                text = message
                    ?: "Bạn có chắc chắn muốn xóa \"$itemName\"? Hành động này không thể hoàn tác.",
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm()
                    onDismiss()
                },
            ) {
                Text(
                    text = confirmLabel,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(cancelLabel)
            }
        },
    )
}
