package com.notepay.domain.model

import kotlin.time.Clock
import kotlinx.datetime.Instant

/**
 * Ví tài chính. Phase 1 chỉ cho phép 1 ví active duy nhất.
 *
 * iconKey/colorKey là string key — UI layer sẽ map sang resource.
 * Không hardcode icon class trong domain (giữ domain layer pure Kotlin).
 */
data class Wallet(
    val id: Long = 0L,
    val name: String,
    val initialBalance: Money,
    val iconKey: String,
    val colorKey: String,
    val isActive: Boolean = false,
    val budgetLimit: Money? = null,
    val linkedPackageName: String? = null,
    val bankBin: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null,
    val createdAt: Instant = Clock.System.now(),
) {
    init {
        require(name.isNotBlank()) { "Wallet name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) {
            "Wallet name too long: ${name.length} > $MAX_NAME_LENGTH"
        }
        require(iconKey.isNotBlank()) { "iconKey must not be blank" }
        require(colorKey.isNotBlank()) { "colorKey must not be blank" }
    }

    companion object {
        const val MAX_NAME_LENGTH = 50
        const val ICON_CASH = "cash"
        const val ICON_BANK = "bank"
        const val ICON_MOMO = "momo"
        const val ICON_CARD = "card"

        const val COLOR_PRIMARY = "primary"
        const val COLOR_SECONDARY = "secondary"
        const val COLOR_TERTIARY = "tertiary"

        /** Ví mặc định khi user mở app lần đầu. */
        fun default(): Wallet = Wallet(
            name = "Tiền mặt",
            initialBalance = Money.ZERO,
            iconKey = ICON_CASH,
            colorKey = COLOR_PRIMARY,
            isActive = true,
        )
    }
}
