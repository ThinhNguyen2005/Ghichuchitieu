package com.notepay.data.repository

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.AuthUser
import com.notepay.domain.repository.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation của AuthRepository sử dụng Firebase Authentication.
 * Hỗ trợ an toàn khi Firebase chưa được cấu hình (thiếu google-services.json) mà không gây crash app.
 */
@Singleton
class AuthRepositoryImpl internal constructor(
    private val authProvider: () -> FirebaseAuth?,
    private val ioDispatcher: CoroutineDispatcher
) : AuthRepository {

    @Inject
    constructor(
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ) : this(
        authProvider = {
            try {
                if (FirebaseApp.getApps(context).isNotEmpty()) {
                    FirebaseAuth.getInstance()
                } else {
                    null
                }
            } catch (_: Exception) {
                null
            }
        },
        ioDispatcher = ioDispatcher
    )

    override fun observeAuthState(): Flow<AuthUser?> = callbackFlow {
        val auth = authProvider()
        if (auth == null) {
            trySend(null)
            awaitClose { }
            return@callbackFlow
        }

        // Phát ra trạng thái ban đầu
        val initialUser = auth.currentUser?.toDomainModel()
        trySend(initialUser)

        val listener = FirebaseAuth.AuthStateListener { currentAuth ->
            trySend(currentAuth.currentUser?.toDomainModel())
        }

        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }.flowOn(ioDispatcher)

    override val currentUser: AuthUser?
        get() = authProvider()?.currentUser?.toDomainModel()

    override suspend fun signInWithGoogle(idToken: String, rawNonce: String?): Result<AuthUser> =
        withContext(ioDispatcher) {
            runCatching {
                val auth = authProvider()
                    ?: throw IllegalStateException("Firebase chưa được khởi tạo. Vui lòng thêm google-services.json vào project.")
                val credential = GoogleAuthProvider.getCredential(idToken, rawNonce)
                val authResult = auth.signInWithCredential(credential).await()
                val user = authResult.user
                    ?: throw IllegalStateException("Không tìm thấy thông tin người dùng sau khi xác thực.")
                user.toDomainModel()
            }
        }

    override suspend fun signOut(): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            authProvider()?.signOut()
            Unit
        }
    }

    private fun com.google.firebase.auth.FirebaseUser.toDomainModel(): AuthUser {
        return AuthUser(
            uid = uid,
            email = email,
            displayName = displayName,
            photoUrl = photoUrl?.toString()
        )
    }
}
