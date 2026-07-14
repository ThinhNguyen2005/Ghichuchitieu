package com.notepay.ui.feature.stats

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.analytics.AdvisorProvider
import com.notepay.domain.analytics.ForecastConfidence
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.TransactionItem
import com.notepay.ui.feature.subscription.AddSubscriptionBottomSheet
import com.notepay.ui.feature.subscription.AddSubscriptionDialogState
import com.notepay.ui.util.MoneyFormatter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

import com.notepay.ui.component.GradientTopAppBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: StatsViewModel = hiltViewModel(),
    onAddTransaction: () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val addSubFormState by viewModel.addSubForm.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableIntStateOf(0) }
    var showDateRangePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            GradientTopAppBar(
                title = { Text("Thống kê", fontWeight = FontWeight.Bold) }
            )
        },
    ) { padding ->
        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
            return@Scaffold
        }

        if (state.breakdown.isEmpty() && state.incomeBreakdown.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyStateWithAction(
                    icon = Icons.Outlined.PieChart,
                    title = "Chưa có dữ liệu thống kê",
                    description = "Chưa có thống kê giao dịch nào.",
                )
            }
            return@Scaffold
        }

        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0; viewModel.selectCategory(null) },
                    text = { Text("Cơ cấu chi tiêu") },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1; viewModel.selectCategory(null) },
                    text = { Text("Cơ cấu thu nhập") },
                )
            }

            Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                when (selectedTab) {
                    0 -> ExpenseBreakdownContent(
                        state = state,
                        wallets = state.wallets,
                        onWalletSelect = { viewModel.selectWallet(it) },
                        onTimeFilterSelect = { filter ->
                            if (filter == TimeFilterType.CUSTOM) showDateRangePicker = true
                            else viewModel.selectTimeFilter(filter)
                        },
                        onPrevMonth = viewModel::onPreviousMonth,
                        onNextMonth = viewModel::onNextMonth,
                        onCategorySelected = viewModel::selectCategory,
                        onAdviceFeedback = viewModel::sendAdviceFeedback,
                        onAddSubscription = viewModel::showAddSubscription,
                        onGenerateLocalAdvice = viewModel::generateLocalAdvice,
                    )
                    1 -> IncomeBreakdownContent(
                        state = state,
                        wallets = state.wallets,
                        onWalletSelect = { viewModel.selectWallet(it) },
                        onTimeFilterSelect = { filter ->
                            if (filter == TimeFilterType.CUSTOM) showDateRangePicker = true
                            else viewModel.selectTimeFilter(filter)
                        },
                        onPrevMonth = viewModel::onPreviousMonth,
                        onNextMonth = viewModel::onNextMonth,
                        onCategorySelected = viewModel::selectCategory,
                    )
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
                        if (start != null && end != null) viewModel.selectCustomDateRange(start, end)
                        showDateRangePicker = false
                    },
                    enabled = dateRangePickerState.selectedStartDateMillis != null && dateRangePickerState.selectedEndDateMillis != null
                ) { Text("Xác nhận", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { showDateRangePicker = false }) { Text("Hủy") } }
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
        AddSubscriptionBottomSheet(
            state = AddSubscriptionDialogState(
                name = addSubFormState.name, amountInput = addSubFormState.amountInput,
                repeatMonths = addSubFormState.repeatMonths, remindDaysBefore = addSubFormState.remindDaysBefore,
                note = addSubFormState.note, category = addSubFormState.category, nextDueEpochMs = addSubFormState.nextDueEpochMs
            ),
            onNameChanged = viewModel::updateSubFormName, onAmountChanged = viewModel::updateSubFormAmount,
            onRepeatMonthsChanged = viewModel::updateSubFormRepeatMonths, onRemindDaysChanged = viewModel::updateSubFormRemindDays,
            onNoteChanged = viewModel::updateSubFormNote, onCategoryChanged = viewModel::updateSubFormCategory,
            onNextDueDateChanged = viewModel::updateSubFormNextDueDate, onConfirm = viewModel::saveSubscription, onDismiss = viewModel::dismissSubForm
        )
    }
}

@Composable
private fun ExpenseBreakdownContent(
    state: StatsUiState,
    wallets: List<com.notepay.domain.model.Wallet>,
    onWalletSelect: (Long?) -> Unit,
    onTimeFilterSelect: (TimeFilterType) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategorySelected: (Category?) -> Unit,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (String, Long, String, Long) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
) {
    val filteredTransactions = remember(state.transactions, state.selectedCategory) {
        state.transactions.filter {
            it.type == com.notepay.domain.model.TransactionType.EXPENSE && it.category == state.selectedCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FilterControlRow(
                        walletLabel = state.selectedWallet?.name ?: "Tất cả ví",
                        timeLabel = if (state.timeFilter == TimeFilterType.MONTH) "Tháng %02d/%d".format(state.month, state.year) else state.dateRangeLabel,
                        showNavigation = state.timeFilter == TimeFilterType.MONTH,
                        isNextEnabled = !state.isCurrentMonth,
                        wallets = wallets,
                        onWalletSelect = onWalletSelect,
                        onTimeFilterSelect = onTimeFilterSelect,
                        onPrevClick = onPrevMonth,
                        onNextClick = onNextMonth
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth().height(230.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DonutChart(
                            breakdown = state.breakdown,
                            totalExpense = state.totalExpense,
                            centerLabel = "Tổng chi tiêu",
                            selectedCategory = state.selectedCategory,
                            onCategorySelected = onCategorySelected,
                            modifier = Modifier.size(220.dp)
                        )
                    }
                }
            }
        }

        item { SummaryCards(totalIncome = state.totalIncome, totalExpense = state.totalExpense) }

        if (state.budgetLimit != null) {
            item { BudgetProgressBar(spent = state.budgetSpent, limit = state.budgetLimit, percentage = state.budgetPercentage) }
        }

        state.spendingForecast?.prediction?.let { prediction ->
            item { SpendingPredictionCard(prediction = prediction) }
        }

        if (state.spendingForecast != null || state.dynamicDailyBudget != null || state.aiAdvices.isNotEmpty() || state.detectedSubscriptions.isNotEmpty()) {
            item {
                AiInsightsCarousel(
                    state = state,
                    onAdviceFeedback = onAdviceFeedback,
                    onAddSubscription = onAddSubscription,
                    onGenerateLocalAdvice = onGenerateLocalAdvice,
                )
            }
        }

        item {
            Text(
                text = "Chi tiết danh mục chi",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        items(state.breakdown, key = { it.category.name }) { item ->
            val isSelected = state.selectedCategory == item.category

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 200))
            ) {
                CategoryBreakdownRow(
                    item = item,
                    isSelected = isSelected,
                    onClick = { onCategorySelected(if (isSelected) null else item.category) }
                )

                if (isSelected) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, start = 8.dp, end = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Lịch sử giao dịch ${item.category.displayName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )

                        if (filteredTransactions.isEmpty()) {
                            Text(
                                text = "Không có giao dịch nào trong khoảng thời gian này.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        } else {
                            filteredTransactions.forEach { tx ->
                                TransactionItem(transaction = tx, onClick = {})
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun IncomeBreakdownContent(
    state: StatsUiState,
    wallets: List<com.notepay.domain.model.Wallet>,
    onWalletSelect: (Long?) -> Unit,
    onTimeFilterSelect: (TimeFilterType) -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onCategorySelected: (Category?) -> Unit,
) {
    val filteredTransactions = remember(state.transactions, state.selectedCategory) {
        state.transactions.filter {
            it.type == com.notepay.domain.model.TransactionType.INCOME && it.category == state.selectedCategory
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                shape = RoundedCornerShape(20.dp),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    FilterControlRow(
                        walletLabel = state.selectedWallet?.name ?: "Tất cả ví",
                        timeLabel = if (state.timeFilter == TimeFilterType.MONTH) "Tháng %02d/%d".format(state.month, state.year) else state.dateRangeLabel,
                        showNavigation = state.timeFilter == TimeFilterType.MONTH,
                        isNextEnabled = !state.isCurrentMonth,
                        wallets = wallets,
                        onWalletSelect = onWalletSelect,
                        onTimeFilterSelect = onTimeFilterSelect,
                        onPrevClick = onPrevMonth,
                        onNextClick = onNextMonth
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(modifier = Modifier.fillMaxWidth().height(230.dp), contentAlignment = Alignment.Center) {
                        DonutChart(
                            breakdown = state.incomeBreakdown, totalExpense = state.totalIncome,
                            centerLabel = "Tổng thu nhập", selectedCategory = state.selectedCategory,
                            onCategorySelected = onCategorySelected, modifier = Modifier.size(220.dp)
                        )
                    }
                }
            }
        }

        item { SummaryCards(totalIncome = state.totalIncome, totalExpense = state.totalExpense) }

        item {
            Text(
                text = "Chi tiết danh mục thu",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }

        items(state.incomeBreakdown, key = { it.category.name }) { item ->
            val isSelected = state.selectedCategory == item.category
            Column(modifier = Modifier.fillMaxWidth().animateContentSize(animationSpec = tween(200))) {
                CategoryBreakdownRow(item = item, isSelected = isSelected, onClick = { onCategorySelected(if (isSelected) null else item.category) })
                if (isSelected) {
                    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, start = 8.dp, end = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Lịch sử thu nhập ${item.category.displayName}", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                        if (filteredTransactions.isEmpty()) {
                            Text("Không có giao dịch nào.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
                        } else {
                            filteredTransactions.forEach { tx -> TransactionItem(transaction = tx, onClick = {}) }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterControlRow(
    walletLabel: String,
    timeLabel: String,
    showNavigation: Boolean,
    isNextEnabled: Boolean,
    wallets: List<com.notepay.domain.model.Wallet>,
    onWalletSelect: (Long?) -> Unit,
    onTimeFilterSelect: (TimeFilterType) -> Unit,
    onPrevClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWalletDropdown by remember { mutableStateOf(false) }
    var showTimeDropdown by remember { mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Cụm trái: ĐÃ SỬA kẹp Box neo Dropdown loại ví chuẩn xác
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showWalletDropdown = true }
                    .padding(vertical = 6.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(Icons.Rounded.Wallet, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                Text(walletLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Icon(Icons.Rounded.ArrowDropDown, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }

            DropdownMenu(
                expanded = showWalletDropdown,
                onDismissRequest = { showWalletDropdown = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Tất cả ví", fontWeight = FontWeight.Bold) },
                    onClick = {
                        onWalletSelect(null)
                        showWalletDropdown = false
                    }
                )
                wallets.forEach { wallet ->
                    DropdownMenuItem(
                        text = { Text(wallet.name) },
                        onClick = {
                            onWalletSelect(wallet.id)
                            showWalletDropdown = false
                        }
                    )
                }
            }
        }

        // Cụm phải: ĐÃ SỬA kẹp Box neo Dropdown chọn mốc thời gian tuần/tháng/năm
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (showNavigation) {
                IconButton(onClick = onPrevClick, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Rounded.ChevronLeft, "Trước", modifier = Modifier.size(20.dp))
                }
            }

            Box {
                Text(
                    text = timeLabel,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showTimeDropdown = true }
                        .padding(vertical = 4.dp, horizontal = 6.dp)
                )

                DropdownMenu(
                    expanded = showTimeDropdown,
                    onDismissRequest = { showTimeDropdown = false }
                ) {
                    TimeFilterType.entries.forEach { filter ->
                        DropdownMenuItem(
                            text = { Text(filter.label) },
                            onClick = {
                                showTimeDropdown = false
                                onTimeFilterSelect(filter)
                            }
                        )
                    }
                }
            }

            if (showNavigation) {
                IconButton(onClick = onNextClick, enabled = isNextEnabled, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.ChevronRight, "Sau", modifier = Modifier.size(20.dp),
                        tint = if (isNextEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(0.3f)
                    )
                }
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
    val selectionProgress by animateFloatAsState(targetValue = if (selectedCategory != null) 1f else 0f, animationSpec = tween(250), label = "")
    val trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    LaunchedEffect(breakdown) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, tween(800))
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(breakdown, selectedCategory) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        val radius = min(size.width, size.height) / 2f
                        val strokeWidthPx = 20.dp.toPx()

                        if (distance in (radius - strokeWidthPx - 20.dp.toPx())..(radius + 20.dp.toPx())) {
                            var normalizedAngle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                            if (normalizedAngle < 0f) normalizedAngle += 360f

                            var currentAngle = 0f
                            var clicked: Category? = null
                            for (item in breakdown) {
                                val sweep = item.percentage * 360f
                                if (normalizedAngle in currentAngle..(currentAngle + sweep)) { clicked = item.category; break }
                                currentAngle += sweep
                            }
                            onCategorySelected(if (clicked == selectedCategory) null else clicked)
                        } else { onCategorySelected(null) }
                    }
                }
        ) {
            val strokeWidthPx = 20.dp.toPx()
            val minDim = min(size.width, size.height)
            val radius = (minDim - strokeWidthPx) / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val topLeft = Offset(center.x - radius, center.y - radius)
            val arcSize = Size(radius * 2, radius * 2)

            drawCircle(color = trackColor, radius = radius, center = center, style = Stroke(strokeWidthPx))

            var startAngle = -90f
            breakdown.forEach { item ->
                val isSelected = selectedCategory == item.category
                val currentStroke = if (isSelected) (20f + 6f * selectionProgress).dp.toPx() else 20.dp.toPx()
                val alpha = if (selectedCategory != null && !isSelected) 0.35f else 1f
                val sweep = item.percentage * 360f * animationProgress.value

                drawArc(
                    color = Color(item.category.colorArgb).copy(alpha = alpha),
                    startAngle = startAngle, sweepAngle = sweep, useCenter = false,
                    topLeft = topLeft, size = arcSize, style = Stroke(currentStroke, cap = StrokeCap.Round)
                )
                startAngle += item.percentage * 360f
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(36.dp)) {
            Text(centerLabel.uppercase(), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(MoneyFormatter.format(totalExpense), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun CategoryBreakdownRow(
    item: CategoryBreakdownItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(0.25f) else MaterialTheme.colorScheme.surfaceContainerLow
        ),
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(0.3f)) else null
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CategoryAvatar(category = item.category, size = 40.dp, iconSize = 18.dp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(item.category.displayName, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
                    Text(MoneyFormatter.format(item.amount), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LinearProgressIndicator(
                        progress = { item.percentage },
                        modifier = Modifier.weight(1f).height(6.dp),
                        color = Color(item.category.colorArgb),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        strokeCap = StrokeCap.Round,
                    )

                    Text(
                        text = String.format(Locale.US, "%.1f%%", item.percentage * 100f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(40.dp),
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCards(totalIncome: Money, totalExpense: Money) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        listOf(Triple("Thu nhập", totalIncome, Color(0xFF2E7D32)), Triple("Chi tiêu", totalExpense, Color(0xFFC62828))).forEach { (label, money, color) ->
            Card(modifier = Modifier.weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (label == "Thu nhập") Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown, null, tint = color, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(MoneyFormatter.format(money), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@Composable
private fun BudgetProgressBar(spent: Money, limit: Money, percentage: Float) {
    val progress = percentage.coerceIn(0f, 1f)
    val color = when { percentage >= 1f -> Color(0xFFC62828); percentage >= 0.8f -> Color(0xFFEF6C00); else -> Color(0xFF2E7D32) }
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(imageVector = if (percentage >= 0.8f) Icons.Rounded.Warning else Icons.Rounded.Info, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
                    Text("Hạn mức chi tiêu", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = String.format(Locale.US, "%.1f%%", percentage * 100f),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(8.dp), color = color, trackColor = MaterialTheme.colorScheme.surfaceVariant, strokeCap = StrokeCap.Round)
            Text("${MoneyFormatter.format(spent)} / ${MoneyFormatter.format(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun SpendingPredictionCard(prediction: com.notepay.domain.analytics.SpendingPrediction) {
    val probability = prediction.overBudgetProbability
    val riskColor = when {
        probability == null -> MaterialTheme.colorScheme.secondary
        probability >= 0.70 -> MaterialTheme.colorScheme.error
        probability >= 0.35 -> Color(0xFFEF6C00)
        else -> Color(0xFF2E7D32)
    }
    val confidenceLabel = when (prediction.confidence) {
        ForecastConfidence.LOW -> "thấp (${prediction.observedDays} ngày dữ liệu)"
        ForecastConfidence.MEDIUM -> "trung bình (${prediction.observedDays} ngày dữ liệu)"
        ForecastConfidence.HIGH -> "cao (${prediction.observedDays} ngày dữ liệu)"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = riskColor.copy(alpha = 0.07f)),
        border = BorderStroke(1.dp, riskColor.copy(alpha = 0.22f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.AutoGraph, contentDescription = null, tint = riskColor)
                Text("Dự báo chi tiêu local", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(
                MoneyFormatter.format(Money(prediction.predictedMonthTotalInCents)),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = riskColor,
            )
            Text(
                "Khoảng dự báo: ${MoneyFormatter.format(Money(prediction.lowerBoundInCents))} – ${MoneyFormatter.format(Money(prediction.upperBoundInCents))}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                probability?.let { "Khả năng vượt định mức: ${(it * 100).toInt()}% · Tin cậy $confidenceLabel" }
                    ?: "Hãy đặt định mức để tính xác suất vượt chi · Tin cậy $confidenceLabel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiInsightsCarousel(
    state: StatsUiState,
    onAdviceFeedback: (String, Int) -> Unit,
    onAddSubscription: (String, Long, String, Long) -> Unit,
    onGenerateLocalAdvice: () -> Unit,
) {
    val localView = androidx.compose.ui.platform.LocalView.current
    val playHaptic = { try { localView.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP) } catch (_: Exception) {} }
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("AI Insights & Trợ lý thông minh", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 4.dp))
        androidx.compose.foundation.lazy.LazyRow(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                LocalAdvisorCard(
                    advisor = state.localAdvisor,
                    onGenerate = onGenerateLocalAdvice,
                    modifier = Modifier.width(310.dp),
                )
            }
            state.dynamicDailyBudget?.let { budget -> item { DynamicDailyBudgetCard(budget = budget, modifier = Modifier.width(290.dp)) } }
            items(state.detectedSubscriptions) { sub -> SubscriptionProposalCard(sub = sub, onAdd = { playHaptic(); onAddSubscription(sub.name, sub.amount.amountInCents, sub.category.id, sub.possibleNextDueDate) }, modifier = Modifier.width(290.dp)) }
            items(state.aiAdvices) { advice -> AiAdviceCard(advice = advice, onFeedback = { score -> playHaptic(); onAdviceFeedback(advice.id, score) }, modifier = Modifier.width(290.dp)) }
        }
    }
}

@Composable
private fun LocalAdvisorCard(
    advisor: LocalAdvisorUiState,
    onGenerate: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val result = advisor.result
    Card(
        modifier = modifier.height(190.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.14f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Psychology, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(result?.title ?: "Trợ lý chi tiêu trên máy", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            when (advisor.status) {
                LocalAdvisorStatus.RUNNING -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 3.dp)
                        Text("Đang chuẩn bị Gemini Nano và phân tích…", style = MaterialTheme.typography.labelSmall)
                    }
                }
                LocalAdvisorStatus.NOT_REQUESTED -> {
                    Text(
                        "Gemini Nano chỉ nhận số liệu đã tổng hợp; giao dịch thô không rời khỏi thiết bị.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) { Text("Phân tích trên thiết bị") }
                }
                LocalAdvisorStatus.READY -> {
                    Text(result?.content.orEmpty(), style = MaterialTheme.typography.bodySmall, maxLines = 4, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                    Text(
                        if (result?.provider == AdvisorProvider.GEMINI_NANO) "Gemini Nano · local" else "Mô hình thống kê · local fallback",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    result?.providerMessage?.let {
                        Text(it, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DynamicDailyBudgetCard(budget: DynamicDailyBudgetData, modifier: Modifier = Modifier) {
    val progress = if (budget.dailyBudget.amountInCents > 0) (budget.spentToday.amountInCents.toFloat() / budget.dailyBudget.amountInCents).coerceIn(0f, 1f) else 1f
    val cardColor = if (budget.isExceeded) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    Card(modifier = modifier.height(170.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = cardColor), border = BorderStroke(1.dp, (if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary).copy(0.25f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.Wallet, null, tint = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Ngân sách ngày linh hoạt", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Column {
                Text("Còn lại hôm nay: ${MoneyFormatter.format(budget.remainingToday)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.ExtraBold)
                Text("Hạn mức gốc: ${MoneyFormatter.format(budget.dailyBudget)} (Đã tiêu: ${MoneyFormatter.format(budget.spentToday)})", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape), color = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
            Text(text = if (budget.isExceeded) "⚠️ Tiêu quá hạn mức! Ngày mai giảm còn ${MoneyFormatter.format(budget.tomorrowBudget)} để bù tiền." else "💡 Chi tiêu thông minh giúp bảo toàn ngân sách tháng.", style = MaterialTheme.typography.labelSmall, color = if (budget.isExceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AiAdviceCard(advice: AiAdviceItem, onFeedback: (Int) -> Unit, modifier: Modifier = Modifier) {
    val iconColor = if (advice.type == "warning") MaterialTheme.colorScheme.error else if (advice.type == "success") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
    Card(modifier = modifier.height(170.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = iconColor.copy(alpha = 0.08f)), border = BorderStroke(1.dp, iconColor.copy(alpha = 0.2f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(if (advice.type == "warning") Icons.Rounded.Warning else Icons.Rounded.Info, null, tint = iconColor, modifier = Modifier.size(20.dp))
                Text(advice.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Text(advice.content, style = MaterialTheme.typography.bodySmall, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onFeedback(1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.ThumbUp, null, tint = if (advice.feedback == 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
                IconButton(onClick = { onFeedback(-1) }, modifier = Modifier.size(28.dp)) { Icon(Icons.Rounded.ThumbDown, null, tint = if (advice.feedback == -1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun SubscriptionProposalCard(sub: DetectedSubscription, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.height(170.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Rounded.CalendarMonth, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Text("Phát hiện hóa đơn định kỳ", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(sub.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text("${MoneyFormatter.format(sub.amount)} / tháng", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().height(36.dp), shape = RoundedCornerShape(10.dp), contentPadding = PaddingValues(0.dp)) { Text("Thêm hóa đơn", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold) }
        }
    }
}
