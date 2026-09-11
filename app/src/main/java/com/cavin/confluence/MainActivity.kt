package com.cavin.confluence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.cavin.confluence.core.ui.components.ConfluenceSplash
import com.cavin.confluence.core.ui.theme.ConfluenceColors
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.navigation.ConfluenceNavHost
import kotlinx.coroutines.delay

private const val BrandSplashMillis = 700L

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // MOB-1.2: dark theme default
            ConfluenceTheme(darkTheme = true) {
                var showSplash by remember { mutableStateOf(true) }
                LaunchedEffect(Unit) {
                    delay(BrandSplashMillis)
                    showSplash = false
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ConfluenceColors.Void,
                ) {
                    if (showSplash) {
                        ConfluenceSplash()
                    } else {
                        ConfluenceNavHost()
                    }
                }
            }
        }
    }
}
