package com.notepay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.notepay.BuildConfig
import com.notepay.R
import com.notepay.data.preferences.AutoCaptureSettingsStore
import com.notepay.data.preferences.KnownBankApps
import com.notepay.data.preferences.BudgetSettingsStore
import com.notepay.data.preferences.LearnedCaptureDecision
import com.notepay.data.preferences.NotificationCaptureStore
import com.notepay.data.preferences.PendingBankNotification
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.notification.NotificationClassifier
import com.notepay.domain.notification.BankNotificationClassifier
import com.notepay.domain.notification.LearnedNotificationDecision
import com.notepay.domain.notification.NotificationDecision
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.util.StringUtils
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
        private const val CHANNEL_ID = "notepay_local_parse"
        private const val NOTIFICATION_ID = 99
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
        if (sbn == null) return

        val rawPackageName = sbn.packageName
        val isTpBankDebugSimulation = com.notepay.BuildConfig.DEBUG &&
            rawPackageName == applicationContext.packageName &&
            sbn.notification.channelId == "tpbank_simulation_channel"
        val packageName = when {
            isTpBankDebugSimulation -> KnownBankApps.TPBANK_PACKAGE
            rawPackageName == applicationContext.packageName -> return
            else -> rawPackageName
        }
        if (!isCaptureAllowed(packageName)) return

        // Chuyển toàn bộ các tác vụ xử lý chuỗi và tương tác DB xuống luồng ngầm ioDispatcher
        captureScope.launch {
            if (!isCaptureAllowed(packageName)) return@launch
            val extras = sbn.notification.extras
            val title = extras.getCharSequence("android.title")?.toString()
            val text = extras.getCharSequence("android.text")?.toString()
            val bigText = extras.getCharSequence("android.bigText")?.toString()
            
            // Trích xuất android.textLines (InboxStyle) thường dùng cho tin nhắn nhiều dòng của ngân hàng
            val textLines = extras.getCharSequenceArray("android.textLines")
            val textLinesStr = textLines?.joinToString("\n") { it.toString() }

            // Kết hợp tất cả để tìm chuỗi chứa thông tin đầy đủ nhất
            val textToParse = listOfNotNull(text, bigText, textLinesStr)
                .maxByOrNull { it.length }

            // Optimization B: Fast Path Keyword Check
            if (!isPotentiallyTransaction(textToParse)) {
                return@launch
            }

            val preliminary = BankNotificationClassifier.classify(packageName, title, textToParse)
            if (preliminary.decision == NotificationDecision.REJECTED) return@launch

            val learnedDecision = if (::notificationCaptureStore.isInitialized) {
                when (notificationCaptureStore.learnedDecision(preliminary.fingerprint.orEmpty())) {
                    LearnedCaptureDecision.APPROVED -> LearnedNotificationDecision.APPROVED
                    LearnedCaptureDecision.REJECTED -> LearnedNotificationDecision.REJECTED
                    LearnedCaptureDecision.NONE -> LearnedNotificationDecision.NONE
                }
            } else {
                LearnedNotificationDecision.NONE
            }
            val classification = if (learnedDecision == LearnedNotificationDecision.NONE) {
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
            if (classification.decision == NotificationDecision.REJECTED) return@launch

            val parsedResult = classification.parsed ?: return@launch
            if (!reserveNotificationKey(classification.dedupeKey.orEmpty())) return@launch
            val parsed = parsedResult.copy(sourcePackage = packageName)

            // Tìm ví liên kết với package name của thông báo
            val wallets = walletRepository.observeAll().firstOrNull() ?: emptyList()
            val linkedWallet = wallets.find { 
                KnownBankApps.getPrimaryPackageName(it.linkedPackageName.orEmpty()) == KnownBankApps.getPrimaryPackageName(packageName)
            }
            var walletToUse = linkedWallet ?: walletRepository.observeActive().firstOrNull()
            
            if (walletToUse == null) {
                walletToUse = wallets.firstOrNull()
            }
            
            if (walletToUse == null) {
                debugLog("Database trống ví, tiến hành tự động tạo ví mặc định.")
                val defaultWallet = Wallet.default()
                val newId = walletRepository.upsert(defaultWallet)
                walletRepository.setActive(newId)
                walletToUse = defaultWallet.copy(id = newId)
            }

            debugLog("Using selected wallet")

            if (classification.decision == NotificationDecision.PENDING) {
                if (!::notificationCaptureStore.isInitialized) return@launch
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
                return@launch
            }

            val currentTime = System.currentTimeMillis()
            // Clean up old cached items
            pendingTransferCache.entries.removeIf { currentTime - it.value.timestamp > 60000 }

            // Check Case 7 (Internal Transfer detection)
            var isMatchedTransfer = false
            var matchedTransferTxId: Long? = null
            var matchedTransferPending: PendingTransfer? = null

            val matchingKey = pendingTransferCache.keys.firstOrNull { key ->
                val p = pendingTransferCache[key]!!
                p.amountCents == parsed.amount.amountInCents &&
                p.type != parsed.type &&
                p.packageName != parsed.sourcePackage &&
                (currentTime - p.timestamp) < 60000
            }

            if (matchingKey != null) {
                matchedTransferPending = pendingTransferCache.remove(matchingKey)
                isMatchedTransfer = true
                matchedTransferTxId = matchingKey
            }

            if (isMatchedTransfer && matchedTransferTxId != null && matchedTransferPending != null) {
                // Save current transaction as internal transfer
                val currentTx = Transaction(
                    id = 0L,
                    amount = parsed.amount,
                    type = parsed.type,
                    category = suggestCategoryUseCase.suggest(parsed.note, parsed.type == TransactionType.INCOME),
                    note = parsed.note,
                    occurredAt = Clock.System.now(),
                    walletId = walletToUse.id,
                    isAutoCapture = true,
                    isInternalTransfer = true
                )
                val currentTxId = addTransaction(currentTx).getOrNull() ?: 0L

                // Update previous transaction to be internal transfer
                val prevTx = transactionRepository.getById(matchedTransferTxId)
                if (prevTx != null) {
                    transactionRepository.upsert(prevTx.copy(isInternalTransfer = true))
                }

                debugLog("Phát hiện chuyển khoản nội bộ thành công!")
                
                val sourceWallet = wallets.find { 
                    KnownBankApps.getPrimaryPackageName(it.linkedPackageName.orEmpty()) == KnownBankApps.getPrimaryPackageName(matchedTransferPending.packageName)
                }
                val sourceWalletName = sourceWallet?.name ?: getString(R.string.notif_source_wallet_fallback)
                
                val fromWallet = if (parsed.type == TransactionType.EXPENSE) walletToUse.name else sourceWalletName
                val toWallet = if (parsed.type == TransactionType.INCOME) walletToUse.name else sourceWalletName

                showInternalTransferNotification(
                    amountCents = parsed.amount.amountInCents,
                    fromWalletName = fromWallet,
                    toWalletName = toWallet
                )
                return@launch
            }

            // Kiểm tra xem đây có phải giao dịch nhận tiền khớp mã đối soát chia tiền không (Case 3 / Case 4)
            if (parsed.type == TransactionType.INCOME) {
                val unpaidSplits = billSplitRepository.observeUnpaid().firstOrNull() ?: emptyList()
                
                // 1. Khớp mã đối soát đơn lẻ (Case 3)
                val matchingSplit = unpaidSplits.find { 
                    val cleanSplitMemo = StringUtils.removeVietnameseAccents(it.memoCode).uppercase(Locale.ROOT)
                    val cleanText = StringUtils.removeVietnameseAccents(textToParse.orEmpty()).uppercase(Locale.ROOT)
                    cleanText.contains(cleanSplitMemo)
                }
                
                if (matchingSplit != null) {
                    debugLog("Matched single debt repayment")
                    
                    val parentTx = transactionRepository.getById(matchingSplit.transactionId)
                    if (parentTx != null) {
                        val newAmountCents = (parentTx.amount.amountInCents - matchingSplit.amount.amountInCents).coerceAtLeast(0L)
                        val paidNote = "${matchingSplit.debtorName} trả ${com.notepay.ui.util.MoneyFormatter.format(matchingSplit.amount)}"
                        val newNote = if (parentTx.note.contains(" trả ")) {
                            "${parentTx.note}, $paidNote"
                        } else {
                            "${parentTx.note} ($paidNote)"
                        }.take(Transaction.MAX_NOTE_LENGTH)

                        val updatedParentTx = parentTx.copy(
                            amount = Money(newAmountCents),
                            note = newNote
                        )
                        val saveResult = try {
                            Result.success(transactionRepository.upsert(updatedParentTx))
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (error: Throwable) {
                            Result.failure(error)
                        }
                        if (saveResult.isSuccess) {
                            billSplitRepository.markAsPaid(matchingSplit.id, Clock.System.now())
                            val savedTxId = saveResult.getOrNull() ?: 0L
                            // Save to cache for Case 7 just in case
                            pendingTransferCache[savedTxId] = PendingTransfer(
                                amountCents = parsed.amount.amountInCents,
                                type = parsed.type,
                                timestamp = currentTime,
                                transactionId = savedTxId,
                                packageName = packageName
                            )

                            // Tính dư nợ còn lại
                            val currentUnpaidSplits = billSplitRepository.observeUnpaid().firstOrNull() ?: emptyList()
                            val debtorRemainingDebt = currentUnpaidSplits
                                .filter { it.debtorName == matchingSplit.debtorName }
                                .sumOf { it.amount.amountInCents }

                            showDebtRepaymentSingleNotification(
                                debtorName = matchingSplit.debtorName,
                                paidAmountCents = matchingSplit.amount.amountInCents,
                                remainingDebtCents = debtorRemainingDebt,
                                walletName = walletToUse.name
                            )
                        } else {
                            val errorMsg = saveResult.exceptionOrNull()?.message
                                ?: getString(R.string.error_sqlite_domain)
                            showErrorNotification(getString(R.string.error_record_debt_payment), errorMsg)
                        }
                    }
                    return@launch
                } else {
                    // 2. Khớp mã đối soát gộp: NP <TÊN_NGƯỜI_NỢ> (Case 4)
                    val uniqueDebtors = unpaidSplits.map { it.debtorName }.distinct()
                    var matchedDebtor: String? = null
                    
                    for (debtor in uniqueDebtors) {
                        val sanitized = debtor.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
                        val combinedMemo = "NP $sanitized"
                        val cleanMemo = StringUtils.removeVietnameseAccents(combinedMemo).uppercase(Locale.ROOT)
                        val cleanText = StringUtils.removeVietnameseAccents(textToParse.orEmpty()).uppercase(Locale.ROOT)
                        if (cleanText.contains(cleanMemo)) {
                            matchedDebtor = debtor
                            break
                        }
                    }
                    
                    if (matchedDebtor != null) {
                        val debtorSplits = unpaidSplits.filter { it.debtorName == matchedDebtor }
                        debugLog("Matched bulk debt repayment")
                        
                        val splitsByTx = debtorSplits.groupBy { it.transactionId }

                        splitsByTx.forEach { (transactionId, splits) ->
                            splits.forEach { split ->
                                billSplitRepository.markAsPaid(split.id, Clock.System.now())
                            }

                            val parentTx = transactionRepository.getById(transactionId)
                            if (parentTx != null) {
                                var currentAmountCents = parentTx.amount.amountInCents
                                var currentNote = parentTx.note

                                splits.forEach { split ->
                                    currentAmountCents = (currentAmountCents - split.amount.amountInCents).coerceAtLeast(0L)
                                    val paidNote = "$matchedDebtor trả ${com.notepay.ui.util.MoneyFormatter.format(split.amount)}"
                                    currentNote = if (currentNote.contains(" trả ")) {
                                        "$currentNote, $paidNote"
                                    } else {
                                        "$currentNote ($paidNote)"
                                    }.take(Transaction.MAX_NOTE_LENGTH)
                                }

                                val updatedParentTx = parentTx.copy(
                                    amount = Money(currentAmountCents),
                                    note = currentNote
                                )
                                transactionRepository.upsert(updatedParentTx)
                            }
                        }
                        
                        val totalCents = debtorSplits.sumOf { it.amount.amountInCents }
                        // Save to cache for Case 7 just in case
                        pendingTransferCache[currentTime] = PendingTransfer(
                            amountCents = parsed.amount.amountInCents,
                            type = parsed.type,
                            timestamp = currentTime,
                            transactionId = 0L,
                            packageName = packageName
                        )

                        showDebtRepaymentBulkNotification(
                            debtorName = matchedDebtor,
                            totalAmountCents = totalCents,
                            billCount = debtorSplits.size,
                            walletName = walletToUse.name
                        )
                        return@launch
                    }
                }
            }

            // Giao dịch thông thường (không phải trả nợ) (Case 1 / Case 2)
            val category = suggestCategoryUseCase.suggest(
                parsed.note,
                parsed.type == TransactionType.INCOME
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
                isInternalTransfer = false
            )

            if (!isCaptureAllowed(packageName)) return@launch
            val result = addTransaction(transaction)
            if (result.isSuccess) {
                val savedTxId = result.getOrNull() ?: 0L
                debugLog("Transaction saved")

                // Lưu vào cache cho khớp chuyển khoản nội bộ
                pendingTransferCache[savedTxId] = PendingTransfer(
                    amountCents = parsed.amount.amountInCents,
                    type = parsed.type,
                    timestamp = currentTime,
                    transactionId = savedTxId,
                    packageName = packageName
                )

                if (parsed.type == TransactionType.INCOME) {
                    // Case 2: Standard Income
                    showIncomeNotification(
                        walletName = walletToUse.name,
                        amountCents = parsed.amount.amountInCents,
                        note = parsed.note
                    )
                } else {
                    // Case 1: Standard Expense
                    val emoji = NotificationClassifier.getCategoryEmoji(category.id)
                    showExpenseNotification(
                        walletName = walletToUse.name,
                        amountCents = parsed.amount.amountInCents,
                        note = parsed.note,
                        categoryName = category.displayName,
                        categoryEmoji = emoji
                    )

                    // Case 5: Cảnh báo ngân sách
                    checkBudgetAlert(parsed.amount.amountInCents)

                    // Case 6: Phát hiện gói đăng ký định kỳ
                    checkSubscriptionDetection(transaction)
                }
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: getString(R.string.error_sqlite_domain)
                debugLog("Transaction save failed")
                showErrorNotification(getString(R.string.error_record_transaction), errorMsg)
            }
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

    private suspend fun checkBudgetAlert(addedAmountCents: Long) {
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

    private fun showPendingTransactionNotification(
        pending: PendingBankNotification,
        walletName: String,
    ) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(Money(pending.amountCents))
        val notificationId = pendingNotificationId(pending.id)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val confirmIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "com.notepay.ACTION_SAVE_TRANSACTION"
            putExtra("pending_id", pending.id)
            putExtra("notification_id", notificationId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val ignoreIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "com.notepay.ACTION_IGNORE_TRANSACTION"
            putExtra("pending_id", pending.id)
            putExtra("notification_id", notificationId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            this,
            notificationId + 1,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_transaction_pending_title))
            .setContentText(getString(R.string.notif_transaction_pending_content, amountFormat, walletName))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    getString(
                        R.string.notif_transaction_pending_big_text,
                        amountFormat,
                        walletName,
                        pending.note,
                    ),
                ),
            )
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                android.R.drawable.ic_menu_save,
                getString(R.string.notif_transaction_confirm_action),
                confirmPendingIntent,
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notif_transaction_ignore_action),
                ignorePendingIntent,
            )
            .build()

        manager.notify(notificationId, notification)
    }

    private fun pendingNotificationId(id: String): Int =
        NOTIFICATION_ID + 100 + (id.hashCode() and Int.MAX_VALUE) % 100_000

    private fun showExpenseNotification(
        walletName: String,
        amountCents: Long,
        note: String,
        categoryName: String,
        categoryEmoji: String
    ) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(Money(amountCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(getString(R.string.notif_expense_big_title))
            .bigText(
                getString(
                    R.string.notif_expense_big_text,
                    amountFormat,
                    walletName,
                    categoryEmoji,
                    categoryName,
                    note,
                ),
            )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_expense_title))
            .setContentText(getString(R.string.notif_expense_content, amountFormat, walletName, categoryEmoji, categoryName))
            .setStyle(bigTextStyle)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showIncomeNotification(
        walletName: String,
        amountCents: Long,
        note: String
    ) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(Money(amountCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(getString(R.string.notif_income_big_title))
            .bigText(
                getString(R.string.notif_income_big_text, amountFormat, walletName, note),
            )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_income_title))
            .setContentText(getString(R.string.notif_income_content, amountFormat, walletName))
            .setStyle(bigTextStyle)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showDebtRepaymentSingleNotification(
        debtorName: String,
        paidAmountCents: Long,
        remainingDebtCents: Long,
        walletName: String
    ) {
        val paidFormat = com.notepay.ui.util.MoneyFormatter.format(Money(paidAmountCents))
        val remainingFormat = com.notepay.ui.util.MoneyFormatter.format(Money(remainingDebtCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(getString(R.string.notif_debt_single_big_title))
            .bigText(
                getString(
                    R.string.notif_debt_single_big_text,
                    debtorName,
                    paidFormat,
                    walletName,
                    remainingFormat,
                ),
            )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_debt_single_title, debtorName))
            .setContentText(getString(R.string.notif_debt_single_content, paidFormat, remainingFormat))
            .setStyle(bigTextStyle)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 2, notification)
    }

    private fun showDebtRepaymentBulkNotification(
        debtorName: String,
        totalAmountCents: Long,
        billCount: Int,
        walletName: String
    ) {
        val totalFormat = com.notepay.ui.util.MoneyFormatter.format(Money(totalAmountCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(getString(R.string.notif_debt_bulk_big_title))
            .bigText(
                getString(
                    R.string.notif_debt_bulk_big_text,
                    debtorName,
                    totalFormat,
                    walletName,
                    billCount,
                ),
            )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_debt_bulk_title, debtorName))
            .setContentText(getString(R.string.notif_debt_bulk_content, totalFormat, billCount))
            .setStyle(bigTextStyle)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 3, notification)
    }

    private fun showBudgetAlertNotification(
        spentCents: Long,
        budgetCents: Long,
        percentUsed: Int,
        isLimitExceeded: Boolean
    ) {
        val spentFormat = com.notepay.ui.util.MoneyFormatter.format(Money(spentCents))
        val budgetFormat = com.notepay.ui.util.MoneyFormatter.format(Money(budgetCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val title = getString(
            if (isLimitExceeded) R.string.notif_budget_exceeded_title else R.string.notif_budget_warning_title,
        )
        val content = if (isLimitExceeded) {
            getString(R.string.notif_budget_exceeded_content, spentFormat, budgetFormat, percentUsed)
        } else {
            getString(R.string.notif_budget_warning_content, spentFormat, percentUsed, budgetFormat)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(title)
            .bigText(
                getString(R.string.notif_budget_advice).let { "$content\n$it" }
            )

        val notification = NotificationCompat.Builder(this, "notepay_budget_alert")
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(bigTextStyle)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 4, notification)
    }

    private fun showSubscriptionDetectedNotification(
        name: String,
        amountCents: Long
    ) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(Money(amountCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notifId = (amountCents + System.currentTimeMillis() / 1000).toInt()

        val addIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "com.notepay.ACTION_ADD_SUBSCRIPTION"
            putExtra("name", name)
            putExtra("amount_cents", amountCents)
            putExtra("notification_id", notifId)
        }
        val addPendingIntent = PendingIntent.getBroadcast(
            this,
            notifId,
            addIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val ignoreIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "com.notepay.ACTION_IGNORE_SUBSCRIPTION"
            putExtra("notification_id", notifId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            this,
            notifId + 1,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_subscription_detected_title))
            .setContentText(getString(R.string.notif_subscription_detected_content, name, amountFormat))
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                getString(R.string.notif_subscription_detected_big_text, name, amountFormat),
            ))
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_add, getString(R.string.notif_subscription_add_action), addPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notif_subscription_ignore_action), ignorePendingIntent)
            .build()

        manager.notify(notifId, notification)
    }

    private fun showInternalTransferNotification(
        amountCents: Long,
        fromWalletName: String,
        toWalletName: String
    ) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(Money(amountCents))
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .setBigContentTitle(getString(R.string.notif_transfer_big_title))
            .bigText(
                getString(
                    R.string.notif_transfer_big_text,
                    amountFormat,
                    fromWalletName,
                    toWalletName,
                    getString(R.string.notif_transfer_description),
                ),
            )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notif_transfer_title))
            .setContentText(getString(R.string.notif_transfer_content, fromWalletName, toWalletName, amountFormat))
            .setStyle(bigTextStyle)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 5, notification)
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notif_channel_auto_capture_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_channel_auto_capture_desc)
        }
        val budgetChannel = NotificationChannel(
            "notepay_budget_alert",
            getString(R.string.notif_channel_budget_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = getString(R.string.notif_channel_budget_desc)
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        manager.createNotificationChannel(budgetChannel)
    }

    private fun showErrorNotification(title: String, message: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(com.notepay.R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 1, notification)
    }
}

