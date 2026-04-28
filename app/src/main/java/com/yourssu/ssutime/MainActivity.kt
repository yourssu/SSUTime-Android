package com.yourssu.ssutime

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourssu.ssutime.screen.login.LoginScreen
import com.yourssu.ssutime.screen.onboarding.OnBoardingScreen
import com.yourssu.ssutime.screen.splash.Screens
import com.yourssu.ssutime.screen.splash.SplashScreen
import com.yourssu.ssutime.ui.theme.SSUTimeTheme
import com.yourssu.ssutime.ui.theme.WHITE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            SSUTimeTheme {
                Scaffold(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WHITE)
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screens.SPLASH.name,
                        modifier = Modifier
//                            .safeDrawingPadding()
                    ) {
                        composable(route = Screens.SPLASH.name) {
                            SplashScreen(
                                navigateToLogin = {
                                    navController.navigate(Screens.LOGIN.name)
                                }
                            )
                        }

                        composable(route = Screens.LOGIN.name) {
                            LoginScreen(
                                successLogin = {
                                    navController.navigate(Screens.ONBORADING.name)
                                }
                            )
                        }

                        composable(route = Screens.ONBORADING.name) {
                            OnBoardingScreen()
                        }
                    }
                }
            }
        }
    }
}
