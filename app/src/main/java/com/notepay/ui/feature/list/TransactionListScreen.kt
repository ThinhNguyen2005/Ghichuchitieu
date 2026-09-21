package com.notepay.ui.feature.list

import com.notepay.ui.theme.AppTheme

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.notepay.R
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.zIndex
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.ui.text.style.TextOverflow
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.formatter.TransactionDateHeaderFormatter
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import com.notepay.domain.model.Category
import kotlinx.datetime.LocalDate
import com.notepay.domain.model.Transaction
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.component.SwipeableTransactionItem
import com.notepay.ui.component.MonthlyCalendarView
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.component.DayDetailDialog
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.util.MoneyFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onTransactionClick: (Long) -> Unit = {},
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Ngày được tap trên bảng lịch -> mặc định là hôm nay
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var selectedDay by remember { mutableStateOf<LocalDate?>(now.date) }
    // Lưu giao dịch đang chờ xác nhận xóa (khi user nhấn icon Delete trên card).
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(state.query) }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
        }
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
                            imageVector = if (state.isCalendarView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.CalendarMonth,
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
            today = now.date,
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

    pendingDeleteTransaction?.let { tx ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.confirm_delete_transaction_title),
            itemName = "${tx.category.displayName} • ${MoneyFormatter.format(tx.amount)}",
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
    today: LocalDate,
    onQueryChanged: (String) -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onDelete: (Transaction) -> Unit,
    onTransactionClick: (Long) -> Unit = {},
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    selectedDate: LocalDate?,
    modifier: Modifier = Modifier,
    topSystemPadding: Dp,
    bottomSystemPadding: Dp,
) {
    val todayFormat = stringResource(R.string.date_today_format)
    val yesterdayFormat = stringResource(R.string.date_yesterday_format)
    val otherDateFormat = stringResource(R.string.date_other_format)
    val dayOfWeekLabels = listOf(
        stringResource(R.string.day_monday),
        stringResource(R.string.day_tuesday),
        stringResource(R.string.day_wednesday),
        stringResource(R.string.day_thursday),
        stringResource(R.string.day_friday),
        stringResource(R.string.day_saturday),
        stringResource(R.string.day_sunday),
    )

    if (state.isCalendarView) {
        val dateTxMap = remember(state.transactions) {
            state.transactions.groupBy { tx ->
                tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
            }
        }
        val dayTxList = remember(selectedDate, dateTxMap) {
            if (selectedDate != null) dateTxMap[selectedDate] ?: emptyList() else emptyList()
        }

        Column(
            modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
        ) {
            // Calendar Card — same pattern as SubscriptionScreen CalendarTab
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(top = topSystemPadding + 8.dp, bottom = 8.dp),
                shape = AppTheme.shapes.corner16,
                colors = CardDefaults.cardColors(
                    containerColor = if (!androidx.compose.foundation.isSystemInDarkTheme()) {
                        Color.White
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    }
                )
            ) {
                MonthlyCalendarView(
                    year = state.calendarYear,
                    month = state.calendarMonth,
                    transactions = state.transactions,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    selectedDate = selectedDate,
                    onDayClick = onDayClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp),
                )
            }

            // Day header
            if (selectedDate != null) {
                Text(
                    text = stringResource(R.string.transaction_day_details_format, selectedDate.day, selectedDate.month.number),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
                )
            }

            // Day transaction list
            if (selectedDate != null && dayTxList.isNotEmpty()) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(bottom = bottomSystemPadding + 96.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    dayTxList.forEach { transaction ->
                        val walletName = state.walletsMap[transaction.walletId] ?: stringResource(R.string.wallet_default)
                        SwipeableTransactionItem(
                            transaction = transaction,
                            walletName = walletName,
                            onClick = { onTransactionClick(transaction.id) },
                            onDelete = { onDelete(transaction) },
                            onEdit = { onTransactionClick(transaction.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        )
                    }
                }
            } else if (selectedDate != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = AppTheme.shapes.corner16,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
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
        val categoriesList = remember { listOf(null) + Category.getAll() }
        val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()

        // Find index of selectedCategory
        val selectedIndex = remember(state.selectedCategory, categoriesList) {
            val idx = categoriesList.indexOfFirst { it?.id == state.selectedCategory?.id }
            if (idx == -1) 0 else idx
        }

        val pagerState = androidx.compose.foundation.pager.rememberPagerState(
            initialPage = selectedIndex,
            pageCount = { categoriesList.size }
        )

        // Synchronize pager state page changes back to ViewModel
        LaunchedEffect(pagerState.currentPage) {
            val cat = categoriesList[pagerState.currentPage]
            if (state.selectedCategory?.id != cat?.id) {
                onCategorySelected(cat)
            }
        }

        // Synchronize ViewModel selectedCategory changes back to pagerState
        LaunchedEffect(selectedIndex) {
            if (pagerState.currentPage != selectedIndex) {
                pagerState.animateScrollToPage(selectedIndex)
            }
        }

        Column(
            modifier = modifier.fillMaxSize()
        ) {
            // Category Filter Row — fixed above pager, bọc trong Box để thêm khoảng cách với TopAppBar
            Box(
                modifier = Modifier.padding(top = topSystemPadding + 8.dp, bottom = 4.dp)
            ) {
                CategoryFilterRow(
                    selectedCategory = categoriesList[pagerState.currentPage],
                    onCategorySelected = { category ->
                        val idx = categoriesList.indexOfFirst { it?.id == category?.id }
                        if (idx != -1) {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(idx)
                            }
                        }
                    },
                )
            }

            // 3. HorizontalPager — only the transaction list scrolls horizontally
            androidx.compose.foundation.pager.HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                val pageCategory = categoriesList[page]
                val pageTransactions = remember(state.transactions, pageCategory) {
                    if (pageCategory == null) state.transactions
                    else state.transactions.filter { it.category.id == pageCategory.id }
                }

                val groupedTransactions = remember(pageTransactions) {
                    pageTransactions.groupBy { tx ->
                        tx.occurredAt.toLocalDateTime(TimeZone.currentSystemDefault()).date
                    }.toList().sortedByDescending { it.first }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        bottom = bottomSystemPadding + 108.dp
                    )
                ) {
                    if (state.isLoading) {
                        item { LoadingState() }
                    } else if (pageTransactions.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 48.dp, bottom = 96.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateWithAction(
                                    title = stringResource(R.string.transaction_none_found),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    } else {
                        groupedTransactions.forEach { (date, dayTxList) ->
                            @OptIn(ExperimentalFoundationApi::class)
                            stickyHeader(key = "${page}_${date}") {
                                val (totalIncome, totalExpense) = remember(dayTxList) {
                                    val income = dayTxList.filter { it.type == TransactionType.INCOME }.sumOf { it.amount.amountInCents }
                                    val expense = dayTxList.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount.amountInCents }
                                    income to expense
                                }

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.95f))
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(bottom = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = TransactionDateHeaderFormatter.format(
                                                target = date,
                                                today = today,
                                                todayFormat = todayFormat,
                                                yesterdayFormat = yesterdayFormat,
                                                otherFormat = otherDateFormat,
                                                dayOfWeek = dayOfWeekLabels[date.dayOfWeek.ordinal],
                                            ),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
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
                                val walletName = state.walletsMap[transaction.walletId]
                                    ?: stringResource(R.string.wallet_fallback)
                                SwipeableTransactionItem(
                                    transaction = transaction,
                                    walletName = walletName,
                                    onClick = { onTransactionClick(transaction.id) },
                                    onDelete = { onDelete(transaction) },
                                    onEdit = { onTransactionClick(transaction.id) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionMonthOverview(
    income: Money,
    expense: Money,
    transactionCount: Int,
) {
    val isLightTheme = !androidx.compose.foundation.isSystemInDarkTheme()
    val cardBgColor = if (isLightTheme) Color.White else MaterialTheme.colorScheme.surfaceContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = cardBgColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        stringResource(R.string.transaction_cash_flow_this_month),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        pluralStringResource(
                            R.plurals.transaction_recorded_count,
                            transactionCount,
                            transactionCount,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TransactionAmountSummary(
                    label = stringResource(R.string.transaction_income_label),
                    amount = income,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TransactionAmountSummary(
                    label = stringResource(R.string.transaction_expense_label),
                    amount = expense,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun TransactionAmountSummary(
    label: String,
    amount: Money,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            MoneyFormatter.format(amount),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
) {
    val categoriesList = remember { listOf(null) + Category.getAll() }

    // Find index of selectedCategory
    val selectedIndex = remember(selectedCategory, categoriesList) {
        val idx = categoriesList.indexOfFirst { it?.id == selectedCategory?.id }
        if (idx == -1) 0 else idx
    }

    PrimaryScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 16.dp,
        containerColor = Color.Transparent,
        divider = {}, // No bottom line
        indicator = {
            Box(
                Modifier
                    .tabIndicatorOffset(selectedIndex)
                    .fillMaxHeight()
                    .padding(vertical = 6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .zIndex(-1f)
            )
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        categoriesList.forEachIndexed { index, category ->
            val isSelected = selectedIndex == index
            val text = category?.displayName ?: stringResource(R.string.transaction_list_filter_all)

            Tab(
                selected = isSelected,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = text,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                },
                selectedContentColor = MaterialTheme.colorScheme.onPrimary,
                unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .padding(vertical = 4.dp)
                    .clip(CircleShape)
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
