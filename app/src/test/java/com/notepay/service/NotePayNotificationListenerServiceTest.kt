package com.notepay.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.Money
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.repository.TransactionRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@org.robolectric.annotation.Config(sdk = [34])
class NotePayNotificationListenerServiceTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    
    private val walletRepository = mockk<WalletRepository>(relaxed = true)
    private val transactionRepository = mockk<TransactionRepository>(relaxed = true)
    private lateinit var addTransaction: AddTransactionUseCase
    private lateinit var service: NotePayNotificationListenerService

    private val activeWallet = Wallet(
        id = 2L,
        name = "Ví ATM",
        initialBalance = Money.ZERO,
        iconKey = "bank",
        colorKey = "secondary",
        isActive = true
    )

    @Before
    fun setUp() {
        addTransaction = AddTransactionUseCase(
            transactionRepository,
            walletRepository,
            testDispatcher
        )
        service = org.robolectric.Robolectric.buildService(NotePayNotificationListenerService::class.java).create().get()
        service.walletRepository = walletRepository
        service.addTransaction = addTransaction
        service.ioDispatcher = testDispatcher
        
        every { walletRepository.observeActive() } returns flowOf(activeWallet)
    }

    @Test
    fun `onNotificationPosted parses TPBank notification and inserts transaction`() = runTest {
        val extras = Bundle().apply {
            putString("android.title", "TPBank")
            putString("android.text", """
                (TPBank): 14/06/26;05:51
                TK: xxxx1234
                PS: -50,000VND
                SD: 2,450,000VND
                SD KHA DUNG: 2,450,000VND
                ND: Mua ca phe chieu
                SO GD: 987654322
                05:51
            """.trimIndent())
        }
        val notification = Notification().apply {
            this.extras = extras
        }
        val sbn = StatusBarNotification(
            "com.tpbank",
            "com.tpbank",
            1,
            "tag",
            1000,
            1000,
            0, // score
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        
        coEvery { walletRepository.getById(activeWallet.id) } returns activeWallet
        val capturedTransactions = mutableListOf<Transaction>()
        coEvery { transactionRepository.upsert(capture(capturedTransactions)) } returns 1L

        // Trigger notification post
        service.onNotificationPosted(sbn)

        // Verify transaction added
        coVerify(exactly = 1) { transactionRepository.upsert(any()) }
        assertThat(capturedTransactions).hasSize(1)
        
        val tx = capturedTransactions.first()
        assertThat(tx.amount).isEqualTo(Money(50_000_00)) // 50,000 đ
        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.category).isEqualTo(Category.DEFAULT_EXPENSE)
        assertThat(tx.note).isEqualTo("Mua ca phe chieu")
        assertThat(tx.walletId).isEqualTo(activeWallet.id)
    }

    @Test
    fun `onNotificationPosted parses real TPBank screenshot notification`() = runTest {
        val extras = Bundle().apply {
            putString("android.title", "TPBank Mobile")
            putString("android.text", """
                (TPBank): 14/06/26;06:25
                TK: xxxx5539020
                PS:-30.000VND
                SD: 410.054VND
                SD KHA DUNG: 410.054VND
                ND: NAP TIEN VI MOMO - 0945553902
                - 133366724699
                SO GD: 661TTMB261662918
            """.trimIndent())
        }
        val notification = Notification().apply {
            this.extras = extras
        }
        val sbn = StatusBarNotification(
            "com.tpbank",
            "com.tpbank",
            3,
            "tag",
            1000,
            1000,
            0,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        
        coEvery { walletRepository.getById(activeWallet.id) } returns activeWallet
        val capturedTransactions = mutableListOf<Transaction>()
        coEvery { transactionRepository.upsert(capture(capturedTransactions)) } returns 1L

        service.onNotificationPosted(sbn)

        coVerify(exactly = 1) { transactionRepository.upsert(any()) }
        assertThat(capturedTransactions).hasSize(1)
        
        val tx = capturedTransactions.first()
        assertThat(tx.amount).isEqualTo(Money(30_000_00)) // 30.000 đ
        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.note).isEqualTo("NAP TIEN VI MOMO - 0945553902")
        assertThat(tx.walletId).isEqualTo(activeWallet.id)
    }

    @Test
    fun `onNotificationPosted parses InboxStyle notification using textLines`() = runTest {
        val extras = Bundle().apply {
            putString("android.title", "TPBank Mobile")
            putString("android.text", "(TPBank): 14/06/26;06:25...")
            putCharSequenceArray("android.textLines", arrayOf(
                "(TPBank): 14/06/26;06:25",
                "TK: xxxx5539020",
                "PS:-30.000VND",
                "SD: 410.054VND",
                "SD KHA DUNG: 410.054VND",
                "ND: NAP TIEN VI MOMO - 0945553902",
                "- 133366724699",
                "SO GD: 661TTMB261662918"
            ))
        }
        val notification = Notification().apply {
            this.extras = extras
        }
        val sbn = StatusBarNotification(
            "com.tpbank",
            "com.tpbank",
            4,
            "tag",
            1000,
            1000,
            0,
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )
        
        coEvery { walletRepository.getById(activeWallet.id) } returns activeWallet
        val capturedTransactions = mutableListOf<Transaction>()
        coEvery { transactionRepository.upsert(capture(capturedTransactions)) } returns 1L

        service.onNotificationPosted(sbn)

        coVerify(exactly = 1) { transactionRepository.upsert(any()) }
        assertThat(capturedTransactions).hasSize(1)
        
        val tx = capturedTransactions.first()
        assertThat(tx.amount).isEqualTo(Money(30_000_00)) // 30.000 đ
        assertThat(tx.type).isEqualTo(TransactionType.EXPENSE)
        assertThat(tx.note).isEqualTo("NAP TIEN VI MOMO - 0945553902")
    }

    @Test
    fun `onNotificationPosted ignores non-transaction notification`() = runTest {
        val extras = Bundle().apply {
            putString("android.title", "Momo")
            putString("android.text", "Chúc bạn một ngày mới vui vẻ!")
        }
        val notification = Notification().apply {
            this.extras = extras
        }
        val sbn = StatusBarNotification(
            "com.momo",
            "com.momo",
            2,
            "tag",
            1000,
            1000,
            0, // score
            notification,
            android.os.Process.myUserHandle(),
            System.currentTimeMillis()
        )

        service.onNotificationPosted(sbn)

        coVerify(exactly = 0) { transactionRepository.upsert(any()) }
    }
}
