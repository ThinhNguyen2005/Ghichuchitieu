package com.notepay.feature.auth.main

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.notepay.BuildConfig
import com.notepay.R
import com.notepay.domain.repository.AuthRepository
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @param:ApplicationContext private val appContext: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback: SharedFlow<UiFeedback> = _feedback.asSharedFlow()

    init {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { user ->
                _uiState.update { it.copy(user = user) }
            }
        }
    }

    /**
     * Bắt đầu luồng đăng nhập Google qua Credential Manager.
     * Cần [activityContext] để Credential Manager hiển thị BottomSheet đăng nhập trên giao diện.
     */
    fun signInWithGoogle(
        activityContext: Context,
        overrideClientId: String? = null,
    ) {
        if (_uiState.value.isLoading) return

        val webClientId = overrideClientId ?: BuildConfig.WEB_CLIENT_ID
        if (webClientId.isBlank()) {
            _feedback.tryEmit(
                UiFeedback(
                    message = appContext.getString(R.string.auth_error_missing_client_id),
                    type = FeedbackType.Error
                )
            )
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                // Tạo raw nonce ngẫu nhiên và băm SHA-256 để chống tấn công replay attack
                val rawNonce = UUID.randomUUID().toString()
                val bytes = rawNonce.toByteArray()
                val md = MessageDigest.getInstance("SHA-256")
                val digest = md.digest(bytes)
                val hashedNonce = digest.fold("") { str, byte -> str + "%02x".format(byte) }

                val credentialManager = CredentialManager.create(activityContext)
                val credential = requestGoogleCredential(
                    credentialManager = credentialManager,
                    activityContext = activityContext,
                    webClientId = webClientId,
                    hashedNonce = hashedNonce
                )
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val signInResult = authRepository.signInWithGoogle(
                        idToken = idToken,
                        rawNonce = rawNonce
                    )

                    signInResult.fold(
                        onSuccess = {
                            // Thành công, state sẽ được tự động cập nhật qua observeAuthState()
                        },
                        onFailure = { error ->
                            val msg = error.localizedMessage ?: error.javaClass.simpleName
                            _feedback.emit(
                                UiFeedback(
                                    message = appContext.getString(R.string.auth_error_sign_in_failed, msg),
                                    type = FeedbackType.Error
                                )
                            )
                        }
                    )
                } else {
                    _feedback.emit(
                        UiFeedback(
                            message = appContext.getString(R.string.auth_error_sign_in_failed, "Unsupported credential"),
                            type = FeedbackType.Error
                        )
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                // Play Services trả "[16] Account reauth failed" cũng dưới dạng cancellation:
                // phải phân biệt với thao tác huỷ thật vì cách xử lý của người dùng khác hẳn.
                if (e.isAccountReauthRequired()) {
                    Log.e(TAG, "Google account on this device requires re-authentication", e)
                    _feedback.emit(
                        UiFeedback(
                            message = appContext.getString(R.string.auth_error_account_reauth),
                            type = FeedbackType.Error
                        )
                    )
                } else {
                    Log.w(TAG, "Sign in cancelled: ${e.message}")
                    _feedback.emit(
                        UiFeedback(
                            message = appContext.getString(R.string.auth_error_canceled),
                            type = FeedbackType.Info
                        )
                    )
                }
            } catch (e: NoCredentialException) {
                Log.e(TAG, "No credentials on device: ${e.message}", e)
                _feedback.emit(
                    UiFeedback(
                        message = appContext.getString(R.string.auth_error_no_credentials),
                        type = FeedbackType.Error
                    )
                )
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException: ${e.message}", e)
                val msg = e.localizedMessage ?: e.javaClass.simpleName
                _feedback.emit(
                    UiFeedback(
                        message = appContext.getString(R.string.auth_error_sign_in_failed, msg),
                        type = FeedbackType.Error
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Sign in unexpected error: ${e.message}", e)
                val msg = e.localizedMessage ?: e.javaClass.simpleName
                _feedback.emit(
                    UiFeedback(
                        message = appContext.getString(R.string.auth_error_sign_in_failed, msg),
                        type = FeedbackType.Error
                    )
                )
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    /**
     * Lấy credential từ Credential Manager, tự phục hồi một lần khi Google Play Services báo
     * phiên đăng nhập của thiết bị đã hỏng ("[16] Account reauth failed"). Xoá credential state
     * rồi gọi lại là cách Google khuyến nghị để thoát khỏi cache phiên cũ trên máy.
     */
    private suspend fun requestGoogleCredential(
        credentialManager: CredentialManager,
        activityContext: Context,
        webClientId: String,
        hashedNonce: String,
    ): Credential {
        return try {
            requestCredentialOnce(credentialManager, activityContext, webClientId, hashedNonce)
        } catch (e: GetCredentialCancellationException) {
            if (!e.isAccountReauthRequired()) throw e
            Log.w(TAG, "Credential state is stale (account reauth failed), clearing and retrying once", e)
            clearCredentialStateQuietly(credentialManager)
            requestCredentialOnce(credentialManager, activityContext, webClientId, hashedNonce)
        }
    }

    private suspend fun requestCredentialOnce(
        credentialManager: CredentialManager,
        activityContext: Context,
        webClientId: String,
        hashedNonce: String,
    ): Credential {
        // Sử dụng GetGoogleIdOption để hiển thị Bottom Sheet chuẩn hiện đại (như Cue)
        // thay vì hộp thoại Center Dialog của GetSignInWithGoogleOption cũ.
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(webClientId)
            .setFilterByAuthorizedAccounts(false)
            .setAutoSelectEnabled(false)
            .setNonce(hashedNonce)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        return try {
            credentialManager.getCredential(request = request, context = activityContext).credential
        } catch (e: NoCredentialException) {
            // Máy chưa có tài khoản nào uỷ quyền cho app: mở luồng đăng nhập đầy đủ.
            Log.i(TAG, "No authorized credential, trying GetSignInWithGoogleOption: ${e.message}")
            val fallbackOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                .setNonce(hashedNonce)
                .build()
            val fallbackRequest = GetCredentialRequest.Builder()
                .addCredentialOption(fallbackOption)
                .build()
            credentialManager.getCredential(request = fallbackRequest, context = activityContext).credential
        }
    }

    private suspend fun clearCredentialStateQuietly(credentialManager: CredentialManager) {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            // Không xoá được cache không chặn được lần thử lại, chỉ ghi log lại để debug.
            Log.w(TAG, "Could not clear credential state: ${e.message}")
        }
    }

    /**
     * Đăng xuất khỏi Firebase Auth và xóa trạng thái lưu phiên Credential Manager trên máy.
     */
    fun signOut(context: Context) {
        if (_uiState.value.isSigningOut) return

        _uiState.update { it.copy(isSigningOut = true) }

        viewModelScope.launch {
            try {
                authRepository.signOut()
                // Xóa trạng thái lựa chọn tài khoản để lần sau người dùng có thể chọn tài khoản khác
                val credentialManager = CredentialManager.create(context)
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                val msg = e.localizedMessage ?: e.javaClass.simpleName
                _feedback.emit(
                    UiFeedback(
                        message = msg,
                        type = FeedbackType.Error
                    )
                )
            } finally {
                _uiState.update { it.copy(isSigningOut = false) }
            }
        }
    }
}

private const val TAG = "AuthViewModel"

/**
 * Google Play Services nhét mã lỗi nội bộ vào message, ví dụ "[16] Account reauth failed.",
 * và Credential Manager ánh xạ nó thành [GetCredentialCancellationException] giống hệt khi
 * người dùng bấm huỷ. Vì vậy phải đọc message mới phân biệt được hai trường hợp này.
 */
internal fun GetCredentialException.isAccountReauthRequired(): Boolean {
    val message = message.orEmpty()
    return message.startsWith("[16]") || message.contains("reauth", ignoreCase = true)
}
