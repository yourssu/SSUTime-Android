package com.yourssu.ssutime.v2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.window.core.layout.WindowSizeClass
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.UpdateAvailability
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.screen.login.LoginScreen
import com.yourssu.ssutime.v2.screen.main.MainScreen
import com.yourssu.ssutime.v2.screen.my.MyPageScreen
import com.yourssu.ssutime.v2.screen.onboarding.OnBoardingScreen
import com.yourssu.ssutime.v2.screen.splash.Screens
import com.yourssu.ssutime.v2.screen.splash.SplashScreen
import com.yourssu.ssutime.v2.ui.theme.SSUTimeTheme
import com.yourssu.ssutime.v2.ui.theme.WHITE

class MainActivity : ComponentActivity() {
    private val skipInitialLmsRefresh = mutableStateOf(false)
    private val forceInitialLmsRefresh = mutableStateOf(false)
    private val homeEntrySource = mutableStateOf(ENTRY_SOURCE_APP)
    private val homeEntryVersion = mutableIntStateOf(0)

    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }

    private val appUpdateLauncher =
        registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result: ActivityResult ->
            if (result.resultCode != RESULT_OK) {
                Log.e(TAG, "Update flow failed! Result code: ${result.resultCode}")
            }
        }

    override fun onResume() {
        super.onResume()

        appUpdateManager
            .appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (
                    appUpdateInfo.updateAvailability() ==
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS
                ) {
                    startImmediateUpdate(appUpdateInfo)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        skipInitialLmsRefresh.value = intent.shouldSkipInitialLmsRefresh()
        homeEntrySource.value = intent.homeEntrySource()
        forceInitialLmsRefresh.value = intent.shouldForceInitialLmsRefresh()

        if (savedInstanceState == null) {
            intent.captureEntryAnalytics()
        }
        enableEdgeToEdge()

        checkForImmediateUpdate()

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
                                navController.navigate(Screens.LOGIN.name) {
                                    popUpTo(Screens.SPLASH.name) { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            navigateToMain = {
                                forceInitialLmsRefresh.value = true
                                navController.navigate(Screens.MAIN.name) {
                                    popUpTo(Screens.SPLASH.name) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(route = Screens.LOGIN.name) {
                        LoginScreen(
                            successLogin = {
                                navController.navigate(Screens.ONBORADING.name) {
                                    popUpTo(Screens.LOGIN.name) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(route = Screens.ONBORADING.name) {
                        OnBoardingScreen(
                            onConfirmClick = {
                                skipInitialLmsRefresh.value = true
                                navController.navigate(Screens.MAIN.name) {
                                    popUpTo(Screens.ONBORADING.name) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }

                    composable(route = Screens.MAIN.name) { backStackEntry ->
                        val skipLoadFromMyPageBack =
                            backStackEntry.savedStateHandle.remove<Boolean>(
                                SKIP_MAIN_LOAD_FROM_MY_PAGE_BACK_KEY
                            ) == true

                        val windowSizeClass = currentWindowAdaptiveInfo().windowSizeClass
                        val isLargeScreen = windowSizeClass.isAtLeastBreakpoint(
                            WindowSizeClass.WIDTH_DP_MEDIUM_LOWER_BOUND,
                            WindowSizeClass.HEIGHT_DP_MEDIUM_LOWER_BOUND
                        )

                        MainScreen(
                            isLargeScreen = isLargeScreen,
                            skipInitialLmsRefresh = skipInitialLmsRefresh.value,
                            forceInitialLmsRefresh = forceInitialLmsRefresh.value,
                            homeEntrySource = homeEntrySource.value,
                            homeEntryVersion = homeEntryVersion.intValue,
                            skipLoadFromMyPageBack = skipLoadFromMyPageBack,
                            onInitialLmsRefreshSkipConsumed = {
                                skipInitialLmsRefresh.value = false
                            },
                            onInitialLmsRefreshForceConsumed = {
                                forceInitialLmsRefresh.value = false
                            },
                            onProfileClick = {
                                navController.navigate(Screens.MY.name)
                            })
                    }

                    composable(route = Screens.MY.name) {
                        MyPageScreen(
                            onPressBack = {
                                navController.previousBackStackEntry
                                    ?.savedStateHandle
                                    ?.set(SKIP_MAIN_LOAD_FROM_MY_PAGE_BACK_KEY, true)
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

    private fun checkForImmediateUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
            if (
                appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                appUpdateInfo.updatePriority() >= IMMEDIATE_UPDATE_PRIORITY_THRESHOLD &&
                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
            ) {
                startImmediateUpdate(appUpdateInfo)
            }
        }
    }

    private fun startImmediateUpdate(appUpdateInfo: AppUpdateInfo) {
        appUpdateManager.startUpdateFlowForResult(
            appUpdateInfo,
            appUpdateLauncher,
            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        skipInitialLmsRefresh.value = intent.shouldSkipInitialLmsRefresh()
        forceInitialLmsRefresh.value = intent.shouldForceInitialLmsRefresh()
        homeEntrySource.value = intent.homeEntrySource()
        homeEntryVersion.value += 1
        intent.captureEntryAnalytics()
    }

    companion object {
        private const val TAG = "In-App Update"
        private const val IMMEDIATE_UPDATE_PRIORITY_THRESHOLD = 4

        const val EXTRA_SKIP_INITIAL_LMS_REFRESH = "extra_skip_initial_lms_refresh"
        const val EXTRA_ENTRY_SOURCE = "extra_entry_source"
        const val EXTRA_WIDGET_SIZE = "extra_widget_size"
        const val EXTRA_NOTIFICATION_D_DAY = "extra_notification_d_day"
        const val EXTRA_NOTIFICATION_TASK_COUNT = "extra_notification_task_count"
        const val EXTRA_NOTIFICATION_TASK_TYPE = "extra_notification_task_type"
        const val EXTRA_NOTIFICATION_SUBJECT_NAME = "extra_notification_subject_name"

        const val ENTRY_SOURCE_APP = "app"
        const val ENTRY_SOURCE_WIDGET = "widget"
        const val ENTRY_SOURCE_NOTIFICATION = "notification"
        const val ENTRY_SOURCE_CALL_ALERT = "call_alert"
    }
}

private fun Intent?.shouldSkipInitialLmsRefresh(): Boolean =
    this?.getBooleanExtra(MainActivity.EXTRA_SKIP_INITIAL_LMS_REFRESH, false) == true

private fun Intent?.shouldForceInitialLmsRefresh(): Boolean =
    this.homeEntrySource() == MainActivity.ENTRY_SOURCE_WIDGET

private fun Intent?.homeEntrySource(): String =
    this?.getStringExtra(MainActivity.EXTRA_ENTRY_SOURCE)
        ?.takeIf { it in knownHomeEntrySources }
        ?: MainActivity.ENTRY_SOURCE_APP

private val knownHomeEntrySources = setOf(
    MainActivity.ENTRY_SOURCE_APP,
    MainActivity.ENTRY_SOURCE_WIDGET,
    MainActivity.ENTRY_SOURCE_NOTIFICATION,
    MainActivity.ENTRY_SOURCE_CALL_ALERT,
)

private const val SKIP_MAIN_LOAD_FROM_MY_PAGE_BACK_KEY = "skip_main_load_from_my_page_back"

private fun Intent.captureEntryAnalytics() {
    getStringExtra(MainActivity.EXTRA_WIDGET_SIZE)
        ?.takeIf { it in knownWidgetSizes }
        ?.let(Analytics::widgetTap)

    if (
        hasExtra(MainActivity.EXTRA_NOTIFICATION_D_DAY) &&
        hasExtra(MainActivity.EXTRA_NOTIFICATION_TASK_COUNT)
    ) {
        val representativeTodo = notificationRepresentativeTodo()
        if (representativeTodo != null) {
            Analytics.notificationTap(
                dDay = getIntExtra(MainActivity.EXTRA_NOTIFICATION_D_DAY, 0),
                notificationTaskCount = getIntExtra(MainActivity.EXTRA_NOTIFICATION_TASK_COUNT, 1),
                representativeTodo = representativeTodo,
            )
        }
    }
}

private val knownWidgetSizes = setOf("small", "medium", "large")

private fun Intent.notificationRepresentativeTodo(): TodoInfo? {
    val taskType = getStringExtra(MainActivity.EXTRA_NOTIFICATION_TASK_TYPE).toTodoTypeOrNull()
        ?: return null
    val subjectName = getStringExtra(MainActivity.EXTRA_NOTIFICATION_SUBJECT_NAME).orEmpty()

    return TodoInfo(
        todoId = 0,
        title = "",
        due_date = "",
        type = taskType,
        subject = SubjectInfo(
            id = 0,
            name = subjectName,
            professor = "",
        ).takeIf { subjectName.isNotBlank() },
    )
}

private fun String?.toTodoTypeOrNull(): TodoType? =
    TodoType.values().firstOrNull { type ->
        equals(type.name, ignoreCase = true) || this == type.kor
    }
