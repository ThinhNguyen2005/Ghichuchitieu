package com.notepay.data.repository

import com.notepay.data.local.dao.WalletDao
import com.notepay.data.mapper.WalletMapper
import com.notepay.data.remote.supabase.SharedTransactionRemoteGateway
import com.notepay.data.remote.supabase.SupabaseClientProvider
import com.notepay.data.remote.supabase.dto.CreateSharedWalletInsertDto
import com.notepay.data.remote.supabase.dto.CreateSharedWalletInviteDto
import com.notepay.data.remote.supabase.dto.JoinSharedWalletRpcResult
import com.notepay.data.remote.supabase.dto.RemoteSharedWalletDto
import com.notepay.data.remote.supabase.dto.RemoteSharedWalletInviteDto
import com.notepay.data.remote.supabase.dto.RemoteSharedWalletMemberDto
import com.notepay.data.remote.supabase.dto.UpdateSharedWalletMemberDto
import com.notepay.data.sync.SharedWalletRealtimeManager
import com.notepay.data.sync.SyncOutboxProcessor
import com.notepay.di.IoDispatcher
import com.notepay.domain.model.AccountNature
import com.notepay.domain.model.SharedWalletInvite
import com.notepay.domain.model.SharedWalletMember
import com.notepay.domain.model.SharedWalletRole
import com.notepay.domain.model.SyncStatus
import com.notepay.domain.model.SyncSummary
import com.notepay.domain.model.Wallet
import com.notepay.domain.model.WalletPurpose
import com.notepay.domain.model.WalletType
import com.notepay.domain.money.Money
import com.notepay.domain.repository.AuthRepository
import com.notepay.domain.repository.SharedWalletRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Instant

@Singleton
class SharedWalletRepositoryImpl @Inject constructor(
    private val clientProvider: SupabaseClientProvider,
    private val remoteGateway: SharedTransactionRemoteGateway,
    private val outboxProcessor: SyncOutboxProcessor,
    private val realtimeManager: SharedWalletRealtimeManager,
    private val walletDao: WalletDao,
    private val walletMapper: WalletMapper,
    private val authRepository: AuthRepository,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SharedWalletRepository {

    override val isCloudConfigured: Boolean
        get() = clientProvider.isConfigured

    override fun currentUserId(): String? =
        clientProvider.client?.auth?.currentUserOrNull()?.id ?: authRepository.currentUser?.uid

    override suspend fun createSharedWallet(
        name: String,
        currencyCode: String,
        monthlyBudgetMinor: Long?,
    ): Result<Wallet> = executeCloudCall { client ->
        requireAuthenticatedUser(client)
        val trimmedName = name.trim().take(Wallet.MAX_NAME_LENGTH)
        require(trimmedName.isNotEmpty()) { "Shared wallet name cannot be blank" }

        val insertDto = CreateSharedWalletInsertDto(
            name = trimmedName,
            currency = currencyCode.ifBlank { "VND" },
            purpose = WalletPurpose.SHARED.name,
            monthlyBudgetMinor = monthlyBudgetMinor?.takeIf { it > 0L },
        )
        val remoteWallet = client.postgrest["shared_wallets"]
            .insert(insertDto) { select() }
            .decodeSingle<RemoteSharedWalletDto>()

        upsertLocalSharedWallet(
            remoteId = remoteWallet.id,
            name = remoteWallet.name,
            currencyCode = remoteWallet.currency,
            monthlyBudgetMinor = remoteWallet.monthlyBudgetMinor,
            role = SharedWalletRole.OWNER,
        )
    }

    override suspend fun joinWalletByCode(
        inviteCode: String,
        displayName: String,
    ): Result<Wallet> = executeCloudCall { client ->
        requireAuthenticatedUser(client)
        val normalizedCode = inviteCode.trim().uppercase()
        require(normalizedCode.length >= MIN_INVITE_CODE_LENGTH) { "Invalid invite code" }

        val resolvedDisplayName = displayName.trim().ifBlank {
            authRepository.currentUser?.displayName.orEmpty()
        }
        val joined = client.postgrest.rpc(
            function = "join_shared_wallet",
            parameters = buildJsonObject {
                put("invite_code", normalizedCode)
                put("p_display_name", resolvedDisplayName)
            },
        ).decodeSingle<JoinSharedWalletRpcResult>()

        val role = SharedWalletRole.fromString(joined.role) ?: SharedWalletRole.VIEWER
        val wallet = upsertLocalSharedWallet(
            remoteId = joined.walletId,
            name = joined.walletName,
            currencyCode = joined.currency,
            monthlyBudgetMinor = joined.monthlyBudgetMinor,
            role = role,
        )
        pullWalletTransactions(wallet)
        wallet
    }

    override suspend fun getMembers(walletRemoteId: String): Result<List<SharedWalletMember>> =
        executeCloudCall { client ->
            requireAuthenticatedUser(client)
            val rows = client.postgrest["shared_wallet_members"]
                .select { filter { eq("wallet_id", walletRemoteId) } }
                .decodeList<RemoteSharedWalletMemberDto>()
            rows.map { dto ->
                SharedWalletMember(
                    walletRemoteId = dto.walletId,
                    userId = dto.userId,
                    displayName = dto.displayName.ifBlank { dto.userId.take(8) },
                    role = SharedWalletRole.fromString(dto.role) ?: SharedWalletRole.VIEWER,
                    spendingLimitMinor = dto.spendingLimitMinor,
                    joinedAtEpochMs = parseEpochMsOrZero(dto.joinedAt),
                )
            }
        }

    override suspend fun updateMember(
        walletRemoteId: String,
        userId: String,
        role: SharedWalletRole,
        spendingLimitMinor: Long?,
    ): Result<Unit> = executeCloudCall { client ->
        requireAuthenticatedUser(client)
        require(role != SharedWalletRole.OWNER) { "Cannot transfer OWNER role via member edit" }
        client.postgrest["shared_wallet_members"]
            .update(
                UpdateSharedWalletMemberDto(
                    role = role.name,
                    spendingLimitMinor = spendingLimitMinor?.takeIf { it >= 0L },
                ),
            ) {
                filter {
                    eq("wallet_id", walletRemoteId)
                    eq("user_id", userId)
                }
            }
        Unit
    }

    override suspend fun removeMember(walletRemoteId: String, userId: String): Result<Unit> =
        executeCloudCall { client ->
            requireAuthenticatedUser(client)
            client.postgrest["shared_wallet_members"]
                .delete {
                    filter {
                        eq("wallet_id", walletRemoteId)
                        eq("user_id", userId)
                    }
                }
            Unit
        }

    override suspend fun createInvite(
        walletRemoteId: String,
        role: SharedWalletRole,
        expiresInHours: Int,
        maxUses: Int,
    ): Result<SharedWalletInvite> = executeCloudCall { client ->
        requireAuthenticatedUser(client)
        val safeRole = if (role == SharedWalletRole.EDITOR) SharedWalletRole.EDITOR else SharedWalletRole.VIEWER
        val expiresMs = System.currentTimeMillis() + expiresInHours.coerceIn(1, MAX_INVITE_HOURS) * MILLIS_PER_HOUR
        val dto = CreateSharedWalletInviteDto(
            code = generateInviteCode(),
            walletId = walletRemoteId,
            role = safeRole.name,
            expiresAt = Instant.fromEpochMilliseconds(expiresMs).toString(),
            maxUses = maxUses.coerceIn(1, MAX_INVITE_USES),
        )
        val created = client.postgrest["shared_wallet_invites"]
            .insert(dto) { select() }
            .decodeSingle<RemoteSharedWalletInviteDto>()
        created.toDomain()
    }

    override suspend fun listInvites(walletRemoteId: String): Result<List<SharedWalletInvite>> =
        executeCloudCall { client ->
            requireAuthenticatedUser(client)
            client.postgrest["shared_wallet_invites"]
                .select { filter { eq("wallet_id", walletRemoteId) } }
                .decodeList<RemoteSharedWalletInviteDto>()
                .filter { it.revokedAt == null }
                .map { it.toDomain() }
        }

    override suspend fun revokeInvite(code: String): Result<Unit> = executeCloudCall { client ->
        requireAuthenticatedUser(client)
        client.postgrest["shared_wallet_invites"]
            .update(mapOf("revoked_at" to Instant.fromEpochMilliseconds(System.currentTimeMillis()).toString())) {
                filter { eq("code", code) }
            }
        Unit
    }

    override suspend fun syncAllSharedWallets(): Result<SyncSummary> = executeCloudCall { client ->
        val uid = client.auth.currentUserOrNull()?.id
        val pushResult = outboxProcessor.processPending()
        var pulledWallets = 0
        var pulledTransactions = 0

        if (uid != null) {
            pulledWallets = pullJoinedWalletsMetadata(client, uid)
        }
        val localSharedWallets = walletDao.getSharedWallets().map(walletMapper::toDomain)
        for (wallet in localSharedWallets) {
            pulledTransactions += pullWalletTransactions(wallet)
        }
        SyncSummary(
            pushedCount = pushResult.syncedCount,
            failedPushCount = pushResult.failedCount,
            pulledTransactionsCount = pulledTransactions,
            pulledWalletsCount = pulledWallets,
        )
    }

    override suspend fun startRealtimeSync() {
        realtimeManager.startForJoinedWallets()
    }

    override suspend fun stopRealtimeSync() {
        realtimeManager.stopAll()
    }

    private suspend fun pullJoinedWalletsMetadata(client: SupabaseClient, userId: String): Int {
        val memberships = client.postgrest["shared_wallet_members"]
            .select { filter { eq("user_id", userId) } }
            .decodeList<RemoteSharedWalletMemberDto>()
        if (memberships.isEmpty()) return 0

        val roleByWallet = memberships.associate {
            it.walletId to (SharedWalletRole.fromString(it.role) ?: SharedWalletRole.VIEWER)
        }
        val remoteWallets = client.postgrest["shared_wallets"]
            .select()
            .decodeList<RemoteSharedWalletDto>()
            .filter { it.id in roleByWallet }

        for (remoteWallet in remoteWallets) {
            upsertLocalSharedWallet(
                remoteId = remoteWallet.id,
                name = remoteWallet.name,
                currencyCode = remoteWallet.currency,
                monthlyBudgetMinor = remoteWallet.monthlyBudgetMinor,
                role = roleByWallet[remoteWallet.id] ?: SharedWalletRole.VIEWER,
            )
        }
        return remoteWallets.size
    }

    private suspend fun pullWalletTransactions(wallet: Wallet): Int {
        val remoteId = wallet.remoteId ?: return 0
        if (!remoteGateway.isAvailable) return 0
        val rows = remoteGateway.fetchTransactions(remoteId)
        for (row in rows) {
            outboxProcessor.applyRemoteTransaction(row, wallet.id)
        }
        return rows.size
    }

    private suspend fun upsertLocalSharedWallet(
        remoteId: String,
        name: String,
        currencyCode: String,
        monthlyBudgetMinor: Long?,
        role: SharedWalletRole,
    ): Wallet {
        val existingEntity = walletDao.getByRemoteId(remoteId)
        val existing = existingEntity?.let(walletMapper::toDomain)
        val now = System.currentTimeMillis()
        val domain = if (existing != null) {
            existing.copy(
                name = name.trim().take(Wallet.MAX_NAME_LENGTH).ifBlank { existing.name },
                currencyCode = currencyCode.ifBlank { existing.currencyCode },
                budgetLimit = monthlyBudgetMinor?.let { Money(it) } ?: existing.budgetLimit,
                purpose = WalletPurpose.SHARED,
                isShared = true,
                sharedRole = role,
                syncStatus = SyncStatus.SYNCED,
                updatedAtEpochMs = maxOf(existing.updatedAtEpochMs, now),
            )
        } else {
            Wallet(
                name = name.trim().take(Wallet.MAX_NAME_LENGTH),
                initialBalance = Money.ZERO,
                iconKey = Wallet.ICON_BANK,
                colorKey = Wallet.COLOR_PRIMARY,
                isActive = false,
                budgetLimit = monthlyBudgetMinor?.let { Money(it) },
                type = WalletType.BANK,
                nature = AccountNature.ASSET,
                currencyCode = currencyCode.ifBlank { "VND" },
                purpose = WalletPurpose.SHARED,
                includeInTotalBalance = true,
                remoteId = remoteId,
                isShared = true,
                syncStatus = SyncStatus.SYNCED,
                sharedRole = role,
                updatedAtEpochMs = now,
            )
        }
        val id = walletDao.upsert(walletMapper.toEntity(domain))
        return domain.copy(id = if (domain.id > 0L) domain.id else id)
    }

    private suspend fun <T> executeCloudCall(block: suspend (SupabaseClient) -> T): Result<T> =
        withContext(ioDispatcher) {
            val client = clientProvider.client
                ?: return@withContext Result.failure(
                    IllegalStateException("Supabase is not configured (offline-only mode)"),
                )
            try {
                Result.success(block(client))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                Result.failure(error)
            }
        }

    private fun requireAuthenticatedUser(client: SupabaseClient): String =
        client.auth.currentUserOrNull()?.id
            ?: throw IllegalStateException("Please sign in with Google before using shared wallets")

    private fun RemoteSharedWalletInviteDto.toDomain(): SharedWalletInvite = SharedWalletInvite(
        code = code,
        walletRemoteId = walletId,
        role = SharedWalletRole.fromString(role) ?: SharedWalletRole.VIEWER,
        expiresAtEpochMs = parseEpochMsOrZero(expiresAt),
        maxUses = maxUses,
        usedCount = usedCount,
        revokedAtEpochMs = revokedAt?.let(::parseEpochMsOrZero),
    )

    private fun parseEpochMsOrZero(raw: String?): Long {
        if (raw.isNullOrBlank()) return 0L
        return runCatching { Instant.parse(raw.trim()).toEpochMilliseconds() }.getOrDefault(0L)
    }

    private fun generateInviteCode(): String = buildString(INVITE_CODE_LENGTH) {
        repeat(INVITE_CODE_LENGTH) {
            append(INVITE_ALPHABET[random.nextInt(INVITE_ALPHABET.length)])
        }
    }

    companion object {
        private const val INVITE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        private const val INVITE_CODE_LENGTH = 8
        private const val MIN_INVITE_CODE_LENGTH = 6
        private const val MAX_INVITE_HOURS = 24 * 30
        private const val MAX_INVITE_USES = 50
        private const val MILLIS_PER_HOUR = 3_600_000L
        private val random = SecureRandom()
    }
}
