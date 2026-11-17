package com.hddev.byahero

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hddev.byahero.data.UserProfile
import com.hddev.byahero.ui.screens.AuthGateScreen
import com.hddev.byahero.ui.screens.MainScreen
import com.hddev.byahero.ui.screens.SplashIntroScreen
import com.hddev.byahero.ui.theme.ByaheroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        setContent {
            ByaheroTheme {
                var showSplash by remember { mutableStateOf(true) }
                var authenticatedProfile by remember { mutableStateOf<UserProfile?>(null) }

                LaunchedEffect(showSplash) {
                    if (showSplash) {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
                            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
                        )
                    } else {
                        enableEdgeToEdge(
                            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                        )
                    }
                }

                when {
                    showSplash -> SplashIntroScreen(onFinished = { showSplash = false })
                    authenticatedProfile == null -> AuthGateScreen(
                        onAuthenticated = { profile -> authenticatedProfile = profile }
                    )
                    else -> MainScreen(
                        profile = authenticatedProfile!!,
                        onUpdateProfile = { updated -> authenticatedProfile = updated },
                        onLogout = { authenticatedProfile = null }
                    )
                }
            }
        }
    }
}
