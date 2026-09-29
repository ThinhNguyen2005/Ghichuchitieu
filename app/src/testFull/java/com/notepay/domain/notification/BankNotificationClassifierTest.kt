package com.notepay.domain.notification

import com.google.common.truth.Truth.assertThat
import com.notepay.data.preferences.NotificationCaptureStore
import com.notepay.data.preferences.LearnedCaptureDecision
import com.notepay.data.preferences.LearnedCapturePolicy
import com.notepay.data.preferences.KnownBankApps
import com.notepay.domain.model.Money
import com.notepay.domain.model.TransactionType
import org.junit.Test

class BankNotificationClassifierTest {

    @Test
    fun `parses Vietcombank posted amount as confirmed income`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.vietcombank.digibank",
            title = "Vietcombank 1838 19/05/2021",
            body = """
                TK: 001100433XXXX
                Số tiền GD: +50,000 VND
                Nội dung: MBVP48892 Hoan tien giao dich
                Số dư cuối: 20,244,200 VND
            """.trimIndent(),
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.CONFIRMED)
        assertThat(result.parsed?.amount).isEqualTo(Money(50_000_00))
        assertThat(result.parsed?.type).isEqualTo(TransactionType.INCOME)
    }

    @Test
    fun `parses MBBank posted amount as confirmed income`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.mbmobile",
            title = "MBBank",
            body = """
                Thông báo biến động số dư
                MB MASTER: xxxx
                Ngày GD: [2024-06-20 14:51:5
                Số tiền GD: +100,000 VND
                Phí: 0 VND
                TKTT: 0941028291
                Nội dung GD: Cam on quy khach da tra no the tin dung;
            """.trimIndent(),
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.CONFIRMED)
        assertThat(result.parsed?.amount).isEqualTo(Money(100_000_00))
        assertThat(result.parsed?.type).isEqualTo(TransactionType.INCOME)
    }

    @Test
    fun `parses VietinBank expense without treating balance as amount`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.vietinbank.ipay",
            title = "VietinBank",
            body = """
                Thời gian: 31/05/2023 11:33
                Tài khoản: 103874026589
                Giao dịch: -5,000 VND
                Số dư hiện tại: 1,057 VND
                Nội dung: TRAN THI BINH TU chuyen tien; tai iPay
            """.trimIndent(),
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.CONFIRMED)
        assertThat(result.parsed?.amount).isEqualTo(Money(5_000_00))
        assertThat(result.parsed?.type).isEqualTo(TransactionType.EXPENSE)
    }

    @Test
    fun `recognised unverified banks start pending and require learned approval`() {
        val body = """
            TK: xxxx1234
            GD: +50,000 VND
            SD: 2,000,000 VND
            ND: Chuyen khoan
        """.trimIndent()

        listOf("com.bidv.smartbanking", "com.acb.dcb").forEach { packageName ->
            val initial = BankNotificationClassifier.classify(
                packageName = packageName,
                title = "Thông báo biến động số dư",
                body = body,
            )

            assertThat(initial.decision).isEqualTo(NotificationDecision.PENDING)
            assertThat(initial.parsed?.amount).isEqualTo(Money(50_000_00))

            val learned = BankNotificationClassifier.classify(
                packageName = packageName,
                title = "Thông báo biến động số dư",
                body = body,
                learnedDecision = LearnedNotificationDecision.APPROVED,
                learnedFingerprint = initial.fingerprint,
            )

            assertThat(learned.decision).isEqualTo(NotificationDecision.CONFIRMED)
        }
    }

    @Test
    fun `missing direction is pending instead of automatic capture`() {
        val result = BankNotificationClassifier.classify(
            packageName = KnownBankApps.TPBANK_PACKAGE,
            title = "TPBank",
            body = "TK: xxxx1234\nSố tiền GD: 50,000 VND\nSố dư: 2,000,000 VND\nNội dung: Chuyển khoản",
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.PENDING)
        assertThat(result.parsed?.amount).isEqualTo(Money(50_000_00))
    }

    @Test
    fun `rejects wallet even when text looks like a transfer`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.mservice.momotransfer",
            title = "MoMo",
            body = "Bạn đã chuyển 111đ để có cơ hội nhận voucher abc",
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.REJECTED)
        assertThat(result.parsed).isNull()
    }

    @Test
    fun `rejects bank promotion containing an amount`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.mbmobile",
            title = "Ưu đãi MBBank",
            body = "Chuyển 111đ để có cơ hội nhận voucher và hoàn tiền đến 500.000đ",
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.REJECTED)
    }

    @Test
    fun `rejects balance only notification`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.mbmobile",
            title = "MBBank",
            body = "Số dư tài khoản: 1,234,567,890 VND",
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.REJECTED)
    }

    @Test
    fun `rejects fee only notification`() {
        val result = BankNotificationClassifier.classify(
            packageName = "com.mbmobile",
            title = "MBBank",
            body = "Phí dịch vụ: 5,000 VND",
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.REJECTED)
    }

    @Test
    fun `learned approval only applies to the same bank structure`() {
        val body = "TK: xxxx1234\nSố tiền GD: 50,000 VND\nSố dư: 2,000,000 VND\nNội dung: Chuyển khoản"
        val fingerprint = NotificationFingerprint.create(KnownBankApps.TPBANK_PACKAGE, body)

        val sameBank = BankNotificationClassifier.classify(
            packageName = KnownBankApps.TPBANK_PACKAGE,
            title = "TPBank",
            body = body,
            learnedDecision = LearnedNotificationDecision.APPROVED,
            learnedFingerprint = fingerprint,
        )
        val otherBank = BankNotificationClassifier.classify(
            packageName = "com.mbmobile",
            title = "MBBank",
            body = body,
            learnedDecision = LearnedNotificationDecision.APPROVED,
            learnedFingerprint = fingerprint,
        )

        assertThat(sameBank.decision).isEqualTo(NotificationDecision.CONFIRMED)
        assertThat(otherBank.decision).isEqualTo(NotificationDecision.PENDING)
    }

    @Test
    fun `hard promotion rejection wins over learned approval`() {
        val body = "Chuyển 111đ để có cơ hội nhận voucher"
        val fingerprint = NotificationFingerprint.create("com.mbmobile", body)
        val result = BankNotificationClassifier.classify(
            packageName = "com.mbmobile",
            title = "MBBank",
            body = body,
            learnedDecision = LearnedNotificationDecision.APPROVED,
            learnedFingerprint = fingerprint,
        )

        assertThat(result.decision).isEqualTo(NotificationDecision.REJECTED)
    }

    @Test
    fun `dedupe key changes with amount but fingerprint does not store raw account`() {
        val first = "TK: 001100433XXXX\nSố tiền GD: +50,000 VND\nNội dung: MBVP48892"
        val second = "TK: 001100433XXXX\nSố tiền GD: +60,000 VND\nNội dung: MBVP48892"

        assertThat(NotificationFingerprint.create("com.VCB", first))
            .isEqualTo(NotificationFingerprint.create("com.VCB", second))
        assertThat(NotificationFingerprint.createDedupe("com.VCB", "VCB", first))
            .isNotEqualTo(NotificationFingerprint.createDedupe("com.VCB", "VCB", second))
        assertThat(NotificationCaptureStore.sanitizeNote("TK: 001100433XXXX 0941028291"))
            .doesNotContain("001100433")
    }

    @Test
    fun `fingerprint ignores values in the same compact bank layout`() {
        val first = "TK 99xxx999 | GD: +1,000,000,000,000VND\n06/07/24 20:56 | SD: 1,234,567,890VND | ND:\nlum"
        val second = "TK 11xxx111 | GD: +50,000VND\n07/08/25 21:01 | SD: 2,345,678VND | ND:\nhoa don dien"

        assertThat(NotificationFingerprint.create(KnownBankApps.MBBANK_PACKAGE, first))
            .isEqualTo(NotificationFingerprint.create(KnownBankApps.MBBANK_PACKAGE, second))
    }

    @Test
    fun `learning requires two clean approvals and never overrides a rejection`() {
        assertThat(LearnedCapturePolicy.decide(1, 0)).isEqualTo(LearnedCaptureDecision.NONE)
        assertThat(LearnedCapturePolicy.decide(2, 0)).isEqualTo(LearnedCaptureDecision.APPROVED)
        assertThat(LearnedCapturePolicy.decide(0, 1)).isEqualTo(LearnedCaptureDecision.REJECTED)
        assertThat(LearnedCapturePolicy.decide(2, 1)).isEqualTo(LearnedCaptureDecision.NONE)
    }

    @Test
    fun `settings normalization keeps recognised bank aliases and drops wallets`() {
        val normalized = KnownBankApps.normalizeSupportedPackages(
            listOf(
                "com.bidv.smartbanking",
                "com.acb.dcb",
                "com.mservice.momo",
                "com.sacombank.ewallet",
            ),
        )

        assertThat(normalized).contains("com.bidv.smartbanking")
        assertThat(normalized).contains("com.acb.dcb")
        assertThat(normalized).doesNotContain("com.mservice.momo")
        assertThat(normalized).doesNotContain("com.sacombank.ewallet")
    }
}
