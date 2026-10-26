package com.notepay.ui.feature.billsplit

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Group
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.notepay.R
import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.ui.component.CategoryAvatar
import com.notepay.ui.component.GradientBottomActionBar
import com.notepay.ui.component.LiquidButton
import com.notepay.ui.theme.AppTheme
import com.notepay.ui.util.MoneyFormatter
import com.notepay.ui.util.VietnamCurrencyVisualTransformation
import kotlin.math.abs
import com.notepay.ui.util.localizedName
import kotlinx.coroutines.launch

private const val MAX_VND_INPUT_LENGTH = 15

/** Giá trị "chưa chọn giao dịch" — dùng Long thay vì null để rememberSaveable chắc chắn lưu được. */
private const val NoTransactionSelected = -1L

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
private enum class BillSplitStep { SELECT_TRANSACTION, MANAGE_DEBTORS }

private fun vndInputToCents(input: String): Long? {
    val vnd = input.toLongOrNull() ?: return null

    if (vnd <= 0L || vnd > Long.MAX_VALUE / 100L) {
        return null
    }

    return vnd * 100L
}

private fun enteredTotalCents(entries: List<DebtorEntry>): Long {
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

    val remainingCents = parentCents - totalCents
    val isOverLimit = totalCents > parentCents

    val progress = if (parentCents > 0L) {
        (totalCents.toFloat() / parentCents.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    // Một câu trạng thái tiền duy nhất cho cả màn, hiện ở thanh dưới cố định.
    val moneyStatus = when {
        isOverLimit -> stringResource(
            R.string.billsplit_status_over_format,
            MoneyFormatter.format(Money(abs(remainingCents))),
        )

        remainingCents == 0L -> stringResource(R.string.billsplit_status_exact)

        else -> stringResource(
            R.string.billsplit_status_remaining_format,
            MoneyFormatter.format(Money(remainingCents)),
        )
    }

    // accentColor chỉ dùng cho thanh tiến độ; chữ vẫn dùng onSurface để đủ tương phản ở mọi theme.
    val statusAccent = when {
        isOverLimit -> MaterialTheme.colorScheme.error
        remainingCents == 0L -> AppTheme.colors.success
        else -> MaterialTheme.colorScheme.primary
    }
    val statusTextColor = if (isOverLimit) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    val allAmountsValid by remember {
        derivedStateOf {
            debtors.isNotEmpty() &&
                    debtors.all { entry ->
                        entry.name.isNotBlank() &&
                                vndInputToCents(entry.amountInput) != null
                    }
        }
    }

    /*
     * SnapshotStateList không đổi reference khi thêm hoặc xóa phần tử,
     * vì vậy derivedStateOf phù hợp hơn remember(allDebtorNames, debtors).
     */
    val availableSuggestions by remember(allDebtorNames) {
        derivedStateOf {
            allDebtorNames.filter { suggestion ->
                debtors.none { debtor ->
                    debtor.name.equals(
                        suggestion,
                        ignoreCase = true,
                    )
                }
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
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                }

                if (step == BillSplitStep.SELECT_TRANSACTION) {
                    item {
                        BillSplitSectionTitle(
                            step = 1,
                            title = stringResource(R.string.billsplit_choose_transaction),
                            completed = selectedTx != null,
                        )
                    }
                }

                if (step == BillSplitStep.SELECT_TRANSACTION && expenses.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = AppTheme.shapes.corner16,
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme
                                    .errorContainer
                                    .copy(alpha = 0.3f),
                            ),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = stringResource(R.string.billsplit_no_expense_title),
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )

                                    Text(
                                        text = stringResource(R.string.billsplit_no_expense_desc),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                } else if (step == BillSplitStep.SELECT_TRANSACTION) {
                    items(
                        items = expenses.take(20),
                        key = { transaction -> transaction.id },
                    ) { transaction ->
                        TransactionPickerRow(
                            transaction = transaction,
                            selected = transaction.id == selectedTransactionId,
                            onClick = {
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
                    }
                }

                if (step == BillSplitStep.MANAGE_DEBTORS) {
                    selectedTx?.let { transaction ->
                        item {
                            // Bấm vào thẻ để quay lại bước 1 đổi giao dịch.
                            SelectedTransactionCard(
                                transaction = transaction,
                                onClick = { step = BillSplitStep.SELECT_TRANSACTION },
                            )
                        }
                    }

                    item {
                        BillSplitSectionTitle(
                            step = 2,
                            title = stringResource(R.string.billsplit_add_participant),
                            completed = debtors.isNotEmpty(),
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = newDebtorName,
                            onValueChange = { newDebtorName = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.billsplit_person_name)) },
                            placeholder = {
                                Text(stringResource(R.string.billsplit_person_name_placeholder))
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Rounded.Group,
                                    contentDescription = null,
                                )
                            },
                            trailingIcon = {
                                IconButton(
                                    // Không clearFocus: giữ bàn phím để thêm người tiếp theo.
                                    onClick = { addDebtor(newDebtorName) },
                                    enabled = newDebtorName.isNotBlank(),
                                    modifier = Modifier.size(48.dp),
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.billsplit_add_person),
                                    )
                                }
                            },
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = { addDebtor(newDebtorName) },
                            ),
                            singleLine = true,
                            shape = AppTheme.shapes.corner12,
                        )
                    }

                    if (availableSuggestions.isNotEmpty()) {
                        item {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = stringResource(R.string.billsplit_recent_suggestions),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )

                                LazyRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    items(
                                        items = availableSuggestions.take(10),
                                        key = { it },
                                    ) { name ->
                                        AssistChip(
                                            onClick = { addDebtor(name) },
                                            label = {
                                                Text(
                                                    text = name,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = Icons.Rounded.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.billsplit_participants),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )

                                Text(
                                    text = if (debtors.isEmpty()) {
                                        stringResource(R.string.billsplit_no_debtor_yet)
                                    } else {
                                        pluralStringResource(
                                            R.plurals.billsplit_debtor_added_count,
                                            debtors.size,
                                            debtors.size,
                                        )
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            FilledTonalButton(
                                onClick = ::splitEvenly,
                                enabled = debtors.isNotEmpty(),
                                modifier = Modifier.heightIn(min = 48.dp),
                            ) {
                                Text(stringResource(R.string.billsplit_split_evenly))
                            }
                        }
                    }

                    itemsIndexed(
                        items = debtors,
                        key = { _, entry -> entry.name },
                    ) { index, entry ->
                        SelectedDebtorCard(
                            entry = entry,
                            onAmountChange = { input ->
                                debtors[index] = entry.copy(amountInput = input)
                            },
                            onRemove = {
                                debtors.removeAt(index)
                                playSelectionHaptic()
                            },
                            currencyTransformation = currencyTransformation,
                        )
                    }
                }
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

            /*
             * Thanh dưới cố định: tiến độ + câu trạng thái tiền + nút hành động.
             * Trước đây tiến độ nằm trong danh sách nên cuộn mất khi đang nhập ở dưới,
             * còn trạng thái tiền bị lặp ở thẻ tiến độ, thẻ tổng kết và lý do khoá nút.
             */
            GradientBottomActionBar(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    if (step == BillSplitStep.MANAGE_DEBTORS) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = statusAccent,
                            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            strokeCap = StrokeCap.Round,
                        )

                        Text(
                            text = moneyStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = statusTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else if (selectedTx == null) {
                        Text(
                            text = stringResource(R.string.billsplit_select_transaction_hint),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    if (step == BillSplitStep.SELECT_TRANSACTION) {
                        SheetPrimaryButton(
                            label = stringResource(R.string.billsplit_continue),
                            enabled = selectedTx != null,
                            onClick = {
                                localView.performHapticFeedback(
                                    HapticFeedbackConstants.KEYBOARD_TAP,
                                )

                                step = BillSplitStep.MANAGE_DEBTORS
                            },
                        )
                    } else {
                        SheetPrimaryButton(
                            label = stringResource(R.string.billsplit_save_invoice_split),
                            enabled = canSave,
                            onClick = {
                                selectedTx?.let { transaction ->
                                    val entries = debtors.mapNotNull { entry ->
                                        val cents = vndInputToCents(entry.amountInput)

                                        if (cents != null) {
                                            entry.name.trim() to cents
                                        } else {
                                            null
                                        }
                                    }

                                    localView.performHapticFeedback(
                                        HapticFeedbackConstants.KEYBOARD_TAP,
                                    )

                                    onConfirm(
                                        transaction.id,
                                        entries,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            }
        }
    }

    if (showDiscardConfirm) {
        AlertDialog(
            onDismissRequest = { showDiscardConfirm = false },
            title = { Text(stringResource(R.string.billsplit_discard_title)) },
            text = { Text(stringResource(R.string.billsplit_discard_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardConfirm = false
                        onDismiss()
                    },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.billsplit_discard_confirm))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDiscardConfirm = false },
                    modifier = Modifier.heightIn(min = 48.dp),
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            },
        )
    }
}

@Composable
private fun BillSplitSectionTitle(
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

@Composable
private fun SheetPrimaryButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    LiquidButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SelectedTransactionCard(
    transaction: Transaction,
    onClick: () -> Unit,
) {
    val category = transaction.category
    val noteText = transaction.note.trim()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner16,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme
                .primaryContainer
                .copy(alpha = 0.35f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 44.dp,
                iconSize = 22.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = MoneyFormatter.format(transaction.amount),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )

                // Trước đây là một dòng chữ nhỏ trong thẻ nên rất dễ bỏ sót.
                TextButton(
                    onClick = onClick,
                    modifier = Modifier.heightIn(min = 48.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.billsplit_change_transaction_action),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionPickerRow(
    transaction: Transaction,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val category = transaction.category
    val noteText = transaction.note.trim()

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme
                    .primaryContainer
                    .copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = 14.dp,
                    vertical = 12.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CategoryAvatar(
                category = category,
                size = 40.dp,
                iconSize = 20.dp,
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category.localizedName(),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                if (noteText.isNotBlank()) {
                    Text(
                        text = noteText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Text(
                text = MoneyFormatter.format(transaction.amount),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
            )

            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = stringResource(R.string.billsplit_selecting),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
fun SelectedDebtorCard(
    entry: DebtorEntry,
    onAmountChange: (String) -> Unit,
    onRemove: () -> Unit,
    currencyTransformation: VietnamCurrencyVisualTransformation,
) {
    val focusManager = LocalFocusManager.current

    val amountIsInvalid =
        entry.amountInput.isNotBlank() &&
                vndInputToCents(entry.amountInput) == null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = AppTheme.shapes.corner12,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    modifier = Modifier.size(36.dp),
                    shape = AppTheme.shapes.circle,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = entry.name
                                .firstOrNull()
                                ?.uppercase()
                                ?: "?",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = entry.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.billsplit_delete_person_format, entry.name),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }

            OutlinedTextField(
                value = entry.amountInput,
                onValueChange = { rawInput ->
                    onAmountChange(
                        rawInput
                            .filter(Char::isDigit)
                            .take(MAX_VND_INPUT_LENGTH),
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.billsplit_person_amount)) },
                placeholder = { Text("0") },
                suffix = { Text("₫") },
                supportingText = if (amountIsInvalid) {
                    { Text(stringResource(R.string.billsplit_amount_invalid)) }
                } else {
                    null
                },
                isError = amountIsInvalid,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                    },
                ),
                visualTransformation = currencyTransformation,
                singleLine = true,
                shape = AppTheme.shapes.corner12,
            )
        }
    }
}
