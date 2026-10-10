package com.notepay.ui.feature.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
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
import com.notepay.ui.feedback.FeedbackType
import com.notepay.ui.feedback.UiFeedback
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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

                val credentialManager = CredentialManager.create(activityContext)
                val result = try {
                    credentialManager.getCredential(
                        request = request,
                        context = activityContext
                    )
                } catch (e: NoCredentialException) {
                    android.util.Log.i("AuthViewModel", "GetGoogleIdOption returned NoCredentialException, trying GetSignInWithGoogleOption fallback: ${e.message}")
                    val fallbackOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
                        .setNonce(hashedNonce)
                        .build()
                    val fallbackRequest = GetCredentialRequest.Builder()
                        .addCredentialOption(fallbackOption)
                        .build()
                    credentialManager.getCredential(
                        request = fallbackRequest,
                        context = activityContext
                    )
                }

                val credential = result.credential
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
                android.util.Log.w("AuthViewModel", "Sign in cancelled: ${e.message}", e)
                _feedback.emit(
                    UiFeedback(
                        message = appContext.getString(R.string.auth_error_canceled),
                        type = FeedbackType.Info
                    )
                )
            } catch (e: NoCredentialException) {
                android.util.Log.e("AuthViewModel", "No credentials on device: ${e.message}", e)
                _feedback.emit(
                    UiFeedback(
                        message = appContext.getString(R.string.auth_error_no_credentials),
                        type = FeedbackType.Error
                    )
                )
            } catch (e: GetCredentialException) {
                android.util.Log.e("AuthViewModel", "GetCredentialException: ${e.message}", e)
                val msg = e.localizedMessage ?: e.javaClass.simpleName
                _feedback.emit(
                    UiFeedback(
                        message = appContext.getString(R.string.auth_error_sign_in_failed, msg),
                        type = FeedbackType.Error
                    )
                )
            } catch (e: Exception) {
                android.util.Log.e("AuthViewModel", "Sign in unexpected error: ${e.message}", e)
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
