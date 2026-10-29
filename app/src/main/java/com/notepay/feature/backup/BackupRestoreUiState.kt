package com.notepay.feature.backup

data class BackupRestoreUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastBackupDate: String? = null,
)
