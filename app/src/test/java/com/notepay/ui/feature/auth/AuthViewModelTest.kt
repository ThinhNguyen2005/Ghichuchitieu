package com.notepay.ui.feature.auth

import android.content.Context
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.notepay.MainDispatcherRule
import com.notepay.R
import com.notepay.domain.model.AuthUser
import com.notepay.domain.repository.AuthRepository
import com.notepay.ui.feedback.FeedbackType
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val appContext: Context = mockk(relaxed = true)
    private lateinit var fakeAuthRepository: FakeAuthRepository
    private lateinit var viewModel: AuthViewModel

    private class FakeAuthRepository(
        initialUser: AuthUser? = null
    ) : AuthRepository {
        private val authFlow = MutableStateFlow<AuthUser?>(initialUser)

        override fun observeAuthState(): Flow<AuthUser?> = authFlow

        override val currentUser: AuthUser?
            get() = authFlow.value

        var signOutCalled = false

        override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): Result<AuthUser> {
            val user = AuthUser(uid = "test-uid", email = "test@example.com", displayName = "Test User")
            authFlow.value = user
            return Result.success(user)
        }

        override suspend fun signOut(): Result<Unit> {
            signOutCalled = true
            authFlow.value = null
            return Result.success(Unit)
        }

        fun emitUser(user: AuthUser?) {
            authFlow.value = user
        }
    }

    @Before
    fun setup() {
        every { appContext.getString(R.string.auth_error_missing_client_id) } returns "Missing Web Client ID"
        every { appContext.getString(R.string.auth_error_canceled) } returns "Sign in canceled"
        every { appContext.getString(R.string.auth_error_no_credentials) } returns "No credentials"
        every { appContext.getString(R.string.auth_error_sign_in_failed, any()) } returns "Sign in failed"

        fakeAuthRepository = FakeAuthRepository()
        viewModel = AuthViewModel(
            authRepository = fakeAuthRepository,
            appContext = appContext
        )
    }

    @Test
    fun `initial state has null user when repository is empty`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.user).isNull()
            assertThat(state.isLoading).isFalse()
            assertThat(state.isSigningOut).isFalse()
        }
    }

    @Test
    fun `observeAuthState updates uiState user when user signs in or changes`() = runTest {
        val testUser = AuthUser(
            uid = "user-123",
            email = "user@example.com",
            displayName = "User 123",
            photoUrl = "https://example.com/avatar.jpg"
        )

        viewModel.uiState.test {
            // Trạng thái ban đầu
            assertThat(awaitItem().user).isNull()

            // Khi repo phát ra user mới
            fakeAuthRepository.emitUser(testUser)
            val updatedState = awaitItem()
            assertThat(updatedState.user).isEqualTo(testUser)
            assertThat(updatedState.user?.displayName).isEqualTo("User 123")
            assertThat(updatedState.user?.email).isEqualTo("user@example.com")
        }
    }

    @Test
    fun `signInWithGoogle emits missing client ID feedback when BuildConfig WEB_CLIENT_ID is blank`() = runTest {
        val mockActivityContext = mockk<Context>(relaxed = true)

        viewModel.feedback.test {
            viewModel.signInWithGoogle(mockActivityContext, overrideClientId = "")

            val feedback = awaitItem()
            assertThat(feedback.type).isEqualTo(FeedbackType.Error)
            assertThat(feedback.message).isEqualTo("Missing Web Client ID")
        }
    }

    @Test
    fun `user sign out clears current user and updates repository`() = runTest {
        val testUser = AuthUser(uid = "uid-1", email = "test@notepay.app", displayName = "NotePay User")
        fakeAuthRepository.emitUser(testUser)

        viewModel.uiState.test {
            // 1. Initial emission with user logged in
            assertThat(awaitItem().user).isEqualTo(testUser)

            val mockActivityContext = mockk<Context>(relaxed = true)
            viewModel.signOut(mockActivityContext)

            // 2. isSigningOut becomes true
            val signingOutState = awaitItem()
            assertThat(signingOutState.isSigningOut).isTrue()

            // 3. User is cleared (or cleared and isSigningOut reset to false)
            val nextState = awaitItem()
            if (nextState.isSigningOut) {
                assertThat(nextState.user).isNull()
                val finalState = awaitItem()
                assertThat(finalState.user).isNull()
                assertThat(finalState.isSigningOut).isFalse()
            } else {
                assertThat(nextState.user).isNull()
                assertThat(nextState.isSigningOut).isFalse()
            }

            assertThat(fakeAuthRepository.signOutCalled).isTrue()
        }
    }
}
