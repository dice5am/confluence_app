package com.cavin.confluence

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.cavin.confluence.core.ui.theme.ConfluenceTheme
import com.cavin.confluence.navigation.ConfluenceNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // MOB-1.2: dark theme default
            ConfluenceTheme(darkTheme = true) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ConfluenceNavHost()
                }
            }
        }
    }
}
