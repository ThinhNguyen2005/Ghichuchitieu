package com.notepay.ui.feature.subscription

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
    viewModel: SubscriptionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val dialogState by viewModel.dialogState.collectAsStateWithLifecycle()
    // 0 = Lịch (default), 1 = Nhắc nhở
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddSheet by remember { mutableStateOf(false) }

    // Calendar state
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var calendarYear by remember { mutableIntStateOf(now.date.year) }
    var calendarMonth by remember { mutableIntStateOf(now.date.monthNumber) }

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
            TopAppBar(title = { Text("Nhắc nhở gia hạn") })
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    viewModel.showAddDialog()
                    showAddSheet = true
                },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("Thêm nhắc nhở") },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("Lịch")
                        }
                    },
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        val activeCount = state.allSubscriptions.count { it.isActive }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (activeCount > 0) {
                                Box(
                                    modifier = Modifier.size(18.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$activeCount",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White,
                                    )
                                }
                            }
                            Text("Nhắc nhở")
                        }
                    },
                )
            }

            when (selectedTab) {
                0 -> CalendarTab(
                    year = calendarYear,
                    month = calendarMonth,
                    subscriptions = state.allSubscriptions,
                    highlightDates = upcomingDates,
                    onPreviousMonth = {
                        if (calendarMonth == 1) { calendarMonth = 12; calendarYear -= 1 } else calendarMonth -= 1
                    },
                    onNextMonth = {
                        if (calendarMonth == 12) { calendarMonth = 1; calendarYear += 1 } else calendarMonth += 1
                    },
                    onDayClick = { date -> selectedDay = date },
                )
                1 -> ReminderListTab(
                    subscriptions = state.allSubscriptions,
                    onDelete = { viewModel.deleteSubscription(it.id) },
                )
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
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDayClick: (LocalDate) -> Unit,
) {
    val tz = TimeZone.currentSystemDefault()
    val calendarSubs = remember(subscriptions, year, month) {
        subscriptions.filter { sub ->
            val date = sub.nextDueDate.toLocalDateTime(tz).date
            date.year == year && date.monthNumber == month
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        MonthlyCalendarView(
            year = year,
            month = month,
            transactions = emptyList(),
            highlightDates = highlightDates,
            onDayClick = onDayClick,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth,
            modifier = Modifier.fillMaxWidth().weight(1f),
        )
        if (calendarSubs.isNotEmpty()) {
            Text(
                "Nhắc nhở tháng này",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            LazyColumn(
                modifier = Modifier.fillMaxWidth().padding(bottom = 80.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(calendarSubs, key = { it.id }) { sub ->
                    val d = sub.nextDueDate.toLocalDateTime(tz).date
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${d.dayOfMonth}/${d.monthNumber}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Text(sub.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
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
    onDelete: (Subscription) -> Unit,
) {
    var filter by remember { mutableStateOf(ReminderFilter.ALL) }
    var filterMenuExpanded by remember { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Subscription?>(null) }

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
        // Hàng tìm kiếm + lọc: search bar 3/4 trái, nút lọc 1/4 phải
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Thanh tìm kiếm 3/4
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .weight(3f)
                    .clip(RoundedCornerShape(20.dp)),
                leadingIcon = {
                    Icon(
                        Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
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
                        "Tìm nhắc nhở...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    disabledBorderColor = Color.Transparent,
                ),
            )

            // Nút lọc 1/4
            Box(
                modifier = Modifier.weight(1f),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable { filterMenuExpanded = true }
                        .padding(horizontal = 10.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.ArrowDropDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            filter.shortLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                DropdownMenu(
                    expanded = filterMenuExpanded,
                    onDismissRequest = { filterMenuExpanded = false },
                ) {
                    ReminderFilter.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    option.label,
                                    fontWeight = if (option == filter) FontWeight.Bold else FontWeight.Normal,
                                )
                            },
                            onClick = {
                                filter = option
                                filterMenuExpanded = false
                            },
                        )
                    }
                }
            }
        }

        if (filtered.isEmpty()) {
            // P1-6: thống nhất dùng EmptyStateWithAction, bỏ emoji trong copy.
            Box(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                when {
                    query.isNotBlank() -> EmptyStateWithAction(
                        icon = Icons.Rounded.Search,
                        title = "Không tìm thấy",
                        description = "Không có nhắc nhở nào khớp với \"$query\"",
                    )
                    filter == ReminderFilter.ALL -> EmptyStateWithAction(
                        icon = Icons.Rounded.Notifications,
                        title = "Chưa có nhắc nhở",
                        description = "Thêm gói đăng ký để được nhắc trước khi đến hạn.",
                        actionLabel = "Thêm nhắc nhở",
                        onClick = { /* mở AddSubscriptionBottomSheet qua FAB ở trang cha */ },
                    )
                    filter == ReminderFilter.OVERDUE -> EmptyStateWithAction(
                        icon = Icons.Rounded.SentimentSatisfied,
                        title = "Không có khoản quá hạn",
                        description = "Tất cả nhắc nhở đều còn hạn. Tuyệt vời!",
                    )
                    filter == ReminderFilter.UPCOMING -> EmptyStateWithAction(
                        icon = Icons.Rounded.CalendarMonth,
                        title = "Chưa có nhắc nhở sắp đến hạn",
                        description = "Các nhắc nhở sẽ hiện ở đây khi gần đến ngày hết hạn.",
                    )
                    filter == ReminderFilter.COMPLETED -> EmptyStateWithAction(
                        icon = Icons.Rounded.CheckCircle,
                        title = "Chưa hoàn thành nhắc nhở nào",
                        description = "Khi bạn đánh dấu hoàn thành, chúng sẽ xuất hiện tại đây.",
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(filtered, key = { it.id }) { sub ->
                    SubscriptionCard(
                        subscription = sub,
                        onDelete = { pendingDelete = sub },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
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
        isCompleted -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        isOverdue -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
        isUpcoming -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    val accentColor = when {
        isCompleted -> MaterialTheme.colorScheme.outline
        isOverdue -> MaterialTheme.colorScheme.error
        isUpcoming -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.size(46.dp).clip(CircleShape).background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    when {
                        isCompleted -> Icons.Rounded.Notifications
                        isUpcoming -> Icons.Rounded.NotificationsActive
                        else -> Icons.Rounded.Notifications
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

            IconButton(onClick = onDelete) {
                Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}
