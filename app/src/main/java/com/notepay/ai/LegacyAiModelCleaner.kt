package com.notepay.ai

import android.content.Context
import com.notepay.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reclaims storage on user devices by removing legacy .litertlm models
 * previously stored in app-private files.
 */
@Singleton
class LegacyAiModelCleaner @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun cleanLegacyModels() = withContext(ioDispatcher) {
        try {
            val legacyDir = File(context.filesDir, "ai_models")
            if (legacyDir.exists()) {
                legacyDir.deleteRecursively()
            }
            context.getSharedPreferences("notepay_local_ai_model", Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
        } catch (_: Throwable) {
            // Best effort cleanup
        }
    }
}
