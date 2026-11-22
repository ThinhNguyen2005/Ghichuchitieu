package com.notepay.feature.assets.shared

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.R
import com.notepay.core.designsystem.feedback.FeedbackType
import com.notepay.core.designsystem.feedback.UiFeedback
import com.notepay.core.navigation.Route
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.repository.SharedWalletRepository
import com.notepay.domain.repository.WalletRepository
import com.notepay.domain.shared.SharedWalletPermissionGuard
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SharedWalletMembersViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val walletRepository: WalletRepository,
    private val sharedWalletRepository: SharedWalletRepository,
    @param:ApplicationContext private val context: Context,
) : ViewModel() {

    private val walletId: Long = savedStateHandle.get<Long>(Route.SharedWalletMembers.ARG_WALLET_ID) ?: 0L

    private val _state = MutableStateFlow(
        SharedWalletMembersUiState(
            currentUserId = sharedWalletRepository.currentUserId(),
            isCloudConfigured = sharedWalletRepository.isCloudConfigured,
        ),
    )
    val state: StateFlow<SharedWalletMembersUiState> = _state.asStateFlow()

    private val _feedback = MutableSharedFlow<UiFeedback>(extraBufferCapacity = 1)
    val feedback = _feedback.asSharedFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    errorMessage = null,
                    isCloudConfigured = sharedWalletRepository.isCloudConfigured,
                    currentUserId = sharedWalletRepository.currentUserId(),
                )
            }
            val wallet = walletRepository.getById(walletId)
            val remoteId = wallet?.remoteId
            if (wallet == null || !wallet.isShared || remoteId.isNullOrBlank()) {
                _state.update {
                    it.copy(
                        wallet = wallet,
                        isLoading = false,
                        errorMessage = context.getString(R.string.shared_wallet_not_found),
                    )
                }
                return@launch
            }
            if (!sharedWalletRepository.isCloudConfigured) {
                _state.update { it.copy(wallet = wallet, isLoading = false, errorMessage = null) }
                return@launch
            }

            try {
                val membersResult = sharedWalletRepository.getMembers(remoteId)
                val invitesResult = if (SharedWalletPermissionGuard.canManageMembers(wallet)) {
                    sharedWalletRepository.listInvites(remoteId).getOrDefault(emptyList())
                } else {
                    emptyList()
                }
                _state.update {
                    it.copy(
                        wallet = wallet,
                        members = membersResult.getOrDefault(emptyList()),
                        invites = invitesResult,
                        isLoading = false,
                        errorMessage = membersResult.exceptionOrNull()?.message,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update {
                    it.copy(
                        wallet = wallet,
                        isLoading = false,
                        errorMessage = error.message,
                    )
                }
            }
        }
    }

    fun updateMember(
        userId: String,
        role: SharedWalletRole,
        spendingLimitMinor: Long?,
    ) {
        val remoteId = _state.value.wallet?.remoteId ?: return
        runMutation {
            sharedWalletRepository.updateMember(remoteId, userId, role, spendingLimitMinor).getOrThrow()
        }
    }

    fun removeMember(userId: String) {
        val remoteId = _state.value.wallet?.remoteId ?: return
        runMutation {
            sharedWalletRepository.removeMember(remoteId, userId).getOrThrow()
        }
    }

    fun createInvite(role: SharedWalletRole, expiresInHours: Int, maxUses: Int) {
        val remoteId = _state.value.wallet?.remoteId ?: return
        runMutation {
            sharedWalletRepository.createInvite(remoteId, role, expiresInHours, maxUses).getOrThrow()
        }
    }

    fun revokeInvite(code: String) {
        runMutation {
            sharedWalletRepository.revokeInvite(code).getOrThrow()
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            val result = sharedWalletRepository.syncAllSharedWallets()
            _state.update { it.copy(isSubmitting = false) }
            result.fold(
                onSuccess = { summary ->
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(
                                R.string.shared_wallet_sync_success,
                                summary.pushedCount,
                                summary.pulledTransactionsCount,
                            ),
                            type = FeedbackType.Success,
                        ),
                    )
                    refresh()
                },
                onFailure = { err ->
                    _feedback.emit(
                        UiFeedback(
                            message = context.getString(
                                R.string.shared_wallet_action_failed,
                                err.message.orEmpty(),
                            ),
                            type = FeedbackType.Error,
                        ),
                    )
                },
            )
        }
    }

    private fun runMutation(block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            try {
                block()
                _state.update { it.copy(isSubmitting = false) }
                refresh()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                _state.update { it.copy(isSubmitting = false) }
                _feedback.emit(
                    UiFeedback(
                        message = context.getString(
                            R.string.shared_wallet_action_failed,
                            error.message.orEmpty(),
                        ),
                        type = FeedbackType.Error,
                    ),
                )
            }
        }
    }
}
