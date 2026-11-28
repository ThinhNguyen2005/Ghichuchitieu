package com.notepay.feature.billsplit.main.sheet

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import com.notepay.core.designsystem.component.effect.AppAtmosphericBackground
import com.notepay.core.designsystem.component.effect.iosCardShadow
import com.notepay.core.designsystem.feedback.FeedbackSnackbarHost
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.designsystem.feedback.showFeedback
import com.notepay.core.designsystem.theme.AppTheme
import com.notepay.core.designsystem.util.VietnamCurrencyVisualTransformation
import com.notepay.core.navigation.NotePayAnimatedContent
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.feature.billsplit.main.component.BillSplitSummaryFooter
import com.notepay.feature.billsplit.main.component.billSplitDebtorListItems
import com.notepay.feature.billsplit.main.component.billSplitTransactionSelectorItems
import kotlinx.coroutines.launch

/** Sentinel for "no transaction selected" stored cleanly inside `rememberSaveable`. */
internal const val NoTransactionSelected = -1L

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

internal enum class BillSplitStep { SELECT_TRANSACTION, MANAGE_DEBTORS }

internal fun vndInputToCents(input: String): Long? {
    val vnd = input.toLongOrNull() ?: return null
    if (vnd <= 0L || vnd > Long.MAX_VALUE / 100L) return null
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
 * Splits [totalCents] in whole VND either across `debtorCount + 1` (when the payer shares the bill
 * via [includePayer] = true) or solely across `debtorCount` ([includePayer] = false).
 */
internal fun splitEvenlyInVnd(
    totalCents: Long,
    debtorCount: Int,
    includePayer: Boolean = false,
): List<String> {
    if (totalCents <= 0L || debtorCount <= 0) return emptyList()
    val participantCount = debtorCount + if (includePayer) 1 else 0
    val totalVnd = totalCents / 100L
    val baseAmount = totalVnd / participantCount
    val remainder = totalVnd % participantCount
    return List(debtorCount) { index ->
        val extra = if (!includePayer && index < remainder) 1L else 0L
        (baseAmount + extra).toString()
    }
}

@Composable
fun BillSplitCreateSheet(
    recentTransactions: List<Transaction>,
    allDebtorNames: List<String> = emptyList(),
    onAddExpense: () -> Unit = {},
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
    var selectedTransactionId by rememberSaveable { mutableLongStateOf(NoTransactionSelected) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var newDebtorName by rememberSaveable { mutableStateOf("") }
    var showDiscardConfirm by rememberSaveable { mutableStateOf(false) }

    val debtors = rememberSaveable(saver = DebtorEntriesSaver) {
        mutableStateListOf<DebtorEntry>()
    }

    val selectedTx = remember(expenses, selectedTransactionId) {
        expenses.find { it.id == selectedTransactionId }
    }

    LaunchedEffect(expenses) {
        if (selectedTransactionId == NoTransactionSelected && expenses.size == 1) {
            selectedTransactionId = expenses.first().id
        }
    }

    val currencyTransformation = remember { VietnamCurrencyVisualTransformation() }
    val localView = LocalView.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val parentCents = selectedTx?.amount?.amountInCents ?: 0L
    val totalCents by remember { derivedStateOf { enteredTotalCents(debtors) } }
    val isOverLimit = totalCents > parentCents
    val allAmountsValid by remember {
        derivedStateOf {
            debtors.isNotEmpty() &&
                debtors.all { entry ->
                    entry.name.isNotBlank() && vndInputToCents(entry.amountInput) != null
                }
        }
    }
    val canSave = selectedTx != null && allAmountsValid && !isOverLimit

    fun requestDismiss() {
        when {
            showDiscardConfirm -> showDiscardConfirm = false
            step == BillSplitStep.MANAGE_DEBTORS && debtors.isEmpty() -> step = BillSplitStep.SELECT_TRANSACTION
            debtors.isNotEmpty() -> showDiscardConfirm = true
            else -> onDismiss()
        }
    }

    fun playSelectionHaptic() {
        localView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun addDebtor(rawName: String) {
        val name = rawName.trim()
        if (name.isBlank() || debtors.any { it.name.equals(name, ignoreCase = true) }) return
        debtors.add(DebtorEntry(name = name, amountInput = ""))
        newDebtorName = ""
        playSelectionHaptic()
    }

    val evenSplitOverwrittenMessage = stringResource(R.string.billsplit_even_split_overwritten)
    val undoLabel = stringResource(R.string.feedback_undo)
    val chooseTransactionTitle = stringResource(R.string.billsplit_choose_transaction)
    val addParticipantTitle = stringResource(R.string.billsplit_add_participant)

    fun splitEvenly(includePayer: Boolean) {
        val blankIndexes = debtors.indices.filter { debtors[it].amountInput.isBlank() }
        val overwritesTypedAmounts = blankIndexes.isEmpty() && debtors.isNotEmpty()
        val previousEntries = if (overwritesTypedAmounts) debtors.toList() else emptyList()

        val targets = blankIndexes.ifEmpty { debtors.indices.toList() }
        val alreadyCents = debtors.indices
            .filterNot(targets::contains)
            .sumOf { index -> vndInputToCents(debtors[index].amountInput) ?: 0L }

        val amounts = splitEvenlyInVnd(
            totalCents = (parentCents - alreadyCents).coerceAtLeast(0L),
            debtorCount = targets.size,
            includePayer = includePayer,
        )

        targets.forEachIndexed { position, index ->
            val amount = amounts.getOrNull(position) ?: return@forEachIndexed
            debtors[index] = debtors[index].copy(amountInput = amount)
        }
        playSelectionHaptic()

        if (overwritesTypedAmounts) {
            coroutineScope.launch {
                val wasUndone = snackbarHostState.showFeedback(
                    UiFeedback(message = evenSplitOverwrittenMessage, actionLabel = undoLabel),
                )
                if (wasUndone) {
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
        Box(modifier = Modifier.fillMaxSize()) {
            AppAtmosphericBackground(
                modifier = Modifier.matchParentSize(),
                forceSoftBlurOnPhoto = true,
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding(),
            ) {
                BillSplitCreateHeader(
                    stepNumber = if (step == BillSplitStep.SELECT_TRANSACTION) 1 else 2,
                    isSecondStep = step == BillSplitStep.MANAGE_DEBTORS,
                    onLeadingClick = {
                        if (step == BillSplitStep.MANAGE_DEBTORS) {
                            step = BillSplitStep.SELECT_TRANSACTION
                        } else {
                            requestDismiss()
                        }
                    },
                    onCloseClick = ::requestDismiss,
                )

                NotePayAnimatedContent(
                    targetState = step,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    label = "bill-split-create-step",
                ) { activeStep ->
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        billSplitTransactionSelectorItems(
                            step = activeStep,
                            expenses = expenses,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedTx = selectedTx,
                            selectedTransactionId = selectedTransactionId,
                            chooseTransactionTitle = chooseTransactionTitle,
                            onPickTransaction = { transaction ->
                                if (selectedTransactionId != transaction.id) debtors.clear()
                                selectedTransactionId = transaction.id
                                playSelectionHaptic()
                            },
                            onAddExpense = onAddExpense,
                        )

                        billSplitDebtorListItems(
                            step = activeStep,
                            selectedTx = selectedTx,
                            debtors = debtors,
                            allDebtorNames = allDebtorNames,
                            addParticipantTitle = addParticipantTitle,
                            newDebtorName = newDebtorName,
                            onNewDebtorNameChange = { newDebtorName = it },
                            onAddDebtor = ::addDebtor,
                            onSplitEvenlyIncludePayer = { splitEvenly(includePayer = true) },
                            onSplitEvenlyGroupOnly = { splitEvenly(includePayer = false) },
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
                }

                FeedbackSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.fillMaxWidth(),
                )

                BillSplitSummaryFooter(
                    step = step,
                    selectedTx = selectedTx,
                    debtors = debtors,
                    canSave = canSave,
                    onContinue = { step = BillSplitStep.MANAGE_DEBTORS },
                    onSaveSplit = onConfirm,
                    onHapticTap = {
                        localView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
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
private fun BillSplitCreateHeader(
    stepNumber: Int,
    isSecondStep: Boolean,
    onLeadingClick: () -> Unit,
    onCloseClick: () -> Unit,
) {
    val pillColor = AppTheme.colors.surface.copy(alpha = if (AppTheme.colors.isLight) 0.92f else 0.88f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Surface(
            onClick = onLeadingClick,
            shape = CircleShape,
            color = pillColor,
            border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
            modifier = Modifier
                .size(48.dp)
                .iosCardShadow(shape = CircleShape, elevated = false),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isSecondStep) Icons.AutoMirrored.Rounded.ArrowBack else Icons.Rounded.Close,
                    contentDescription = stringResource(
                        if (isSecondStep) R.string.billsplit_cd_back else R.string.action_close,
                    ),
                    tint = AppTheme.colors.textPrimary,
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.quick_add_bill_split_title),
                style = AppTheme.typography.displaySerif,
                color = AppTheme.colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Surface(
            shape = AppTheme.shapes.capsule,
            color = AppTheme.colors.sand,
        ) {
            Text(
                text = stringResource(R.string.billsplit_step_indicator_format, stepNumber),
                style = AppTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = AppTheme.colors.onSand,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }

        if (isSecondStep) {
            Surface(
                onClick = onCloseClick,
                shape = CircleShape,
                color = pillColor,
                border = BorderStroke(0.5.dp, AppTheme.colors.hairline),
                modifier = Modifier
                    .size(48.dp)
                    .iosCardShadow(shape = CircleShape, elevated = false),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = AppTheme.colors.textPrimary,
                    )
                }
            }
        }
    }
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
            color = if (completed) AppTheme.colors.sand else AppTheme.colors.surfaceSunken,
            contentColor = if (completed) AppTheme.colors.onSand else AppTheme.colors.textSecondary,
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
            color = AppTheme.colors.textPrimary,
        )
    }
}
