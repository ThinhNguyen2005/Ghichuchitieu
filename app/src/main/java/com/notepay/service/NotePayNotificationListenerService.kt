package com.notepay.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.Transaction
import com.notepay.domain.notification.NotificationParser
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
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
    @IoDispatcher
    lateinit var ioDispatcher: CoroutineDispatcher

    private val job = SupervisorJob()
    private val serviceScope by lazy { CoroutineScope(job + ioDispatcher) }

    companion object {
        private const val CHANNEL_ID = "notepay_local_parse"
        private const val CHANNEL_NAME = "Tự động nhận diện chi tiêu"
        private const val NOTIFICATION_ID = 99
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

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

        android.util.Log.d("NotePayNotif", "--- NHẬN THÔNG BÁO ---")
        android.util.Log.d("NotePayNotif", "Package: ${sbn.packageName}")
        android.util.Log.d("NotePayNotif", "Title: $title")
        android.util.Log.d("NotePayNotif", "Text: $text")
        android.util.Log.d("NotePayNotif", "BigText: $bigText")
        android.util.Log.d("NotePayNotif", "TextLines: $textLinesStr")
        android.util.Log.d("NotePayNotif", "Final Text to Parse: $textToParse")

        val parsed = NotificationParser.parse(title, textToParse)
        if (parsed == null) {
            android.util.Log.d("NotePayNotif", "Không thể parse thông tin giao dịch từ thông báo này.")
            return
        }

        android.util.Log.d("NotePayNotif", "Parsed thành công: Số tiền = ${parsed.amount.amountInCents}, Loại = ${parsed.type}, Nội dung = ${parsed.note}")

        serviceScope.launch {
            // Tìm ví liên kết với package name của thông báo
            val wallets = walletRepository.observeAll().firstOrNull() ?: emptyList()
            val linkedWallet = wallets.find { it.linkedPackageName == sbn.packageName }
            val walletToUse = linkedWallet ?: walletRepository.observeActive().firstOrNull()
            
            if (walletToUse == null) {
                android.util.Log.d("NotePayNotif", "Lỗi: Không tìm thấy ví hoạt động hoặc ví liên kết.")
                showErrorNotification(
                    "Chưa chọn ví hoạt động",
                    "Hãy mở ứng dụng và kích hoạt ví của bạn để tự động ghi chép."
                )
                return@launch
            }

            android.util.Log.d("NotePayNotif", "Sử dụng ví: ${walletToUse.name} (ID: ${walletToUse.id})")

            // Kiểm tra xem đây có phải giao dịch nhận tiền khớp mã đối soát chia tiền không
            if (parsed.type == com.notepay.domain.model.TransactionType.INCOME) {
                val unpaidSplits = billSplitRepository.observeUnpaid().firstOrNull() ?: emptyList()
                
                // 1. Khớp mã đối soát đơn lẻ
                val matchingSplit = unpaidSplits.find { 
                    val cleanSplitMemo = removeVietnameseAccents(it.memoCode).uppercase(Locale.ROOT)
                    val cleanText = removeVietnameseAccents(textToParse.orEmpty()).uppercase(Locale.ROOT)
                    cleanText.contains(cleanSplitMemo)
                }
                
                if (matchingSplit != null) {
                    android.util.Log.d("NotePayNotif", "Khớp mã đối soát chia tiền đơn lẻ: ${matchingSplit.memoCode} cho ${matchingSplit.debtorName}")
                    
                    // 1. Đánh dấu đã trả
                    billSplitRepository.markAsPaid(matchingSplit.id, Clock.System.now())
                    
                    // 2. Lấy giao dịch gốc để làm nội dung ghi chú
                    val parentTx = transactionRepository.getById(matchingSplit.transactionId)
                    val parentNote = parentTx?.note ?: "khoản chia tiền"
                    
                    // 3. Tạo giao dịch thu nhập (INCOME) vào ví để cập nhật số dư
                    val transaction = Transaction(
                        id = 0L,
                        amount = matchingSplit.amount,
                        type = com.notepay.domain.model.TransactionType.INCOME,
                        category = com.notepay.domain.model.Category.DEFAULT_INCOME,
                        note = "${matchingSplit.debtorName} trả tiền: $parentNote",
                        occurredAt = Clock.System.now(),
                        walletId = walletToUse.id,
                        isAutoCapture = true,
                    )
                    
                    val result = addTransaction(transaction)
                    if (result.isSuccess) {
                        android.util.Log.d("NotePayNotif", "Lưu trả nợ thành công!")
                        showSuccessNotification(walletToUse.name, matchingSplit.amount.amountInCents, "${matchingSplit.debtorName} trả tiền: $parentNote")
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Lỗi SQLite/Domain"
                        android.util.Log.d("NotePayNotif", "Lưu trả nợ thất bại: $errorMsg")
                        showErrorNotification("Lỗi ghi nhận trả nợ", errorMsg)
                    }
                    return@launch
                } else {
                    // 2. Khớp mã đối soát gộp: NP <TÊN_NGƯỜI_NỢ>
                    val uniqueDebtors = unpaidSplits.map { it.debtorName }.distinct()
                    var matchedDebtor: String? = null
                    
                    for (debtor in uniqueDebtors) {
                        val sanitized = debtor.filter { it.isLetterOrDigit() || it.isWhitespace() }.trim().uppercase()
                        val combinedMemo = "NP $sanitized"
                        val cleanMemo = removeVietnameseAccents(combinedMemo).uppercase(Locale.ROOT)
                        val cleanText = removeVietnameseAccents(textToParse.orEmpty()).uppercase(Locale.ROOT)
                        if (cleanText.contains(cleanMemo)) {
                            matchedDebtor = debtor
                            break
                        }
                    }
                    
                    if (matchedDebtor != null) {
                        val debtorSplits = unpaidSplits.filter { it.debtorName == matchedDebtor }
                        android.util.Log.d("NotePayNotif", "Khớp mã đối soát chia tiền gộp cho debtor: $matchedDebtor, số khoản nợ = ${debtorSplits.size}")
                        
                        debtorSplits.forEach { split ->
                            billSplitRepository.markAsPaid(split.id, Clock.System.now())
                            
                            val parentTx = transactionRepository.getById(split.transactionId)
                            val parentNote = parentTx?.note ?: "khoản chia tiền"
                            
                            val transaction = Transaction(
                                id = 0L,
                                amount = split.amount,
                                type = com.notepay.domain.model.TransactionType.INCOME,
                                category = com.notepay.domain.model.Category.DEFAULT_INCOME,
                                note = "$matchedDebtor trả tiền: $parentNote",
                                occurredAt = Clock.System.now(),
                                walletId = walletToUse.id,
                                isAutoCapture = true,
                            )
                            addTransaction(transaction)
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
                category = if (parsed.type == com.notepay.domain.model.TransactionType.INCOME) {
                    com.notepay.domain.model.Category.DEFAULT_INCOME
                } else {
                    com.notepay.domain.model.Category.DEFAULT_EXPENSE
                },
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
        job.cancel()
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

    private fun removeVietnameseAccents(text: String): String {
        val map = mapOf(
            'a' to "aàáảãạăằắẳẵặâầấẩẫậ",
            'A' to "AÀÁẢÃẠĂẰẮẲẴẶÂẦẤẨẪẬ",
            'd' to "dđ",
            'D' to "DĐ",
            'e' to "eèéẻẽẹêềếểễệ",
            'E' to "EÈÉẺẼẸÊỀẾỂỄỆ",
            'i' to "iìíỉĩị",
            'I' to "IÌÍỈĨỊ",
            'o' to "oòóỏõọôồốổỗộơờớởỡợ",
            'O' to "OÒÓỎÕỌÔỒỐỔỖỘƠỜỚỞỠỢ",
            'u' to "uùúủũụưừứửữự",
            'U' to "UÙÚỦŨỤƯỪỨỬỮỰ",
            'y' to "yỳýỷỹỵ",
            'Y' to "YỲÝỶỸỴ"
        )
        var result = text
        for ((replaceChar, charList) in map) {
            for (c in charList) {
                result = result.replace(c, replaceChar)
            }
        }
        return result
    }
}
