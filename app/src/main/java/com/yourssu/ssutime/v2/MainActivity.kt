package com.yourssu.ssutime.v2

import android.app.NotificationManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourssu.ssutime.v2.screen.login.LoginScreen
import com.yourssu.ssutime.v2.screen.main.MainScreen
import com.yourssu.ssutime.v2.screen.my.MyPageScreen
import com.yourssu.ssutime.v2.screen.onboarding.OnBoardingScreen
import com.yourssu.ssutime.v2.screen.splash.Screens
import com.yourssu.ssutime.v2.screen.splash.SplashScreen
import com.yourssu.ssutime.v2.notification.ACTION_ANSWER_CALL
import com.yourssu.ssutime.v2.notification.EXTRA_CALL_NOTIFICATION_ID
import com.yourssu.ssutime.v2.ui.theme.SSUTimeTheme
import com.yourssu.ssutime.v2.ui.theme.WHITE

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        cancelCallNotificationIfNeeded(intent)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()

            SSUTimeTheme {
                NavHost(
                    navController = navController,
                    startDestination = Screens.SPLASH.name,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(WHITE)
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
                        OnBoardingScreen(
                            onConfirmClick = {
                                navController.navigate(Screens.MAIN.name)
                            }
                        )
                    }

                    composable(route = Screens.MAIN.name) {
                        MainScreen(
                            onProfileClick = {
                                navController.navigate(Screens.MY.name)
                            })
                    }

                    composable(route = Screens.MY.name) {
                        MyPageScreen(
                            onPressBack = {
                                navController.popBackStack()
                            },
                            onLogout = {
                                navController.navigate(Screens.LOGIN.name) {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                }
            }

        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        cancelCallNotificationIfNeeded(intent)
    }

    private fun cancelCallNotificationIfNeeded(intent: Intent?) {
        if (intent?.action != ACTION_ANSWER_CALL) return

        val notificationId = intent.getIntExtra(EXTRA_CALL_NOTIFICATION_ID, Int.MIN_VALUE)
        if (notificationId == Int.MIN_VALUE) return

        getSystemService(NotificationManager::class.java).cancel(notificationId)
    }
}
