package com.notepay.ui.feature.transaction.list

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.feedback.UiFeedback
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.WalletUiHelper
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.rounded.Close
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.DayDetailDialog
import com.notepay.ui.component.GradientTopAppBar
import com.notepay.ui.component.MonthlyCalendarView
import com.notepay.ui.component.SwipeableTransactionItem
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    onBack: () -> Unit = {},
    onTransactionClick: (Long) -> Unit = {},
    onEditTransaction: (Long) -> Unit = {},
    onFeedback: suspend (UiFeedback) -> Boolean = { false },
    viewModel: TransactionListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dateRangeSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val filterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val walletPickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isSearchActive by remember { mutableStateOf(false) }
    var selectedDayForDetail by remember { mutableStateOf<LocalDate?>(null) }
    var pendingDeleteTransaction by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.feedback.collect { feedback ->
            onFeedback(feedback)
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
            // Gradient Top Bar mờ dần tích hợp tìm kiếm và chuyển đổi 1 chạm Lịch / Danh sách
            TransactionTopBar(
                isSearchActive = isSearchActive,
                searchQuery = state.query,
                onQueryChange = { viewModel.onQueryChanged(it) },
                onToggleSearch = { active ->
                    isSearchActive = active
                    if (!active) {
                        viewModel.onQueryChanged("")
                    }
                },
                isCalendarView = state.isCalendarView,
                onToggleCalendar = { viewModel.toggleViewMode() },
                selectedWalletId = state.selectedWalletId,
                wallets = state.wallets,
                onBack = onBack,
                onOpenWalletPicker = { viewModel.openWalletPicker(true) }
            )

            if (state.isCalendarView) {
                // CHẾ ĐỘ LỊCH: Xem theo lịch tháng 1 chạm
                MonthlyCalendarView(
                    year = state.calendarYear,
                    month = state.calendarMonth,
                    transactions = state.transactions,
                    selectedDate = selectedDayForDetail,
                    onDayClick = { date -> selectedDayForDetail = date },
                    onPreviousMonth = viewModel::onPreviousMonth,
                    onNextMonth = viewModel::onNextMonth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                // CHẾ ĐỘ DANH SÁCH: Thống kê và nhóm giao dịch theo ngày
                // Dual Filter Buttons: [Khoảng thời gian v] [Bộ lọc]
                FilterButtonsRow(
                    currentPreset = state.selectedDateRangePreset,
                    hasActiveFilters = state.selectedTransactionType != null ||
                            state.isTransferOnly ||
                            state.selectedCategoryIds.isNotEmpty(),
                    onOpenDateRange = { viewModel.openDateRangeSheet(true) },
                    onOpenFilter = { viewModel.openFilterSheet(true) }
                )

                // Summary Card (Thu Nhập / Chi Tiêu)
                SummaryStatisticsCard(
                    totalIncome = state.totalIncome,
                    totalExpense = state.totalExpense,
                    transactionCount = state.transactionCount,
                    netBalance = state.netBalance,
                )

                // Transaction Day Groups
                if (state.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                } else if (state.isEmpty) {
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
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 100.dp, top = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.dayGroups, key = { it.date.toString() }) { dayGroup ->
                            DayGroupSection(
                                dayGroup = dayGroup,
                                walletsMap = state.walletsMap,
                                onTransactionClick = onTransactionClick,
                                onEditTransaction = onEditTransaction,
                                onDeleteTransaction = { tx -> pendingDeleteTransaction = tx }
                            )
                        }
                    }
                }
            }
        }

        // Dialog chi tiết ngày (khi tap vào một ngày trên lịch)
        if (selectedDayForDetail != null) {
            DayDetailDialog(
                date = selectedDayForDetail!!,
                transactions = state.transactionsByDate[selectedDayForDetail!!] ?: emptyList(),
                onDismiss = { selectedDayForDetail = null }
            )
        }

        // Bottom Sheet: Khoảng thời gian (Ảnh 3)
        if (state.isDateRangeSheetOpen) {
            DateRangeBottomSheet(
                currentPreset = state.selectedDateRangePreset,
                sheetState = dateRangeSheetState,
                onDismiss = { viewModel.openDateRangeSheet(false) },
                onSelectPreset = { preset ->
                    viewModel.setDateRangePreset(preset)
                }
            )
        }

        // Bottom Sheet: Bộ lọc (Ảnh 4)
        if (state.isFilterSheetOpen) {
            FilterBottomSheet(
                currentType = state.selectedTransactionType,
                isTransferOnly = state.isTransferOnly,
                currentCategoryIds = state.selectedCategoryIds,
                sheetState = filterSheetState,
                onDismiss = { viewModel.openFilterSheet(false) },
                onClearAll = { viewModel.resetFilters() },
                onApply = { type, isTransfer, categories ->
                    viewModel.applyFilters(type, isTransfer, categories)
                }
            )
        }

        // Bottom Sheet: Chọn tài khoản
        if (state.isWalletPickerOpen) {
            WalletPickerBottomSheet(
                wallets = state.wallets,
                selectedWalletId = state.selectedWalletId,
                sheetState = walletPickerSheetState,
                onDismiss = { viewModel.openWalletPicker(false) },
                onSelectWallet = { walletId ->
                    viewModel.selectWallet(walletId)
                }
            )
        }

        pendingDeleteTransaction?.let { tx ->
            val itemName = tx.note.ifBlank { tx.category.displayName }
            ConfirmDeleteDialog(
                title = stringResource(R.string.confirm_delete_transaction_title),
                itemName = itemName,
                onConfirm = {
                    viewModel.delete(tx)
                    pendingDeleteTransaction = null
                },
                onDismiss = {
                    pendingDeleteTransaction = null
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TransactionTopBar(
    isSearchActive: Boolean,
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
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
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    GradientTopAppBar(
        navigationIcon = {
            if (isSearchActive) {
                IconButton(onClick = { onToggleSearch(false) }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        },
        title = {
            if (isSearchActive) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                    singleLine = true,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.filter_search_hint),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .focusRequester(focusRequester)
                )
            } else {
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

                    Surface(
                        onClick = onOpenWalletPicker,
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.AccountBalanceWallet,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = selectedWalletName,
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
        },
        actions = {
            if (isSearchActive) {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(
                            imageVector = Icons.Rounded.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { onToggleSearch(false) }) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                IconButton(onClick = { onToggleSearch(true) }) {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = stringResource(R.string.action_search),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = onToggleCalendar) {
                    Icon(
                        imageVector = if (isCalendarView) Icons.AutoMirrored.Rounded.List else Icons.Rounded.CalendarMonth,
                        contentDescription = if (isCalendarView) stringResource(R.string.cd_switch_to_list) else stringResource(R.string.cd_switch_to_calendar),
                        tint = if (isCalendarView) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    )
}

@Composable
private fun FilterButtonsRow(
    currentPreset: DateRangePreset,
    hasActiveFilters: Boolean,
    onOpenDateRange: () -> Unit,
    onOpenFilter: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Date Range Button
        OutlinedButton(
            onClick = onOpenDateRange,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = currentPreset.labelVi,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
        }

        // Advanced Filter Button
        OutlinedButton(
            onClick = onOpenFilter,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(12.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            BadgedBox(
                badge = {
                    if (hasActiveFilters) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary)
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Rounded.FilterList,
                    contentDescription = null,
                    tint = if (hasActiveFilters) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.filter_advanced),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (hasActiveFilters) FontWeight.Bold else FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun SummaryStatisticsCard(
    totalIncome: Money,
    totalExpense: Money,
    transactionCount: Int,
    netBalance: Money,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        color = Color(0xFF34C759)
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
                        color = Color(0xFFFF3B30)
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
                Text(
                    text = stringResource(
                        R.string.summary_sub_format,
                        transactionCount,
                        "$netPrefix${MoneyFormatter.format(netBalance)}"
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {}
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
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

@Composable
private fun DayGroupSection(
    dayGroup: TransactionDayGroup,
    walletsMap: Map<Long, String>,
    onTransactionClick: (Long) -> Unit,
    onEditTransaction: (Long) -> Unit,
    onDeleteTransaction: (Transaction) -> Unit,
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
            val netColor = if (net.amountInCents >= 0) Color(0xFF34C759) else Color(0xFFFF3B30)
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
            SwipeableTransactionItem(
                transaction = tx,
                walletName = walletsMap[tx.walletId] ?: stringResource(R.string.wallet_fallback),
                onClick = { onTransactionClick(tx.id) },
                onEdit = { onEditTransaction(tx.id) },
                onDelete = { onDeleteTransaction(tx) },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangeBottomSheet(
    currentPreset: DateRangePreset,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectPreset: (DateRangePreset) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_time_period),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            DateRangePreset.entries.forEach { preset ->
                val isSelected = preset == currentPreset
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectPreset(preset) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preset.labelVi,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FilterBottomSheet(
    currentType: TransactionType?,
    isTransferOnly: Boolean,
    currentCategoryIds: Set<String>,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onApply: (TransactionType?, Boolean, Set<String>) -> Unit,
) {
    var selectedType by remember { mutableStateOf(currentType) }
    var selectedTransferOnly by remember { mutableStateOf(isTransferOnly) }
    var selectedCategories by remember { mutableStateOf(currentCategoryIds) }

    val expenseCategories = remember { Category.getAll().filter { !it.isIncome } }
    val incomeCategories = remember { Category.getAll().filter { it.isIncome } }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_advanced),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        selectedType = null
                        selectedTransferOnly = false
                        selectedCategories = emptySet()
                        onClearAll()
                    }) {
                        Text(
                            text = stringResource(R.string.filter_clear_all),
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Phân loại: [ Tất cả ] [ Chi tiêu ] [ Thu nhập ] [ Chuyển khoản ]
            Text(
                text = stringResource(R.string.filter_classification),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val isAllSelected = selectedType == null && !selectedTransferOnly
                FilterChip(
                    selected = isAllSelected,
                    onClick = {
                        selectedType = null
                        selectedTransferOnly = false
                    },
                    label = { Text(stringResource(R.string.filter_type_all)) }
                )
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE && !selectedTransferOnly,
                    onClick = {
                        selectedType = TransactionType.EXPENSE
                        selectedTransferOnly = false
                    },
                    label = { Text(stringResource(R.string.filter_type_expense)) }
                )
                FilterChip(
                    selected = selectedType == TransactionType.INCOME && !selectedTransferOnly,
                    onClick = {
                        selectedType = TransactionType.INCOME
                        selectedTransferOnly = false
                    },
                    label = { Text(stringResource(R.string.filter_type_income)) }
                )
                FilterChip(
                    selected = selectedTransferOnly,
                    onClick = {
                        selectedType = null
                        selectedTransferOnly = true
                    },
                    label = { Text(stringResource(R.string.filter_type_transfer)) }
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Danh mục Chi tiêu Grid
            Text(
                text = stringResource(R.string.filter_category_expense),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                expenseCategories.take(12).forEach { category ->
                    val isCatSelected = category.id in selectedCategories
                    FilterChip(
                        selected = isCatSelected,
                        onClick = {
                            selectedCategories = if (isCatSelected) {
                                selectedCategories - category.id
                            } else {
                                selectedCategories + category.id
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.colorArgb))
                            )
                        },
                        label = { Text(category.displayName, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Danh mục Thu nhập Grid
            Text(
                text = stringResource(R.string.filter_category_income),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                incomeCategories.forEach { category ->
                    val isCatSelected = category.id in selectedCategories
                    FilterChip(
                        selected = isCatSelected,
                        onClick = {
                            selectedCategories = if (isCatSelected) {
                                selectedCategories - category.id
                            } else {
                                selectedCategories + category.id
                            }
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(Color(category.colorArgb))
                            )
                        },
                        label = { Text(category.displayName, fontSize = 12.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Nút Áp dụng lớn
            Button(
                onClick = {
                    onApply(selectedType, selectedTransferOnly, selectedCategories)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = stringResource(R.string.filter_apply),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WalletPickerBottomSheet(
    wallets: List<Wallet>,
    selectedWalletId: Long?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelectWallet: (Long?) -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .padding(bottom = 36.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.tx_select_wallet_sheet_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // "Tất cả tài khoản" option
            val isAllSelected = selectedWalletId == null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onSelectWallet(null) }
                    .padding(horizontal = 12.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccountBalanceWallet,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.filter_all_accounts),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isAllSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                if (isAllSelected) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Individual wallets
            wallets.forEach { wallet ->
                val isSelected = wallet.id == selectedWalletId
                val icon = WalletUiHelper.getIcon(wallet.iconKey)
                val color = WalletUiHelper.getColor(wallet.colorKey)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelectWallet(wallet.id) }
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = color,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = wallet.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
