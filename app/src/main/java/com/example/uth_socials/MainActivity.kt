package com.example.uth_socials

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.uth_socials.ui.screen.OnboardingScreen
import com.example.uth_socials.ui.screen.home.HomeScreen
import com.example.uth_socials.ui.theme.UTH_SocialsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            UTH_SocialsTheme {

//                OnboardingScreen()
                HomeScreen()
            }
        }
    }
}