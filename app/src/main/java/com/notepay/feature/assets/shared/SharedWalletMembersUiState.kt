package com.notepay.feature.assets.shared

import androidx.compose.runtime.Immutable
import com.notepay.domain.model.SharedWalletInvite
import com.notepay.domain.model.SharedWalletMember
import com.notepay.domain.model.Wallet
import com.notepay.domain.shared.SharedWalletPermissionGuard

@Immutable
data class SharedWalletMembersUiState(
    val wallet: Wallet? = null,
    val members: List<SharedWalletMember> = emptyList(),
    val invites: List<SharedWalletInvite> = emptyList(),
    val currentUserId: String? = null,
    val isCloudConfigured: Boolean = false,
    val isLoading: Boolean = true,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
) {
    val isOwner: Boolean
        get() = wallet?.let { SharedWalletPermissionGuard.canManageMembers(it) } == true
}
