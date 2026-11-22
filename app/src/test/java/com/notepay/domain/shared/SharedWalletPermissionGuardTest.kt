package com.notepay.domain.shared

import com.google.common.truth.Truth.assertThat
import com.notepay.domain.model.Category
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.Transaction
import com.notepay.domain.model.TransactionType
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.money.Money
import org.junit.Test
import kotlin.time.Instant

class SharedWalletPermissionGuardTest {

    private val ownerId = "user-owner"
    private val editorId = "user-editor"
    private val otherEditorId = "user-other-editor"

    private fun walletWithRole(isShared: Boolean, role: SharedWalletRole?) = Wallet(
        id = 1L,
        name = if (isShared) "Ví chung" else "Ví cá nhân",
        initialBalance = Money.ZERO,
        iconKey = Wallet.ICON_BANK,
        colorKey = Wallet.COLOR_PRIMARY,
        purpose = if (isShared) WalletPurpose.SHARED else WalletPurpose.PERSONAL,
        isShared = isShared,
        sharedRole = role,
    )

    private fun txBy(creatorId: String?) = Transaction(
        id = 10L,
        amount = Money.fromMajorUnit(50_000L),
        type = TransactionType.EXPENSE,
        category = Category.FOOD,
        note = "Cơm trưa",
        occurredAt = Instant.fromEpochMilliseconds(1_700_000_000_000L),
        walletId = 1L,
        createdByUserId = creatorId,
    )

    @Test
    fun `personal wallet permits all transaction and delete operations`() {
        val personal = walletWithRole(isShared = false, role = null)
        val foreignTx = txBy("anyone")

        assertThat(SharedWalletPermissionGuard.canAddTransaction(personal)).isTrue()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(personal, foreignTx, ownerId)).isTrue()
        assertThat(SharedWalletPermissionGuard.canDeleteTransaction(personal, foreignTx, ownerId)).isTrue()
        assertThat(SharedWalletPermissionGuard.canManageMembers(personal)).isFalse()
        assertThat(SharedWalletPermissionGuard.canCreateInvite(personal)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteWallet(personal)).isTrue()
    }

    @Test
    fun `shared wallet OWNER has full permissions`() {
        val ownerWallet = walletWithRole(isShared = true, role = SharedWalletRole.OWNER)
        val editorTx = txBy(editorId)

        assertThat(SharedWalletPermissionGuard.canAddTransaction(ownerWallet)).isTrue()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(ownerWallet, editorTx, ownerId)).isTrue()
        assertThat(SharedWalletPermissionGuard.canDeleteTransaction(ownerWallet, editorTx, ownerId)).isTrue()
        assertThat(SharedWalletPermissionGuard.canManageMembers(ownerWallet)).isTrue()
        assertThat(SharedWalletPermissionGuard.canCreateInvite(ownerWallet)).isTrue()
        assertThat(SharedWalletPermissionGuard.canDeleteWallet(ownerWallet)).isTrue()
    }

    @Test
    fun `shared wallet EDITOR can add and modify own tx but not others or wallet settings`() {
        val editorWallet = walletWithRole(isShared = true, role = SharedWalletRole.EDITOR)
        val ownTx = txBy(editorId)
        val offlineOwnTx = txBy(null)
        val otherTx = txBy(otherEditorId)

        assertThat(SharedWalletPermissionGuard.canAddTransaction(editorWallet)).isTrue()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(editorWallet, ownTx, editorId)).isTrue()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(editorWallet, offlineOwnTx, editorId)).isTrue()
        assertThat(SharedWalletPermissionGuard.canDeleteTransaction(editorWallet, ownTx, editorId)).isTrue()

        assertThat(SharedWalletPermissionGuard.canEditTransaction(editorWallet, otherTx, editorId)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteTransaction(editorWallet, otherTx, editorId)).isFalse()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(editorWallet, ownTx, null)).isFalse()

        assertThat(SharedWalletPermissionGuard.canManageMembers(editorWallet)).isFalse()
        assertThat(SharedWalletPermissionGuard.canCreateInvite(editorWallet)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteWallet(editorWallet)).isFalse()
    }

    @Test
    fun `shared wallet VIEWER is strictly read-only`() {
        val viewerWallet = walletWithRole(isShared = true, role = SharedWalletRole.VIEWER)
        val ownTx = txBy(editorId)

        assertThat(SharedWalletPermissionGuard.canAddTransaction(viewerWallet)).isFalse()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(viewerWallet, ownTx, editorId)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteTransaction(viewerWallet, ownTx, editorId)).isFalse()
        assertThat(SharedWalletPermissionGuard.canManageMembers(viewerWallet)).isFalse()
        assertThat(SharedWalletPermissionGuard.canCreateInvite(viewerWallet)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteWallet(viewerWallet)).isFalse()
    }

    @Test
    fun `shared wallet with null role denies all write actions`() {
        val unknownRoleWallet = walletWithRole(isShared = true, role = null)
        val tx = txBy(ownerId)

        assertThat(SharedWalletPermissionGuard.canAddTransaction(unknownRoleWallet)).isFalse()
        assertThat(SharedWalletPermissionGuard.canEditTransaction(unknownRoleWallet, tx, ownerId)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteTransaction(unknownRoleWallet, tx, ownerId)).isFalse()
        assertThat(SharedWalletPermissionGuard.canDeleteWallet(unknownRoleWallet)).isFalse()
    }
}
