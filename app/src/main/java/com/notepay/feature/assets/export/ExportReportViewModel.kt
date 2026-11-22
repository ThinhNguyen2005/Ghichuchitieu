package com.notepay.feature.assets.export

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.di.IoDispatcher
import com.notepay.domain.export.ExportDatePreset
import com.notepay.domain.export.ExportFormat
import com.notepay.domain.export.ExportTypeFilter
import com.notepay.domain.export.FinancialCsvExporter
import com.notepay.domain.export.FinancialReportBuilder
import com.notepay.domain.export.FinancialReportData
import com.notepay.domain.export.FinancialReportFilter
import com.notepay.domain.export.PureKotlinXlsxExporter
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import java.io.File
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import kotlin.time.Clock

@Immutable
data class ExportReportUiState(
    val datePreset: ExportDatePreset = ExportDatePreset.THIS_MONTH,
    val startDate: LocalDate = LocalDate(2026, 1, 1),
    val endDate: LocalDate = LocalDate(2026, 1, 31),
    val availableWallets: List<Wallet> = emptyList(),
    val selectedWalletIds: Set<Long> = emptySet(),
    val typeFilter: ExportTypeFilter = ExportTypeFilter.ALL,
    val format: ExportFormat = ExportFormat.XLSX,
    val matchingCount: Int = 0,
    val isExporting: Boolean = false,
) {
    fun suggestedFileName(): String {
        val ext = if (format == ExportFormat.XLSX) "xlsx" else "csv"
        return String.format(
            Locale.ROOT,
            "notepay_baocao_%04d%02d%02d.%s",
            endDate.year,
            endDate.month.number,
            endDate.day,
            ext,
        )
    }

    fun mimeType(): String = if (format == ExportFormat.XLSX) {
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    } else {
        "text/csv"
    }
}

private data class ExportFilterSelection(
    val datePreset: ExportDatePreset,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val selectedWalletIds: Set<Long>,
    val typeFilter: ExportTypeFilter,
    val format: ExportFormat,
    val isExporting: Boolean,
)

@HiltViewModel
class ExportReportViewModel @Inject constructor(
    transactionRepository: TransactionRepository,
    walletRepository: WalletRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val zone = TimeZone.currentSystemDefault()
    private val initialRange = presetRange(ExportDatePreset.THIS_MONTH, todayLocal())
    private val _selection = MutableStateFlow(
        ExportFilterSelection(
            datePreset = ExportDatePreset.THIS_MONTH,
            startDate = initialRange.first,
            endDate = initialRange.second,
            selectedWalletIds = emptySet(),
            typeFilter = ExportTypeFilter.ALL,
            format = ExportFormat.XLSX,
            isExporting = false,
        ),
    )

    private var cachedTransactions: List<Transaction> = emptyList()
    private var cachedWallets: List<Wallet> = emptyList()

    private val reportBuilder = FinancialReportBuilder()
    private val xlsxExporter = PureKotlinXlsxExporter()
    private val csvExporter = FinancialCsvExporter()

    val state: StateFlow<ExportReportUiState> = combine(
        transactionRepository.observeAll(),
        walletRepository.observeAll(),
        _selection,
    ) { transactions, wallets, sel ->
        cachedTransactions = transactions
        cachedWallets = wallets
        val report = buildReport(transactions, wallets, sel)
        ExportReportUiState(
            datePreset = sel.datePreset,
            startDate = sel.startDate,
            endDate = sel.endDate,
            availableWallets = wallets.filterNot { it.isArchived },
            selectedWalletIds = sel.selectedWalletIds,
            typeFilter = sel.typeFilter,
            format = sel.format,
            matchingCount = report.rows.size,
            isExporting = sel.isExporting,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExportReportUiState(startDate = initialRange.first, endDate = initialRange.second),
    )

    fun selectPreset(preset: ExportDatePreset) {
        if (preset == ExportDatePreset.CUSTOM) {
            _selection.update { it.copy(datePreset = preset) }
            return
        }
        val (start, end) = presetRange(preset, todayLocal())
        _selection.update { it.copy(datePreset = preset, startDate = start, endDate = end) }
    }

    fun setCustomStartDate(date: LocalDate) {
        _selection.update { sel ->
            val adjustedEnd = if (date > sel.endDate) date else sel.endDate
            sel.copy(datePreset = ExportDatePreset.CUSTOM, startDate = date, endDate = adjustedEnd)
        }
    }

    fun setCustomEndDate(date: LocalDate) {
        _selection.update { sel ->
            val adjustedStart = if (date < sel.startDate) date else sel.startDate
            sel.copy(datePreset = ExportDatePreset.CUSTOM, startDate = adjustedStart, endDate = date)
        }
    }

    fun toggleWallet(walletId: Long?) {
        if (walletId == null) {
            _selection.update { it.copy(selectedWalletIds = emptySet()) }
            return
        }
        _selection.update { sel ->
            val updated = if (walletId in sel.selectedWalletIds) {
                sel.selectedWalletIds - walletId
            } else {
                sel.selectedWalletIds + walletId
            }
            sel.copy(selectedWalletIds = updated)
        }
    }

    fun selectTypeFilter(typeFilter: ExportTypeFilter) {
        _selection.update { it.copy(typeFilter = typeFilter) }
    }

    fun selectFormat(format: ExportFormat) {
        _selection.update { it.copy(format = format) }
    }

    suspend fun writeToUri(uri: Uri): Boolean = withContext(ioDispatcher) {
        _selection.update { it.copy(isExporting = true) }
        try {
            val bytes = generateReportBytes()
            val out = context.contentResolver.openOutputStream(uri) ?: return@withContext false
            out.use { it.write(bytes) }
            true
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            false
        } finally {
            _selection.update { it.copy(isExporting = false) }
        }
    }

    suspend fun createShareUri(): Uri? = withContext(ioDispatcher) {
        _selection.update { it.copy(isExporting = true) }
        try {
            val currentState = state.value
            val bytes = generateReportBytes()
            val dir = File(context.cacheDir, "exported_reports").apply { mkdirs() }
            val file = File(dir, currentState.suggestedFileName())
            file.writeBytes(bytes)
            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            null
        } finally {
            _selection.update { it.copy(isExporting = false) }
        }
    }

    private fun generateReportBytes(): ByteArray {
        val sel = _selection.value
        val report = buildReport(cachedTransactions, cachedWallets, sel)
        return when (sel.format) {
            ExportFormat.XLSX -> xlsxExporter.toByteArray(report)
            ExportFormat.CSV -> csvExporter.toUtf8Bytes(
                report = report,
                incomeLabel = context.getString(R.string.transaction_type_income),
                expenseLabel = context.getString(R.string.transaction_type_expense),
            )
        }
    }

    private fun buildReport(
        transactions: List<Transaction>,
        wallets: List<Wallet>,
        sel: ExportFilterSelection,
    ): FinancialReportData = reportBuilder.build(
        transactions = transactions,
        wallets = wallets,
        filter = FinancialReportFilter(
            startDate = sel.startDate,
            endDate = sel.endDate,
            walletIds = sel.selectedWalletIds,
            typeFilter = sel.typeFilter,
        ),
        zone = zone,
    )

    private fun todayLocal(): LocalDate = Clock.System.now().toLocalDateTime(zone).date

    private fun presetRange(preset: ExportDatePreset, refToday: LocalDate): Pair<LocalDate, LocalDate> {
        return when (preset) {
            ExportDatePreset.THIS_MONTH -> {
                val start = LocalDate(refToday.year, refToday.month.number, 1)
                val end = start.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
                start to end
            }
            ExportDatePreset.THIS_QUARTER -> {
                val qStartMonth = ((refToday.month.number - 1) / 3) * 3 + 1
                val start = LocalDate(refToday.year, qStartMonth, 1)
                val end = start.plus(DatePeriod(months = 3)).minus(DatePeriod(days = 1))
                start to end
            }
            ExportDatePreset.THIS_YEAR -> {
                LocalDate(refToday.year, 1, 1) to LocalDate(refToday.year, 12, 31)
            }
            ExportDatePreset.CUSTOM -> {
                refToday.minus(DatePeriod(days = 30)) to refToday
            }
        }
    }
}
