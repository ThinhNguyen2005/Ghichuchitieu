package com.notepay.domain.notification

import com.google.common.truth.Truth.assertThat
import com.notepay.data.datastore.KnownBankApps
import com.notepay.data.datastore.LearnedCaptureDecision
import com.notepay.data.datastore.LearnedCapturePolicy
import com.notepay.data.datastore.NotificationCaptureStore
import com.notepay.domain.model.TransactionType
import com.notepay.domain.money.Money
import org.junit.Test

class BankNotificationClassifierTest {

    @Test
    fun `parses verified bank posted amounts as confirmed`() {
        data class Case(
            val pkg: String,
            val title: String,
            val body: String,
            val expectedAmount: Money,
            val expectedType: TransactionType,
        )
        listOf(
            Case(
                pkg = "com.vietcombank.digibank",
                title = "Vietcombank 1838 19/05/2021",
                body = """
                    TK: 001100433XXXX
                    Số tiền GD: +50,000 VND
                    Nội dung: MBVP48892 Hoan tien giao dich
                    Số dư cuối: 20,244,200 VND
                """.trimIndent(),
                expectedAmount = Money(50_000_00),
                expectedType = TransactionType.INCOME,
            ),
            Case(
                pkg = "com.mbmobile",
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
                expectedAmount = Money(100_000_00),
                expectedType = TransactionType.INCOME,
            ),
            Case(
                pkg = "com.vietinbank.ipay",
                title = "VietinBank",
                body = """
                    Thời gian: 31/05/2023 11:33
                    Tài khoản: 103874026589
                    Giao dịch: -5,000 VND
                    Số dư hiện tại: 1,057 VND
                    Nội dung: TRAN THI BINH TU chuyen tien; tai iPay
                """.trimIndent(),
                expectedAmount = Money(5_000_00),
                expectedType = TransactionType.EXPENSE,
            ),
        ).forEach { c ->
            val result = BankNotificationClassifier.classify(c.pkg, c.title, c.body)
            assertThat(result.decision).isEqualTo(NotificationDecision.CONFIRMED)
            assertThat(result.parsed?.amount).isEqualTo(c.expectedAmount)
            assertThat(result.parsed?.type).isEqualTo(c.expectedType)
        }
    }

    @Test
    fun `pending unverified banks and missing direction require approval matched by fingerprint`() {
        val genericBody = "TK: xxxx1234\nGD: +50,000 VND\nSD: 2,000,000 VND\nND: Chuyen khoan"
        listOf("com.bidv.smartbanking", "com.acb.dcb").forEach { packageName ->
            val initial = BankNotificationClassifier.classify(packageName, "Thông báo biến động số dư", genericBody)
            assertThat(initial.decision).isEqualTo(NotificationDecision.PENDING)
            assertThat(initial.parsed?.amount).isEqualTo(Money(50_000_00))

            val learned = BankNotificationClassifier.classify(
                packageName = packageName,
                title = "Thông báo biến động số dư",
                body = genericBody,
                learnedDecision = LearnedNotificationDecision.APPROVED,
                learnedFingerprint = initial.fingerprint,
            )
            assertThat(learned.decision).isEqualTo(NotificationDecision.CONFIRMED)
        }

        val noDirBody = "TK: xxxx1234\nSố tiền GD: 50,000 VND\nSố dư: 2,000,000 VND\nNội dung: Chuyển khoản"
        val noDirResult = BankNotificationClassifier.classify(KnownBankApps.TPBANK_PACKAGE, "TPBank", noDirBody)
        assertThat(noDirResult.decision).isEqualTo(NotificationDecision.PENDING)
        assertThat(noDirResult.parsed?.amount).isEqualTo(Money(50_000_00))

        val fingerprint = NotificationFingerprint.create(KnownBankApps.TPBANK_PACKAGE, noDirBody)
        assertThat(
            BankNotificationClassifier.classify(
                KnownBankApps.TPBANK_PACKAGE,
                "TPBank",
                noDirBody,
                learnedDecision = LearnedNotificationDecision.APPROVED,
                learnedFingerprint = fingerprint,
            ).decision,
        ).isEqualTo(NotificationDecision.CONFIRMED)
        assertThat(
            BankNotificationClassifier.classify(
                "com.mbmobile",
                "MBBank",
                noDirBody,
                learnedDecision = LearnedNotificationDecision.APPROVED,
                learnedFingerprint = fingerprint,
            ).decision,
        ).isEqualTo(NotificationDecision.PENDING)
    }

    @Test
    fun `rejects wallets promotions balance-only and fee-only notifications even with learned approval`() {
        listOf(
            Triple("com.mservice.momotransfer", "MoMo", "Bạn đã chuyển 111đ để có cơ hội nhận voucher abc"),
            Triple("com.mbmobile", "Ưu đãi MBBank", "Chuyển 111đ để có cơ hội nhận voucher và hoàn tiền đến 500.000đ"),
            Triple("com.mbmobile", "MBBank", "Số dư tài khoản: 1,234,567,890 VND"),
            Triple("com.mbmobile", "MBBank", "Phí dịch vụ: 5,000 VND"),
        ).forEach { (pkg, title, body) ->
            val result = BankNotificationClassifier.classify(pkg, title, body)
            assertThat(result.decision).isEqualTo(NotificationDecision.REJECTED)
        }

        val promoBody = "Chuyển 111đ để có cơ hội nhận voucher"
        val promoFingerprint = NotificationFingerprint.create("com.mbmobile", promoBody)
        assertThat(
            BankNotificationClassifier.classify(
                "com.mbmobile",
                "MBBank",
                promoBody,
                learnedDecision = LearnedNotificationDecision.APPROVED,
                learnedFingerprint = promoFingerprint,
            ).decision,
        ).isEqualTo(NotificationDecision.REJECTED)
    }

    @Test
    fun `dedupe key changes with amount while fingerprint ignores compact layout values`() {
        val first = "TK: 001100433XXXX\nSố tiền GD: +50,000 VND\nNội dung: MBVP48892"
        val second = "TK: 001100433XXXX\nSố tiền GD: +60,000 VND\nNội dung: MBVP48892"
        assertThat(NotificationFingerprint.create("com.VCB", first))
            .isEqualTo(NotificationFingerprint.create("com.VCB", second))
        assertThat(NotificationFingerprint.createDedupe("com.VCB", "VCB", first))
            .isNotEqualTo(NotificationFingerprint.createDedupe("com.VCB", "VCB", second))
        assertThat(NotificationCaptureStore.sanitizeNote("TK: 001100433XXXX 0941028291"))
            .doesNotContain("001100433")

        val layoutA = "TK 99xxx999 | GD: +1,000,000,000,000VND\n06/07/24 20:56 | SD: 1,234,567,890VND | ND:\nlum"
        val layoutB = "TK 11xxx111 | GD: +50,000VND\n07/08/25 21:01 | SD: 2,345,678VND | ND:\nhoa don dien"
        assertThat(NotificationFingerprint.create(KnownBankApps.MBBANK_PACKAGE, layoutA))
            .isEqualTo(NotificationFingerprint.create(KnownBankApps.MBBANK_PACKAGE, layoutB))
    }

    @Test
    fun `learning requires two clean approvals and never overrides a rejection`() {
        listOf(
            Triple(1, 0, LearnedCaptureDecision.NONE),
            Triple(2, 0, LearnedCaptureDecision.APPROVED),
            Triple(0, 1, LearnedCaptureDecision.REJECTED),
            Triple(2, 1, LearnedCaptureDecision.NONE),
        ).forEach { (approvals, rejections, expected) ->
            assertThat(LearnedCapturePolicy.decide(approvals, rejections)).isEqualTo(expected)
        }
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
        assertThat(normalized).containsAtLeast("com.bidv.smartbanking", "com.acb.dcb")
        assertThat(normalized).containsNoneOf("com.mservice.momo", "com.sacombank.ewallet")
    }
}
