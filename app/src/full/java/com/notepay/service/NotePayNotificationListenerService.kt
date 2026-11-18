package com.notepay.service

import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.content.Context
import com.notepay.BuildConfig
import com.notepay.R
import com.notepay.data.datastore.AutoCaptureSettingsStore
import com.notepay.data.datastore.KnownBankApps
import com.notepay.data.datastore.BudgetSettingsStore
import com.notepay.data.datastore.LearnedCaptureDecision
import com.notepay.data.datastore.NotificationCaptureStore
import com.notepay.data.datastore.PendingBankNotification
import com.notepay.di.IoDispatcher
import com.notepay.domain.money.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.notification.BankNotificationClassifier
import com.notepay.domain.notification.ClassifiedNotification
import com.notepay.domain.notification.LearnedNotificationDecision
import com.notepay.domain.notification.NotificationClassifier
import com.notepay.domain.notification.NotificationDecision
import com.notepay.domain.notification.ParsedNotification
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.core.designsystem.formatter.MoneyFormatter
import com.notepay.core.common.util.StringUtils
import com.notepay.core.designsystem.util.localizedName
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

@AndroidEntryPoint
class NotePayNotificationListenerService : NotificationListenerService() {

    @Inject
    lateinit var addTransaction: AddTransactionUseCase

    @Inject
    lateinit var walletRepository: WalletRepository

    @Inject
    lateinit var billSplitRepository: com.notepay.domain.repository.BillSplitRepository

    @Inject
    lateinit var transactionRepository: com.notepay.domain.repository.TransactionRepository

    @Inject
    lateinit var subscriptionRepository: com.notepay.domain.repository.SubscriptionRepository

    @Inject
    lateinit var suggestCategoryUseCase: SuggestCategoryUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var autoCaptureSettingsStore: AutoCaptureSettingsStore

    @Inject
    lateinit var budgetSettingsStore: BudgetSettingsStore

    @Inject
    lateinit var notificationCaptureStore: NotificationCaptureStore

    private val job = SupervisorJob()
    private val serviceScope: CoroutineScope
        get() = CoroutineScope(job + ioDispatcher)
    private val captureJob = SupervisorJob(job)
    private val captureScope: CoroutineScope
        get() = CoroutineScope(captureJob + ioDispatcher)

    // Fail closed until the first DataStore value is loaded.
    @Volatile internal var settingsLoaded = false
    @Volatile internal var enabledPackages: Set<String> = emptySet()
    @Volatile internal var autoCaptureEnabled = false
    @Volatile internal var monthlyBudgetCents = 0L

    // Pending Transfer cache for Internal Transfer leg matching (Case 7)
    private val pendingTransferCache = ConcurrentHashMap<Long, PendingTransfer>()
    private val processedNotificationKeys = ConcurrentHashMap<String, Long>()

    data class PendingTransfer(
        val amountCents: Long,
        val type: TransactionType,
        val timestamp: Long,
        val transactionId: Long,
        val packageName: String
    )

    companion object {
        internal const val CHANNEL_ID = "notepay_local_parse"
        internal const val NOTIFICATION_ID = 99
        private const val NOTIFICATION_DEDUP_WINDOW_MILLIS = 2 * 60 * 1000L

        @Volatile
        var isConnected = false

        fun heal(context: Context) {
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val isEnabled = flat != null && flat.contains(context.packageName)
            if (isEnabled && !isConnected) {
                if (BuildConfig.DEBUG) android.util.Log.d("NotePayNotif", "Listener rebind requested")
                val pm = context.packageManager
                val componentName = android.content.ComponentName(context, NotePayNotificationListenerService::class.java)
                pm.setComponentEnabledSetting(
                    componentName,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    componentName,
                    android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    android.content.pm.PackageManager.DONT_KILL_APP
                )
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()

        serviceScope.launch {
            autoCaptureSettingsStore.settings.collect { settings ->
                val supportedEnabledPackages =
                    KnownBankApps.normalizeSupportedPackages(settings.enabledPackages)
                val capturePolicyChanged = settingsLoaded &&
                    (autoCaptureEnabled != settings.autoCaptureEnabled ||
                        enabledPackages != supportedEnabledPackages)

                settingsLoaded = false
                if (capturePolicyChanged) {
                    captureJob.cancelChildren()
                    pendingTransferCache.clear()
                }
                autoCaptureEnabled = settings.autoCaptureEnabled
                enabledPackages = supportedEnabledPackages
                settingsLoaded = true
            }
        }

        serviceScope.launch {
            budgetSettingsStore.settings.collect { settings ->
                monthlyBudgetCents = settings.monthlyBudgetCents
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        debugLog("NotificationListenerService Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        debugLog("NotificationListenerService Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val packageName = resolveSourcePackage(sbn) ?: return
        if (!isCaptureAllowed(packageName)) return

        // Chuyển toàn bộ các tác vụ xử lý chuỗi và tương tác DB xuống luồng ngầm ioDispatcher
        captureScope.launch {
            if (!isCaptureAllowed(packageName)) return@launch
            val extras = sbn?.notification?.extras ?: return@launch
            val title = extras.getCharSequence("android.title")?.toString()
            val textToParse = extractNotificationBody(extras)
            if (!isPotentiallyTransaction(textToParse)) return@launch

            val classification = classifyNotification(packageName, title, textToParse)
            if (classification.decision == NotificationDecision.REJECTED) return@launch

            val parsedResult = classification.parsed ?: return@launch
            if (!reserveNotificationKey(classification.dedupeKey.orEmpty())) return@launch
            val parsed = parsedResult.copy(sourcePackage = packageName)

            val wallets = walletRepository.observeAll().firstOrNull() ?: emptyList()
            val walletToUse = resolveTargetWallet(packageName, wallets)
            debugLog("Using selected wallet")

            if (classification.decision == NotificationDecision.PENDING) {
                handlePendingCapture(classification, parsed, walletToUse)
                return@launch
            }

            val currentTime = System.currentTimeMillis()
            pendingTransferCache.entries.removeIf { currentTime - it.value.timestamp > 60000 }

            if (tryHandleMatchedTransfer(parsed, walletToUse, wallets, currentTime)) return@launch
            if (tryHandleDebtRepayment(parsed, textToParse, walletToUse, packageName, currentTime)) return@launch

            saveStandardAutoTransaction(parsed, walletToUse, packageName, currentTime)
        }
    }

    private fun resolveSourcePackage(sbn: StatusBarNotification?): String? {
        if (sbn == null) return null
        val rawPackageName = sbn.packageName
        val isTpBankDebugSimulation = BuildConfig.DEBUG &&
            rawPackageName == applicationContext.packageName &&
            sbn.notification.channelId == "tpbank_simulation_channel"
        return when {
            isTpBankDebugSimulation -> KnownBankApps.TPBANK_PACKAGE
            rawPackageName == applicationContext.packageName -> null
            else -> rawPackageName
        }
    }

    private fun extractNotificationBody(extras: Bundle): String? {
        val text = extras.getCharSequence("android.text")?.toString()
        val bigText = extras.getCharSequence("android.bigText")?.toString()
        // Trích xuất android.textLines (InboxStyle) thường dùng cho tin nhắn nhiều dòng của ngân hàng
        val textLinesStr = extras.getCharSequenceArray("android.textLines")
            ?.joinToString("\n") { it.toString() }
        return listOfNotNull(text, bigText, textLinesStr).maxByOrNull { it.length }
    }

    private suspend fun classifyNotification(
        packageName: String,
        title: String?,
        textToParse: String?,
    ): ClassifiedNotification {
        val preliminary = BankNotificationClassifier.classify(packageName, title, textToParse)
        if (preliminary.decision == NotificationDecision.REJECTED) return preliminary

        val learnedDecision = if (::notificationCaptureStore.isInitialized) {
            when (notificationCaptureStore.learnedDecision(preliminary.fingerprint.orEmpty())) {
                LearnedCaptureDecision.APPROVED -> LearnedNotificationDecision.APPROVED
                LearnedCaptureDecision.REJECTED -> LearnedNotificationDecision.REJECTED
                LearnedCaptureDecision.NONE -> LearnedNotificationDecision.NONE
            }
        } else {
            LearnedNotificationDecision.NONE
        }
        return if (learnedDecision == LearnedNotificationDecision.NONE) {
            preliminary
        } else {
            BankNotificationClassifier.classify(
                packageName = packageName,
                title = title,
                body = textToParse,
                learnedDecision = learnedDecision,
                learnedFingerprint = preliminary.fingerprint,
            )
        }
    }

    private suspend fun resolveTargetWallet(packageName: String, wallets: List<Wallet>): Wallet {
        val primaryPkg = KnownBankApps.getPrimaryPackageName(packageName)
        val linkedWallet = wallets.find {
            KnownBankApps.getPrimaryPackageName(it.linkedPackageName.orEmpty()) == primaryPkg
        }
        val existingWallet = linkedWallet ?: walletRepository.observeActive().firstOrNull() ?: wallets.firstOrNull()
        if (existingWallet != null) return existingWallet

        debugLog("Database trống ví, tiến hành tự động tạo ví mặc định.")
        val defaultWallet = Wallet.default()
        val newId = walletRepository.upsert(defaultWallet)
        walletRepository.setActive(newId)
        return defaultWallet.copy(id = newId)
    }

    private suspend fun handlePendingCapture(
        classification: ClassifiedNotification,
        parsed: ParsedNotification,
        walletToUse: Wallet,
    ) {
        if (!::notificationCaptureStore.isInitialized) return
        val pending = PendingBankNotification(
            id = UUID.randomUUID().toString(),
            fingerprint = classification.fingerprint.orEmpty(),
            dedupeKey = classification.dedupeKey.orEmpty(),
            amountCents = parsed.amount.amountInCents,
            type = parsed.type,
            note = NotificationCaptureStore.sanitizeNote(parsed.note),
            walletId = walletToUse.id,
            primaryPackageName = classification.primaryPackageName.orEmpty(),
            createdAtMillis = System.currentTimeMillis(),
        )
        if (notificationCaptureStore.savePending(pending)) {
            showPendingTransactionNotification(
                pending = pending,
                walletName = walletToUse.name,
            )
        }
    }

    private suspend fun tryHandleMatchedTransfer(
        parsed: ParsedNotification,
        walletToUse: Wallet,
        wallets: List<Wallet>,
        currentTime: Long,
    ): Boolean {
        val matchingKey = pendingTransferCache.entries.firstOrNull { (_, p) ->
            p.amountCents == parsed.amount.amountInCents &&
                p.type != parsed.type &&
                p.packageName != parsed.sourcePackage &&
                (currentTime - p.timestamp) < 60000
        }?.key ?: return false

        val matchedTransferPending = pendingTransferCache.remove(matchingKey) ?: return false
        val currentTx = Transaction(
            id = 0L,
            amount = parsed.amount,
            type = parsed.type,
            category = suggestCategoryUseCase.suggest(parsed.note, parsed.type == TransactionType.INCOME),
            note = parsed.note,
            occurredAt = Clock.System.now(),
            walletId = walletToUse.id,
            isAutoCapture = true,
            isInternalTransfer = true,
        )
        addTransaction(currentTx)

        val prevTx = transactionRepository.getById(matchingKey)
        if (prevTx != null) {
            transactionRepository.upsert(prevTx.copy(isInternalTransfer = true))
        }
        debugLog("Phát hiện chuyển khoản nội bộ thành công!")

        val matchedPrimary = KnownBankApps.getPrimaryPackageName(matchedTransferPending.packageName)
        val sourceWallet = wallets.find {
            KnownBankApps.getPrimaryPackageName(it.linkedPackageName.orEmpty()) == matchedPrimary
        }
        val sourceWalletName = sourceWallet?.name ?: getString(R.string.notif_source_wallet_fallback)
        val fromWallet = if (parsed.type == TransactionType.EXPENSE) walletToUse.name else sourceWalletName
        val toWallet = if (parsed.type == TransactionType.INCOME) walletToUse.name else sourceWalletName

        showInternalTransferNotification(
            amountCents = parsed.amount.amountInCents,
            fromWalletName = fromWallet,
            toWalletName = toWallet,
        )
        return true
    }

    private suspend fun tryHandleDebtRepayment(
        parsed: ParsedNotification,
        textToParse: String?,
        walletToUse: Wallet,
        packageName: String,
        currentTime: Long,
    ): Boolean {
        if (parsed.type != TransactionType.INCOME) return false
        val unpaidSplits = billSplitRepository.observeUnpaid().firstOrNull() ?: emptyList()
        val cleanText = StringUtils.removeVietnameseAccents(textToParse.orEmpty()).uppercase(Locale.ROOT)

        // 1. Khớp mã đối soát đơn lẻ (Case 3)
        val matchingSplit = unpaidSplits.find { split ->
            val cleanSplitMemo = StringUtils.removeVietnameseAccents(split.memoCode).uppercase(Locale.ROOT)
            cleanText.contains(cleanSplitMemo)
        }
        if (matchingSplit != null) {
            handleSingleDebtRepayment(matchingSplit, parsed, walletToUse, packageName, currentTime)
            return true
        }

        // 2. Khớp mã đối soát gộp: NP <TÊN_NGƯỜI_NỢ> (Case 4)
        val matchedDebtor = unpaidSplits
            .map { it.debtorName }
            .distinct()
            .firstOrNull { debtor ->
                val sanitized = debtor.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
                val cleanMemo = StringUtils.removeVietnameseAccents("NP $sanitized").uppercase(Locale.ROOT)
                cleanText.contains(cleanMemo)
            } ?: return false

        handleBulkDebtRepayment(
            matchedDebtor = matchedDebtor,
            unpaidSplits = unpaidSplits,
            parsed = parsed,
            walletToUse = walletToUse,
            packageName = packageName,
            currentTime = currentTime,
        )
        return true
    }

    private suspend fun handleSingleDebtRepayment(
        matchingSplit: com.notepay.domain.model.BillSplit,
        parsed: ParsedNotification,
        walletToUse: Wallet,
        packageName: String,
        currentTime: Long,
    ) {
        debugLog("Matched single debt repayment")
        val parentTx = transactionRepository.getById(matchingSplit.transactionId) ?: return
        val newAmountCents = (parentTx.amount.amountInCents - matchingSplit.amount.amountInCents).coerceAtLeast(0L)
        val newNote = appendPaidNote(parentTx.note, matchingSplit.debtorName, matchingSplit.amount)

        val saveResult = try {
            Result.success(transactionRepository.upsert(parentTx.copy(amount = Money(newAmountCents), note = newNote)))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
        if (saveResult.isSuccess) {
            billSplitRepository.markAsPaid(matchingSplit.id, Clock.System.now())
            val savedTxId = saveResult.getOrNull() ?: 0L
            pendingTransferCache[savedTxId] = PendingTransfer(
                amountCents = parsed.amount.amountInCents,
                type = parsed.type,
                timestamp = currentTime,
                transactionId = savedTxId,
                packageName = packageName,
            )
            val currentUnpaidSplits = billSplitRepository.observeUnpaid().firstOrNull() ?: emptyList()
            val debtorRemainingDebt = currentUnpaidSplits
                .filter { it.debtorName == matchingSplit.debtorName }
                .sumOf { it.amount.amountInCents }

            showDebtRepaymentSingleNotification(
                debtorName = matchingSplit.debtorName,
                paidAmountCents = matchingSplit.amount.amountInCents,
                remainingDebtCents = debtorRemainingDebt,
                walletName = walletToUse.name,
            )
        } else {
            val errorMsg = saveResult.exceptionOrNull()?.message
                ?: getString(R.string.error_sqlite_domain)
            showErrorNotification(getString(R.string.error_record_debt_payment), errorMsg)
        }
    }

    private suspend fun handleBulkDebtRepayment(
        matchedDebtor: String,
        unpaidSplits: List<com.notepay.domain.model.BillSplit>,
        parsed: ParsedNotification,
        walletToUse: Wallet,
        packageName: String,
        currentTime: Long,
    ) {
        val debtorSplits = unpaidSplits.filter { it.debtorName == matchedDebtor }
        debugLog("Matched bulk debt repayment")

        debtorSplits.groupBy { it.transactionId }.forEach { (transactionId, splits) ->
            splits.forEach { split ->
                billSplitRepository.markAsPaid(split.id, Clock.System.now())
            }
            val parentTx = transactionRepository.getById(transactionId)
            if (parentTx != null) {
                var currentAmountCents = parentTx.amount.amountInCents
                var currentNote = parentTx.note
                splits.forEach { split ->
                    currentAmountCents = (currentAmountCents - split.amount.amountInCents).coerceAtLeast(0L)
                    currentNote = appendPaidNote(currentNote, matchedDebtor, split.amount)
                }
                transactionRepository.upsert(parentTx.copy(amount = Money(currentAmountCents), note = currentNote))
            }
        }

        pendingTransferCache[currentTime] = PendingTransfer(
            amountCents = parsed.amount.amountInCents,
            type = parsed.type,
            timestamp = currentTime,
            transactionId = 0L,
            packageName = packageName,
        )
        showDebtRepaymentBulkNotification(
            debtorName = matchedDebtor,
            totalAmountCents = debtorSplits.sumOf { it.amount.amountInCents },
            billCount = debtorSplits.size,
            walletName = walletToUse.name,
        )
    }

    private fun appendPaidNote(existingNote: String, debtorName: String, amount: Money): String {
        val paidNote = "$debtorName trả ${MoneyFormatter.format(amount)}"
        return if (existingNote.contains(" trả ")) {
            "$existingNote, $paidNote"
        } else {
            "$existingNote ($paidNote)"
        }.take(Transaction.MAX_NOTE_LENGTH)
    }

    private suspend fun saveStandardAutoTransaction(
        parsed: ParsedNotification,
        walletToUse: Wallet,
        packageName: String,
        currentTime: Long,
    ) {
        val category = suggestCategoryUseCase.suggest(
            parsed.note,
            parsed.type == TransactionType.INCOME,
        )
        val transaction = Transaction(
            id = 0L,
            amount = parsed.amount,
            type = parsed.type,
            category = category,
            note = parsed.note,
            occurredAt = Clock.System.now(),
            walletId = walletToUse.id,
            isAutoCapture = true,
            isInternalTransfer = false,
        )

        if (!isCaptureAllowed(packageName)) return
        val result = addTransaction(transaction)
        if (result.isSuccess) {
            val savedTxId = result.getOrNull() ?: 0L
            debugLog("Transaction saved")
            pendingTransferCache[savedTxId] = PendingTransfer(
                amountCents = parsed.amount.amountInCents,
                type = parsed.type,
                timestamp = currentTime,
                transactionId = savedTxId,
                packageName = packageName,
            )

            if (parsed.type == TransactionType.INCOME) {
                showIncomeNotification(
                    walletName = walletToUse.name,
                    amountCents = parsed.amount.amountInCents,
                    note = parsed.note,
                )
            } else {
                val emoji = NotificationClassifier.getCategoryEmoji(category.id)
                showExpenseNotification(
                    walletName = walletToUse.name,
                    amountCents = parsed.amount.amountInCents,
                    note = parsed.note,
                    categoryName = category.localizedName(this@NotePayNotificationListenerService),
                    categoryEmoji = emoji,
                )
                checkBudgetAlert()
                checkSubscriptionDetection(transaction)
            }
        } else {
            val errorMsg = result.exceptionOrNull()?.message ?: getString(R.string.error_sqlite_domain)
            debugLog("Transaction save failed")
            showErrorNotification(getString(R.string.error_record_transaction), errorMsg)
        }
    }

    private fun reserveNotificationKey(notificationKey: String): Boolean {
        val now = System.currentTimeMillis()
        processedNotificationKeys.entries.removeIf { now - it.value > NOTIFICATION_DEDUP_WINDOW_MILLIS }
        return processedNotificationKeys.putIfAbsent(notificationKey, now) == null
    }
    private fun debugLog(message: String) {
        if (BuildConfig.DEBUG) android.util.Log.d("NotePayNotif", message)
    }

    private fun isCaptureAllowed(packageName: String): Boolean =
        settingsLoaded &&
            autoCaptureEnabled &&
            KnownBankApps.isSupported(packageName) &&
            packageName in enabledPackages

    override fun onDestroy() {
        super.onDestroy()
        isConnected = false
        job.cancel()
    }

    private fun isPotentiallyTransaction(text: String?): Boolean {
        if (text.isNullOrBlank()) return false
        val lower = text.lowercase(Locale.ROOT)
        return lower.contains("vnd") || 
               lower.contains("đ") || 
               lower.contains("giao dich") || 
               lower.contains("gd") || 
               lower.contains("ps:") ||
               lower.contains("chuyen") ||
               lower.contains("nhan") ||
               lower.contains("so du") ||
               lower.contains("+") ||
               lower.contains("-")
    }

    private suspend fun checkBudgetAlert() {
        if (monthlyBudgetCents <= 0L) return

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now().toLocalDateTime(zone)
        val transactions = transactionRepository.observeByMonth(now.year, now.month.number).firstOrNull() ?: emptyList()
        
        val totalSpentCents = transactions
            .filter { it.type == TransactionType.EXPENSE && !it.isInternalTransfer }
            .sumOf { it.amount.amountInCents }

        if (totalSpentCents >= monthlyBudgetCents) {
            val percentUsed = (totalSpentCents * 100 / monthlyBudgetCents).toInt()
            showBudgetAlertNotification(
                spentCents = totalSpentCents,
                budgetCents = monthlyBudgetCents,
                percentUsed = percentUsed,
                isLimitExceeded = true
            )
        } else if (totalSpentCents >= (monthlyBudgetCents * 80 / 100)) {
            val percentUsed = (totalSpentCents * 100 / monthlyBudgetCents).toInt()
            showBudgetAlertNotification(
                spentCents = totalSpentCents,
                budgetCents = monthlyBudgetCents,
                percentUsed = percentUsed,
                isLimitExceeded = false
            )
        }
    }

    private suspend fun checkSubscriptionDetection(transaction: Transaction) {
        val subscriptions = subscriptionRepository.observeAll().firstOrNull() ?: emptyList()
        
        val isAlreadySubscribed = subscriptions.any {
            it.name.contains(transaction.note, ignoreCase = true) || 
            transaction.note.contains(it.name, ignoreCase = true)
        }
        if (isAlreadySubscribed) return

        val zone = TimeZone.currentSystemDefault()
        val now = Clock.System.now()
        val fromInstant = now - 32.days
        val toInstant = now - 28.days

        val similarTxList = transactionRepository.findRecentSimilar(
            noteKeyword = transaction.note,
            fromMillis = fromInstant.toEpochMilliseconds(),
            toMillis = toInstant.toEpochMilliseconds()
        )

        val similarTx = similarTxList.firstOrNull {
            val diff = kotlin.math.abs(it.amount.amountInCents - transaction.amount.amountInCents)
            diff <= (transaction.amount.amountInCents * 10 / 100)
        }

        if (similarTx != null) {
            showSubscriptionDetectedNotification(
                name = transaction.note,
                amountCents = transaction.amount.amountInCents
            )
        }
    }
}

