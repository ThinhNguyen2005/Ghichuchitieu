package com.notepay.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notepay.domain.model.TransactionNotePolicy
import com.notepay.domain.model.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

enum class LearnedCaptureDecision {
    NONE,
    APPROVED,
    REJECTED,
}

object LearnedCapturePolicy {
    fun decide(approved: Int, rejected: Int): LearnedCaptureDecision = when {
        approved >= 2 && rejected == 0 -> LearnedCaptureDecision.APPROVED
        rejected >= approved && rejected > 0 -> LearnedCaptureDecision.REJECTED
        else -> LearnedCaptureDecision.NONE
    }
}

data class PendingBankNotification(
    val id: String,
    val fingerprint: String,
    val dedupeKey: String,
    val amountCents: Long,
    val type: TransactionType,
    val note: String,
    val walletId: Long,
    val primaryPackageName: String,
    val createdAtMillis: Long,
)

private val Context.notificationCaptureDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "bank_notification_capture",
)

@Singleton
class NotificationCaptureStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.applicationContext.notificationCaptureDataStore

    suspend fun savePending(pending: PendingBankNotification): Boolean {
        val safePending = pending.copy(note = sanitizeNote(pending.note))
        var inserted = false
        dataStore.edit { preferences ->
            val current = preferences[PENDING_KEY].orEmpty().mapNotNull(::decodePending)
            if (current.none { it.dedupeKey == safePending.dedupeKey }) {
                preferences[PENDING_KEY] = (current.map(::encodePending) + encodePending(safePending)).toSet()
                inserted = true
            }
        }
        return inserted
    }

    suspend fun takePending(id: String): PendingBankNotification? {
        var found: PendingBankNotification? = null
        dataStore.edit { preferences ->
            val current = preferences[PENDING_KEY].orEmpty().mapNotNull(::decodePending)
            found = current.firstOrNull { it.id == id }
            if (found != null) {
                preferences[PENDING_KEY] = current.filterNot { it.id == id }.map(::encodePending).toSet()
            }
        }
        return found
    }

    suspend fun learnedDecision(fingerprint: String): LearnedCaptureDecision {
        val stats = dataStore.data.first()[LEARNING_KEY].orEmpty()
            .mapNotNull(::decodeLearning)
            .firstOrNull { it.fingerprint == fingerprint }
            ?: return LearnedCaptureDecision.NONE
        return LearnedCapturePolicy.decide(stats.approved, stats.rejected)
    }

    suspend fun recordDecision(fingerprint: String, approved: Boolean) {
        dataStore.edit { preferences ->
            val current = preferences[LEARNING_KEY].orEmpty().mapNotNull(::decodeLearning).toMutableList()
            val index = current.indexOfFirst { it.fingerprint == fingerprint }
            val old = if (index >= 0) current[index] else LearningStats(fingerprint, 0, 0)
            val updated = if (approved) old.copy(approved = old.approved + 1)
            else old.copy(rejected = old.rejected + 1)
            if (index >= 0) current[index] = updated else current += updated
            preferences[LEARNING_KEY] = current.map(::encodeLearning).toSet()
        }
    }

    companion object {
        private val PENDING_KEY = stringSetPreferencesKey("pending_bank_notifications")
        private val LEARNING_KEY = stringSetPreferencesKey("bank_notification_learning")
        private const val SEPARATOR = "|"

        fun sanitizeNote(note: String): String = TransactionNotePolicy.truncate(
            note
            .replace(
                Regex("(?i)\\b(?:tktt|tk|tài khoản|tai khoan)\\s*[:#|]?\\s*[a-z0-9*x-]{4,}"),
                "tài khoản: [đã ẩn]",
            )
            .replace(Regex("(?i)\\b[a-z*x]{2,}\\d{4,}\\b"), "[đã ẩn]")
            .replace(Regex("\\b\\d{6,}\\b"), "[đã ẩn]")
        )

        private fun encodePending(value: PendingBankNotification): String = listOf(
            value.id,
            value.fingerprint,
            value.dedupeKey,
            value.amountCents.toString(),
            value.type.name,
            encodeText(value.note),
            value.walletId.toString(),
            value.primaryPackageName,
            value.createdAtMillis.toString(),
        ).joinToString(SEPARATOR)

        private fun decodePending(value: String): PendingBankNotification? = runCatching {
            val fields = value.split(SEPARATOR)
            if (fields.size != 9) return null
            PendingBankNotification(
                id = fields[0],
                fingerprint = fields[1],
                dedupeKey = fields[2],
                amountCents = fields[3].toLong(),
                type = TransactionType.valueOf(fields[4]),
                note = decodeText(fields[5]),
                walletId = fields[6].toLong(),
                primaryPackageName = fields[7],
                createdAtMillis = fields[8].toLong(),
            )
        }.getOrNull()

        private data class LearningStats(
            val fingerprint: String,
            val approved: Int,
            val rejected: Int,
        )

        private fun encodeLearning(value: LearningStats): String =
            listOf(value.fingerprint, value.approved, value.rejected).joinToString(SEPARATOR)

        private fun decodeLearning(value: String): LearningStats? = runCatching {
            val fields = value.split(SEPARATOR)
            if (fields.size != 3) return null
            LearningStats(fields[0], fields[1].toInt(), fields[2].toInt())
        }.getOrNull()

        private fun encodeText(value: String): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(value.toByteArray())

        private fun decodeText(value: String): String =
            String(Base64.getUrlDecoder().decode(value))
    }
}
