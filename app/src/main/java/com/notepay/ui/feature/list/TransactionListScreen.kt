package com.notepay.ui.feature.list

import com.notepay.ui.theme.AppTheme

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.notepay.R
import android.content.Context
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
import androidx.compose.material3.FilterChipDefaults
import com.notepay.ui.component.GradientTopAppBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.feedback.FeedbackDuration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.ui.unit.Dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.CategoryAvatar
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
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
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Ngày được tap trên bảng lịch -> mặc định là hôm nay
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(now.date) }
    // Lưu giao dịch đang chờ xác nhận xóa (khi user nhấn icon Delete trên card).
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(state.query) }

    LaunchedEffect(state.pendingUndoTransaction) {
        val transaction = state.pendingUndoTransaction ?: return@LaunchedEffect
        val result = onFeedback(
            UiFeedback(
                message = context.getString(R.string.transaction_deleted),
                actionLabel = context.getString(R.string.feedback_undo),
                duration = FeedbackDuration.Short
            )
        )
        if (result) {
            viewModel.undoDelete()
        } else {
            viewModel.clearUndo()
        }
    }

    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        onFeedback(
            UiFeedback(
                message = message,
                duration = FeedbackDuration.Short
            )
        )
        viewModel.clearError()
    }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = {
                    if (isSearchActive) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { 
                                searchQuery = it
                                viewModel.onQueryChanged(it)
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            stringResource(R.string.transaction_search_placeholder),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                        )
                    } else {
                        Text(stringResource(R.string.nav_transactions))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                searchQuery = ""
                                viewModel.onQueryChanged("")
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = if (isSearchActive) stringResource(R.string.action_close) else stringResource(R.string.action_search)
                        )
                    }
                    IconButton(onClick = { viewModel.toggleViewMode() }) {
                        Icon(
                            imageVector = if (state.isCalendarView) Icons.Rounded.List else Icons.Rounded.CalendarMonth,
                            contentDescription = if (state.isCalendarView) stringResource(R.string.cd_switch_to_list) else stringResource(R.string.cd_switch_to_calendar),
                        )
                    }
                },
            )
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        TransactionListContent(
            state = state,
            onQueryChanged = viewModel::onQueryChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onDelete = { pendingDeleteTransaction = it },
            onTransactionClick = onTransactionClick,
            onPreviousMonth = viewModel::onPreviousMonth,
            onNextMonth = viewModel::onNextMonth,
            onDayClick = { date -> selectedDay = date },
            selectedDate = selectedDay,
            modifier = Modifier.padding(
                start = padding.calculateStartPadding(layoutDirection),
                end = padding.calculateEndPadding(layoutDirection)
            ),
            topSystemPadding = padding.calculateTopPadding(),
            bottomSystemPadding = padding.calculateBottomPadding()
        )
    }

    // DayDetailDialog is removed since the details are shown in the bottom half of the calendar view

    pendingDeleteTransaction?.let { tx ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_transaction_title),
            itemName = "${tx.note.ifBlank { tx.category.displayName }} • ${MoneyFormatter.format(tx.amount)}",
            message = stringResource(R.string.confirm_delete_permanent),
            onConfirm = {
                viewModel.delete(tx)
                pendingDeleteTransaction = null
            },
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
    selectedDate: LocalDate?,
    modifier: Modifier = Modifier,
    topSystemPadding: Dp = 0.dp,
    bottomSystemPadding: Dp = 0.dp,
) {
    if (state.isCalendarView) {
        val dateToShow = selectedDate ?: remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
        val dayTransactions = state.transactionsByDate[dateToShow].orEmpty()
        
        LazyColumn(
            modifier = modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(
                top = topSystemPadding + 8.dp,
                bottom = bottomSystemPadding + 108.dp
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) {
                            MaterialTheme.colorScheme.surfaceContainer
                        } else {
                            Color.White
                        }
                    )
                ) {
                    MonthlyCalendarView(
                        year = state.calendarYear,
                        month = state.calendarMonth,
                        transactions = state.transactions,
                        selectedDate = dateToShow,
                        bottomContentPadding = 0.dp,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onDayClick = onDayClick,
                        modifier = Modifier.fillMaxWidth().height(320.dp),
                    )
                }
            }
            
            item {
                Text(
                    text = stringResource(R.string.transaction_day_details_format, dateToShow.dayOfMonth, dateToShow.monthNumber),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 20.dp, top = 8.dp, bottom = 4.dp)
                )
            }
            
            if (dayTransactions.isNotEmpty()) {
                items(dayTransactions, key = { it.id }) { transaction ->
                    val walletName = state.walletsMap[transaction.walletId] ?: "Ví"
                    TransactionItem(
                        transaction = transaction,
                        walletName = walletName,
                        onClick = { onTransactionClick(transaction.id) },
                        onLongClick = { onDelete(transaction) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.transaction_no_transactions_today),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    } else {
        val groupedTransactions = remember(state.transactions) {
            state.transactions.groupBy { tx ->
                tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            }.toList().sortedByDescending { it.first }
        }

        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                top = topSystemPadding + 12.dp,
                end = 16.dp,
                bottom = bottomSystemPadding + 108.dp
            ),
        ) {
            // ModernSearchBar has been moved to TopAppBar!

            item {
                if (!state.isLoading && !state.isEmpty) {
                    CategoryFilterRow(
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = onCategorySelected,
                    )
                    Spacer(Modifier.height(10.dp))
                }
            }

            when {
                state.isLoading -> item { LoadingState() }
                state.isEmpty -> item {
                    Box(
                        modifier = Modifier
                            .fillParentMaxSize()
                            .padding(bottom = 80.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(stringResource(R.string.transaction_none_found))
                    }
                }
                else -> {
                    groupedTransactions.forEach { (date, dayTxList) ->
                        @OptIn(ExperimentalFoundationApi::class)
                        stickyHeader(key = date.toString()) {
                            val totalIncome = dayTxList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.amountInCents }
                            val totalExpense = dayTxList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.amountInCents }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                            ) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = formatDateHeader(date, LocalContext.current).uppercase(),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (totalIncome > 0L) {
                                            Text(
                                                text = "+${MoneyFormatter.format(Money(totalIncome))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (totalExpense > 0L) {
                                            Text(
                                                text = "-${MoneyFormatter.format(Money(totalExpense))}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        items(dayTxList, key = { it.id }) { transaction ->
                            val walletName = state.walletsMap[transaction.walletId] ?: "Ví"
                            TransactionItem(
                                transaction = transaction,
                                walletName = walletName,
                                onClick = { onTransactionClick(transaction.id) },
                                onLongClick = { onDelete(transaction) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

private fun formatDateHeader(date: LocalDate, context: Context): String {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val diff = date.toEpochDays() - today.toEpochDays()
    return when (diff) {
        0L -> context.getString(R.string.date_today_format, date.dayOfMonth, date.monthNumber)
        -1L -> context.getString(R.string.date_yesterday_format, date.dayOfMonth, date.monthNumber)
        else -> {
            val dayOfWeekStr = when (date.dayOfWeek.ordinal + 1) {
                1 -> context.getString(R.string.day_monday)
                2 -> context.getString(R.string.day_tuesday)
                3 -> context.getString(R.string.day_wednesday)
                4 -> context.getString(R.string.day_thursday)
                5 -> context.getString(R.string.day_friday)
                6 -> context.getString(R.string.day_saturday)
                7 -> context.getString(R.string.day_sunday)
                else -> ""
            }
            context.getString(R.string.date_other_format, dayOfWeekStr, date.dayOfMonth, date.monthNumber)
        }
    }
}

/**
 * Thanh tìm kiếm "phong cách mới":
 *  - Bo góc lớn (AppTheme.shapes.corner24)
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
            .clip(AppTheme.shapes.corner24),
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
                        contentDescription = stringResource(R.string.cd_clear_search),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        placeholder = {
            Text(
                stringResource(R.string.transaction_search_notes_category_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        singleLine = true,
        shape = AppTheme.shapes.corner24,
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
    val lazyListState = rememberLazyListState()
    val showLeftFade by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val showRightFade by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            if (totalItemsNumber == 0) {
                false
            } else {
                val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
                lastVisibleItem == null || lastVisibleItem.index < totalItemsNumber - 1 || 
                    (lastVisibleItem.index == totalItemsNumber - 1 && lastVisibleItem.offset + lastVisibleItem.size > layoutInfo.viewportEndOffset)
            }
        }
    }

    LazyRow(
        state = lazyListState,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalFadingEdge(
                showLeftFade = showLeftFade,
                showRightFade = showRightFade,
                backgroundColor = MaterialTheme.colorScheme.background
            )
    ) {
        item {
            val isSelected = selectedCategory == null
            FilterChip(
                selected = isSelected,
                onClick = { onCategorySelected(null) },
                label = { Text("Tất cả", fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                )
            )
        }
        items(Category.entries, key = { it.id }) { category ->
            val isSelected = selectedCategory?.id == category.id
            FilterChip(
                selected = isSelected,
                onClick = {
                    onCategorySelected(if (selectedCategory?.id == category.id) null else category)
                },
                label = { Text(category.displayName, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = isSelected,
                    borderColor = Color.Transparent,
                    selectedBorderColor = Color.Transparent
                )
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

private fun Modifier.horizontalFadingEdge(
    showLeftFade: Boolean,
    showRightFade: Boolean,
    backgroundColor: Color,
    fadeLength: Dp = 16.dp
): Modifier = this.drawWithContent {
    drawContent()
    
    val fadeLengthPx = fadeLength.toPx()
    
    if (showLeftFade) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(backgroundColor, Color.Transparent),
                startX = 0f,
                endX = fadeLengthPx
            ),
            topLeft = Offset(0f, 0f),
            size = Size(fadeLengthPx, size.height)
        )
    }
    
    if (showRightFade) {
        drawRect(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, backgroundColor),
                startX = size.width - fadeLengthPx,
                endX = size.width
            ),
            topLeft = Offset(size.width - fadeLengthPx, 0f),
            size = Size(fadeLengthPx, size.height)
        )
    }
}
