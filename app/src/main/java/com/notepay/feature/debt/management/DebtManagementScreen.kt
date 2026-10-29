package com.notepay.ui.feature.debt

import android.content.res.Configuration
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.debt.Debt
import com.notepay.domain.model.debt.DebtPayment
import com.notepay.domain.model.debt.DebtType
import com.notepay.domain.model.debt.DebtWithHistory
import com.notepay.domain.money.Money
import com.notepay.domain.usecase.debt.DebtSummary
import com.notepay.feature.debt.DebtUiEvent
import com.notepay.feature.debt.DebtUiState
import com.notepay.feature.debt.DebtViewModel
import com.notepay.ui.adaptive.LocalWindowLayout
import com.notepay.ui.adaptive.WindowLayout
import com.notepay.ui.component.DetailPaneEmptyState
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.theme.NotePayTheme
import kotlin.time.Instant

@Composable
fun DebtManagementScreen(
    onBack: () -> Unit,
    onDebtClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DebtViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCreateSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    // Tablet/foldable: danh sách bên trái, chi tiết bên phải. Lựa chọn phải sống sót qua
    // xoay máy và resize cửa sổ nên dùng rememberSaveable thay vì remember.
    val isTwoPane = LocalWindowLayout.current == WindowLayout.Expanded
    var selectedDebtId by rememberSaveable { mutableStateOf<Long?>(null) }

    // Khoản nợ đang chọn có thể bị xóa hoặc rơi ra khỏi bộ lọc hiện tại — khi đó pane phải
    // quay về trạng thái chờ thay vì cố hiển thị một bản ghi không còn tồn tại.
    val resolvedDebtId = selectedDebtId?.let { id ->
        if (uiState.isLoading) {
            id
        } else {
            id.takeIf { candidate -> uiState.debts.any { it.debt.id == candidate } }
        }
    }

    // Back đóng pane chi tiết trước khi thoát khỏi màn danh sách.
    BackHandler(enabled = isTwoPane && resolvedDebtId != null) {
        selectedDebtId = null
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is DebtUiEvent.DebtCreated -> {
                    Toast.makeText(context, context.getString(R.string.debt_create_success), Toast.LENGTH_SHORT).show()
                    showCreateSheet = false
                }
                is DebtUiEvent.PaymentRecorded -> {
                    Toast.makeText(context, context.getString(R.string.debt_payment_success), Toast.LENGTH_SHORT).show()
                }
                is DebtUiEvent.DebtDeleted -> {
                    Toast.makeText(context, context.getString(R.string.debt_delete_success), Toast.LENGTH_SHORT).show()
                }
                is DebtUiEvent.ShowToast -> {
                    Toast.makeText(context, context.getString(event.messageResId), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    DebtManagementContent(
        uiState = uiState,
        isSearchActive = isSearchActive,
        isTwoPane = isTwoPane,
        selectedDebtId = resolvedDebtId,
        onBack = onBack,
        onToggleSearch = { isSearchActive = !isSearchActive },
        onSearchQueryChange = viewModel::setSearchQuery,
        onTabSelected = viewModel::setTab,
        onToggleUnsettled = { viewModel.setOnlyUnsettled(!uiState.onlyUnsettled) },
        onDebtClick = { debtId ->
            if (isTwoPane) selectedDebtId = debtId else onDebtClick(debtId)
        },
        onAddClick = { showCreateSheet = true },
        onClosePane = { selectedDebtId = null },
        modifier = modifier,
    )

    if (showCreateSheet) {
        CreateDebtBottomSheet(
            wallets = uiState.wallets,
            onDismiss = { showCreateSheet = false },
            onConfirm = { name, phone, type, amount, walletId, dueDate, note, sync ->
                viewModel.createDebt(
                    personName = name,
                    phoneNumber = phone,
                    type = type,
                    amount = amount,
                    walletId = walletId,
                    dueDate = dueDate,
                    note = note,
                    syncWithWallet = sync
                )
            }
        )
    }
}

/**
 * Giao diện stateless của màn hình Sổ nợ, hỗ trợ xem trước (Preview) và phân tách trạng thái.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtManagementContent(
    uiState: DebtUiState,
    isSearchActive: Boolean,
    isTwoPane: Boolean,
    selectedDebtId: Long?,
    onBack: () -> Unit,
    onToggleSearch: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onTabSelected: (DebtType?) -> Unit,
    onToggleUnsettled: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onClosePane: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Không còn Scaffold: màn này tự dựng top bar + FAB, nhờ vậy ở Expanded có thể chia
    // danh sách và chi tiết thành hai pane mà không sinh thêm một lớp inset lồng nhau.
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.debt_nav_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onToggleSearch) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Rounded.Clear else Icons.Rounded.Search,
                            contentDescription = stringResource(
                                if (isSearchActive) R.string.debt_action_close_search else R.string.debt_action_search
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                // FAB đặt trong pane danh sách để ở chế độ hai pane không đè lên pane chi tiết.
                Box(
                    modifier = Modifier
                        .weight(if (isTwoPane) 0.45f else 1f)
                        .fillMaxHeight()
                ) {
                    DebtListPane(
                        uiState = uiState,
                        isSearchActive = isSearchActive,
                        selectedDebtId = selectedDebtId,
                        onSearchQueryChange = onSearchQueryChange,
                        onTabSelected = onTabSelected,
                        onToggleUnsettled = onToggleUnsettled,
                        onDebtClick = onDebtClick,
                        onAddClick = onAddClick,
                        modifier = Modifier.fillMaxSize(),
                    )

                    // Khi danh sách trống, nút "Thêm khoản nợ" đã nằm giữa màn nên ẩn FAB để
                    // không có hai nút cùng một chức năng.
                    if (uiState.isLoading || uiState.debts.isNotEmpty()) {
                        ExtendedFloatingActionButton(
                            onClick = onAddClick,
                            containerColor = AppTheme.colors.ink,
                            contentColor = AppTheme.colors.onInk,
                            shape = AppTheme.shapes.capsule,
                            icon = { Icon(imageVector = Icons.Rounded.Add, contentDescription = null) },
                            text = { Text(stringResource(R.string.debt_action_add)) },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(16.dp),
                        )
                    }
                }

                if (isTwoPane) {
                    DebtDetailPane(
                        debtId = selectedDebtId,
                        onClosePane = onClosePane,
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }
}

@Composable
private fun DebtListPane(
    uiState: DebtUiState,
    isSearchActive: Boolean,
    selectedDebtId: Long?,
    onSearchQueryChange: (String) -> Unit,
    onTabSelected: (DebtType?) -> Unit,
    onToggleUnsettled: () -> Unit,
    onDebtClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        // Thanh tìm kiếm
        if (isSearchActive) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(R.string.debt_search_hint)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = AppTheme.shapes.corner16,
            )
        }

        // Tab dạng capsule: Tất cả / Cần thu / Cần trả. Chấm màu dùng token thu/chi của theme.
        // Hỗ trợ cuộn ngang khi phóng to fontScale (1.3x - 2.0x) để không bị cụt mép phải.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            DebtTabPill(
                label = stringResource(R.string.transaction_list_filter_all),
                dotColor = null,
                selected = uiState.selectedTab == null,
                onClick = { onTabSelected(null) },
            )
            DebtTabPill(
                label = stringResource(R.string.debt_collect),
                dotColor = AppTheme.colors.success,
                selected = uiState.selectedTab == DebtType.LEND,
                onClick = { onTabSelected(DebtType.LEND) },
            )
            DebtTabPill(
                label = stringResource(R.string.debt_tab_borrow),
                dotColor = AppTheme.colors.error,
                selected = uiState.selectedTab == DebtType.BORROW,
                onClick = { onTabSelected(DebtType.BORROW) },
            )
        }

        // Filter Chip "Chưa tất toán"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilterChip(
                selected = uiState.onlyUnsettled,
                onClick = onToggleUnsettled,
                label = { Text(stringResource(R.string.debt_filter_unsettled_only)) },
                shape = AppTheme.shapes.capsule,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AppTheme.colors.sand,
                    selectedLabelColor = AppTheme.colors.onSand,
                    selectedLeadingIconColor = AppTheme.colors.onSand,
                )
            )
        }

        // Danh sách nội dung
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.debts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant, AppTheme.shapes.corner24),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Handshake,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.debt_empty_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.debt_empty_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    // Hành động chính nằm ngay trong màn trống, không bắt người dùng đi tìm FAB.
                    Button(
                        onClick = onAddClick,
                        shape = AppTheme.shapes.capsule,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppTheme.colors.ink,
                            contentColor = AppTheme.colors.onInk,
                        ),
                        modifier = Modifier.defaultMinSize(minHeight = 56.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = stringResource(R.string.debt_action_add),
                            modifier = Modifier.padding(start = 8.dp),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary card on top
                if (uiState.summary != null) {
                    item {
                        DebtSummaryCard(
                            summary = uiState.summary
                        )
                    }
                }

                // Danh sách thẻ nợ
                items(
                    items = uiState.debts,
                    key = { it.debt.id }
                ) { item ->
                    DebtItemCard(
                        item = item,
                        onClick = { onDebtClick(item.debt.id) },
                        isSelected = item.debt.id == selectedDebtId
                    )
                }
            }
        }
    }
}

/** Tab dạng capsule: mục đang chọn có nền surface + viền, mục còn lại trong suốt. */
@Composable
private fun DebtTabPill(
    label: String,
    dotColor: Color?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = AppTheme.shapes.capsule,
        color = if (selected) MaterialTheme.colorScheme.surface else Color.Transparent,
        border = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier
            .defaultMinSize(minHeight = 48.dp)
            .semantics { this.selected = selected },
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 48.dp)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (dotColor != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(dotColor, AppTheme.shapes.capsule)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
    }
}

@Composable
private fun DebtDetailPane(
    debtId: Long?,
    onClosePane: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.padding(start = 4.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        shape = AppTheme.shapes.corner24,
        color = AppTheme.colors.surfaceSunken,
    ) {
        if (debtId == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                DetailPaneEmptyState(icon = Icons.Rounded.Handshake)
            }
        } else {
            // Dùng lại nguyên route chi tiết: cùng ViewModel, cùng UI, chỉ khác nguồn id.
            DebtDetailScreen(
                onBack = onClosePane,
                debtId = debtId,
                paneMode = true,
            )
        }
    }
}

// ==== PREVIEWS ====

private val sampleUiState = DebtUiState(
    isLoading = false,
    summary = DebtSummary(
        totalToCollect = Money(15_000_000_00),
        totalToPay = Money(5_000_000_00),
        netBalance = Money(10_000_000_00),
        activeDebtsCount = 4,
        overdueCount = 1,
        dueTodayCount = 0,
    ),
    debts = listOf(
        DebtWithHistory(
            debt = Debt(
                id = 1L,
                personName = "Nguyễn Văn A",
                phoneNumber = "0901234567",
                type = DebtType.LEND,
                originalAmount = Money(5_000_000_00),
                createdAt = Instant.parse("2026-10-01T00:00:00Z"),
                dueDate = Instant.parse("2026-10-30T00:00:00Z"),
                note = "Vay tiền mua laptop",
            ),
            payments = listOf(
                DebtPayment(
                    id = 101L,
                    debtId = 1L,
                    amount = Money(2_000_000_00),
                    paidAt = Instant.parse("2026-10-15T00:00:00Z"),
                )
            )
        ),
        DebtWithHistory(
            debt = Debt(
                id = 2L,
                personName = "Trần Thị B",
                type = DebtType.BORROW,
                originalAmount = Money(3_000_000_00),
                createdAt = Instant.parse("2026-10-05T00:00:00Z"),
                dueDate = Instant.parse("2026-10-20T00:00:00Z"),
                note = "Mượn tiền nhập hàng",
            )
        )
    )
)

@Preview(name = "Light", showBackground = true)
@Composable
private fun DebtManagementContentPreviewLight() {
    NotePayTheme(darkTheme = false) {
        DebtManagementContent(
            uiState = sampleUiState,
            isSearchActive = false,
            isTwoPane = false,
            selectedDebtId = null,
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {},
            onTabSelected = {},
            onToggleUnsettled = {},
            onDebtClick = {},
            onAddClick = {},
            onClosePane = {},
        )
    }
}

@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES, showBackground = true)
@Composable
private fun DebtManagementContentPreviewDark() {
    NotePayTheme(darkTheme = true) {
        DebtManagementContent(
            uiState = sampleUiState,
            isSearchActive = false,
            isTwoPane = false,
            selectedDebtId = null,
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {},
            onTabSelected = {},
            onToggleUnsettled = {},
            onDebtClick = {},
            onAddClick = {},
            onClosePane = {},
        )
    }
}

@Preview(name = "FontScale 1.3", fontScale = 1.3f, showBackground = true)
@Composable
private fun DebtManagementContentPreviewFont13() {
    NotePayTheme(darkTheme = false) {
        DebtManagementContent(
            uiState = sampleUiState,
            isSearchActive = false,
            isTwoPane = false,
            selectedDebtId = null,
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {},
            onTabSelected = {},
            onToggleUnsettled = {},
            onDebtClick = {},
            onAddClick = {},
            onClosePane = {},
        )
    }
}

@Preview(name = "FontScale 2.0", fontScale = 2.0f, showBackground = true)
@Composable
private fun DebtManagementContentPreviewFont20() {
    NotePayTheme(darkTheme = false) {
        DebtManagementContent(
            uiState = sampleUiState,
            isSearchActive = false,
            isTwoPane = false,
            selectedDebtId = null,
            onBack = {},
            onToggleSearch = {},
            onSearchQueryChange = {},
            onTabSelected = {},
            onToggleUnsettled = {},
            onDebtClick = {},
            onAddClick = {},
            onClosePane = {},
        )
    }
}