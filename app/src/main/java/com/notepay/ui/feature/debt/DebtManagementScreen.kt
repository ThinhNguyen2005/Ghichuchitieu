package com.notepay.ui.feature.debt

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notepay.R
import com.notepay.domain.model.debt.DebtType
import com.notepay.ui.adaptive.LocalWindowLayout
import com.notepay.ui.adaptive.WindowLayout
import com.notepay.ui.component.DetailPaneEmptyState
import com.notepay.ui.theme.AppTheme

@OptIn(ExperimentalMaterial3Api::class)
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
                    IconButton(onClick = { isSearchActive = !isSearchActive }) {
                        Icon(
                            imageVector = if (isSearchActive) Icons.Rounded.Clear else Icons.Rounded.Search,
                            contentDescription = null
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
                        selectedDebtId = resolvedDebtId,
                        onSearchQueryChange = viewModel::setSearchQuery,
                        onTabSelected = viewModel::setTab,
                        onToggleUnsettled = { viewModel.setOnlyUnsettled(!uiState.onlyUnsettled) },
                        onDebtClick = { debtId ->
                            // Expanded: chọn tại chỗ thay vì đẩy route chi tiết lên back stack.
                            if (isTwoPane) selectedDebtId = debtId else onDebtClick(debtId)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    FloatingActionButton(
                        onClick = { showCreateSheet = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = AppTheme.shapes.corner20,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.debt_action_add)
                        )
                    }
                }

                if (isTwoPane) {
                    DebtDetailPane(
                        debtId = resolvedDebtId,
                        onClosePane = { selectedDebtId = null },
                        modifier = Modifier
                            .weight(0.55f)
                            .fillMaxHeight(),
                    )
                }
            }
        }
    }

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DebtListPane(
    uiState: DebtUiState,
    isSearchActive: Boolean,
    selectedDebtId: Long?,
    onSearchQueryChange: (String) -> Unit,
    onTabSelected: (DebtType?) -> Unit,
    onToggleUnsettled: () -> Unit,
    onDebtClick: (Long) -> Unit,
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

        // Tabs: Tất cả / Cần thu / Cần trả
        val tabs = listOf<DebtType?>(null, DebtType.LEND, DebtType.BORROW)
        val selectedTabIndex = tabs.indexOf(uiState.selectedTab).coerceAtLeast(0)

        SecondaryTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxWidth()
        ) {
            Tab(
                selected = uiState.selectedTab == null,
                onClick = { onTabSelected(null) },
                text = {
                    Text(
                        text = stringResource(R.string.transaction_list_filter_all),
                        fontWeight = if (uiState.selectedTab == null) FontWeight.Bold else FontWeight.Medium
                    )
                }
            )
            Tab(
                selected = uiState.selectedTab == DebtType.LEND,
                onClick = { onTabSelected(DebtType.LEND) },
                text = {
                    Text(
                        text = stringResource(R.string.debt_collect),
                        fontWeight = if (uiState.selectedTab == DebtType.LEND) FontWeight.Bold else FontWeight.Medium,
                        color = if (uiState.selectedTab == DebtType.LEND) Color(0xFF248A3D) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Tab(
                selected = uiState.selectedTab == DebtType.BORROW,
                onClick = { onTabSelected(DebtType.BORROW) },
                text = {
                    Text(
                        text = stringResource(R.string.debt_tab_borrow),
                        fontWeight = if (uiState.selectedTab == DebtType.BORROW) FontWeight.Bold else FontWeight.Medium,
                        color = if (uiState.selectedTab == DebtType.BORROW) Color(0xFFC97600) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }

        // Filter Chip "Chưa tất toán"
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            FilterChip(
                selected = uiState.onlyUnsettled,
                onClick = onToggleUnsettled,
                label = { Text(stringResource(R.string.debt_filter_unsettled_only)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.FilterList,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
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
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 8.dp,
                    bottom = 88.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Summary card on top
                if (uiState.summary != null) {
                    item {
                        DebtSummaryCard(
                            summary = uiState.summary,
                            onClick = {}
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

