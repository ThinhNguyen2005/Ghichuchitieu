package com.notepay.ui.feature.backup

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notepay.data.backup.DataExporter
import com.notepay.data.backup.DataImporter
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    private val dataExporter: DataExporter,
    private val dataImporter: DataImporter,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(BackupRestoreUiState(
        lastBackupDate = loadLastBackupDate()
    ))
    val state = _state.asStateFlow()

    fun exportToFile(uri: Uri) {
        if (_state.value.isExporting) return
        _state.update { it.copy(isExporting = true, exportSuccess = false, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = dataExporter.exportToJson()
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.bufferedWriter().use { it.write(json) }
                } ?: throw Exception("Không thể ghi file")

                saveLastBackupDate()
                _state.update {
                    it.copy(
                        isExporting = false,
                        exportSuccess = true,
                        lastBackupDate = loadLastBackupDate(),
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Lỗi sao lưu: ${e.message}",
                    )
                }
            }
        }
    }

    fun importFromFile(uri: Uri) {
        if (_state.value.isImporting) return
        _state.update { it.copy(isImporting = true, importSuccess = false, errorMessage = null) }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val json = dataImporter.readFromFile(uri)
                dataImporter.importFromJson(json)
                _state.update {
                    it.copy(isImporting = false, importSuccess = true)
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "Lỗi khôi phục: ${e.message}",
                    )
                }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorMessage = null) }
    }

    fun clearSuccess() {
        _state.update { it.copy(exportSuccess = false, importSuccess = false) }
    }

    private fun loadLastBackupDate(): String? {
        val prefs = context.getSharedPreferences("notepay_backup", Context.MODE_PRIVATE)
        return prefs.getString("last_backup_date", null)
    }

    private fun saveLastBackupDate() {
        val now = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date())
        context.getSharedPreferences("notepay_backup", Context.MODE_PRIVATE)
            .edit().putString("last_backup_date", now).apply()
    }
}
