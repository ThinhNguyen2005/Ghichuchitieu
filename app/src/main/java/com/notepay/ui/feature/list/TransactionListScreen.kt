package com.notepay.ui.feature.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.Category
import com.notepay.domain.model.Transaction
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.DayDetailDialog
import com.notepay.ui.component.EmptyState
import com.notepay.ui.component.MonthlyCalendarView
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.util.MoneyFormatter
import kotlinx.datetime.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onTransactionClick: (Long) -> Unit = {},
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // Ngày được tap trên bảng lịch -> dùng để mở DayDetailDialog
    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }
    // Lưu giao dịch đang chờ xác nhận xóa (khi user nhấn icon Delete trên card).
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(state.pendingUndoTransaction) {
        val transaction = state.pendingUndoTransaction ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = "Đã xóa giao dịch",
            actionLabel = "Hoàn tác",
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoDelete()
        } else {
            viewModel.clearUndo()
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Giao dịch") },
                actions = {
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (state.isCalendarView) Icons.Rounded.List else Icons.Rounded.CalendarMonth,
                            contentDescription = if (state.isCalendarView) "Chuyển sang danh sách" else "Chuyển sang lịch",
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        TransactionListContent(
            state = state,
            onQueryChanged = viewModel::onQueryChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onDelete = viewModel::delete,
            onTransactionClick = onTransactionClick,
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onDayClick = { date -> selectedDay = date },
            modifier = Modifier.padding(padding),
        )
    }

    selectedDay?.let { date ->
        val dayTransactions = state.transactionsByDate[date].orEmpty()
        DayDetailDialog(
            date = date,
            transactions = dayTransactions,
            subscriptions = emptyList(),
            onDismiss = { selectedDay = null },
        )
    }

    pendingDeleteTransaction?.let { tx ->
        ConfirmDeleteDialog(
            title = "Xóa giao dịch?",
            itemName = "${tx.note.ifBlank { tx.category.displayName }} • ${MoneyFormatter.format(tx.amount)}",
            message = "Giao dịch sẽ bị xóa vĩnh viễn. Bạn có thể khôi phục từ thông báo \"Hoàn tác\" sau khi xóa.",
            onConfirm = { viewModel.delete(tx) },
            onDismiss = { pendingDeleteTransaction = null },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionListContent(
    state: TransactionListUiState,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDelete: (Transaction) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isCalendarView) {
        MonthlyCalendarView(
            year = state.calendarYear,
            month = state.calendarMonth,
            transactions = state.transactions,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            onDayClick = onDayClick,
            modifier = modifier,
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        ) {
            item {
                ModernSearchBar(
                    query = state.query,
                    onQueryChanged = onQueryChanged,
                )
                Spacer(Modifier.height(10.dp))
            }

            item {
                CategoryFilterRow(
                    selectedCategory = state.selectedCategory,
                    onCategorySelected = onCategorySelected,
                )
                Spacer(Modifier.height(10.dp))
            }

            when {
                state.isLoading -> item { LoadingState() }
                state.isEmpty -> item { EmptyState("Chưa có giao dịch phù hợp. Thêm khoản chi đầu tiên để bắt đầu theo dõi dòng tiền.") }
                else -> items(state.transactions, key = { it.id }) { transaction ->
                    // Hiển thị TransactionItem trực tiếp — bỏ SwipeToDismissBox.
                    // Nhấn icon Delete trên card sẽ mở ConfirmDeleteDialog xác nhận.
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction.id) },
                        onDelete = { onDelete(transaction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp),
                    )
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

/**
 * Thanh tìm kiếm "phong cách mới":
 *  - Bo góc lớn (RoundedCornerShape(24.dp))
 *  - Backing fill nhẹ (surfaceVariant 30%) thay vì chỉ outline
 *  - Leading icon kính lúp, trailing icon X (clear) khi có text
 *  - Không label nổi, chỉ placeholder
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModernSearchBar(
    query: String,
    onQueryChanged: (String) -> Unit,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChanged,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp)),
        leadingIcon = {
            Icon(
                Icons.Rounded.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChanged("") }) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Xóa tìm kiếm",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        placeholder = {
            Text(
                "Tìm theo ghi chú hoặc danh mục",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
            // P2-12: khi focus, đổi sang primary.copy(alpha=0.5f) để có affordance rõ ràng,
            // tránh cảm giác border biến mất khi nhấn vào.
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
            unfocusedBorderColor = Color.Transparent,
            disabledBorderColor = Color.Transparent,
        ),
    )
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            AssistChip(
                onClick = { onCategorySelected(null) },
                label = { Text("Tất cả") },
            )
        }
        items(Category.entries, key = { it.id }) { category ->
            FilterChip(
                selected = selectedCategory?.id == category.id,
                onClick = {
                    onCategorySelected(if (selectedCategory?.id == category.id) null else category)
                },
                label = { Text(category.displayName) },
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(color = Color.Unspecified)
    }
}
