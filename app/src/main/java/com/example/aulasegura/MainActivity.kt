package com.example.aulasegura

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.example.aulasegura.navigation.AulaSeguraNavHost
import com.example.aulasegura.ui.components.ShieldPattern
import com.example.aulasegura.ui.theme.AulaSeguraTheme
import com.example.aulasegura.ui.theme.PrimaryContainer

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AulaSeguraTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    AulaSeguraNavHost()
                    // App-wide shield watermark, so every screen of every role carries
                    // the same premium background texture.
                    ShieldPattern(
                        modifier = Modifier.fillMaxSize(),
                        alpha = 0.035f,
                        color = PrimaryContainer,
                    )
                }
            }
        }
    }
}
