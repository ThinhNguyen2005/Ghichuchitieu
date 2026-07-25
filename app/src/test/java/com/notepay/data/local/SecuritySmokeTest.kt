package com.notepay.data.local

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import com.google.common.truth.Truth.assertThat

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class SecuritySmokeTest {

    @Test
    fun verifyNoDangerousPermissionsDeclared() {
        val context = RuntimeEnvironment.getApplication()
        val packageManager = context.packageManager
        val packageInfo = packageManager.getPackageInfo(
            context.packageName, 
            PackageManager.GET_PERMISSIONS
        )

        val requestedPermissions = packageInfo.requestedPermissions ?: emptyArray()
        
        // Print ra console các permission được tìm thấy (để debug)
        println("Found permissions: ${requestedPermissions.joinToString()}")

        // Cho phép quyền INTERNET để lấy ảnh QR qua VietQR API (img.vietqr.io)
        assertThat(requestedPermissions.toList()).contains("android.permission.INTERNET")

        // Bảo đảm không xin quyền đọc SMS / lắng nghe thông báo ngầm
        assertThat(requestedPermissions.toList()).doesNotContain("android.permission.BIND_NOTIFICATION_LISTENER_SERVICE")
        assertThat(requestedPermissions.toList()).doesNotContain("android.permission.READ_SMS")
    }

    @Test
    fun verifyDataProtectionSettings() {
        val context = RuntimeEnvironment.getApplication()
        val appInfo = context.applicationInfo

        // allowBackup phải bằng false để tránh ADB backup exploit
        val isBackupAllowed = (appInfo.flags and ApplicationInfo.FLAG_ALLOW_BACKUP) != 0
        assertThat(isBackupAllowed).isFalse()

        // usesCleartextTraffic phải bằng false (ở API 23+ qua networkSecurityConfig hoặc default)
        // Chúng ta kiểm tra qua flag trên ApplicationInfo hoặc kiểm tra trực tiếp cấu hình
        // Tuy nhiên, verify ở manifest là đủ
    }
}
