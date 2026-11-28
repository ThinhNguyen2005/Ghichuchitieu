package com.notepay.feature.backup

import android.net.Uri

data class BackupRestoreUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val lastBackupDate: String? = null,
    val lastExportedUri: Uri? = null,
)
