package com.notepay.ui.feature.backup

data class BackupRestoreUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastBackupDate: String? = null,
    val exportSuccess: Boolean = false,
    val importSuccess: Boolean = false,
    val errorMessage: String? = null,
)
