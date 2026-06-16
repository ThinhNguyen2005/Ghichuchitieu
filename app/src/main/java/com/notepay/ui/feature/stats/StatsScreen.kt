package com.notepay.ui.feature.stats

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.geometry.Offset
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.math.atan2
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.util.MoneyFormatter
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.rounded.Wallet
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.style.TextOverflow
import com.notepay.ui.feature.subscription.AddSubscriptionBottomSheet
import com.notepay.ui.feature.subscription.AddSubscriptionDialogState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addSubFormState by viewModel.addSubForm.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showWalletDropdown by remember { mutableStateOf(false) }
    var showTimeDropdown by remember { mutableStateOf(false) }
    var showDateRangePicker by remember { mutableStateOf(false) }
    var isForecastDismissed by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.selectedWallet, state.timeFilter, state.dateRangeLabel) {
        isForecastDismissed = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Thống kê", fontWeight = FontWeight.Bold)
                }
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            state.breakdown.isEmpty() && state.incomeBreakdown.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyStateWithAction(
                        icon = Icons.Outlined.PieChart,
                        title = "Chưa có dữ liệu thống kê",
                        description = "Thêm giao dịch thu/chi trong thời gian này để xem biểu đồ cơ cấu chi tiêu và thu nhập.",
                        actionLabel = "Thêm giao dịch",
                        onClick = onAddTransaction,
                    )
                }
            }
            else -> {
                Column(modifier = Modifier.fillMaxSize().padding(padding)) {
                    PrimaryTabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Cơ cấu chi tiêu") },
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("Cơ cấu thu nhập") },
                        )
                    }

                    // Compact custom chips for Wallet and Period filter
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. Wallet Selector Chip
                        Box {
                            Surface(
                                onClick = { showWalletDropdown = true },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Wallet,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = state.selectedWallet?.name ?: "Tất cả ví",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showWalletDropdown,
                                onDismissRequest = { showWalletDropdown = false },
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tất cả ví", fontWeight = FontWeight.Bold) },
                                    onClick = {
                                        viewModel.selectWallet(null)
                                        showWalletDropdown = false
                                    }
                                )
                                state.wallets.forEach { wallet ->
                                    DropdownMenuItem(
                                        text = { Text(wallet.name) },
                                        onClick = {
                                            viewModel.selectWallet(wallet.id)
                                            showWalletDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // 2. Period Filter Chip
                        Box {
                            Surface(
                                onClick = { showTimeDropdown = true },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CalendarMonth,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = state.timeFilter.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Icon(
                                        imageVector = Icons.Rounded.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showTimeDropdown,
                                onDismissRequest = { showTimeDropdown = false },
                            ) {
                                TimeFilterType.values().forEach { filter ->
                                    DropdownMenuItem(
                                        text = { Text(filter.label) },
                                        onClick = {
                                            showTimeDropdown = false
                                            if (filter == TimeFilterType.CUSTOM) {
                                                showDateRangePicker = true
                                            } else {
                                                viewModel.selectTimeFilter(filter)
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        // 3. Inline Month Switcher Navigation
                        if (state.timeFilter == TimeFilterType.MONTH) {
                            Spacer(modifier = Modifier.weight(1f))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = viewModel::onPreviousMonth,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronLeft, contentDescription = "Tháng trước", modifier = Modifier.size(20.dp))
                                }
                                IconButton(
                                    onClick = viewModel::onNextMonth,
                                    enabled = !state.isCurrentMonth,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = "Tháng sau", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    when (selectedTab) {
                        0 -> ExpenseBreakdownContent(
                            state = state,
                            isForecastDismissed = isForecastDismissed,
                            onDismissForecast = { isForecastDismissed = true },
                            onAddTransaction = onAddTransaction,
                            onCategorySelected = viewModel::selectCategory,
                            onAdviceFeedback = viewModel::sendAdviceFeedback,
                            onAddSubscription = viewModel::showAddSubscription
                        )
                        1 -> IncomeBreakdownContent(
                            state = state,
                            onAddTransaction = onAddTransaction,
                            onCategorySelected = viewModel::selectCategory,
                        )
                    }
                }
            }
        }
    }

    if (showDateRangePicker) {
        val dateRangePickerState = rememberDateRangePickerState()
        DatePickerDialog(
            onDismissRequest = { showDateRangePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val start = dateRangePickerState.selectedStartDateMillis
                        val end = dateRangePickerState.selectedEndDateMillis
                        if (start != null && end != null) {
                            viewModel.selectCustomDateRange(start, end)
                        }
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) {
                    Text("Xác nhận", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDateRangePicker = false }) {
                    Text("Hủy")
                }
            }
        ) {
            DateRangePicker(
                state = dateRangePickerState,
                title = { Text("Chọn khoảng thời gian", modifier = Modifier.padding(16.dp)) },
                showModeToggle = false,
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (addSubFormState.isVisible) {
        val dialogState = AddSubscriptionDialogState(
            name = addSubFormState.name,
            amountInput = addSubFormState.amountInput,
            repeatMonths = addSubFormState.repeatMonths,
            remindDaysBefore = addSubFormState.remindDaysBefore,
            note = addSubFormState.note,
            category = addSubFormState.category,
            nextDueEpochMs = addSubFormState.nextDueEpochMs
        )
        AddSubscriptionBottomSheet(
            state = dialogState,
            onNameChanged = viewModel::updateSubFormName,
            onAmountChanged = viewModel::updateSubFormAmount,
            onRepeatMonthsChanged = viewModel::updateSubFormRepeatMonths,
            onRemindDaysChanged = viewModel::updateSubFormRemindDays,
            onNoteChanged = viewModel::updateSubFormNote,
            onCategoryChanged = viewModel::updateSubFormCategory,
            onNextDueDateChanged = viewModel::updateSubFormNextDueDate,
            onConfirm = viewModel::saveSubscription,
            onDismiss = viewModel::dismissSubForm
        )
    }
}

@Composable
private fun ExpenseBreakdownContent(
    state: StatsUiState,
    isForecastDismissed: Boolean,
    onDismissForecast: () -> Unit,
    onAddTransaction: () -> Unit = {},
    onCategorySelected: (Category?) -> Unit,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (String, Long, String, Long) -> Unit,
) {
    if (state.breakdown.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateWithAction(
                icon = Icons.Outlined.BarChart,
                title = "Chưa có chi tiêu trong tháng này",
                description = "Khi có giao dịch chi tiêu, biểu đồ cơ cấu chi tiêu theo danh mục sẽ hiển thị ở đây.",
                actionLabel = "Thêm giao dịch",
                onClick = onAddTransaction,
            )
        }
        return
    }
    val filteredTransactions = remember(state.transactions, state.selectedCategory) {
        state.transactions.filter {
            it.type == com.notepay.domain.model.TransactionType.EXPENSE && it.category == state.selectedCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Donut Chart card at the very top
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phân bổ danh mục",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val periodText = if (state.timeFilter == TimeFilterType.MONTH) {
                            "Tháng %02d, %d".format(state.month, state.year)
                        } else {
                            state.dateRangeLabel
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    DonutChart(
                        breakdown = state.breakdown,
                        totalExpense = state.totalExpense,
                        centerLabel = "TỔNG CHI TIÊU",
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = onCategorySelected,
                        modifier = Modifier.size(220.dp),
                    )
                }
            }
        }

        // 2. SummaryCards
        item { SummaryCards(totalIncome = state.totalIncome, totalExpense = state.totalExpense) }

        // 3. Budget limits progress
        if (state.budgetLimit != null) {
            item {
                BudgetProgressBar(
                    spent = state.budgetSpent,
                    limit = state.budgetLimit,
                    percentage = state.budgetPercentage
                )
            }
        }

        // 4. AI Insights & Suggestions
        if (state.dynamicDailyBudget != null || state.aiAdvices.isNotEmpty() || state.detectedSubscriptions.isNotEmpty()) {
            item {
                AiInsightsCarousel(
                    state = state,
                    onAdviceFeedback = onAdviceFeedback,
                    onAddSubscription = onAddSubscription
                )
            }
        }
        if (state.selectedCategory != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giao dịch ${state.selectedCategory.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { onCategorySelected(null) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hủy lọc"
                        )
                    }
                }
            }
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionItem(transaction = tx, onClick = {})
            }
        } else {
            item {
                Text(
                    text = "Chi tiết danh mục",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                )
            }
            items(state.breakdown, key = { it.category.name }) { item ->
                CategoryBreakdownRow(item = item, onClick = { onCategorySelected(item.category) })
            }
        }
    }
}

@Composable
private fun IncomeBreakdownContent(
    state: StatsUiState,
    onAddTransaction: () -> Unit = {},
    onCategorySelected: (Category?) -> Unit,
) {
    if (state.incomeBreakdown.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            EmptyStateWithAction(
                icon = Icons.AutoMirrored.Outlined.TrendingUp,
                title = "Chưa có thu nhập trong tháng này",
                description = "Khi có giao dịch thu nhập, biểu đồ cơ cấu thu nhập theo danh mục sẽ hiển thị ở đây.",
                actionLabel = "Thêm giao dịch",
                onClick = onAddTransaction,
            )
        }
        return
    }
    val filteredTransactions = remember(state.transactions, state.selectedCategory) {
        state.transactions.filter {
            it.type == com.notepay.domain.model.TransactionType.INCOME && it.category == state.selectedCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 1. Donut Chart card at the very top
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Phân bổ danh mục",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        val periodText = if (state.timeFilter == TimeFilterType.MONTH) {
                            "Tháng %02d, %d".format(state.month, state.year)
                        } else {
                            state.dateRangeLabel
                        }
                        
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            Text(
                                text = periodText,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    DonutChart(
                        breakdown = state.incomeBreakdown,
                        totalExpense = state.totalIncome,
                        centerLabel = "TỔNG THU NHẬP",
                        selectedCategory = state.selectedCategory,
                        onCategorySelected = onCategorySelected,
                        modifier = Modifier.size(220.dp),
                    )
                }
            }
        }

        // 2. SummaryCards
        item { SummaryCards(totalIncome = state.totalIncome, totalExpense = state.totalExpense) }
        if (state.selectedCategory != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Giao dịch ${state.selectedCategory.displayName}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    IconButton(onClick = { onCategorySelected(null) }) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Hủy lọc"
                        )
                    }
                }
            }
            items(filteredTransactions, key = { it.id }) { tx ->
                TransactionItem(transaction = tx, onClick = {})
            }
        } else {
            item {
                Text(
                    text = "Chi tiết danh mục",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, top = 8.dp),
                )
            }
            items(state.incomeBreakdown, key = { it.category.name }) { item ->
                CategoryBreakdownRow(item = item, onClick = { onCategorySelected(item.category) })
            }
        }
    }
}

@Composable
private fun SummaryCards(
    totalIncome: Money,
    totalExpense: Money,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingUp,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Thu nhập",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = MoneyFormatter.format(totalIncome),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32),
                )
            }
        }

        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.TrendingDown,
                        contentDescription = null,
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Chi tiêu",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = MoneyFormatter.format(totalExpense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828),
                )
            }
        }
    }
}

@Composable
private fun DonutChart(
    breakdown: List<CategoryBreakdownItem>,
    totalExpense: Money,
    centerLabel: String,
    selectedCategory: Category?,
    onCategorySelected: (Category?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val animationProgress = remember { Animatable(0f) }
    val selectionProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (selectedCategory != null) 1f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "selection"
    )
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    LaunchedEffect(breakdown) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800),
        )
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(breakdown, selectedCategory) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val height = size.height
                        val center = Offset(width / 2f, height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)

                        val outerRadius = min(width, height) / 2f
                        val strokeWidthPx = 24.dp.toPx()
                        val tolerancePx = 30.dp.toPx() // Generous touch target padding

                        if (distance >= (outerRadius - strokeWidthPx - tolerancePx) && distance <= (outerRadius + tolerancePx)) {
                            val angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            var normalizedAngle = angleDeg + 90f
                            if (normalizedAngle < 0f) normalizedAngle += 360f

                            var currentAngle = 0f
                            var clickedCategory: Category? = null
                            for (item in breakdown) {
                                val sweepAngle = item.percentage * 360f
                                if (normalizedAngle >= currentAngle && normalizedAngle <= currentAngle + sweepAngle) {
                                    clickedCategory = item.category
                                    break
                                }
                                currentAngle += sweepAngle
                            }
                            if (clickedCategory == selectedCategory) {
                                onCategorySelected(null)
                            } else {
                                onCategorySelected(clickedCategory)
                            }
                        } else {
                            onCategorySelected(null)
                        }
                    }
                }
        ) {
            val strokeWidthPx = 24.dp.toPx()
            // Draw background track ring
            drawCircle(
                color = trackColor,
                radius = (size.minDimension - strokeWidthPx) / 2f,
                style = Stroke(width = strokeWidthPx)
            )

            var startAngle = -90f
            breakdown.forEach { item ->
                val isSelected = selectedCategory == item.category
                val isAnySelected = selectedCategory != null
                
                val currentStrokeWidth = if (isSelected) {
                    (24f + 8f * selectionProgress).dp.toPx()
                } else {
                    24.dp.toPx()
                }
                
                val alpha = if (isAnySelected && !isSelected) 0.4f else 1f
                val sweepAngle = item.percentage * 360f * animationProgress.value
                drawArc(
                    color = Color(item.category.colorArgb).copy(alpha = alpha),
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = Stroke(width = currentStrokeWidth, cap = StrokeCap.Round),
                )
                startAngle += item.percentage * 360f
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp),
        ) {
            Text(
                text = centerLabel.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = MoneyFormatter.format(totalExpense),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdownItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(
                category = item.category,
                size = 40.dp,
                iconSize = 18.dp
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = item.category.displayName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = MoneyFormatter.format(item.amount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { item.percentage },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = Color(item.category.colorArgb),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )
                    Text(
                        text = "%.1f%%".format(item.percentage * 100f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.width(36.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(
    spent: Money,
    limit: Money,
    percentage: Float,
    modifier: Modifier = Modifier,
) {
    val progress = percentage.coerceIn(0f, 1f)
    val color = when {
        percentage >= 1f -> Color(0xFFC62828) // Đỏ khi vượt hạn mức
        percentage >= 0.8f -> Color(0xFFEF6C00) // Cam khi đạt 80%
        else -> Color(0xFF2E7D32) // Xanh mặc định
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (percentage >= 0.8f) Icons.Rounded.Warning else Icons.Rounded.Info,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Hạn mức chi tiêu",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "${(percentage * 100).coerceAtLeast(0f).format(1)}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${MoneyFormatter.format(spent)} / ${MoneyFormatter.format(limit)} (Đã tiêu ${(percentage * 100).coerceAtLeast(0f).format(1)}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}


private fun Float.format(digits: Int): String = "%.${digits}f".format(this)

@Composable
private fun AiInsightsCarousel(
    state: StatsUiState,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (String, Long, String, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val localView = androidx.compose.ui.platform.LocalView.current
    val playHaptic = {
        try {
            localView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP)
        } catch (_: Exception) {}
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "AI Insights & Gợi ý",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        
        androidx.compose.foundation.lazy.LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card 1: Ngân sách ngày linh hoạt
            state.dynamicDailyBudget?.let { budget ->
                item {
                    DynamicDailyBudgetCard(
                        budget = budget,
                        modifier = Modifier.width(280.dp)
                    )
                }
            }

            // Card 2: Các hóa đơn định kỳ phát hiện được
            items(state.detectedSubscriptions) { sub ->
                SubscriptionProposalCard(
                    sub = sub,
                    onAdd = {
                        playHaptic()
                        onAddSubscription(sub.name, sub.amount.amountInCents, sub.category.id, sub.possibleNextDueDate)
                    },
                    modifier = Modifier.width(280.dp)
                )
            }

            // Card 3: Lời khuyên tài chính
            items(state.aiAdvices) { advice ->
                AiAdviceCard(
                    advice = advice,
                    onFeedback = { score ->
                        playHaptic()
                        onAdviceFeedback(advice.id, score)
                    },
                    modifier = Modifier.width(280.dp)
                )
            }
        }
    }
}

@Composable
private fun DynamicDailyBudgetCard(
    budget: DynamicDailyBudgetData,
    modifier: Modifier = Modifier
) {
    val progress = if (budget.dailyBudget.amountInCents > 0) {
        (budget.spentToday.amountInCents.toFloat() / budget.dailyBudget.amountInCents).coerceIn(0f, 1f)
    } else {
        1f
    }
    
    val cardColor = if (budget.isExceeded) {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
    } else {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
    }
    
    val borderColor = if (budget.isExceeded) {
        MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
    } else {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
    }

    Card(
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wallet,
                    contentDescription = null,
                    tint = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Ngân sách ngày linh hoạt",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (budget.isExceeded) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Column {
                Text(
                    text = "Hôm nay: " + MoneyFormatter.format(budget.remainingToday) + " còn lại",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Hạn mức ngày: " + MoneyFormatter.format(budget.dailyBudget) + " (Đã tiêu: " + MoneyFormatter.format(budget.spentToday) + ")",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                color = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            if (budget.isExceeded) {
                Text(
                    text = "⚠️ Bạn đã tiêu quá hạn mức ngày! Dự kiến ngân sách ngày mai sẽ giảm còn " + MoneyFormatter.format(budget.tomorrowBudget) + " để bù lại.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = "💡 Chi tiêu thông minh giúp bảo toàn ngân sách tháng.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AiAdviceCard(
    advice: AiAdviceItem,
    onFeedback: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cardColor = when (advice.type) {
        "warning" -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
        "success" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
    }

    val iconColor = when (advice.type) {
        "warning" -> MaterialTheme.colorScheme.error
        "success" -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.secondary
    }

    Card(
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (advice.type == "warning") Icons.Rounded.Warning else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = advice.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = advice.content,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Lời khuyên hữu ích?",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp)
                )
                
                IconButton(
                    onClick = { onFeedback(1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ThumbUp,
                        contentDescription = "Hữu ích",
                        tint = if (advice.feedback == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = { onFeedback(-1) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ThumbDown,
                        contentDescription = "Không hữu ích",
                        tint = if (advice.feedback == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SubscriptionProposalCard(
    sub: DetectedSubscription,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(165.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Phát hiện hóa đơn định kỳ",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = sub.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Số tiền: " + MoneyFormatter.format(sub.amount) + " / tháng",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            androidx.compose.material3.Button(
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth().height(36.dp),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Thêm subscription",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
