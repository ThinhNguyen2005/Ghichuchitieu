package com.notepay.domain.model

/**
 * Bản chất của tài khoản / ví tài chính.
 */
enum class AccountNature {
    ASSET,
    LIABILITY;

    val isAsset: Boolean get() = this == ASSET
    val isLiability: Boolean get() = this == LIABILITY
}
