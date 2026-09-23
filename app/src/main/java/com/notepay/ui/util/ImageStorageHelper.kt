package com.notepay.ui.util

import android.content.Context
import android.net.Uri
import java.io.File

object ImageStorageHelper {
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri, walletId: Long): String? {
        return try {
            // Xóa các file ảnh nền cũ của ví này nếu có để dọn dẹp bộ nhớ
            context.filesDir.listFiles { _, name ->
                name.startsWith("wallet_bg_${walletId}_") || name == "wallet_bg_${walletId}.jpg"
            }?.forEach {
                try { it.delete() } catch (_: Exception) {}
            }

            // Dùng timestamp để tránh bị stale cache ở Coil
            val fileName = "wallet_bg_${walletId}_${System.currentTimeMillis()}.jpg"
            val destinationFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            destinationFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}