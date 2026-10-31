package com.notepay.domain.repository

import com.notepay.domain.model.AuthUser
import kotlinx.coroutines.flow.Flow

/**
 * Hợp đồng Repository cho Authentication theo Clean Architecture.
 * Domain layer chỉ phụ thuộc vào AuthUser, độc lập hoàn toàn với Firebase và Credential Manager.
 */
interface AuthRepository {
    /**
     * Luồng phát trạng thái xác thực theo thời gian thực (realtime).
     * Phát ra null khi chưa đăng nhập, hoặc AuthUser khi đã đăng nhập thành công.
     */
    fun observeAuthState(): Flow<AuthUser?>

    /**
     * Người dùng hiện tại tại thời điểm gọi (synchronous snapshot).
     */
    val currentUser: AuthUser?

    /**
     * Xác thực với Firebase bằng Google ID Token.
     * Nonce chỉ dùng khi yêu cầu ID Token từ Google; Firebase không nhận nonce như access token.
     */
    suspend fun signInWithGoogle(idToken: String): Result<AuthUser>

    /**
     * Đăng xuất khỏi Firebase Auth.
     */
    suspend fun signOut(): Result<Unit>
}

/** Firebase Authentication không được cấu hình trong bản ứng dụng hiện tại. */
class AuthUnavailableException : IllegalStateException("Firebase Authentication is unavailable")
