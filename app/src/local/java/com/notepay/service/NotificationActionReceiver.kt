package com.notepay.service

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.notepay.R
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var addTransaction: AddTransactionUseCase

    @Inject
    lateinit var suggestCategoryUseCase: SuggestCategoryUseCase

    @Inject
    lateinit var subscriptionRepository: com.notepay.domain.repository.SubscriptionRepository

    private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /** Hiện Toast trên main thread và chờ xong, để không bị cắt bởi pendingResult.finish(). */
    private suspend fun toast(context: Context, message: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = intent.getIntExtra("notification_id", -1)

        when (intent.action) {
            "com.notepay.ACTION_SAVE_TRANSACTION" -> {
                val amountCents = intent.getLongExtra("amount_cents", 0L)
                val typeStr = intent.getStringExtra("type") ?: return
                val note = intent.getStringExtra("note") ?: ""
                val walletId = intent.getLongExtra("wallet_id", -1L)

                if (walletId == -1L || amountCents == 0L) return

                // valueOf() ném IllegalArgumentException nếu intent mang giá trị lạ; ném từ
                // onReceive sẽ làm crash app nên phải chặn tại đây.
                val type = runCatching { TransactionType.valueOf(typeStr) }.getOrNull() ?: return
                val category = suggestCategoryUseCase.suggest(note, type == TransactionType.INCOME)

                val transaction = Transaction(
                    id = 0L,
                    amount = Money(amountCents),
                    type = type,
                    category = category,
                    note = note,
                    occurredAt = Clock.System.now(),
                    walletId = walletId,
                    isAutoCapture = true
                )

                // goAsync() giữ process sống tới khi ghi xong. Không có nó, Android được phép
                // kill process ngay sau onReceive và giao dịch có thể không bao giờ vào DB.
                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        val result = runCatching { addTransaction(transaction) }.getOrNull()
                        if (result?.isSuccess == true) {
                            toast(context, context.getString(R.string.autocapture_save_success))
                        } else {
                            toast(context, context.getString(R.string.autocapture_save_error))
                        }
                        if (notificationId != -1) {
                            notificationManager.cancel(notificationId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "com.notepay.ACTION_IGNORE_TRANSACTION" -> {
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
            "com.notepay.ACTION_ADD_SUBSCRIPTION" -> {
                val name = intent.getStringExtra("name") ?: return
                val amountCents = intent.getLongExtra("amount_cents", 0L)
                if (name.isBlank() || amountCents <= 0L) return

                val subscription = com.notepay.domain.model.Subscription(
                    id = 0L,
                    name = name,
                    amount = Money(amountCents),
                    category = "subscription",
                    nextDueDate = Clock.System.now() + 30.days,
                    repeatMonths = 1,
                    remindDaysBefore = 1,
                    note = "Tự động phát hiện từ thông báo",
                    isActive = true
                )

                val pendingResult = goAsync()
                receiverScope.launch {
                    try {
                        val result = runCatching { subscriptionRepository.upsert(subscription) }
                        if (result.isSuccess) {
                            toast(context, context.getString(R.string.autocapture_subscription_success))
                        } else {
                            toast(context, context.getString(R.string.autocapture_subscription_error))
                        }
                        if (notificationId != -1) {
                            notificationManager.cancel(notificationId)
                        }
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
            "com.notepay.ACTION_IGNORE_SUBSCRIPTION" -> {
                if (notificationId != -1) {
                    notificationManager.cancel(notificationId)
                }
            }
        }
    }
}

