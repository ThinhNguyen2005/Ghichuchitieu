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
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.notepay.BuildConfig
import com.notepay.R
import com.notepay.domain.repository.AuthRepository
import com.notepay.domain.repository.AuthUnavailableException
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.FeedbackDuration
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
import java.io.IOException
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

    /** Mở bottom sheet chọn tài khoản Google do Credential Manager cung cấp. */
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
                    type = FeedbackType.Error,
                    duration = FeedbackDuration.Long
                )
            )
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            try {
                val credentialManager = CredentialManager.create(activityContext)
                val credential = requestGoogleCredential(webClientId) { request ->
                    credentialManager.getCredential(request = request, context = activityContext).credential
                }
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val user = authRepository.signInWithGoogle(idToken).getOrThrow()
                    _uiState.update { it.copy(user = user) }
                } else {
                    emitError(appContext.getString(R.string.auth_error_unsupported_credential))
                }
            } catch (e: GetCredentialCancellationException) {
                // Google Play services có thể ánh xạ lỗi [16] thành cancellation.
                // Mã này cũng có thể xuất hiện khi OAuth/SHA hoặc quyền truy cập thử nghiệm
                // bị cấu hình sai; không thể kết luận chắc chắn tài khoản cần đăng nhập lại.
                if (e.isAccountReauthRequired()) {
                    Log.e(TAG, "Google Play services failed to reauthenticate (check OAuth configuration)", e)
                    emitError(appContext.getString(R.string.auth_error_account_reauth))
                } else {
                    Log.w(TAG, "Sign in cancelled: ${e.message}")
                    _feedback.emit(
                        UiFeedback(
                            message = appContext.getString(R.string.auth_error_canceled),
                            type = FeedbackType.Info,
                            duration = FeedbackDuration.Long
                        )
                    )
                }
            } catch (e: NoCredentialException) {
                Log.e(TAG, "Google sign-in returned no credentials in sheet or button flow; check Google account, sign-in prompts, and OAuth configuration", e)
                emitError(appContext.getString(R.string.auth_error_no_credentials))
            } catch (e: GetCredentialException) {
                Log.e(TAG, "GetCredentialException: ${e.message}", e)
                emitError(appContext.getString(R.string.auth_error_google_services))
            } catch (e: CancellationException) {
                throw e
            } catch (e: AuthUnavailableException) {
                Log.e(TAG, "Firebase Authentication is unavailable", e)
                emitError(appContext.getString(R.string.auth_error_firebase_unavailable))
            } catch (e: FirebaseAuthInvalidCredentialsException) {
                Log.e(TAG, "Firebase rejected Google credentials", e)
                emitError(appContext.getString(R.string.auth_error_invalid_credentials))
            } catch (e: FirebaseNetworkException) {
                Log.e(TAG, "Firebase authentication network failure", e)
                emitError(appContext.getString(R.string.auth_error_network))
            } catch (e: IOException) {
                Log.e(TAG, "Google sign-in network failure", e)
                emitError(appContext.getString(R.string.auth_error_network))
            } catch (e: Exception) {
                Log.e(TAG, "Sign in unexpected error: ${e.message}", e)
                emitError(appContext.getString(R.string.auth_error_generic))
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
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
                authRepository.signOut().getOrThrow()
                _uiState.update { it.copy(user = null) }
                // Xóa trạng thái lựa chọn tài khoản để lần sau người dùng có thể chọn tài khoản khác
                try {
                    val credentialManager = CredentialManager.create(context)
                    credentialManager.clearCredentialState(ClearCredentialStateRequest())
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Log.w(TAG, "Signed out, but could not clear credential selection", e)
                    _feedback.emit(
                        UiFeedback(
                            message = appContext.getString(R.string.auth_error_clear_credential),
                            type = FeedbackType.Info,
                            duration = FeedbackDuration.Long
                        )
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Firebase sign out failed", e)
                emitError(appContext.getString(R.string.auth_error_sign_out_failed))
            } finally {
                _uiState.update { it.copy(isSigningOut = false) }
            }
        }
    }

    private suspend fun emitError(message: String) {
        _feedback.emit(
            UiFeedback(message = message, type = FeedbackType.Error, duration = FeedbackDuration.Long)
        )
    }
}

private const val TAG = "AuthViewModel"

/** Prefer the Google account sheet, then use the explicit button flow if it is unavailable. */
internal suspend fun requestGoogleCredential(
    webClientId: String,
    getCredential: suspend (GetCredentialRequest) -> Credential,
): Credential {
    try {
        return getCredential(googleSheetCredentialRequest(webClientId, authorizedOnly = true))
    } catch (e: NoCredentialException) {
        Log.i(TAG, "No authorized Google credentials; trying all accounts")
    }

    try {
        return getCredential(googleSheetCredentialRequest(webClientId, authorizedOnly = false))
    } catch (e: NoCredentialException) {
        // Google documents that disabled Sign-in prompts can suppress the bottom sheet.
        Log.w(TAG, "Google bottom sheet has no credentials; trying explicit button sign-in")
    }

    return getCredential(
        GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(webClientId).build())
            .build()
    )
}

/** Yêu cầu Google ID Token qua bottom sheet; có thể lọc tài khoản đã cấp quyền. */
internal fun googleSheetCredentialRequest(
    webClientId: String,
    authorizedOnly: Boolean,
): GetCredentialRequest =
    GetCredentialRequest.Builder()
        .addCredentialOption(
            GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(authorizedOnly)
                .setAutoSelectEnabled(false)
                .build()
        )
        .build()

/**
 * Google Play Services nhét mã lỗi nội bộ vào message, ví dụ "[16] Account reauth failed.",
 * và Credential Manager ánh xạ nó thành [GetCredentialCancellationException] giống hệt khi
 * người dùng bấm huỷ. Đây là lỗi từ Google Play services và có thể liên quan OAuth,
 * không nhất thiết là phiên tài khoản hết hạn.
 */
internal fun GetCredentialException.isAccountReauthRequired(): Boolean {
    val message = message.orEmpty()
    return message.startsWith("[16]") || message.contains("reauth", ignoreCase = true)
}
