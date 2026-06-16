package com.notepay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.notepay.data.preferences.KnownBankApps
import com.notepay.data.preferences.NotificationSettingsStore
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.Wallet
import com.notepay.domain.notification.NotificationParser
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import com.notepay.util.StringUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.time.Clock
import java.util.Locale
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
    lateinit var suggestCategoryUseCase: SuggestCategoryUseCase

    @Inject
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    @Inject
    lateinit var notificationSettingsStore: NotificationSettingsStore

    private val job = SupervisorJob()
    private val serviceScope: CoroutineScope
        get() = CoroutineScope(job + ioDispatcher)

    // Memory cache for settings to avoid persistent reads for every notification.
    internal var enabledPackages = KnownBankApps.packages
    internal var autoCaptureEnabled = true

    companion object {
        private const val CHANNEL_ID = "notepay_local_parse"
        private const val CHANNEL_NAME = "Tự động nhận diện chi tiêu"
        private const val NOTIFICATION_ID = 99

        val KNOWN_PACKAGES = KnownBankApps.packages

        @Volatile
        var isConnected = false

        fun heal(context: Context) {
            val flat = android.provider.Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            val isEnabled = flat != null && flat.contains(context.packageName)
            if (isEnabled && !isConnected) {
                android.util.Log.d("NotePayNotif", "Service is enabled in settings but not connected. Toggling component to force rebind.")
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
            notificationSettingsStore.settings.collect { settings ->
                autoCaptureEnabled = settings.autoCaptureEnabled
                enabledPackages = settings.enabledPackages
            }
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        android.util.Log.d("NotePayNotif", "NotificationListenerService Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        android.util.Log.d("NotePayNotif", "NotificationListenerService Disconnected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = if (sbn.packageName.contains("com.notepay")) {
            "com.tpb.mb.gprsandroid"
        } else {
            sbn.packageName
        }
        if (!autoCaptureEnabled) return

        // Chỉ xử lý thông báo từ các package ngân hàng đã biết (whitelist)
        val isWhitelisted = packageName in KNOWN_PACKAGES || packageName in enabledPackages
        if (!isWhitelisted) return

        // Chuyển toàn bộ các tác vụ xử lý chuỗi và tương tác DB xuống luồng ngầm ioDispatcher
        serviceScope.launch(ioDispatcher) {
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

            // Output log only when it passes fast filter to optimize log resource
            android.util.Log.d("NotePayNotif", "--- NHẬN THÔNG BÁO ---")
            android.util.Log.d("NotePayNotif", "Package: $packageName")
            android.util.Log.d("NotePayNotif", "Title: $title")
            android.util.Log.d("NotePayNotif", "Text: $text")
            android.util.Log.d("NotePayNotif", "BigText: $bigText")
            android.util.Log.d("NotePayNotif", "TextLines: $textLinesStr")
            android.util.Log.d("NotePayNotif", "Final Text to Parse: $textToParse")

            val parsed = NotificationParser.parse(title, textToParse)
            if (parsed == null) {
                android.util.Log.d("NotePayNotif", "Không thể parse thông tin giao dịch từ thông báo này.")
                return@launch
            }

            android.util.Log.d("NotePayNotif", "Parsed thành công: Số tiền = ${parsed.amount.amountInCents}, Loại = ${parsed.type}, Nội dung = ${parsed.note}")

            // Tìm ví liên kết với package name của thông báo
            val wallets = walletRepository.observeAll().firstOrNull() ?: emptyList()
            val linkedWallet = wallets.find { 
                KnownBankApps.getPrimaryPackageName(it.linkedPackageName.orEmpty()) == KnownBankApps.getPrimaryPackageName(packageName)
            }
            var walletToUse = linkedWallet ?: walletRepository.observeActive().firstOrNull()
            
            if (walletToUse == null) {
                // Fallback 1: Lấy ví đầu tiên trong danh sách (nếu có)
                walletToUse = wallets.firstOrNull()
                if (walletToUse != null) {
                    android.util.Log.d("NotePayNotif", "Không tìm thấy ví hoạt động/liên kết, sử dụng ví đầu tiên tìm thấy: ${walletToUse.name}")
                }
            }
            
            if (walletToUse == null) {
                // Fallback 2: Nếu DB hoàn toàn trống ví, tự động tạo mới ví mặc định "Tiền mặt"
                android.util.Log.d("NotePayNotif", "Database trống ví, tiến hành tự động tạo ví mặc định.")
                val defaultWallet = Wallet.default()
                val newId = walletRepository.upsert(defaultWallet)
                walletRepository.setActive(newId)
                walletToUse = defaultWallet.copy(id = newId)
            }

            android.util.Log.d("NotePayNotif", "Sử dụng ví: ${walletToUse.name} (ID: ${walletToUse.id})")

            // Kiểm tra xem đây có phải giao dịch nhận tiền khớp mã đối soát chia tiền không
            if (parsed.type == com.notepay.domain.model.TransactionType.INCOME) {
                val unpaidSplits = billSplitRepository.observeUnpaid().firstOrNull() ?: emptyList()
                
                // 1. Khớp mã đối soát đơn lẻ
                val matchingSplit = unpaidSplits.find { 
                    val cleanSplitMemo = StringUtils.removeVietnameseAccents(it.memoCode).uppercase(Locale.ROOT)
                    val cleanText = StringUtils.removeVietnameseAccents(textToParse.orEmpty()).uppercase(Locale.ROOT)
                    cleanText.contains(cleanSplitMemo)
                }
                
                if (matchingSplit != null) {
                    android.util.Log.d("NotePayNotif", "Khớp mã đối soát chia tiền đơn lẻ: ${matchingSplit.memoCode} cho ${matchingSplit.debtorName}")
                    
                    // 1. Đánh dấu đã trả
                    billSplitRepository.markAsPaid(matchingSplit.id, Clock.System.now())
                    
                    // 2. Lấy giao dịch gốc và giảm trừ số tiền nợ
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
                        val result = runCatching { transactionRepository.upsert(updatedParentTx) }
                        if (result.isSuccess) {
                            android.util.Log.d("NotePayNotif", "Cập nhật giảm tiền giao dịch gốc thành công!")
                            showSuccessNotification(walletToUse.name, matchingSplit.amount.amountInCents, "${matchingSplit.debtorName} trả tiền: ${parentTx.note}")
                        } else {
                            val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi SQLite/Domain"
                            android.util.Log.d("NotePayNotif", "Cập nhật thất bại: $errorMsg")
                            showErrorNotification("Lỗi ghi nhận trả nợ", errorMsg)
                        }
                    } else {
                        android.util.Log.d("NotePayNotif", "Không tìm thấy giao dịch gốc để giảm trừ.")
                    }
                    return@launch
                } else {
                    // 2. Khớp mã đối soát gộp: NP <TÊN_NGƯỜI_NỢ>
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
                        android.util.Log.d("NotePayNotif", "Khớp mã đối soát chia tiền gộp cho debtor: $matchedDebtor, số khoản nợ = ${debtorSplits.size}")
                        
                        // Group splits by transactionId to avoid concurrent read/write race conditions
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
                        showSuccessNotification(walletToUse.name, totalCents, "$matchedDebtor trả nợ gộp (${debtorSplits.size} khoản)")
                        return@launch
                    }
                }
            }

            // Giao dịch thông thường (không phải trả nợ)
            val transaction = Transaction(
                id = 0L,
                amount = parsed.amount,
                type = parsed.type,
                category = suggestCategoryUseCase.suggest(
                    parsed.note,
                    parsed.type == com.notepay.domain.model.TransactionType.INCOME
                ),
                note = parsed.note,
                occurredAt = Clock.System.now(),
                walletId = walletToUse.id,
                isAutoCapture = true,
            )

            val result = addTransaction(transaction)
            if (result.isSuccess) {
                android.util.Log.d("NotePayNotif", "Lưu giao dịch thành công!")
                showSuccessNotification(walletToUse.name, parsed.amount.amountInCents, parsed.note)
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi SQLite/Domain"
                android.util.Log.d("NotePayNotif", "Lưu giao dịch thất bại: $errorMsg")
                showErrorNotification(
                    "Lỗi ghi nhận giao dịch",
                    errorMsg
                )
            }
        }
    }

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

    private fun showConfirmationNotification(walletName: String, transaction: Transaction) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(transaction.amount)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notifId = (transaction.amount.amountInCents + transaction.occurredAt.epochSeconds).toInt()
        
        val saveIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "com.notepay.ACTION_SAVE_TRANSACTION"
            putExtra("amount_cents", transaction.amount.amountInCents)
            putExtra("type", transaction.type.name)
            putExtra("note", transaction.note)
            putExtra("wallet_id", transaction.walletId)
            putExtra("notification_id", notifId)
        }
        
        val savePendingIntent = PendingIntent.getBroadcast(
            this,
            notifId,
            saveIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val ignoreIntent = Intent(this, NotificationActionReceiver::class.java).apply {
            action = "com.notepay.ACTION_IGNORE_TRANSACTION"
            putExtra("notification_id", notifId)
        }
        val ignorePendingIntent = PendingIntent.getBroadcast(
            this,
            notifId + 1,
            ignoreIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Phát hiện giao dịch mới 💸")
            .setContentText("Nhận diện ${parsedTypeLabel(transaction.type)} $amountFormat từ ví $walletName. Nhấp để lưu.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(
                "Ví: $walletName\nSố tiền: $amountFormat (${parsedTypeLabel(transaction.type)})\nNội dung: ${transaction.note}"
            ))
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_save, "Lưu", savePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Bỏ qua", ignorePendingIntent)
            .build()

        manager.notify(notifId, notification)
    }

    private fun parsedTypeLabel(type: com.notepay.domain.model.TransactionType): String = when (type) {
        com.notepay.domain.model.TransactionType.INCOME -> "Thu nhập"
        com.notepay.domain.model.TransactionType.EXPENSE -> "Chi tiêu"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kênh thông báo tự động ghi nhận giao dịch của NotePay"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun showSuccessNotification(walletName: String, amountCents: Long, note: String) {
        val amountFormat = com.notepay.ui.util.MoneyFormatter.format(com.notepay.domain.model.Money(amountCents))
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Đã tự động nhận diện giao dịch ✨")
            .setContentText("Ghi nhận $amountFormat vào ví \"$walletName\" ($note)")
            .setSmallIcon(android.R.drawable.stat_notify_chat) // Tạm thời dùng icon hệ thống
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(title: String, message: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .build()

        manager.notify(NOTIFICATION_ID + 1, notification)
    }
}
