package com.notepay.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.BillSplit
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.BillSplitRepository
import com.notepay.domain.repository.TransactionRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.time.Clock
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric

/**
 * Unit tests cho cơ chế đối soát tự động (Auto-Reconciliation) khi nhận thông báo ngân hàng
 * chứa mã chuyển khoản (memo_code) khớp với khoản chia tiền chưa thanh toán.
 *
 * Mô phỏng luồng: Nhận thông báo thu nhập → Tìm memoCode → Đánh dấu đã trả → Ghi INCOME.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class NotificationReconciliationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private val billSplitRepository = mockk<BillSplitRepository>(relaxed = true)
    private lateinit var addTransaction: AddTransactionUseCase
    private lateinit var service: NotePayNotificationListenerService

    // Ví mặc định liên kết với TPBank
    private val tpBankWallet = Wallet(
        id = 1L,
        name = "Ví TPBank",
        initialBalance = Money.ZERO,
        iconKey = "bank",
        colorKey = "primary",
        isActive = true,
        linkedPackageName = "com.tpbank",
        bankBin = "970423",
        accountNumber = "0945553902",
        accountName = "NGUYEN VAN A"
    )

    // Khoản chia tiền "Bạn A nợ 20.000đ" - chưa thanh toán
    private val unpaidSplitBanA = BillSplit(
        id = 10L,
        transactionId = 15L,
        debtorName = "Ban A",
        amount = Money(20_000_00L),
        isPaid = false,
        memoCode = "NP15 BAN A",
        createdAt = Clock.System.now()
    )

    // Giao dịch gốc của khoản chia tiền
    private val parentTransaction = Transaction(
        id = 15L,
        amount = Money(90_000_00L),
        type = TransactionType.EXPENSE,
        category = Category.DEFAULT_EXPENSE,
        note = "Bữa tối nhà hàng",
        occurredAt = Clock.System.now(),
        walletId = tpBankWallet.id
    )

    @Before
    fun setUp() {
        addTransaction = AddTransactionUseCase(
            transactionRepository,
            walletRepository,
            testDispatcher
        )
        service = Robolectric.buildService(NotePayNotificationListenerService::class.java)
            .create().get()
        service.walletRepository = walletRepository
        service.addTransaction = addTransaction
        service.billSplitRepository = billSplitRepository
        service.transactionRepository = transactionRepository
        service.ioDispatcher = testDispatcher

        every { walletRepository.observeActive() } returns flowOf(tpBankWallet)
        every { walletRepository.observeAll() } returns flowOf(listOf(tpBankWallet))
        coEvery { walletRepository.getById(tpBankWallet.id) } returns tpBankWallet
        coEvery { transactionRepository.upsert(any()) } returns 100L
    }

    // ─────────────────────── Reconciliation: Khớp mã ───────────────────────

    @Test
    fun `reconciliation - income notification matching memoCode marks split as paid`() = runTest {
        // Given: thông báo nhận tiền +20.000đ với nội dung "NP15 BAN A"
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(unpaidSplitBanA))
        coEvery { transactionRepository.getById(15L) } returns parentTransaction

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+20,000",
            memoContent = "NP15 BAN A"
        )

        service.onNotificationPosted(sbn)

        // Verify đánh dấu đã trả
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(unpaidSplitBanA.id, any()) }
    }

    @Test
    fun `reconciliation - income notification matching memoCode reduces parent transaction amount`() = runTest {
        // Given: thông báo nhận tiền +20.000đ với nội dung "NP15 BAN A"
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(unpaidSplitBanA))
        coEvery { transactionRepository.getById(15L) } returns parentTransaction

        val capturedTx = slot<Transaction>()
        coEvery { transactionRepository.upsert(capture(capturedTx)) } returns 100L

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+20,000",
            memoContent = "NP15 BAN A"
        )

        service.onNotificationPosted(sbn)

        // Xác minh giao dịch gốc được cập nhật với số tiền giảm trừ
        assertThat(capturedTx.captured.id).isEqualTo(parentTransaction.id)
        assertThat(capturedTx.captured.amount).isEqualTo(Money(70_000_00L)) // 90k - 20k = 70k
        val expectedNote = "Bữa tối nhà hàng (Ban A trả ${com.notepay.ui.util.MoneyFormatter.format(unpaidSplitBanA.amount)})"
        assertThat(capturedTx.captured.note).isEqualTo(expectedNote)
    }

    @Test
    fun `reconciliation - income notification with wrong memoCode does not mark as paid`() = runTest {
        // Given: thông báo có "NP15 BAN C" - không khớp "NP15 BAN A"
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(unpaidSplitBanA))

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+30,000",
            memoContent = "NP15 BAN C"  // khác memo code
        )

        service.onNotificationPosted(sbn)

        // Không được đánh dấu đã trả
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(any(), any()) }
    }

    @Test
    fun `reconciliation - expense notification does not trigger reconciliation`() = runTest {
        // Given: thông báo chi tiêu (PS: -20,000VND) không kích hoạt đối soát
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(unpaidSplitBanA))

        val sbn = buildExpenseNotification(
            packageName = "com.tpbank",
            amountStr = "-20,000",
            memoContent = "NP15 BAN A"  // cùng memo nhưng là giao dịch chi tiêu
        )

        service.onNotificationPosted(sbn)

        // Không được đánh dấu đã trả dù memo khớp (vì là giao dịch chi tiêu)
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(any(), any()) }
    }

    @Test
    fun `reconciliation - no unpaid splits means no reconciliation check`() = runTest {
        // Given: không có khoản nợ chưa thanh toán
        every { billSplitRepository.observeUnpaid() } returns flowOf(emptyList())

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+20,000",
            memoContent = "NP15 BAN A"
        )

        service.onNotificationPosted(sbn)

        // Không gọi markAsPaid
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(any(), any()) }
        // Nhưng giao dịch thông thường vẫn được ghi
        coVerify(exactly = 1) { transactionRepository.upsert(any()) }
    }

    @Test
    fun `reconciliation - multiple unpaid splits, only matching one is marked paid`() = runTest {
        // Given: 2 khoản nợ, chỉ 1 khớp
        val unpaidSplitBanB = BillSplit(
            id = 11L,
            transactionId = 15L,
            debtorName = "Ban B",
            amount = Money(30_000_00L),
            isPaid = false,
            memoCode = "NP15 BAN B",
            createdAt = Clock.System.now()
        )
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(unpaidSplitBanA, unpaidSplitBanB))
        coEvery { transactionRepository.getById(15L) } returns parentTransaction

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+30,000",
            memoContent = "NP15 BAN B"  // chỉ khớp Ban B
        )

        service.onNotificationPosted(sbn)

        // Chỉ Ban B được đánh dấu
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(unpaidSplitBanB.id, any()) }
        coVerify(exactly = 0) { billSplitRepository.markAsPaid(unpaidSplitBanA.id, any()) }
    }

    @Test
    fun `reconciliation - memoCode matching is case insensitive`() = runTest {
        // Thông báo có memo viết thường "np15 ban a" phải khớp với "NP15 BAN A" (uppercase)
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(unpaidSplitBanA))
        coEvery { transactionRepository.getById(15L) } returns parentTransaction
        coEvery { transactionRepository.upsert(any()) } returns 100L

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+20,000",
            memoContent = "np15 ban a"  // lowercase
        )

        service.onNotificationPosted(sbn)

        // Phải đánh dấu đã trả dù memo viết thường
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(unpaidSplitBanA.id, any()) }
    }

    @Test
    fun `reconciliation - income notification matching combined memoCode marks all splits of debtor as paid`() = runTest {
        // Given: 2 unpaid splits for the same debtor "Ban A"
        val split1 = unpaidSplitBanA.copy(id = 10L, amount = Money(20_000_00L))
        val split2 = unpaidSplitBanA.copy(id = 20L, amount = Money(30_000_00L))
        every { billSplitRepository.observeUnpaid() } returns flowOf(listOf(split1, split2))
        coEvery { transactionRepository.getById(any()) } returns parentTransaction

        val capturedTx = slot<Transaction>()
        coEvery { transactionRepository.upsert(capture(capturedTx)) } returns 100L

        val sbn = buildIncomeNotification(
            packageName = "com.tpbank",
            amountStr = "+50,000",
            memoContent = "NP BAN A" // combined memo
        )

        service.onNotificationPosted(sbn)

        // Verify both splits marked as paid
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(10L, any()) }
        coVerify(exactly = 1) { billSplitRepository.markAsPaid(20L, any()) }

        // Verify parent transaction amount was reduced
        assertThat(capturedTx.captured.id).isEqualTo(parentTransaction.id)
        assertThat(capturedTx.captured.amount).isEqualTo(Money(40_000_00L)) // 90k - 20k - 30k = 40k
        val expectedNote = "Bữa tối nhà hàng (Ban A trả ${com.notepay.ui.util.MoneyFormatter.format(split1.amount)}), Ban A trả ${com.notepay.ui.util.MoneyFormatter.format(split2.amount)}"
        assertThat(capturedTx.captured.note).isEqualTo(expectedNote)
    }


    // ─────────────────────── Linked wallet: Tìm ví theo package name ───────────────────────

    @Test
    fun `linked wallet - notification from linked package uses linked wallet`() = runTest {
        // Given: ví 1 liên kết TPBank, ví 2 là ví hoạt động chung
        val genericWallet = tpBankWallet.copy(
            id = 2L,
            name = "Ví Tiền mặt",
            isActive = true,
            linkedPackageName = null
        )
        every { walletRepository.observeAll() } returns flowOf(listOf(tpBankWallet, genericWallet))
        every { walletRepository.observeActive() } returns flowOf(genericWallet)
        every { billSplitRepository.observeUnpaid() } returns flowOf(emptyList())

        val capturedTx = slot<Transaction>()
        coEvery { transactionRepository.upsert(capture(capturedTx)) } returns 100L

        val sbn = buildExpenseNotification(
            packageName = "com.tpbank",  // thông báo từ TPBank
            amountStr = "-50,000",
            memoContent = "Mua hang"
        )

        service.onNotificationPosted(sbn)

        // Phải dùng ví TPBank (ID=1) chứ không phải ví mặc định (ID=2)
        assertThat(capturedTx.captured.walletId).isEqualTo(tpBankWallet.id)
    }

    // ─────────────────────── Helpers ───────────────────────

    private fun buildIncomeNotification(
        packageName: String,
        amountStr: String,
        memoContent: String
    ): StatusBarNotification {
        val body = """
            (TPBank): 14/06/26;10:00
            TK: xxxx1234
            PS: ${amountStr}VND
            SD: 1,000,000VND
            ND: $memoContent
            SO GD: 123456789
        """.trimIndent()

        return buildSbn(packageName, "TPBank Mobile", body)
    }

    private fun buildExpenseNotification(
        packageName: String,
        amountStr: String,
        memoContent: String
    ): StatusBarNotification {
        val body = """
            (TPBank): 14/06/26;10:00
            TK: xxxx1234
            PS: ${amountStr}VND
            SD: 1,000,000VND
            ND: $memoContent
            SO GD: 123456789
        """.trimIndent()

        return buildSbn(packageName, "TPBank Mobile", body)
    }

    private fun buildSbn(
        packageName: String,
        title: String,
        body: String
    ): StatusBarNotification {
        val extras = Bundle().apply {
            putString("android.title", title)
            putString("android.text", body)
        }
        val notification = Notification().apply {
            this.extras = extras
        }
        return StatusBarNotification(
            packageName,
            packageName,
            1,
            "tag",
            1000,
            1000,
            0,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
    }
}
