package com.notepay.ui.feature.subscription

import com.notepay.ui.theme.AppTheme

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.Subscription
import com.notepay.ui.component.ConfirmDeleteDialog
import com.notepay.ui.component.EmptyStateWithAction
import com.notepay.ui.component.MonthlyCalendarView
import com.notepay.ui.util.MoneyFormatter
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * Trạng thái lọc cho tab Nhắc nhở.
 *  - ALL: tất cả
 *  - OVERDUE: quá hạn (nextDueDate < now)
 *  - UPCOMING: sắp đến hạn trong khoảng remindDaysBefore
 *  - COMPLETED: đã hoàn thành (isActive = false)
 */
private enum class ReminderFilter(val label: String, val shortLabel: String) {
    ALL("Tất cả", "Tất cả"),
    OVERDUE("Quá hạn", "Quá hạn"),
    UPCOMING("Sắp đến hạn", "Sắp đến"),
    COMPLETED("Đã hoàn thành", "Hoàn thành"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    navigationBarOffset: Float = 0f,
    initialShowCreate: Boolean = false,
    onClearShowCreate: () -> Unit = {},
    onBack: () -> Unit = {},
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    // 0 = Lịch (default), 1 = Nhắc nhở
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(initialShowCreate) {
        if (initialShowCreate) {
            showAddSheet = true
            viewModel.showAddDialog()
            onClearShowCreate()
        }
    }

    // Expanding Search Bar states
    var isSearchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    // Calendar state
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var calendarYear by remember { mutableIntStateOf(now.date.year) }
    var calendarMonth by remember { mutableIntStateOf(now.date.month.ordinal + 1) }

    val tz = TimeZone.currentSystemDefault()
    val upcomingDates by remember(state.allSubscriptions) {
        derivedStateOf {
            state.allSubscriptions
                .filter { it.isActive }
                .map { it.nextDueDate.toLocalDateTime(tz).date }
                .toSet()
        }
    }

    var selectedDay by remember { mutableStateOf<LocalDate?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                title = {
                    if (isSearchActive) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier = Modifier.fillMaxWidth(),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            "Tìm hóa đơn...",
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
                        Text(stringResource(R.string.subscription_title))
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isSearchActive = !isSearchActive
                            if (isSearchActive) {
                                viewModel.setSelectedTab(1)
                            } else {
                                searchQuery = ""
                            }
                        }
                    ) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Outlined.Search,
                            contentDescription = if (isSearchActive) "Đóng" else "Tìm kiếm"
                        )
                    }
                    val isCalendarMode = selectedTab == 0
                    if (!isSearchActive) {
                        IconButton(
                            onClick = {
                                if (isCalendarMode) {
                                    viewModel.setSelectedTab(1)
                                } else {
                                    viewModel.setSelectedTab(0)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isCalendarMode) Icons.AutoMirrored.Outlined.List else Icons.Outlined.CalendarMonth,
                                contentDescription = if (isCalendarMode) "Xem dạng danh sách" else "Xem dạng lịch"
                            )
                        }
                    }
                }
            )
        },
    ) { padding ->
        val layoutDirection = LocalLayoutDirection.current
        val bottomPadding = padding.calculateBottomPadding() + 108.dp
        
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = padding.calculateStartPadding(layoutDirection),
                        end = padding.calculateEndPadding(layoutDirection)
                    )
            ) {
                // TabRow has been removed!

                when (selectedTab) {
                    0 -> CalendarTab(
                        year = calendarYear,
                        month = calendarMonth,
                        subscriptions = state.allSubscriptions,
                        highlightDates = upcomingDates,
                        selectedDate = selectedDay,
                        onPreviousMonth = {
                            if (calendarMonth == 1) { calendarMonth = 12; calendarYear -= 1 } else calendarMonth -= 1
                        },
                        onNextMonth = {
                            if (calendarMonth == 12) { calendarMonth = 1; calendarYear += 1 } else calendarMonth += 1
                        },
                        onDayClick = { date -> selectedDay = date },
                        bottomPadding = bottomPadding,
                    )
                    1 -> ReminderListTab(
                        subscriptions = state.allSubscriptions,
                        query = searchQuery,
                        isSearchActive = isSearchActive,
                        onDelete = { viewModel.deleteSubscription(it.id) },
                        bottomPadding = bottomPadding,
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddSubscriptionBottomSheet(
            state = dialogState,
            recentTransactions = state.recentTransactions,
            onNameChanged = viewModel::onNameChanged,
            onAmountChanged = viewModel::onAmountChanged,
            onRepeatMonthsChanged = viewModel::onRepeatMonthsChanged,
            onRemindDaysChanged = viewModel::onRemindDaysChanged,
            onNoteChanged = viewModel::onNoteChanged,
            onCategoryChanged = viewModel::onCategoryChanged,
            onNextDueDateChanged = viewModel::onNextDueDateChanged,
            onConfirm = {
                viewModel.saveSubscription()
                showAddSheet = false
            },
            onDismiss = {
                viewModel.hideAddDialog()
                showAddSheet = false
            },
        )
    }

    selectedDay?.let { date ->
        val dayTransactions = state.calendarTransactions[date].orEmpty()
        val daySubs = state.calendarSubscriptions[date].orEmpty()
        DayDetailDialog(
            date = date,
            transactions = dayTransactions,
            subscriptions = daySubs,
            onDismiss = { selectedDay = null },
        )
    }
}

@Composable
private fun CalendarTab(
    year: Int,
    month: Int,
    subscriptions: List<Subscription>,
    highlightDates: Set<LocalDate>,
    selectedDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    val tz = TimeZone.currentSystemDefault()
    val calendarSubs = remember(subscriptions, year, month) {
        subscriptions.filter { sub ->
            val date = sub.nextDueDate.toLocalDateTime(tz).date
            date.year == year && date.month.ordinal + 1 == month
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = bottomPadding)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
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
                year = year,
                month = month,
                transactions = emptyList(),
                highlightDates = highlightDates,
                selectedDate = selectedDate,
                bottomContentPadding = 0.dp,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onDayClick = onDayClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            )
        }
        
        Text(
            text = "Hóa đơn tháng này",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 20.dp, top = 12.dp, end = 16.dp, bottom = 4.dp),
        )

        if (calendarSubs.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 4.dp,
                    end = 16.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(calendarSubs, key = { it.id }) { sub ->
                    val d = sub.nextDueDate.toLocalDateTime(tz).date
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppTheme.shapes.corner12,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "${d.day}/${d.month.ordinal + 1}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(end = 12.dp),
                            )
                            Text(
                                sub.name,
                                modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                MoneyFormatter.format(sub.amount),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }
        } else {
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
                        text = "Không có hóa đơn nào trong tháng này",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Tab Nhắc nhở với:
 *  - Thanh tìm kiếm bên trái chiếm 3/4 chiều rộng.
 *  - Nút lọc dropdown bên phải chiếm 1/4.
 *  - Bộ lọc: Quá hạn / Sắp đến hạn / Đã hoàn thành / Tất cả.
 *  - Tìm kiếm theo tên nhắc nhở (không phân biệt hoa thường).
 *  - Trước khi xóa phải mở ConfirmDeleteDialog (P0-1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderListTab(
    subscriptions: List<Subscription>,
    query: String,
    isSearchActive: Boolean,
    onDelete: (Subscription) -> Unit,
    bottomPadding: androidx.compose.ui.unit.Dp,
) {
    var filter by remember { mutableStateOf(ReminderFilter.ALL) }
    var pendingDelete by remember { mutableStateOf<Subscription?>(null) }

    // 1. Ẩn bộ lọc và danh sách khi chưa có nhắc nhở nào
    if (subscriptions.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, bottom = bottomPadding),
            contentAlignment = Alignment.Center,
        ) {
            EmptyStateWithAction(
                icon = Icons.Outlined.Notifications,
                title = "Chưa có hóa đơn định kỳ",
                description = "Nhấn vào nút + để thêm hóa đơn đầu tiên.",
            )
        }
        return
    }

    val now = Clock.System.now()
    val filtered = remember(subscriptions, filter, query) {
        val normalized = query.trim().lowercase()
        val base = when (filter) {
            ReminderFilter.ALL -> subscriptions
            ReminderFilter.OVERDUE -> subscriptions.filter {
                it.isActive && (it.nextDueDate - now).inWholeDays < 0
            }
            ReminderFilter.UPCOMING -> subscriptions.filter {
                it.isActive && (it.nextDueDate - now).inWholeDays in 0..it.remindDaysBefore.toLong()
            }
            ReminderFilter.COMPLETED -> subscriptions.filter { !it.isActive }
        }
        if (normalized.isBlank()) base
        else base.filter { it.name.lowercase().contains(normalized) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Dải chip lọc ngang với chỉ báo cuộn mờ ở cạnh phải
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.CenterEnd
        ) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 40.dp, top = 6.dp, bottom = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReminderFilter.entries) { option ->
                    val isSelected = filter == option
                    FilterChip(
                        selected = isSelected,
                        onClick = { filter = option },
                        label = { Text(option.label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

            // Hiệu ứng chuyển màu mờ nhẹ (gradient fade-out) ở cạnh phải màn hình
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .width(32.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color.Transparent,
                                MaterialTheme.colorScheme.background
                            )
                        )
                    )
            )
        }
        Spacer(modifier = Modifier.height(4.dp))

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    query.isNotBlank() -> EmptyStateWithAction(
                        icon = Icons.Outlined.Search,
                        title = "Không tìm thấy",
                        description = "Không có hóa đơn nào khớp với \"$query\"",
                    )
                    filter == ReminderFilter.ALL -> EmptyStateWithAction(
                        icon = Icons.Outlined.Notifications,
                        title = "Chưa có hóa đơn định kỳ",
                        description = "Nhấn vào nút + để thêm hóa đơn đầu tiên.",
                    )
                    filter == ReminderFilter.OVERDUE -> EmptyStateWithAction(
                        icon = Icons.Outlined.SentimentSatisfied,
                        title = "Không có khoản quá hạn",
                        description = "Tất cả hóa đơn đều còn hạn. Tuyệt vời!",
                    )
                    filter == ReminderFilter.UPCOMING -> EmptyStateWithAction(
                        icon = Icons.Outlined.CalendarMonth,
                        title = "Chưa có hóa đơn sắp đến hạn",
                        description = "Các hóa đơn sẽ hiện ở đây khi gần đến ngày hết hạn.",
                    )
                    filter == ReminderFilter.COMPLETED -> EmptyStateWithAction(
                        icon = Icons.Outlined.CheckCircle,
                        title = "Chưa hoàn thành hóa đơn nào",
                        description = "Khi bạn đánh dấu hoàn thành, chúng sẽ xuất hiện tại đây.",
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(
                    start = 16.dp,
                    top = 4.dp,
                    end = 16.dp,
                    bottom = bottomPadding
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered, key = { it.id }) { sub ->
                    SubscriptionCard(
                        subscription = sub,
                        onDelete = { pendingDelete = sub },
                    )
                }
            }
        }
    }

    pendingDelete?.let { sub ->
        ConfirmDeleteDialog(
            title = "Xóa nhắc nhở?",
            itemName = sub.name,
            message = "Nhắc nhở \"${sub.name}\" sẽ bị xóa vĩnh viễn và không thể khôi phục.",
            onConfirm = { onDelete(sub) },
            onDismiss = { pendingDelete = null },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubscriptionCard(
    subscription: Subscription,
    onDelete: () -> Unit,
) {
    val now = Clock.System.now()
    val daysLeft = (subscription.nextDueDate - now).inWholeDays
    val dueDateStr = subscription.nextDueDate
        .toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    val isOverdue = daysLeft < 0
    val isUpcoming = daysLeft in 0..subscription.remindDaysBefore.toLong()
    val isCompleted = !subscription.isActive

    val containerColor = when {
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.85f)
        isOverdue -> MaterialTheme.colorScheme.errorContainer
        isUpcoming -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val accentColor = when {
        isCompleted -> MaterialTheme.colorScheme.outline
        isOverdue -> MaterialTheme.colorScheme.error
        isUpcoming -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onDelete
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(AppTheme.shapes.circle).background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        isCompleted -> Icons.Outlined.Notifications
                        isUpcoming -> Icons.Outlined.NotificationsActive
                        else -> Icons.Outlined.Notifications
                    },
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp),
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        subscription.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    if (isCompleted) {
                        Text(
                            "Đã hoàn thành",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    MoneyFormatter.format(subscription.amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = accentColor,
                    fontWeight = FontWeight.Medium,
                )
                if (!isCompleted) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = when {
                                isOverdue -> "Quá hạn ${-daysLeft} ngày"
                                daysLeft == 0L -> "Hết hạn hôm nay!"
                                daysLeft == 1L -> "Hết hạn ngày mai"
                                else -> "Còn $daysLeft ngày"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (daysLeft <= 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (daysLeft <= 1) FontWeight.Bold else FontWeight.Normal,
                        )
                        Text("•", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            dueDateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (subscription.note.isNotBlank()) {
                    Text(
                        subscription.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "Chu kỳ ${subscription.repeatMonths} tháng • Nhắc trước ${subscription.remindDaysBefore} ngày",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
