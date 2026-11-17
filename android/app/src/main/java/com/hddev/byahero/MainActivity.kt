package com.hddev.byahero

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hddev.byahero.ui.screens.AuthGateScreen
import com.hddev.byahero.ui.screens.MainScreen
import com.hddev.byahero.ui.screens.SplashIntroScreen
import com.hddev.byahero.ui.theme.ByaheroTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ByaheroTheme {
                var showSplash by remember { mutableStateOf(true) }
                var isAuthenticated by remember { mutableStateOf(false) }

                when {
                    showSplash -> SplashIntroScreen(onFinished = { showSplash = false })
                    !isAuthenticated -> AuthGateScreen(onAuthenticated = { isAuthenticated = true })
                    else -> MainScreen()
                }
            }
        }
    }
}
