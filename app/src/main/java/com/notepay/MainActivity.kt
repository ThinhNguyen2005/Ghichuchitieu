package com.notepay

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.notepay.ui.navigation.NotePayNavHost
import com.notepay.ui.theme.NotePayTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Cài đặt SplashScreen trước super.onCreate để tương thích Android 12+.
        // Theme khởi đầu được set là Theme.NotePay.Starting (AndroidManifest.xml).
        val splashScreen = installSplashScreen()
        // Có thể giữ splash cho đến khi dữ liệu sẵn sàng:
        // splashScreen.setKeepOnScreenCondition { ... }
        super.onCreate(savedInstanceState)
        com.notepay.ui.theme.ThemeManager.initialize(this)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = androidx.activity.SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            NotePayTheme {
                NotePayNavHost()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.notepay.service.NotePayNotificationListenerService.heal(this)
    }
}
