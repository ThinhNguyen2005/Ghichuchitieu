package com.notepay.feature.billsplit.main.sheet

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notepay.R
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.feature.billsplit.main.component.BillSplitSummaryFooter
import com.notepay.feature.billsplit.main.component.billSplitDebtorListItems
import com.notepay.feature.billsplit.main.component.billSplitTransactionSelectorItems
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import kotlinx.coroutines.launch

/** Giá trị "chưa chọn giao dịch" — dùng Long thay vì null để rememberSaveable chắc chắn lưu được. */
internal const val NoTransactionSelected = -1L

/*
 * Saver phẳng hoá danh sách người nợ để sống qua tái cấu hình.
 *
 * Không nối tên và số tiền bằng ký tự phân cách: danh sách được lưu xen kẽ
 * [tên, tiền, tên, tiền, …] nên tên chứa dấu phẩy, xuống dòng hay bất kỳ ký tự
 * nào vẫn khôi phục nguyên vẹn.
 */
private val DebtorEntriesSaver = listSaver<SnapshotStateList<DebtorEntry>, String>(
    save = { entries -> entries.flatMap { entry -> listOf(entry.name, entry.amountInput) } },
    restore = { flat ->
        mutableStateListOf<DebtorEntry>().apply {
            flat.chunked(2).forEach { pair ->
                val name = pair.getOrNull(0) ?: return@forEach
                val amountInput = pair.getOrNull(1) ?: return@forEach

                add(DebtorEntry(name = name, amountInput = amountInput))
            }
        }
    },
)

data class DebtorEntry(
    val name: String,
    val amountInput: String,
)

/**
 * Tách luồng chia hóa đơn thành 2 bước rời nhau để mỗi bước chỉ hiển thị đúng phần
 * đang cần thao tác, tránh vừa chọn giao dịch vừa nhập danh sách người nợ trên cùng
 * một màn hình chật.
 */
internal enum class BillSplitStep { SELECT_TRANSACTION, MANAGE_DEBTORS }

internal fun vndInputToCents(input: String): Long? {
    val vnd = input.toLongOrNull() ?: return null

    if (vnd <= 0L || vnd > Long.MAX_VALUE / 100L) {
        return null
    }

    return vnd * 100L
}

internal fun enteredTotalCents(entries: List<DebtorEntry>): Long {
    var total = 0L

    entries.forEach { entry ->
        val cents = vndInputToCents(entry.amountInput) ?: return@forEach

        total = try {
            Math.addExact(total, cents)
        } catch (_: ArithmeticException) {
            return Long.MAX_VALUE
        }
    }

    return total
}

/**
 * Chia theo đơn vị VND nguyên vì field hiện tại không cho nhập phần lẻ.
 *
 * ponytail: nếu domain hỗ trợ phần nhỏ hơn 1 VND, nâng cấp field để nhập trực tiếp
 * amountInCents thay vì bỏ phần dư dưới 100 cents.
 */
private fun splitEvenlyInVnd(
    totalCents: Long,
    personCount: Int,
): List<String> {
    if (totalCents <= 0L || personCount <= 0) return emptyList()

    val totalVnd = totalCents / 100L
    val baseAmount = totalVnd / personCount
    val remainder = totalVnd % personCount

    return List(personCount) { index ->
        (baseAmount + if (index < remainder) 1L else 0L).toString()
    }
}

/**
 * Phiếu tạo hóa đơn chia (Dialog toàn màn, 2 bước).
 *
 * Sheet này chỉ còn vai trò điều phối: giữ toàn bộ state sống qua tái cấu hình,
 * các hàm nghiệp vụ (thêm người, chia đều + hoàn tác, thoát có xác nhận) và khung
 * LazyColumn. Giao diện từng phần nằm trong:
 * - [billSplitTransactionSelectorItems]: bước chọn giao dịch
 * - [billSplitDebtorListItems]: bước quản lý người nợ
 * - [BillSplitSummaryFooter]: thanh tiến độ + trạng thái tiền + nút hành động
 * - [BillSplitDiscardDialog]: hộp thoại xác nhận hủy
 */
@Composable
fun BillSplitCreateSheet(
    recentTransactions: List<Transaction>,
    allDebtorNames: List<String> = emptyList(),
    onConfirm: (
        parentTransactionId: Long,
        entries: List<Pair<String, Long>>,
    ) -> Unit,
    onDismiss: () -> Unit,
) {
    val expenses = remember(recentTransactions) {
        recentTransactions.filter { it.type == TransactionType.EXPENSE }
    }

    var step by rememberSaveable { mutableStateOf(BillSplitStep.SELECT_TRANSACTION) }
    var selectedTransactionId by rememberSaveable { mutableStateOf(NoTransactionSelected) }
    var newDebtorName by rememberSaveable { mutableStateOf("") }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    /*
     * Trước đây tất cả dùng remember nên xoay màn hình là mất sạch người đã nhập.
     * Danh sách người nợ lưu qua Saver phẳng hoá; giao dịch đang chọn lưu theo id.
     */
    val debtors = rememberSaveable(saver = DebtorEntriesSaver) {
        mutableStateListOf<DebtorEntry>()
    }

    val selectedTx = remember(expenses, selectedTransactionId) {
        expenses.find { it.id == selectedTransactionId }
    }

    // Chỉ có đúng một giao dịch chi tiêu: chọn sẵn để bớt một bước bấm.
    LaunchedEffect(expenses) {
        if (selectedTransactionId == NoTransactionSelected && expenses.size == 1) {
            selectedTransactionId = expenses.first().id
        }
    }

    val currencyTransformation = remember {
        VietnamCurrencyVisualTransformation()
    }

    val localView = LocalView.current

    // Dialog có cửa sổ riêng nên snackbar phải sống trong chính sheet này.
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val parentCents = selectedTx?.amount?.amountInCents ?: 0L

    val totalCents by remember {
        derivedStateOf {
            enteredTotalCents(debtors)
        }
    }

    val isOverLimit = totalCents > parentCents

    val allAmountsValid by remember {
        derivedStateOf {
            debtors.isNotEmpty() &&
                    debtors.all { entry ->
                        entry.name.isNotBlank() &&
                                vndInputToCents(entry.amountInput) != null
                    }
        }
    }

    val canSave =
        selectedTx != null &&
                allAmountsValid &&
                !isOverLimit

    /*
     * Back ở bước 2 quay về bước 1 thay vì đóng cả phiếu; chỉ hỏi xác nhận
     * khi đã có dữ liệu người nợ có thể mất.
     *
     * Hàm này được truyền vào Dialog thay vì dùng BackHandler: Dialog tạo window
     * riêng và tự nuốt sự kiện Back trước khi nó tới được activity, nên BackHandler
     * đặt ở composition phía ngoài sẽ không bao giờ chạy.
     */
    fun requestDismiss() {
        when {
            showDiscardConfirm -> showDiscardConfirm = false

            step == BillSplitStep.MANAGE_DEBTORS && debtors.isEmpty() ->
                step = BillSplitStep.SELECT_TRANSACTION

            debtors.isNotEmpty() -> showDiscardConfirm = true

            else -> onDismiss()
        }
    }

    fun playSelectionHaptic() {
        localView.performHapticFeedback(
            HapticFeedbackConstants.CLOCK_TICK,
        )
    }

    fun addDebtor(rawName: String) {
        val name = rawName.trim()

        if (
            name.isBlank() ||
            debtors.any { it.name.equals(name, ignoreCase = true) }
        ) {
            return
        }

        /*
         * Không tự điền toàn bộ tiền còn lại vì hành vi đó gây bất ngờ
         * khi người dùng thêm nhiều người liên tiếp.
         */
        debtors.add(
            DebtorEntry(
                name = name,
                amountInput = "",
            )
        )

        newDebtorName = ""
        playSelectionHaptic()
    }

    val evenSplitOverwrittenMessage = stringResource(R.string.billsplit_even_split_overwritten)
    val undoLabel = stringResource(R.string.feedback_undo)

    // Hai chuỗi tiêu đề bước được hoist ở đây vì body các extension LazyListScope
    // bên dưới không phải Composable scope (không gọi được stringResource trực tiếp).
    val chooseTransactionTitle = stringResource(R.string.billsplit_choose_transaction)
    val addParticipantTitle = stringResource(R.string.billsplit_add_participant)

    /*
     * Chia đều chỉ điền vào các ô đang trống và chia phần còn lại của hóa đơn,
     * để không ghi đè số người dùng đã gõ tay.
     */
    fun splitEvenly() {
        val blankIndexes = debtors.indices.filter { debtors[it].amountInput.isBlank() }
        val overwritesTypedAmounts = blankIndexes.isEmpty() && debtors.isNotEmpty()

        // Không còn ô trống: mọi số đã gõ sẽ bị ghi đè, nên giữ bản sao để hoàn tác.
        val previousEntries = if (overwritesTypedAmounts) debtors.toList() else emptyList()

        val targets = blankIndexes.ifEmpty { debtors.indices.toList() }
        val alreadyCents = debtors.indices
            .filterNot(targets::contains)
            .sumOf { index -> vndInputToCents(debtors[index].amountInput) ?: 0L }

        val amounts = splitEvenlyInVnd(
            totalCents = (parentCents - alreadyCents).coerceAtLeast(0L),
            personCount = targets.size,
        )

        targets.forEachIndexed { position, index ->
            val amount = amounts.getOrNull(position) ?: return@forEachIndexed

            debtors[index] = debtors[index].copy(amountInput = amount)
        }

        playSelectionHaptic()

        if (overwritesTypedAmounts) {
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = evenSplitOverwrittenMessage,
                    actionLabel = undoLabel,
                    duration = SnackbarDuration.Short,
                )

                if (result == SnackbarResult.ActionPerformed) {
                    debtors.clear()
                    debtors.addAll(previousEntries)
                }
            }
        }
    }

    Dialog(
        onDismissRequest = ::requestDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            // Column + weight(1f): thanh nút tự lấy chiều cao thật, danh sách nhận phần
            // còn lại. Trước đây dùng Box chồng lớp với bottom = 104.dp cố định, nhỏ hơn
            // chiều cao thật của thanh nút (~112dp trở lên) nên nội dung bị đè.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Dòng phụ "Bước x/2 — ..." đã bỏ: số bước đã nằm ở
                    // BillSplitSectionTitle, thêm một chỗ nhắc nữa chỉ gây nhiễu.
                    Text(
                        text = stringResource(R.string.quick_add_bill_split_title),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    TextButton(
                        onClick = ::requestDismiss,
                        modifier = Modifier.heightIn(min = 48.dp),
                    ) {
                        Text(stringResource(R.string.action_close))
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = 12.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    billSplitTransactionSelectorItems(
                        step = step,
                        expenses = expenses,
                        selectedTx = selectedTx,
                        selectedTransactionId = selectedTransactionId,
                        chooseTransactionTitle = chooseTransactionTitle,
                        onPickTransaction = { transaction ->
                            // Chỉ xóa danh sách người nợ khi thực sự đổi sang giao dịch
                            // khác, để quay lại bước 1 rồi chọn lại cùng giao dịch
                            // không làm mất dữ liệu đã nhập.
                            if (selectedTransactionId != transaction.id) {
                                debtors.clear()
                            }

                            selectedTransactionId = transaction.id
                            playSelectionHaptic()
                        },
                    )

                    billSplitDebtorListItems(
                        step = step,
                        selectedTx = selectedTx,
                        debtors = debtors,
                        allDebtorNames = allDebtorNames,
                        addParticipantTitle = addParticipantTitle,
                        newDebtorName = newDebtorName,
                        onNewDebtorNameChange = { newDebtorName = it },
                        onAddDebtor = ::addDebtor,
                        onSplitEvenly = ::splitEvenly,
                        onAmountChange = { index, input ->
                            debtors[index] = debtors[index].copy(amountInput = input)
                        },
                        onRemoveDebtor = { index ->
                            debtors.removeAt(index)
                            playSelectionHaptic()
                        },
                        onBackToStep1 = { step = BillSplitStep.SELECT_TRANSACTION },
                        currencyTransformation = currencyTransformation,
                    )
                }

                /*
                 * Snackbar nằm ngay trên thanh hành động. Không dùng snackbar cấp app:
                 * cửa sổ của Dialog sẽ che nó và người dùng không bấm được Hoàn tác.
                 */
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )

                BillSplitSummaryFooter(
                    step = step,
                    selectedTx = selectedTx,
                    debtors = debtors,
                    canSave = canSave,
                    onContinue = { step = BillSplitStep.MANAGE_DEBTORS },
                    onSaveSplit = onConfirm,
                    onHapticTap = {
                        localView.performHapticFeedback(
                            HapticFeedbackConstants.KEYBOARD_TAP,
                        )
                    },
                )
            }
        }
    }

    BillSplitDiscardDialog(
        visible = showDiscardConfirm,
        onStay = { showDiscardConfirm = false },
        onDiscard = {
            showDiscardConfirm = false
            onDismiss()
        },
    )
}

@Composable
internal fun BillSplitSectionTitle(
    step: Int,
    title: String,
    completed: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = AppTheme.shapes.circle,
            color = if (completed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            contentColor = if (completed) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (completed) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = stringResource(R.string.billsplit_completed),
                        modifier = Modifier.size(18.dp),
                    )
                } else {
                    Text(
                        text = step.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
