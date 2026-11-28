package com.notepay.feature.transaction.natural

import android.content.Context
import android.content.res.Resources
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.ai.HybridNaturalTransactionParser
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.domain.TestData
import com.notepay.domain.model.Category
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.money.Money
import com.notepay.domain.natural.NaturalFallbackNotice
import com.notepay.domain.natural.NaturalParseResult
import com.notepay.domain.natural.NaturalParseSource
import com.notepay.domain.natural.NaturalTransactionDraft
import com.notepay.domain.repository.CategoryRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.usecase.AddTransactionUseCase
import com.notepay.domain.usecase.SuggestCategoryUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NaturalEntryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun draft(
        id: String = "d1",
        amountVnd: Long = 45_000L,
        type: TransactionType = TransactionType.EXPENSE,
        category: Category = Category.FOOD,
        note: String = "An trua",
    ) = NaturalTransactionDraft(
        id = id,
        amountVnd = amountVnd,
        type = type,
        category = category,
        note = note,
        date = LocalDate(2026, 11, 26),
        confidence = 0.9f,
    )

    private fun createViewModel(
        wallets: List<Wallet> = listOf(TestData.wallet(id = 1L, name = "Vi chinh")),
        activeWallet: Wallet? = wallets.firstOrNull(),
        isAiConfigured: Boolean = true,
        parser: HybridNaturalTransactionParser = mockk(relaxed = true),
        addTransactionUseCase: AddTransactionUseCase = mockk(relaxed = true),
        suggestCategoryUseCase: SuggestCategoryUseCase = mockk(relaxed = true),
    ): NaturalEntryViewModel {
        val walletRepo = mockk<WalletRepository> {
            every { observeAll() } returns flowOf(wallets)
            every { observeActive() } returns flowOf(activeWallet)
        }
        val categoryRepo = mockk<CategoryRepository> {
            every { observeCategories() } returns flowOf(Category.getAll())
        }
        every { parser.isAiConfiguredFlow } returns flowOf(isAiConfigured)

        val mockResources = mockk<Resources>(relaxed = true) {
            every {
                getQuantityString(R.plurals.natural_entry_saved_batch, any(), *anyVararg())
            } returns "Da luu 2 giao dich"
        }
        val context = mockk<Context>(relaxed = true) {
            every { resources } returns mockResources
            every { getString(R.string.shared_wallet_viewer_cannot_write) } returns "Viewer cannot add"
            every { getString(R.string.transaction_validation_wallet) } returns "Select wallet"
            every { getString(R.string.natural_entry_partial_error) } returns "Partial failure"
            every { getString(R.string.feedback_transaction_pending_sync) } returns "Pending sync"
            every { getString(R.string.natural_entry_ai_fallback_warning) } returns "AI unavailable"
        }

        return NaturalEntryViewModel(
            addTransactionUseCase = addTransactionUseCase,
            walletRepository = walletRepo,
            categoryRepository = categoryRepo,
            suggestCategoryUseCase = suggestCategoryUseCase,
            hybridNaturalParser = parser,
            context = context,
            ioDispatcher = mainDispatcherRule.testDispatcher,
        )
    }

    @Test
    fun `onInputChanged parses multiple drafts via on-device parser`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        val d1 = draft(id = "d1", amountVnd = 45_000L, note = "An trua")
        val d2 = draft(id = "d2", amountVnd = 55_000L, note = "Tra sua")
        every { parser.parseOnDevice("an trua 45k tra sua 55k") } returns NaturalParseResult(
            drafts = listOf(d1, d2),
            confidence = 0.85f,
            needsAiFallback = false,
            source = NaturalParseSource.ON_DEVICE,
        )

        val vm = createViewModel(parser = parser)
        vm.onInputChanged("an trua 45k tra sua 55k")

        val state = vm.state.value
        assertThat(state.drafts).containsExactly(d1, d2).inOrder()
        assertThat(state.source).isEqualTo(NaturalParseSource.ON_DEVICE)
    }

    @Test
    fun `onAnalyzeWithAi updates drafts and source from AI parser`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        val initialDraft = draft(id = "d1", amountVnd = 40_000L, note = "Ca phe")
        val aiDraft = draft(id = "ai1", amountVnd = 65_000L, note = "Ca phe va banh")
        every { parser.parseOnDevice("ca phe va banh 65k") } returns NaturalParseResult(
            drafts = listOf(initialDraft),
            confidence = 0.4f,
            needsAiFallback = true,
            source = NaturalParseSource.ON_DEVICE,
        )
        coEvery { parser.parseWithAi("ca phe va banh 65k", any(), forceAi = true) } returns NaturalParseResult(
            drafts = listOf(aiDraft),
            confidence = 0.95f,
            needsAiFallback = false,
            source = NaturalParseSource.AI_FALLBACK,
        )

        val vm = createViewModel(parser = parser, isAiConfigured = true)
        vm.onInputChanged("ca phe va banh 65k")
        vm.onAnalyzeWithAi()

        val state = vm.state.value
        assertThat(state.drafts).containsExactly(aiDraft)
        assertThat(state.source).isEqualTo(NaturalParseSource.AI_FALLBACK)
        assertThat(state.isAiLoading).isFalse()
    }

    @Test
    fun `onSaveAllDrafts persists drafts to selected wallet and emits success`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        val d1 = draft(id = "d1", amountVnd = 45_000L, note = "An trua")
        val d2 = draft(id = "d2", amountVnd = 30_000L, note = "Gui xe")
        every { parser.parseOnDevice(any()) } returns NaturalParseResult(
            drafts = listOf(d1, d2),
            confidence = 0.9f,
            needsAiFallback = false,
        )
        val savedTransactions = mutableListOf<Transaction>()
        val addTransactionUseCase = mockk<AddTransactionUseCase> {
            coEvery { this@mockk.invoke(capture(savedTransactions)) } returns Result.success(1L)
        }
        val suggestCategoryUseCase = mockk<SuggestCategoryUseCase>(relaxed = true)
        val wallets = listOf(
            TestData.wallet(id = 1L, name = "Vi 1"),
            TestData.wallet(id = 2L, name = "Vi 2"),
        )

        val vm = createViewModel(
            wallets = wallets,
            activeWallet = wallets[0],
            parser = parser,
            addTransactionUseCase = addTransactionUseCase,
            suggestCategoryUseCase = suggestCategoryUseCase,
        )
        vm.onWalletSelected(2L)
        vm.onInputChanged("an trua 45k gui xe 30k")

        vm.feedback.test {
            vm.onSaveAllDrafts()

            val fb = awaitItem()
            assertThat(fb.type).isEqualTo(FeedbackType.Success)
            cancelAndIgnoreRemainingEvents()
        }

        assertThat(savedTransactions).hasSize(2)
        assertThat(savedTransactions.map { it.walletId }).containsExactly(2L, 2L)
        assertThat(savedTransactions.map { it.amount }).containsExactly(
            Money.fromMajorUnit(45_000L),
            Money.fromMajorUnit(30_000L),
        )
        coVerify(exactly = 2) { suggestCategoryUseCase.learn(any(), any(), any()) }
        assertThat(vm.state.value.rawInput).isEmpty()
        assertThat(vm.state.value.drafts).isEmpty()
    }

    @Test
    fun `onSaveAllDrafts rejects viewer shared wallet`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        every { parser.parseOnDevice(any()) } returns NaturalParseResult(
            drafts = listOf(draft()),
            confidence = 0.9f,
            needsAiFallback = false,
        )
        val viewerWallet = TestData.wallet(id = 9L, name = "Vi xem").copy(
            isShared = true,
            sharedRole = SharedWalletRole.VIEWER,
        )
        val addTransactionUseCase = mockk<AddTransactionUseCase>(relaxed = true)

        val vm = createViewModel(
            wallets = listOf(viewerWallet),
            activeWallet = viewerWallet,
            parser = parser,
            addTransactionUseCase = addTransactionUseCase,
        )
        vm.onInputChanged("an trua 45k")

        vm.feedback.test {
            vm.onSaveAllDrafts()
            val fb = awaitItem()
            assertThat(fb.type).isEqualTo(FeedbackType.Error)
            assertThat(fb.message).isEqualTo("Viewer cannot add")
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { addTransactionUseCase.invoke(any()) }
    }

    @Test
    fun `onUpdateDraft and onRemoveDraft mutate draft list cleanly`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        val d1 = draft(id = "d1", amountVnd = 45_000L, note = "An trua")
        val d2 = draft(id = "d2", amountVnd = 55_000L, note = "Tra sua")
        every { parser.parseOnDevice(any()) } returns NaturalParseResult(
            drafts = listOf(d1, d2),
            confidence = 0.9f,
            needsAiFallback = false,
        )

        val vm = createViewModel(parser = parser)
        vm.onInputChanged("test")

        val updatedD1 = d1.copy(amountVnd = 50_000L, note = "Com ga")
        vm.onUpdateDraft(updatedD1)
        assertThat(vm.state.value.drafts).containsExactly(updatedD1, d2).inOrder()

        vm.onRemoveDraft("d2")
        assertThat(vm.state.value.drafts).containsExactly(updatedD1)
    }

    @Test
    fun `voice partial transcripts do not duplicate base text across updates`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        every { parser.parseOnDevice(any()) } returns NaturalParseResult(
            drafts = listOf(draft()),
            confidence = 0.8f,
            needsAiFallback = false,
        )

        val vm = createViewModel(parser = parser)
        vm.onInputChanged("an trua 45k")
        vm.onVoiceSessionStarted()

        vm.onVoicePartialTranscript("tra")
        assertThat(vm.state.value.rawInput).isEqualTo("an trua 45k tra")

        vm.onVoicePartialTranscript("tra sua 55k")
        assertThat(vm.state.value.rawInput).isEqualTo("an trua 45k tra sua 55k")

        vm.onVoiceFinalTranscript("tra sua 55k")
        assertThat(vm.state.value.rawInput).isEqualTo("an trua 45k tra sua 55k")
    }

    @Test
    fun `voice final transcript resets session base so subsequent voice appends cleanly`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        every { parser.parseOnDevice(any()) } returns NaturalParseResult(
            drafts = emptyList(),
            confidence = 0f,
            needsAiFallback = false,
        )

        val vm = createViewModel(parser = parser)
        vm.onVoiceSessionStarted()
        vm.onVoiceFinalTranscript("an sang 30k")
        assertThat(vm.state.value.rawInput).isEqualTo("an sang 30k")

        vm.onVoiceSessionStarted()
        vm.onVoiceFinalTranscript("gui xe 10k")
        assertThat(vm.state.value.rawInput).isEqualTo("an sang 30k gui xe 10k")
    }

    @Test
    fun `onSaveAllDrafts emits Pending feedback when saving to shared wallet`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        every { parser.parseOnDevice(any()) } returns NaturalParseResult(
            drafts = listOf(draft()),
            confidence = 0.9f,
            needsAiFallback = false,
        )
        val sharedWallet = TestData.wallet(id = 5L, name = "Vi chung").copy(
            isShared = true,
            sharedRole = SharedWalletRole.EDITOR,
        )
        val addTransactionUseCase = mockk<AddTransactionUseCase> {
            coEvery { this@mockk.invoke(any()) } returns Result.success(10L)
        }
        val vm = createViewModel(
            wallets = listOf(sharedWallet),
            activeWallet = sharedWallet,
            parser = parser,
            addTransactionUseCase = addTransactionUseCase,
        )
        vm.onInputChanged("an trua 45k")

        vm.feedback.test {
            vm.onSaveAllDrafts()
            val fb = awaitItem()
            assertThat(fb.type).isEqualTo(FeedbackType.Pending)
            assertThat(fb.message).isEqualTo("Pending sync")
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onAnalyzeWithAi emits Warning feedback on AI unavailable fallback`() = runTest {
        val parser = mockk<HybridNaturalTransactionParser>(relaxed = true)
        every { parser.parseOnDevice("ca phe") } returns NaturalParseResult(
            drafts = emptyList(),
            confidence = 0.2f,
            needsAiFallback = true,
        )
        coEvery { parser.parseWithAi("ca phe", any(), forceAi = true) } returns NaturalParseResult(
            drafts = listOf(draft(note = "Ca phe")),
            confidence = 0.4f,
            needsAiFallback = false,
            source = NaturalParseSource.ON_DEVICE,
            fallbackNotice = NaturalFallbackNotice.AI_UNAVAILABLE,
        )
        val vm = createViewModel(parser = parser, isAiConfigured = true)
        vm.onInputChanged("ca phe")

        vm.feedback.test {
            vm.onAnalyzeWithAi()
            val fb = awaitItem()
            assertThat(fb.type).isEqualTo(FeedbackType.Warning)
            assertThat(fb.message).isEqualTo("AI unavailable")
            cancelAndIgnoreRemainingEvents()
        }
    }
}
