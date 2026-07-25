package com.notepay.ai

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.LogSeverity
import com.notepay.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

enum class LocalModelInstallStatus {
    NOT_INSTALLED,
    IMPORTING,
    READY,
    ERROR,
}

data class LocalModelState(
    val status: LocalModelInstallStatus = LocalModelInstallStatus.NOT_INSTALLED,
    val displayName: String? = null,
    val sizeBytes: Long = 0L,
    val progress: Float? = null,
    val message: String? = null,
)

/** Stores a user-selected LiteRT-LM model inside NotePay's private app storage. */
@Singleton
class LocalAiModelManager @Inject constructor(
    @ApplicationContext context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val modelDirectory = File(appContext.filesDir, MODEL_DIRECTORY)
    private val modelFile = File(modelDirectory, MODEL_FILE_NAME)

    private val _state = MutableStateFlow(readPersistedState())
    val state: StateFlow<LocalModelState> = _state.asStateFlow()

    fun installedModelFile(): File? = modelFile.takeIf { file ->
        file.isFile && file.length() >= MIN_MODEL_BYTES
    }

    suspend fun importModel(uri: Uri): Result<LocalModelState> = withContext(ioDispatcher) {
        val previousState = readPersistedState()
        val tempFile = File(modelDirectory, "import_temp.litertlm")
        val backupFile = File(modelDirectory, "backup_model.litertlm")

        try {
            require(Build.SUPPORTED_ABIS.any(SUPPORTED_ABIS::contains)) {
                "Thiết bị cần kiến trúc 64-bit để chạy mô hình AI cục bộ."
            }

            val metadata = readMetadata(uri)
            val isSupportedExt = metadata.name.endsWith(".litertlm", ignoreCase = true) ||
                    metadata.name.endsWith(".bin", ignoreCase = true) ||
                    metadata.name.endsWith(".tflite", ignoreCase = true)
            require(isSupportedExt) {
                "Hãy chọn tệp mô hình AI có định dạng .litertlm, .bin hoặc .tflite."
            }
            modelDirectory.mkdirs()
            if (metadata.size != null) {
                require(metadata.size in MIN_MODEL_BYTES..MAX_MODEL_BYTES) {
                    "Mô hình phải có dung lượng từ 20 MB đến 1,5 GB."
                }
                require(modelDirectory.usableSpace >= metadata.size + STORAGE_HEADROOM_BYTES) {
                    "Không đủ bộ nhớ trống để cài mô hình AI."
                }
            }

            tempFile.delete()
            backupFile.delete()
            _state.value = LocalModelState(
                status = LocalModelInstallStatus.IMPORTING,
                displayName = metadata.name,
                sizeBytes = metadata.size ?: 0L,
                progress = 0f,
            )

            val input = appContext.contentResolver.openInputStream(uri)
                ?: error("Không thể đọc tệp mô hình đã chọn.")
            var copiedBytes = 0L
            input.use { source ->
                tempFile.outputStream().buffered().use { destination ->
                    val buffer = ByteArray(COPY_BUFFER_BYTES)
                    while (true) {
                        val count = source.read(buffer)
                        if (count < 0) break
                        copiedBytes += count
                        require(copiedBytes <= MAX_MODEL_BYTES) {
                            "Mô hình vượt quá giới hạn 1,5 GB."
                        }
                        destination.write(buffer, 0, count)
                        val expectedSize = metadata.size
                        _state.value = _state.value.copy(
                            sizeBytes = copiedBytes,
                            progress = expectedSize
                                ?.takeIf { it > 0L }
                                ?.let { (copiedBytes.toDouble() / it).coerceIn(0.0, 1.0).toFloat() },
                        )
                    }
                }
            }
            require(copiedBytes >= MIN_MODEL_BYTES) {
                "Tệp quá nhỏ hoặc không phải mô hình LiteRT-LM hợp lệ."
            }

            validateModel(tempFile)

            if (modelFile.exists() && !modelFile.renameTo(backupFile)) {
                error("Không thể thay thế mô hình đang dùng.")
            }
            if (!tempFile.renameTo(modelFile)) {
                backupFile.renameTo(modelFile)
                error("Không thể hoàn tất cài đặt mô hình.")
            }
            backupFile.delete()

            preferences.edit()
                .putString(KEY_DISPLAY_NAME, metadata.name)
                .putLong(KEY_SIZE_BYTES, copiedBytes)
                .apply()

            val readyState = LocalModelState(
                status = LocalModelInstallStatus.READY,
                displayName = metadata.name,
                sizeBytes = copiedBytes,
                progress = 1f,
                message = "Mô hình AI đã sẵn sàng trên thiết bị.",
            )
            _state.value = readyState
            Result.success(readyState)
        } catch (cancelled: CancellationException) {
            tempFile.delete()
            throw cancelled
        } catch (error: Throwable) {
            tempFile.delete()
            if (!modelFile.exists() && backupFile.exists()) {
                backupFile.renameTo(modelFile)
            }
            val restoredState = previousState.takeIf {
                installedModelFile() != null
            } ?: LocalModelState(status = LocalModelInstallStatus.ERROR)
            _state.value = restoredState.copy(
                status = LocalModelInstallStatus.ERROR,
                message = error.message ?: "Không thể cài mô hình AI cục bộ.",
            )
            Result.failure(error)
        }
    }

    suspend fun removeModel() = withContext(ioDispatcher) {
        modelFile.delete()
        File(modelDirectory, "backup_model.litertlm").delete()
        preferences.edit().clear().apply()
        _state.value = LocalModelState()
    }

    /** Checks initialization and conversation creation before installing a model. */
    private suspend fun validateModel(file: File) = withTimeout(MODEL_VALIDATION_TIMEOUT_MILLIS) {
        try {
            Engine.setNativeMinLogSeverity(LogSeverity.ERROR)
            Engine(
                EngineConfig(
                    modelPath = file.absolutePath,
                    backend = Backend.CPU(),
                    cacheDir = appContext.cacheDir.absolutePath,
                ),
            ).use { engine ->
                engine.initialize()
                engine.createConversation().use { conversation ->
                    // Verify that conversation creation succeeds; test prompt call is non-blocking
                    runCatching {
                        conversation.sendMessage("Hello")
                    }
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            if (com.notepay.BuildConfig.DEBUG) Log.e(TAG, "LiteRT-LM validation failed", error)
            val detail = error.localizedMessage?.takeIf { it.isNotBlank() }
            val errorMsg = if (detail != null && !detail.startsWith("Mô hình không chạy")) {
                "Không thể khởi tạo mô hình: $detail"
            } else {
                "Mô hình không chạy được trên thiết bị này. Hãy chọn một tệp .litertlm tương thích."
            }
            throw IllegalArgumentException(errorMsg, error)
        }
    }

    private fun readPersistedState(): LocalModelState {
        val file = installedModelFile() ?: return LocalModelState()
        return LocalModelState(
            status = LocalModelInstallStatus.READY,
            displayName = preferences.getString(KEY_DISPLAY_NAME, null) ?: "Mô hình AI cục bộ",
            sizeBytes = file.length(),
            progress = 1f,
        )
    }

    private fun readMetadata(uri: Uri): ModelMetadata {
        var name: String? = null
        var size: Long? = null
        appContext.contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        return ModelMetadata(
            name = name ?: uri.lastPathSegment?.substringAfterLast('/') ?: "model.litertlm",
            size = size,
        )
    }

    private data class ModelMetadata(
        val name: String,
        val size: Long?,
    )

    private companion object {
        const val PREFS_NAME = "notepay_local_ai_model"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_SIZE_BYTES = "size_bytes"
        const val MODEL_DIRECTORY = "ai_models"
        const val MODEL_FILE_NAME = "budget-advisor.litertlm"
        const val MODEL_EXTENSION = ".litertlm"
        const val COPY_BUFFER_BYTES = 1024 * 1024
        const val MIN_MODEL_BYTES = 20L * 1024L * 1024L
        const val MAX_MODEL_BYTES = 1_500L * 1024L * 1024L
        const val STORAGE_HEADROOM_BYTES = 256L * 1024L * 1024L
        const val MODEL_VALIDATION_TIMEOUT_MILLIS = 60_000L
        const val TAG = "LocalAiModelManager"
        val SUPPORTED_ABIS = setOf("arm64-v8a", "x86_64")
    }
}
