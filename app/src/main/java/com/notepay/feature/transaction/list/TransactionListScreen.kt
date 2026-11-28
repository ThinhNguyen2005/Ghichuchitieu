package com.notepay.feature.transaction.list

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.core.designsystem.adaptive.DetailPaneEmptyState
import com.notepay.core.designsystem.adaptive.LocalWindowLayout
import com.notepay.core.designsystem.adaptive.WindowLayout
import com.notepay.core.designsystem.component.bar.GradientTopAppBar
import com.notepay.core.designsystem.component.calendar.CompactMonthCalendar
import com.notepay.core.designsystem.component.calendar.buildMonthGrid
import com.notepay.core.designsystem.component.dialog.ConfirmDeleteDialog
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.localizedName
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.shared.SharedWalletPermissionGuard
import com.notepay.feature.transaction.component.DateRangePreset
import com.notepay.feature.transaction.component.SwipeableTransactionItem
import com.notepay.feature.transaction.component.sheet.WalletPickerSheet
import com.notepay.feature.transaction.detail.TransactionDetailContent
import com.notepay.feature.transaction.detail.TransactionDetailUiState
import com.notepay.feature.transaction.detail.TransactionDetailViewModel
import com.notepay.feature.transaction.list.sheet.DateRangeBottomSheet
import com.notepay.feature.transaction.list.sheet.FilterBottomSheet
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun TransactionListRoute(
    onBack: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onCreateBillSplit: (Long) -> Unit = {},
    onCreateSubscription: (name: String, amountCents: Long) -> Unit = { _, _ -> },
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
    viewModel: TransactionListViewModel = hiltViewModel(),
    detailViewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val hapticFeedbackEnabled by viewModel.hapticFeedbackEnabled.collectAsStateWithLifecycle()
    val detailState by detailViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            launch { onFeedback(feedback) }
        }
    }
    LaunchedEffect(detailViewModel) {
        detailViewModel.feedback.collect { feedback ->
            launch { onFeedback(feedback) }
        }
    }

    TransactionListScreen(
        state = state,
        detailState = detailState,
        hapticFeedbackEnabled = hapticFeedbackEnabled,
        onBack = onBack,
        onTransactionClick = onTransactionClick,
        onEditTransaction = onEditTransaction,
        onCreateBillSplit = onCreateBillSplit,
        onCreateSubscription = onCreateSubscription,
        onFeedback = onFeedback,
        onToggleViewMode = viewModel::toggleViewMode,
        onOpenWalletPicker = viewModel::openWalletPicker,
        onQueryChanged = viewModel::onQueryChanged,
        onOpenDateRangeSheet = viewModel::openDateRangeSheet,
        onOpenFilterSheet = viewModel::openFilterSheet,
        onGoToToday = viewModel::onGoToToday,
        onPreviousMonth = viewModel::onPreviousMonth,
        onNextMonth = viewModel::onNextMonth,
        onSelectDateRangePreset = viewModel::setDateRangePreset,
        onResetFilters = viewModel::resetFilters,
        onApplyFilters = viewModel::applyFilters,
        onSelectWallet = viewModel::selectWallet,
        onDeleteTransaction = viewModel::delete,
        onSelectDetailTransaction = detailViewModel::select,
        onRetryDetailSync = detailViewModel::retrySync,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    state: TransactionListUiState,
    detailState: TransactionDetailUiState,
    hapticFeedbackEnabled: Boolean,
    onBack: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onCreateBillSplit: (Long) -> Unit = {},
    onCreateSubscription: (name: String, amountCents: Long) -> Unit = { _, _ -> },
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
    onToggleViewMode: () -> Unit = {},
    onOpenWalletPicker: (Boolean) -> Unit = {},
    onQueryChanged: (String) -> Unit = {},
    onOpenDateRangeSheet: (Boolean) -> Unit = {},
    onOpenFilterSheet: (Boolean) -> Unit = {},
    onGoToToday: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectDateRangePreset: (DateRangePreset) -> Unit = {},
    onResetFilters: () -> Unit = {},
    onApplyFilters: (TransactionType?, Boolean, Set<String>) -> Unit = { _, _, _ -> },
    onSelectWallet: (Long?) -> Unit = {},
    onDeleteTransaction: (Transaction) -> Unit = {},
    onSelectDetailTransaction: (Long?) -> Unit = {},
    onRetryDetailSync: () -> Unit = {},
) {

    // Xuất CSV của đúng danh sách đang nhìn thấy. Dựng nội dung ngay trước khi mở hộp chọn
    // tệp nên tệp luôn khớp bộ lọc hiện tại, không phải đọc lại cơ sở dữ liệu.
    val onExportCsv = rememberCsvExporter(
        transactions = state.transactions,
        walletsMap = state.walletsMap,
        onFeedback = onFeedback,
    )

    val dateRangeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val walletPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedCalendarDate by remember { mutableStateOf<LocalDate?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }

    // Thanh hành động gập lại khi thẻ thống kê (mục đầu của danh sách) đã cuộn khỏi đỉnh.
    // derivedStateOf chỉ phát recomposition khi vượt ngưỡng thay vì mỗi pixel cuộn nên không
    // có chi phí recompose theo từng khung hình.
    val listState = rememberLazyListState()
    // Chế độ lịch có cấu trúc mục khác hẳn (lịch đứng trước danh sách) nên phải giữ vị trí
    // cuộn riêng; dùng chung listState sẽ khiến đổi chế độ là nhảy sang một vị trí lạ.
    val calendarListState = rememberLazyListState()
    val isActionBarCollapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 24
        }
    }
    val coroutineScope = rememberCoroutineScope()
    val searchFocusRequester = remember { FocusRequester() }
    var pendingSearchFocus by remember { mutableStateOf(false) }

    // Ô nhập chỉ tồn tại khi thanh đã mở nên chỉ xin focus sau khi header bung ra.
    LaunchedEffect(pendingSearchFocus, isActionBarCollapsed) {
        if (pendingSearchFocus && !isActionBarCollapsed) {
            searchFocusRequester.requestFocus()
            pendingSearchFocus = false
        }
    }

    // Đổi tháng thì bỏ chọn ngày: highlight cũ không còn thuộc lưới đang xem.
    LaunchedEffect(state.calendarYear, state.calendarMonth) {
        selectedCalendarDate = null
    }

    // Tablet/foldable: danh sách bên trái, chi tiết bên phải. Lựa chọn phải sống sót qua
    // xoay máy và resize cửa sổ nên dùng rememberSaveable thay vì remember.
    val isTwoPane = LocalWindowLayout.current == WindowLayout.Expanded
    var selectedTransactionId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Giao dịch đang chọn có thể bị xóa hoặc rơi ra khỏi bộ lọc hiện tại — khi đó pane phải
    // quay về trạng thái chờ thay vì cố hiển thị một bản ghi không còn tồn tại.
    val resolvedTransactionId = selectedTransactionId?.let { id ->
        if (state.isLoading) {
            id
        } else {
            id.takeIf { candidate -> state.transactions.any { it.id == candidate } }
        }
    }

    // Back đóng pane chi tiết trước khi thoát khỏi màn danh sách.
    BackHandler(enabled = isTwoPane && resolvedTransactionId != null) {
        selectedTransactionId = null
    }

    LaunchedEffect(isTwoPane, resolvedTransactionId) {
        if (isTwoPane) {
            onSelectDetailTransaction(resolvedTransactionId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Gradient Top Bar mờ dần: điều hướng, phạm vi ví và chuyển đổi 1 chạm Lịch / Danh sách
            TransactionTopBar(
                isCalendarView = state.isCalendarView,
                onToggleCalendar = onToggleViewMode,
                selectedWalletId = state.selectedWalletId,
                wallets = state.wallets,
                onBack = onBack,
                onOpenWalletPicker = { onOpenWalletPicker(true) }
            )

            // Tìm kiếm, thời gian và bộ lọc gộp trong một khung duy nhất, ngay dưới top bar
            TransactionActionBar(
                query = state.query,
                onQueryChange = onQueryChanged,
                dateLabel = stringResource(state.selectedDateRangePreset.labelRes),
                activeFilterCount = state.activeFilterCount,
                collapsed = isActionBarCollapsed && !state.isCalendarView,
                onExpandSearch = {
                    pendingSearchFocus = true
                    coroutineScope.launch { listState.animateScrollToItem(0) }
                },
                onOpenDateRange = { onOpenDateRangeSheet(true) },
                onOpenFilter = { onOpenFilterSheet(true) },
                searchFocusRequester = searchFocusRequester,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // Ở Compact/Medium pane này chiếm trọn bề rộng; ở Expanded chia 45/55.
                TransactionListPane(
                    state = state,
                    listState = listState,
                    calendarListState = calendarListState,
                    selectedCalendarDate = selectedCalendarDate,
                    selectedTransactionId = resolvedTransactionId,
                    bottomPadding = if (isTwoPane) 24.dp else 100.dp,
                    onCalendarDayClick = { date ->
                        selectedCalendarDate = date
                        // Lịch là mục 0 của danh sách nên nhóm ngày thứ n nằm ở mục n + 1.
                        val groupIndex = state.dayGroups.indexOfFirst { it.date == date }
                        if (groupIndex >= 0) {
                            coroutineScope.launch {
                                calendarListState.animateScrollToItem(groupIndex + 1)
                            }
                        }
                    },
                    onGoToToday = onGoToToday,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onTransactionClick = { txId ->
                        // Expanded: chọn tại chỗ thay vì đẩy route chi tiết lên back stack.
                        if (isTwoPane) selectedTransactionId = txId else onTransactionClick(txId)
                    },
                    onEditTransaction = onEditTransaction,
                    onDeleteTransaction = { tx -> pendingDeleteTransaction = tx },
                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                    onExport = onExportCsv,
                    modifier = Modifier
                        .weight(if (isTwoPane) 0.45f else 1f)
                        .fillMaxHeight(),
                )

                if (isTwoPane) {
                    TransactionDetailPane(
                        transactionId = resolvedTransactionId,
                        detailState = detailState,
                        onRetrySync = onRetryDetailSync,
                        onEdit = onEditTransaction,
                        onCreateBillSplit = onCreateBillSplit,
                        onCreateSubscription = onCreateSubscription,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                    )
                }
            }
        }

        // Bottom Sheet: Khoảng thời gian (Ảnh 3)
        if (state.isDateRangeSheetOpen) {
            DateRangeBottomSheet(
                currentPreset = state.selectedDateRangePreset,
                sheetState = dateRangeSheetState,
                onDismiss = { onOpenDateRangeSheet(false) },
                onSelectPreset = onSelectDateRangePreset,
            )
        }

        // Bottom Sheet: Bộ lọc (Ảnh 4)
        if (state.isFilterSheetOpen) {
            FilterBottomSheet(
                currentType = state.selectedTransactionType,
                isTransferOnly = state.isTransferOnly,
                currentCategoryIds = state.selectedCategoryIds,
                sheetState = filterSheetState,
                onDismiss = { onOpenFilterSheet(false) },
                onClearAll = onResetFilters,
                onApply = onApplyFilters,
            )
        }

        // Bottom Sheet: Chọn tài khoản
        if (state.isWalletPickerOpen) {
            WalletPickerSheet(
                wallets = state.wallets,
                selectedWalletId = state.selectedWalletId,
                sheetState = walletPickerSheetState,
                onWalletSelected = onSelectWallet,
                onAllWalletsSelected = { onSelectWallet(null) },
                onDismiss = { onOpenWalletPicker(false) },
            )
        }

        pendingDeleteTransaction?.let { tx ->
            val itemName = tx.note.ifBlank { tx.category.localizedName() }
            ConfirmDeleteDialog(
                title = stringResource(R.string.confirm_delete_transaction_title),
                itemName = itemName,
                onConfirm = {
                    onDeleteTransaction(tx)
                    // Xóa đúng giao dịch đang mở thì chỉ đóng pane chi tiết — không popBackStack().
                    if (tx.id == selectedTransactionId) selectedTransactionId = null
                    pendingDeleteTransaction = null
                },
                onDismiss = {
                    pendingDeleteTransaction = null
                }
            )
        }
    }
}

/**
 * Pane trái: thanh hành động đã nằm ở trên dùng chung, phần này chỉ chứa thẻ thống kê + danh
 * sách (hoặc lịch). Tách khỏi [TransactionListScreen] để bố cục một cột và hai cột dùng chung
 * đúng một bản triển khai.
 */
@Composable
private fun TransactionListPane(
    state: TransactionListUiState,
    listState: LazyListState,
    calendarListState: LazyListState,
    selectedCalendarDate: LocalDate?,
    selectedTransactionId: Long?,
    bottomPadding: Dp,
    onCalendarDayClick: (LocalDate) -> Unit,
    onGoToToday: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    hapticFeedbackEnabled: Boolean,
    onExport: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // CHẾ ĐỘ LỊCH: lưới chọn ngày gọn ở trên, danh sách giao dịch cuộn liền ngay bên dưới.
    // Gộp cả hai vào một LazyColumn để lịch không chiếm trọn màn hình và để chạm một ngày là
    // cuộn tới đúng nhóm ngày đó thay vì mở dialog.
    NotePayAnimatedContent(
        targetState = Triple(state.isCalendarView, state.isLoading, state.isEmpty),
        modifier = modifier,
        label = "transaction-list-pane",
    ) { (isCalendar, isLoading, isEmpty) ->
        if (isCalendar) {
            val grid = remember(state.calendarYear, state.calendarMonth, state.transactionsByDate) {
                buildMonthGrid(
                    year = state.calendarYear,
                    month = state.calendarMonth,
                    transactionsByDate = state.transactionsByDate,
                )
            }

            LazyColumn(
                state = calendarListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = bottomPadding,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item(key = "calendar") {
                    CompactMonthCalendar(
                        grid = grid,
                        selectedDate = selectedCalendarDate,
                        onDayClick = onCalendarDayClick,
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth,
                        onGoToToday = onGoToToday,
                    )
                }

                if (state.dayGroups.isEmpty()) {
                    item(key = "calendar-empty") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = stringResource(R.string.tx_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.tx_empty_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                } else {
                    items(state.dayGroups, key = { it.date.toString() }) { dayGroup ->
                        DayGroupSection(
                            dayGroup = dayGroup,
                            walletsMap = state.walletsMap,
                            walletsById = state.walletsById,
                            currentUserId = state.currentUserId,
                            selectedTransactionId = selectedTransactionId,
                            onTransactionClick = onTransactionClick,
                            onEditTransaction = onEditTransaction,
                            onDeleteTransaction = onDeleteTransaction,
                            hapticFeedbackEnabled = hapticFeedbackEnabled,
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                when {
                    isLoading -> {
                        SummaryStatisticsCard(
                            totalIncome = state.totalIncome,
                            totalExpense = state.totalExpense,
                            transactionCount = state.transactionCount,
                            netBalance = state.netBalance,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    isEmpty -> {
                        SummaryStatisticsCard(
                            totalIncome = state.totalIncome,
                            totalExpense = state.totalExpense,
                            transactionCount = state.transactionCount,
                            netBalance = state.netBalance,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.tx_empty_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.tx_empty_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                bottom = bottomPadding,
                                top = 8.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item(key = "summary") {
                                SummaryStatisticsCard(
                                    totalIncome = state.totalIncome,
                                    totalExpense = state.totalExpense,
                                    transactionCount = state.transactionCount,
                                    netBalance = state.netBalance,
                                    onExport = onExport,
                                )
                            }
                            items(state.dayGroups, key = { it.date.toString() }) { dayGroup ->
                                DayGroupSection(
                                    dayGroup = dayGroup,
                                    walletsMap = state.walletsMap,
                                    walletsById = state.walletsById,
                                    currentUserId = state.currentUserId,
                                    selectedTransactionId = selectedTransactionId,
                                    onTransactionClick = onTransactionClick,
                                    onEditTransaction = onEditTransaction,
                                    onDeleteTransaction = onDeleteTransaction,
                                    hapticFeedbackEnabled = hapticFeedbackEnabled,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Pane phải của bố cục hai pane. Tái dùng nguyên [TransactionDetailContent] của route
 * độc lập nên không có bản sao nào của UI chi tiết.
 */
@Composable
private fun TransactionDetailPane(
    transactionId: Long?,
    detailState: TransactionDetailUiState,
    onRetrySync: () -> Unit,
    onEdit: (Long) -> Unit,
    onCreateBillSplit: (Long) -> Unit,
    onCreateSubscription: (name: String, amountCents: Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        shape = AppTheme.shapes.corner24,
        color = AppTheme.colors.surfaceSunken,
    ) {
        if (transactionId == null) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                DetailPaneEmptyState()
            }
        } else {
            TransactionDetailContent(
                state = detailState,
                onEdit = onEdit,
                onCreateBillSplit = onCreateBillSplit,
                onCreateSubscription = onCreateSubscription,
                onRetrySync = onRetrySync,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberCsvExporter(
    transactions: List<Transaction>,
    walletsMap: Map<Long, String>,
    onFeedback: suspend (UiFeedback) -> Boolean,
): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val header = stringResource(R.string.tx_export_csv_header)
    val expenseLabel = stringResource(R.string.filter_type_expense)
    val incomeLabel = stringResource(R.string.filter_type_income)
    val transferLabel = stringResource(R.string.filter_type_transfer)
    val successMessage = pluralStringResource(R.plurals.tx_export_success, transactions.size, transactions.size)
    val failedMessage = stringResource(R.string.tx_export_failed)
    val fileName = stringResource(R.string.tx_export_file_name, csvFileNameStem())

    var pendingCsv by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(CsvMimeType),
    ) { uri ->
        val csv = pendingCsv
        pendingCsv = null
        if (uri != null && csv != null) {
            scope.launch {
                val written = writeCsvToUri(context.contentResolver, uri, csv)
                onFeedback(
                    if (written) {
                        UiFeedback(message = successMessage, type = FeedbackType.Success)
                    } else {
                        UiFeedback(message = failedMessage, type = FeedbackType.Error)
                    }
                )
            }
        }
    }

    return {
        pendingCsv = buildTransactionsCsv(
            transactions = transactions,
            walletsMap = walletsMap,
            header = header,
            typeLabels = mapOf(
                TransactionType.EXPENSE to expenseLabel,
                TransactionType.INCOME to incomeLabel,
            ),
            transferLabel = transferLabel,
        )
        launcher.launch(fileName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTopBar(
    isCalendarView: Boolean,
    onToggleCalendar: () -> Unit,
    selectedWalletId: Long?,
    wallets: List<Wallet>,
    onBack: () -> Unit,
    onOpenWalletPicker: () -> Unit,
) {
    val selectedWalletName = if (selectedWalletId == null) {
        stringResource(R.string.filter_all_accounts)
    } else {
        wallets.firstOrNull { it.id == selectedWalletId }?.name ?: stringResource(R.string.wallet_fallback)
    }

    GradientTopAppBar(
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.transactions_header_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Một ví thì không có gì để lọc: ẩn hẳn để top bar trả chỗ cho tiêu đề và nút
                // đổi chế độ xem, thay vì giữ một control không đổi được kết quả nào.
                if (wallets.size > 1) {
                    Spacer(modifier = Modifier.width(6.dp))
                    WalletFilterChip(label = selectedWalletName, onClick = onOpenWalletPicker)
                }
            }
        },
        actions = {
            IconButton(onClick = onToggleCalendar) {
                Icon(
                    imageVector = if (isCalendarView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.CalendarMonth,
                    contentDescription = if (isCalendarView) stringResource(R.string.cd_switch_to_list) else stringResource(R.string.cd_switch_to_calendar),
                    tint = if (isCalendarView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
            }
        }
    )
}

/**
 * Chip lọc theo ví ở thanh trên cùng.
 *
 * Vùng chạm bọc ngoài đạt 48dp theo ngưỡng tiếp cận, còn pill bên trong giữ chiều cao 40dp
 * đúng đặc tả `wallet-selector-chip` trong DESIGN.md nên thanh không cao thêm.
 */
@Composable
private fun WalletFilterChip(label: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .clip(AppTheme.shapes.corner12)
            .clickable(
                onClickLabel = stringResource(R.string.tx_select_wallet_sheet_title),
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = AppTheme.shapes.corner12,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
            border = BorderStroke(1.dp, AppTheme.colors.hairline),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .defaultMinSize(minHeight = 40.dp)
                    .padding(horizontal = 10.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.AccountBalanceWallet,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryStatisticsCard(
    totalIncome: Money,
    totalExpense: Money,
    transactionCount: Int,
    netBalance: Money,
    modifier: Modifier = Modifier,
    onExport: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner20,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Income Column
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.summary_income_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "+${MoneyFormatter.format(totalIncome)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = AppTheme.colors.success
                    )
                }

                // Expense Column
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = stringResource(R.string.summary_expense_header),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "-${MoneyFormatter.format(totalExpense)}",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        ),
                        color = AppTheme.colors.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val netPrefix = if (netBalance.amountInCents >= 0) "+" else ""
                // weight(1f) giữ cho nút xuất file luôn còn chỗ: nếu để Text tự do chiếm
                // chiều rộng, ở font scale 2.0 nó ép cụm bên phải xuống 0dp khiến nhãn bị
                // xuống dòng từng ký tự và làm thẻ cao vọt.
                Text(
                    text = pluralStringResource(
                        R.plurals.summary_sub_format,
                        transactionCount,
                        transactionCount,
                        "$netPrefix${MoneyFormatter.format(netBalance)}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (transactionCount > 0 && onExport != null) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            // Hành động thật thì phải bấm trúng được: vùng chạm 48dp thay cho
                            // hàng chữ ~20dp trước đây.
                            .defaultMinSize(minHeight = 48.dp)
                            .clip(AppTheme.shapes.corner12)
                            .clickable(
                                onClickLabel = stringResource(R.string.summary_export_file),
                                onClick = onExport,
                            )
                            .padding(horizontal = 8.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.summary_export_file),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayGroupSection(
    dayGroup: TransactionDayGroup,
    walletsMap: Map<Long, String>,
    walletsById: Map<Long, Wallet>,
    currentUserId: String?,
    selectedTransactionId: Long?,
    onTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
    hapticFeedbackEnabled: Boolean = true,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Date Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = dayGroup.headerText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val net = dayGroup.totalIncome - dayGroup.totalExpense
            val netColor = if (net.amountInCents >= 0) AppTheme.colors.success else AppTheme.colors.error
            val prefix = if (net.amountInCents >= 0) "+" else ""
            Text(
                text = "$prefix${MoneyFormatter.format(net)}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = netColor
            )
        }

        // Transactions Container
        dayGroup.transactions.forEach { tx ->
            val wallet = walletsById[tx.walletId]
            val canEdit = !tx.isInternalTransfer &&
                (wallet == null || SharedWalletPermissionGuard.canEditTransaction(wallet, tx, currentUserId))
            val canDelete = !(tx.isInternalTransfer && tx.transferGroupId == null) &&
                (wallet == null || SharedWalletPermissionGuard.canDeleteTransaction(wallet, tx, currentUserId))
            SwipeableTransactionItem(
                transaction = tx,
                walletName = walletsMap[tx.walletId] ?: stringResource(R.string.wallet_fallback),
                onClick = { onTransactionClick(tx.id) },
                onEdit = if (canEdit) ({ onEditTransaction(tx.id) }) else null,
                onDelete = if (canDelete) ({ onDeleteTransaction(tx) }) else null,
                hapticEnabled = hapticFeedbackEnabled,
                isSelected = tx.id == selectedTransactionId,
            )
        }
    }
}
