package com.yourssu.ssutime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.yourssu.ssutime.screen.splash.Screens
import com.yourssu.ssutime.screen.splash.SplashScreen
import com.yourssu.ssutime.ui.theme.SSUTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            SSUTimeTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screens.SPLASH.name,
                        modifier = Modifier
                        //.safeDrawingPadding()
                    ) {
                        composable(route = Screens.SPLASH.name) {
                            SplashScreen()
                        }
                    }
                }
            }
        }
    }
}
