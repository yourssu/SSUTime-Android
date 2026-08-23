@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.yourssu.ssutime.desktop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.yourssu.data.DiscussionInfo
import com.yourssu.data.todoUniqueKey
import com.yourssu.ssutime.desktop.core.lms.LmsAppService
import com.yourssu.ssutime.desktop.core.login.LmsApiAuthenticator
import com.yourssu.ssutime.desktop.core.login.LoginService
import com.yourssu.ssutime.desktop.core.login.LoginSession
import com.yourssu.ssutime.desktop.core.model.AppProfile
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.aiSummaries
import com.yourssu.ssutime.desktop.core.model.aiSummaryKey
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.core.network.SsuTimeApi
import com.yourssu.ssutime.desktop.core.network.SsuTimeAuthApi
import com.yourssu.ssutime.desktop.core.network.createSsuTimeHttpClient
import com.yourssu.ssutime.desktop.lms.isLmsLoggedIn
import com.yourssu.ssutime.desktop.screen.login.DesktopLoginScreen
import com.yourssu.ssutime.desktop.screen.main.DesktopAiSummaryUiState
import com.yourssu.ssutime.desktop.screen.main.DesktopMainScreen
import com.yourssu.ssutime.desktop.screen.my.DesktopHiddenTodosScreen
import com.yourssu.ssutime.desktop.screen.my.DesktopMyPageScreen
import com.yourssu.ssutime.desktop.screen.onboarding.DesktopOnBoardingScreen
import com.yourssu.ssutime.desktop.screen.splash.DesktopSplashScreen
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.checkbox
import com.yourssu.ssutime.desktop.ui.resources.desktop_open_link_error
import com.yourssu.ssutime.desktop.ui.resources.login_unknown_error
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.ssuTypography
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.Desktop
import java.awt.Dimension
import java.awt.Frame
import java.net.URI
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private enum class DesktopRoute {
    SPLASH,
    LOGIN,
    ONBOARDING,
    MAIN,
    MY,
    HIDDEN_TODOS,
}

fun main() {
    val singleInstance = DesktopSingleInstance.acquireOrNotifyExisting() ?: return

    try {
        application {
            var isWindowVisible by remember { mutableStateOf(true) }
            var windowActivationRequest by remember { mutableStateOf(0L) }
            var isTrayReady by remember { mutableStateOf(false) }
            val showWindow = {
                isWindowVisible = true
                windowActivationRequest += 1
            }
            val deadlineNotifier = remember {
                DesktopDeadlineNotifier(
                    onOpen = showWindow,
                    onExit = ::exitApplication,
                )
            }

            LaunchedEffect(deadlineNotifier) {
                isTrayReady = deadlineNotifier.start()
            }
            DisposableEffect(deadlineNotifier) {
                onDispose {
                    deadlineNotifier.close()
                }
            }
            DisposableEffect(singleInstance) {
                singleInstance.startListening(showWindow)
                onDispose {
                    singleInstance.stopListening()
                }
            }

            Window(
                onCloseRequest = {
                    if (isTrayReady) {
                        isWindowVisible = false
                    } else {
                        exitApplication()
                    }
                },
                visible = isWindowVisible,
                state = rememberWindowState(
                    width = 960.dp,
                    height = 760.dp,
                ),
                title = "SSUTime",
                icon = painterResource(Res.drawable.checkbox),
            ) {
                LaunchedEffect(window, isWindowVisible, windowActivationRequest) {
                    window.minimumSize = Dimension(720, 560)
                    if (isWindowVisible) {
                        window.extendedState = Frame.NORMAL
                        window.toFront()
                        window.requestFocus()
                    }
                }
                MaterialTheme(typography = ssuTypography()) {
                    DesktopApp(
                        deadlineNotifier = deadlineNotifier,
                    )
                }
            }
        }
    } finally {
        singleInstance.close()
    }
}

@Composable
private fun DesktopApp(
    deadlineNotifier: DesktopDeadlineNotifier,
) {
    val idState = rememberTextFieldState()
    val passwordState = rememberTextFieldState()
    val autoLoginState = remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val httpClient = remember { createSsuTimeHttpClient() }
    val authenticator = remember { LmsApiAuthenticator() }
    val loginService = remember(httpClient) {
        LoginService(
            lmsAuthenticator = authenticator,
            authApi = SsuTimeAuthApi(httpClient),
        )
    }
    val lmsService = remember(httpClient) {
        LmsAppService(SsuTimeApi(httpClient))
    }
    val store = remember { DesktopSessionStore() }
    var storedState by remember { mutableStateOf(store.load()) }
    var route by remember { mutableStateOf(DesktopRoute.SPLASH) }
    var session by remember { mutableStateOf<LoginSession?>(null) }
    var todoData by remember { mutableStateOf(storedState.todoData) }
    var profile by remember { mutableStateOf<AppProfile?>(storedState.profile) }
    var terms by remember { mutableStateOf<List<Term>>(emptyList()) }
    var selectedTerm by remember { mutableStateOf<Term?>(null) }
    val aiSummaryStates = remember { mutableStateMapOf<String, DesktopAiSummaryUiState>() }
    var loginMessage by remember { mutableStateOf("") }
    var mainError by remember { mutableStateOf<String?>(null) }
    var profileError by remember { mutableStateOf<String?>(null) }
    var isLoggingIn by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isProfileLoading by remember { mutableStateOf(false) }
    var loadingProgress by remember { mutableStateOf(0f) }
    val unknownLoginError = stringResource(Res.string.login_unknown_error)
    val openLinkError = stringResource(Res.string.desktop_open_link_error)

    fun saveCache(nextTodoData: AppTodoData = todoData, nextProfile: AppProfile? = profile) {
        storedState = store.update(
            storedState.copy(
                todoData = nextTodoData,
                profile = nextProfile,
            ),
        )
    }

    suspend fun reLogin(): LoginSession {
        val credentials = store.credentials(storedState)
            ?: throw IllegalStateException("LMS 로그인이 필요해요.")
        val nextSession = loginService.login(credentials.userId, credentials.password)
        session = nextSession
        return nextSession
    }

    suspend fun ensureLmsSession(force: Boolean = false) {
        val credentials = store.credentials(storedState)
            ?: throw IllegalStateException("LMS 로그인이 필요해요.")
        if (force || !isLmsLoggedIn() || session == null) {
            reLogin()
        }
    }

    fun refreshTodos() {
        if (isRefreshing) return
        scope.launch {
            isRefreshing = true
            loadingProgress = 0f
            mainError = null
            try {
                ensureLmsSession(force = false)
                val snapshot = lmsService.refreshTodos(
                    loadingState = { progress -> loadingProgress = progress },
                    previousData = todoData,
                    onRequireReLogin = { ensureLmsSession(force = true) },
                )
                todoData = snapshot.todoData.copy(
                    aiSummaryCache = todoData.aiSummaries,
                    sentDeadlineReminderKeys = todoData.sentDeadlineReminderKeys,
                )
                loadingProgress = 1f
                saveCache(nextTodoData = todoData)
                launch {
                    val currentAccessToken = session?.accessToken.orEmpty()
                    lmsService.reportSnapshot(
                        accessToken = currentAccessToken,
                        snapshot = snapshot,
                        onRefreshToken = {
                            runCatching { reLogin().accessToken }.getOrNull()
                        },
                    )
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mainError = exception.displayMessage()
            } finally {
                isRefreshing = false
            }
        }
    }

    fun loadProfile() {
        if (isProfileLoading) return
        scope.launch {
            isProfileLoading = true
            profileError = null
            try {
                ensureLmsSession(force = false)
                profile = lmsService.loadProfile(
                    onRequireReLogin = { ensureLmsSession(force = true) },
                )
                terms = lmsService.loadTerms(
                    onRequireReLogin = { ensureLmsSession(force = true) },
                )
                saveCache(nextProfile = profile)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                profileError = exception.displayMessage()
            } finally {
                isProfileLoading = false
            }
        }
    }

    fun selectTerm(term: Term) {
        selectedTerm = term
        scope.launch {
            isRefreshing = true
            loadingProgress = 0f
            mainError = null
            try {
                ensureLmsSession(force = false)
                val nextData = lmsService.loadTodosForTerm(
                    term = term,
                    loadingState = { progress -> loadingProgress = progress },
                    previousData = todoData,
                    onRequireReLogin = { ensureLmsSession(force = true) },
                )
                todoData = nextData.copy(
                    aiSummaryCache = todoData.aiSummaries,
                    sentDeadlineReminderKeys = todoData.sentDeadlineReminderKeys,
                )
                loadingProgress = 1f
                saveCache(nextTodoData = todoData)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                mainError = exception.displayMessage()
            } finally {
                isRefreshing = false
            }
        }
    }

    fun hideTodo(todo: AppTodo) {
        val targetKey = todo.todoUniqueKey()
        val nextHiddenKeys = (todoData.hiddenTodoKeys + targetKey).distinct()
        val nextTodos = todoData.todos.filterNot { it.todoUniqueKey() == targetKey }
        val nextHiddenTodos = (todoData.hiddenTodos + todo).distinctBy { it.todoUniqueKey() }

        todoData = todoData.copy(
            todos = nextTodos,
            hiddenTodos = nextHiddenTodos,
            hiddenTodoKeys = nextHiddenKeys,
        )
        saveCache(nextTodoData = todoData)
    }

    fun restoreTodo(todo: AppTodo) {
        val targetKey = todo.todoUniqueKey()
        val nextHiddenKeys = todoData.hiddenTodoKeys.filterNot { it == targetKey }
        val nextHiddenTodos = todoData.hiddenTodos.filterNot { it.todoUniqueKey() == targetKey }
        val nextTodos = (todoData.todos + todo)
            .distinctBy { it.todoUniqueKey() }
            .sortedWith(
                compareBy<AppTodo> { it.dueDate }
                    .thenBy { it.subject?.name.orEmpty() }
                    .thenBy(AppTodo::title),
            )

        todoData = todoData.copy(
            todos = nextTodos,
            hiddenTodos = nextHiddenTodos,
            hiddenTodoKeys = nextHiddenKeys,
        )
        saveCache(nextTodoData = todoData)
    }

    fun markDiscussionAsRead(discussion: DiscussionInfo) {
        val updatedSubjects = todoData.subjects.map { subject ->
            val updatedDiscussions = subject.discussions.map { item ->
                if (item.id == discussion.id) {
                    item.copy(readState = "read")
                } else {
                    item
                }
            }
            subject.copy(discussions = updatedDiscussions)
        }
        todoData = todoData.copy(subjects = updatedSubjects)
        saveCache(nextTodoData = todoData)
    }

    fun finishLogin(
        loginSession: LoginSession,
        id: String,
        password: String,
        autoLogin: Boolean,
    ) {
        session = loginSession
        storedState = store.save(
            current = storedState,
            userId = id,
            password = password,
            autoLogin = autoLogin,
        )
        route = if (storedState.onboardingCompleted) {
            DesktopRoute.MAIN
        } else {
            DesktopRoute.ONBOARDING
        }
        if (!storedState.onboardingCompleted || shouldRefreshOnOpen(todoData)) {
            refreshTodos()
        }
    }

    fun login(
        id: String,
        password: String,
        autoLogin: Boolean,
        fromSplash: Boolean = false,
    ) {
        if (isLoggingIn) return
        scope.launch {
            isLoggingIn = true
            loginMessage = ""
            try {
                val loginSession = loginService.login(id, password)
                finishLogin(
                    loginSession = loginSession,
                    id = id,
                    password = password,
                    autoLogin = autoLogin,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (exception: Exception) {
                loginMessage = exception.message
                    ?.takeIf(String::isNotBlank)
                    ?: unknownLoginError
                route = DesktopRoute.LOGIN
                if (fromSplash) {
                    idState.edit { replace(0, length, id) }
                }
            } finally {
                isLoggingIn = false
            }
        }
    }

    fun loadAiSummary(todo: AppTodo) {
        val currentSession = session ?: return
        val key = todo.aiSummaryKey()
        if (
            aiSummaryStates[key] == DesktopAiSummaryUiState.Loading ||
            aiSummaryStates[key] == DesktopAiSummaryUiState.Analyzing
        ) {
            return
        }
        todoData.aiSummaries[key]?.let { cached ->
            aiSummaryStates[key] = DesktopAiSummaryUiState.Success(
                summary = cached.summary,
                estimatedDurationMinutes = cached.estimatedDurationMinutes,
            )
            return
        }
        scope.launch {
            aiSummaryStates[key] = DesktopAiSummaryUiState.Loading
            try {
                ensureLmsSession(force = false)
                aiSummaryStates[key] = DesktopAiSummaryUiState.Analyzing
                val currentAccessToken = session?.accessToken ?: currentSession.accessToken
                val summary = lmsService.loadAiSummary(
                    accessToken = currentAccessToken,
                    todo = todo,
                    onRequireReLogin = { ensureLmsSession(force = true) },
                )
                if (summary == null) {
                    aiSummaryStates[key] = DesktopAiSummaryUiState.Empty
                } else {
                    aiSummaryStates[key] = DesktopAiSummaryUiState.Success(
                        summary = summary.summary,
                        estimatedDurationMinutes = summary.estimatedDurationMinutes,
                    )
                    todoData = todoData.copy(
                        aiSummaryCache = todoData.aiSummaries + (key to summary),
                    )
                    saveCache(nextTodoData = todoData)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                aiSummaryStates[key] = DesktopAiSummaryUiState.Error
            }
        }
    }

    fun logout() {
        scope.launch {
            runCatching { authenticator.logout() }
            storedState = store.logout(storedState)
            session = null
            todoData = AppTodoData()
            profile = null
            terms = emptyList()
            selectedTerm = null
            aiSummaryStates.clear()
            idState.edit { replace(0, length, "") }
            passwordState.edit { replace(0, length, "") }
            route = DesktopRoute.LOGIN
        }
    }

    LaunchedEffect(Unit) {
        todoData.aiSummaries.forEach { (key, summary) ->
            aiSummaryStates[key] = DesktopAiSummaryUiState.Success(
                summary = summary.summary,
                estimatedDurationMinutes = summary.estimatedDurationMinutes,
            )
        }
        val credentials = store.credentials(storedState)
        if (credentials == null) {
            route = DesktopRoute.LOGIN
        } else {
            autoLoginState.value = true
            login(
                id = credentials.userId,
                password = credentials.password,
                autoLogin = true,
                fromSplash = true,
            )
        }
    }

    LaunchedEffect(
        storedState.systemNotificationsEnabled,
        todoData.loadedAt,
    ) {
        if (!storedState.systemNotificationsEnabled) {
            return@LaunchedEffect
        }
        while (true) {
            val sentKeys = deadlineNotifier.sendIfNeeded(
                todoData = todoData,
                sentKeys = todoData.sentDeadlineReminderKeys,
            )
            if (sentKeys.isNotEmpty()) {
                todoData = todoData.copy(
                    sentDeadlineReminderKeys = (
                        todoData.sentDeadlineReminderKeys + sentKeys
                    ).distinct().takeLast(500),
                )
                saveCache(nextTodoData = todoData)
            }
            delay(60_000L)
        }
    }

    DisposableEffect(httpClient) {
        onDispose {
            httpClient.close()
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        when (route) {
            DesktopRoute.SPLASH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 480.dp),
                ) {
                    DesktopSplashScreen()
                }
            }

            DesktopRoute.LOGIN -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 480.dp),
                ) {
                    DesktopLoginScreen(
                        idState = idState,
                        passwordState = passwordState,
                        autoLoginState = autoLoginState,
                        message = loginMessage,
                        messageColor = R500,
                        isLoading = isLoggingIn,
                        loginEnabled = !isLoggingIn,
                        onLoginClick = {
                            login(
                                id = idState.text.toString(),
                                password = passwordState.text.toString(),
                                autoLogin = autoLoginState.value,
                            )
                        },
                    )
                }
            }

            DesktopRoute.ONBOARDING -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 480.dp),
                ) {
                    DesktopOnBoardingScreen(
                        onConfirmClick = {
                            storedState = store.update(
                                storedState.copy(onboardingCompleted = true),
                            )
                            route = DesktopRoute.MAIN
                        },
                    )
                }
            }

            DesktopRoute.MAIN -> {
                DesktopMainScreen(
                    todoData = todoData,
                    aiSummaryStates = aiSummaryStates,
                    isLoading = isRefreshing,
                    loadingProgress = loadingProgress,
                    errorMessage = mainError,
                    showBlockingLoading = isRefreshing && todoData.loadedAt.isBlank(),
                    onRefresh = ::refreshTodos,
                    onProfileClick = {
                        route = DesktopRoute.MY
                        loadProfile()
                    },
                    onExpandTodo = ::loadAiSummary,
                    onHideTodo = ::hideTodo,
                    onDiscussionExpanded = ::markDiscussionAsRead,
                    onOpenUrl = { url ->
                        runCatching { openDesktopUrl(url) }
                            .onFailure { mainError = openLinkError }
                    },
                )
            }

            DesktopRoute.MY -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp),
                ) {
                    DesktopMyPageScreen(
                        profile = profile,
                        isLoading = isProfileLoading,
                        errorMessage = profileError,
                        terms = terms,
                        selectedTerm = selectedTerm,
                        onTermSelected = ::selectTerm,
                        onNavigateToHiddenTodos = { route = DesktopRoute.HIDDEN_TODOS },
                        onBack = { route = DesktopRoute.MAIN },
                        onOpenUrl = { url ->
                            runCatching { openDesktopUrl(url) }
                                .onFailure { profileError = openLinkError }
                        },
                        onLogout = ::logout,
                        showSystemNotificationSetting = DesktopDeadlineNotifier.isSupported(),
                        systemNotificationsEnabled = storedState.systemNotificationsEnabled,
                        onSystemNotificationsChanged = { enabled ->
                            storedState = store.update(
                                storedState.copy(systemNotificationsEnabled = enabled),
                            )
                        },
                    )
                }
            }

            DesktopRoute.HIDDEN_TODOS -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 560.dp),
                ) {
                    DesktopHiddenTodosScreen(
                        hiddenTodos = todoData.hiddenTodos,
                        onBack = { route = DesktopRoute.MY },
                        onRestoreClick = ::restoreTodo,
                    )
                }
            }
        }
    }
}

private fun openDesktopUrl(url: String) {
    check(Desktop.isDesktopSupported()) { "브라우저를 열 수 없는 환경입니다." }
    Desktop.getDesktop().browse(URI(url))
}

private fun Throwable.displayMessage(): String =
    message?.takeIf(String::isNotBlank) ?: "알 수 없는 오류가 발생했어요."

private val refreshDateZoneId: ZoneId = ZoneId.of("Asia/Seoul")

internal fun shouldRefreshOnOpen(
    todoData: AppTodoData,
    today: LocalDate = LocalDate.now(refreshDateZoneId),
): Boolean {
    if (todoData.submitted.any { todo ->
            todo.submittedAt.isBlank() && todo.todoId != -1
        }
    ) {
        return true
    }

    val loadedDate = runCatching {
        Instant.parse(todoData.loadedAt)
            .atZone(refreshDateZoneId)
            .toLocalDate()
    }.getOrNull() ?: return true

    return loadedDate.isBefore(today)
}
