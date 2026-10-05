package com.notepay.domain.model

import androidx.compose.runtime.Immutable

/**
 * Domain model đại diện cho người dùng đã xác thực.
 * Không phụ thuộc vào Firebase SDK (giữ Domain layer thuần túy).
 */
@Immutable
data class AuthUser(
    val uid: String,
    val email: String? = null,
    val displayName: String? = null,
    val photoUrl: String? = null
)
