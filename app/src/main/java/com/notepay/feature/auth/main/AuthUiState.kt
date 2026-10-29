package com.notepay.ui.feature.auth

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.AuthUser

/**
 * Trạng thái giao diện người dùng cho tính năng Authentication.
 */
@Immutable
data class AuthUiState(
    val user: AuthUser? = null,
    val isLoading: Boolean = false,
    val isSigningOut: Boolean = false,
)
