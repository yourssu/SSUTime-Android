package com.yourssu.ssutime.v2.screen.main

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.component.SSUTimeBottomBar
import com.yourssu.ssutime.v2.screen.calendar.CalendarScreen
import com.yourssu.ssutime.v2.screen.my.MyPageScreen
import com.yourssu.ssutime.v2.screen.navigation.MainTab
import com.yourssu.ssutime.v2.ui.theme.WHITE

@Composable
fun MainContainerScreen(
    skipInitialLmsRefresh: Boolean = false,
    forceInitialLmsRefresh: Boolean = false,
    homeEntrySource: String = MainActivity.ENTRY_SOURCE_APP,
    homeEntryVersion: Int = 0,
    skipLoadFromMyPageBack: Boolean = false,
    onInitialLmsRefreshSkipConsumed: () -> Unit = {},
    onInitialLmsRefreshForceConsumed: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val bottomNavController = rememberNavController()
    val navBackStackEntry by bottomNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val currentTab = MainTab.fromRoute(currentRoute)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = WHITE,
        bottomBar = {
            SSUTimeBottomBar(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    if (tab.route != currentRoute) {
                        bottomNavController.navigate(tab.route) {
                            popUpTo(bottomNavController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = bottomNavController,
            startDestination = MainTab.startDestination.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(route = MainTab.HOME.route) {
                MainScreen(
                    skipInitialLmsRefresh = skipInitialLmsRefresh,
                    forceInitialLmsRefresh = forceInitialLmsRefresh,
                    homeEntrySource = homeEntrySource,
                    homeEntryVersion = homeEntryVersion,
                    skipLoadFromMyPageBack = skipLoadFromMyPageBack,
                    onInitialLmsRefreshSkipConsumed = onInitialLmsRefreshSkipConsumed,
                    onInitialLmsRefreshForceConsumed = onInitialLmsRefreshForceConsumed,
                )
            }

            composable(route = MainTab.CALENDAR.route) {
                CalendarScreen()
            }

            composable(route = MainTab.MY.route) {
                MyPageScreen(
                    onLogout = onLogout
                )
            }
        }
    }
}
