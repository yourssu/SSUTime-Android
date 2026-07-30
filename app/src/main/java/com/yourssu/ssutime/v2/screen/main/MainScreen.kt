package com.yourssu.ssutime.v2.screen.main

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.yourssu.data.AlertData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.component.OutlinedButton
import com.yourssu.ssutime.v2.component.SButton
import com.yourssu.ssutime.v2.component.SCheckBox
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.getRemainingTimeText
import com.yourssu.ssutime.v2.getStringDate
import com.yourssu.ssutime.v2.getStringDateWithTime
import com.yourssu.ssutime.v2.getStringSimpleDate
import com.yourssu.ssutime.v2.todo.localizedLabel
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstant
import com.yourssu.ssutime.v2.ui.theme.G100
import com.yourssu.ssutime.v2.ui.theme.G400
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(),
    coroutine: CoroutineScope = rememberCoroutineScope(),
    skipInitialLmsRefresh: Boolean = false,
    forceInitialLmsRefresh: Boolean = false,
    homeEntrySource: String = MainActivity.ENTRY_SOURCE_APP,
    homeEntryVersion: Int = 0,
    skipLoadFromMyPageBack: Boolean = false,
    onInitialLmsRefreshSkipConsumed: () -> Unit = {},
    onInitialLmsRefreshForceConsumed: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var showSubmittedBottomSheet by remember { mutableStateOf(false) }
    var showWidgetHelperDialog by remember { mutableStateOf(false) }
    val showOnboardingInitialLoading = remember { mutableStateOf(skipInitialLmsRefresh) }
    val showInitialLmsLoading = showOnboardingInitialLoading.value &&
        viewModel.onboardingInitialRefreshInProgress.value &&
        viewModel.loadedAt.value.isEmpty()
    val fullScreenIntentSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = {},
    )
    LaunchedEffect(homeEntryVersion) {
        if (skipLoadFromMyPageBack) {
            return@LaunchedEffect
        }
        if (!viewModel.shouldRunInitialLoad(homeEntryVersion)) {
            return@LaunchedEffect
        }

        if (context.isNetworkConnected()) {
            val todoData = viewModel.loadTodos(
                forceRefresh = forceInitialLmsRefresh,
                forceLogin = forceInitialLmsRefresh,
                allowRefresh = !skipInitialLmsRefresh,
                showBlockingLoading = !skipInitialLmsRefresh,
                source = RefreshSource.APP_START,
            )
            if (!viewModel.showNetworkError.value && todoData != null) {
                Analytics.viewHome(
                    taskCount = todoData.todos.size,
                    urgentCount = todoData.todos.urgentTodoCount(),
                    entrySource = homeEntrySource,
                )
            }
        } else {
            viewModel.showNetworkErrorScreen()
        }
        if (skipInitialLmsRefresh) {
            onInitialLmsRefreshSkipConsumed()
        }
        if (forceInitialLmsRefresh) {
            onInitialLmsRefreshForceConsumed()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )
    fun refreshTodos(
        showBlockingLoading: Boolean,
        source: RefreshSource,
        captureRefreshEvent: () -> Unit,
    ) {
        captureRefreshEvent()
        coroutine.launch {
            if (context.isNetworkConnected()) {
                viewModel.loadTodos(
                    forceRefresh = true,
                    showBlockingLoading = showBlockingLoading,
                    source = source,
                )
            } else {
                viewModel.showNetworkErrorScreen()
            }
        }
    }


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = WHITE,
        topBar = {
            SSUTimeTopBar(
                modifier = Modifier.statusBarsPadding(),
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        if (viewModel.showNetworkError.value) {
            NetworkErrorFragment(
                modifier = Modifier.padding(innerPadding),
                onRefreshClick = {
                    Analytics.refreshClick()
                    coroutine.launch {
                        if (context.isNetworkConnected()) {
                            viewModel.loadTodos(
                                forceRefresh = true,
                                source = RefreshSource.REFRESH_BUTTON,
                            )
                        } else {
                            viewModel.showNetworkErrorScreen()
                        }
                    }
                },
                errorCause = viewModel.showNetworkCause.value
            )
        } else {
            MainFragment(
                innerPadding = innerPadding,
                todos = viewModel.todos,
                submitted = viewModel.submitted,
                loadedAt = viewModel.loadedAt.value,
                showWidgetBadge = viewModel.showWidgetBadge.value,
                aiSummaryStates = viewModel.aiSummaryStates,
                isRefreshing = viewModel.isLoading.value,
                refreshProgress = viewModel.loadingProgress.value,
                onRefresh = {
                    refreshTodos(
                        showBlockingLoading = false,
                        source = RefreshSource.PULL_TO_REFRESH,
                        captureRefreshEvent = Analytics::pullToRefresh,
                    )
                },
                onClickRefresh = {
                    refreshTodos(
                        showBlockingLoading = true,
                        source = RefreshSource.REFRESH_BUTTON,
                        captureRefreshEvent = Analytics::refreshClick,
                    )
                },
                onClickSubmitted = {
                    showSubmittedBottomSheet = true
                },
                onClickWidgetBadge = {
                    Analytics.widgetBannerClick()
                    showWidgetHelperDialog = true
                },
                onDismissWidgetBadge = {
                    Analytics.widgetBannerDismiss()
                    viewModel.dismissWidgetHelperBadge()
                },
                onExpandTodo = viewModel::loadAiSummary,
            )
        }

        if (showWidgetHelperDialog) {
            Dialog(
                onDismissRequest = {
                    showWidgetHelperDialog = false
                },
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(WHITE)
                        .padding(vertical = 20.dp),
                ) {
                    WidgetHelperDialogContent(
                        onDismissClick = {
                            Analytics.widgetBannerConfirm()
                            showWidgetHelperDialog = false
                            viewModel.dismissWidgetHelperBadge()
                        },
                    )
                }
            }
        }

        if(viewModel.requiredShowAlertBottomSheet.value) {
            CallingAlertBottomSheet(
                onConfirmClick = {
                    Analytics.callAlarmSetting(
                        selectedTime = Analytics.selectedTimeFromMinutes(it) ?: "reject",
                    )
                    val allowSystem = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    var allowCalling = false
                    if  (it > 0)
                        allowCalling = true

                    viewModel.updateAlertState(
                        AlertData(
                            valid = true,
                            allowSystemAlert = allowSystem,
                            allowCallAlert = allowCalling,
                            callingAlertThresholdMinutes = it
                        )
                    )

                    if (allowCalling && !context.canUseFullScreenIntent()) {
                        context.fullScreenIntentSettingsIntent()?.let { intent ->
                            fullScreenIntentSettingsLauncher.launch(intent)
                        }
                    }

                }
            )
        }

        if (showSubmittedBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .fillMaxWidth(),
                containerColor = WHITE,
                onDismissRequest = { showSubmittedBottomSheet = false },
                sheetState = sheetState

            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.main_submitted_title),
                            style = SSUType.H3SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = viewModel.submitted.size.toString(),
                            style = SSUType.H3SemiBold.copy(color = R400)
                        )
                        Spacer(Modifier.weight(1f))
                        Text( //TODO
                            text = if(viewModel.loadedAt.value.isNotEmpty()) {
                                stringResource(R.string.main_date_base, getStringDate(viewModel.loadedAt.value))
                            } else {
                                stringResource(R.string.main_date_placeholder)
                            },
                            style = SSUType.Caption1SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if(viewModel.submitted.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = viewModel.submitted,
                                key = { item -> item.todoId }
                            ) {
                                SubmittedItem(it)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(vertical = 50.dp),
                                text = stringResource(R.string.main_submitted_empty),
                                style = SSUType.H3Medium
                            )
                        }
                    }

                    SButton(
                        modifier = Modifier.fillMaxWidth(),
                        labelText = stringResource(R.string.common_close),
                        onClick = {
                            coroutine.launch {
                                sheetState.hide()
                                showSubmittedBottomSheet = false
                            }
                        }
                    )

                }
            }
        }

        if (showInitialLmsLoading) {
            InitialLmsLoadingOverlay()
        }
    }
}

@Composable
private fun InitialLmsLoadingOverlay() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x80000000)),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            color = R500,
            trackColor = R100,
        )
    }
}

private fun List<TodoInfo>.urgentTodoCount(): Int =
    count { todo -> getRemainingDays(todo.due_date) <= 1 }

@Preview
@Composable
fun NetworkErrorFragment(
    modifier: Modifier = Modifier,
    onRefreshClick: () -> Unit = {},
    errorCause: String = ""
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.network_error_title),
            style = SSUType.H3Medium
        )
        Text(
            textAlign = TextAlign.Center,
            text = stringResource(R.string.network_error_description),
            style = SSUType.Body2Regular
        )

        Spacer(modifier = Modifier.height(10.dp))

        if(errorCause.isNotEmpty()) {
            Text(
                text = stringResource(R.string.network_error_cause, errorCause),
                style = SSUType.Body1Medium,
                color = R400
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable { onRefreshClick() }
                .background(N200)
                .padding(horizontal = 24.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.common_retry),
                style = SSUType.Label2Medium,
            )
        }
    }
}

private fun Context.isNetworkConnected(): Boolean {
    val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val activeNetwork = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

@Composable
@Preview
fun MainFragment(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    todos: List<TodoInfo> = emptyList(),
    submitted: List<TodoInfo> = emptyList(),
    loadedAt: String = "",
    showWidgetBadge: Boolean = false,
    aiSummaryStates: Map<String, AiSummaryUiState> = emptyMap(),
    isRefreshing: Boolean = false,
    refreshProgress: Float = 0f,
    onRefresh: () -> Unit = {},
    onClickRefresh: () -> Unit = {},
    onClickSubmitted: () -> Unit = {},
    onClickWidgetBadge: () -> Unit = {},
    onDismissWidgetBadge: () -> Unit = {},
    onExpandTodo: (TodoInfo) -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    val pullToRefreshState = rememberPullToRefreshState()
    var headerHeightPx by remember { mutableIntStateOf(0) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = pullToRefreshState,
        indicator = {
            LmsRefreshIndicator(
                isRefreshing = isRefreshing,
                progress = refreshProgress,
                state = pullToRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
        ) {
            val headerHeight = with(density) { headerHeightPx.toDp() }
            val emptyStateMinHeight = maxOf(240.dp, maxHeight - headerHeight)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onSizeChanged { headerHeightPx = it.height },
                ) {
                    Spacer(Modifier.height(32.dp))
                    Text(
                        text = stringResource(R.string.main_completed_auto_disappear),
                        style = SSUType.H4SemiBold,
                        color = N500
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = stringResource(R.string.main_todo_count, todos.size),
                        style = SSUType.H1SemiBold
                    )
                    Spacer(Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if(loadedAt.isNotEmpty()) {
                                stringResource(R.string.main_updated_at, getStringSimpleDate(context, loadedAt))
                            } else {
                                stringResource(R.string.main_update_info_none)
                            },
                            style = SSUType.Caption1Medium,
                            color = N500
                        )

                        Spacer(Modifier.width(5.dp))

                        Image(
                            modifier = Modifier
                                .height(13.dp)
                                .clickable { onClickRefresh() },
                            painter = painterResource(R.drawable.refreshbtn),
                            contentDescription = stringResource(R.string.common_refresh)
                        )
                    }

                    Spacer(Modifier.height(18.dp))
                    if(showWidgetBadge)
                        WidgetHelperBadge(
                            onClickBadge = onClickWidgetBadge,
                            onClickDismiss = onDismissWidgetBadge,
                        )
                    Spacer(Modifier.height(12.dp))
                }

                TodoList(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp),
                    todos = todos,
                    aiSummaryStates = aiSummaryStates,
                    emptyStateMinHeight = emptyStateMinHeight,
                    onClickSubmitted = {
                        Analytics.submitCompleteClick()
                        onClickSubmitted()
                    },
                    onExpandTodo = onExpandTodo,
                    submittedSize = submitted.size,
                )
            }
        }
    }
}

@Composable
@Preview
fun WidgetHelperBadge(
    onClickBadge: () -> Unit = {},
    onClickDismiss: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClickBadge() }
            .background(R100)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.checkbox),
                contentDescription = null
            )

            Spacer(Modifier.width(10.dp))

            Column {
                Text(
                    text = stringResource(R.string.main_widget_badge_title),
                    style = SSUType.H4SemiBold,
                    color = R500
                )

                Spacer(Modifier.height(5.dp))

                Text(
                    text = stringResource(R.string.main_widget_badge_description),
                    style = SSUType.Body2Medium,
                )
            }
        }

        Image(
            painter = painterResource(R.drawable.ic_close),
            contentDescription = stringResource(R.string.common_close),
            modifier = Modifier.clickable { onClickDismiss() }
        )
    }
}

@Composable
private fun LmsRefreshIndicator(
    isRefreshing: Boolean,
    progress: Float,
    state: PullToRefreshState,
    modifier: Modifier = Modifier,
) {
    val indicatorProgress = if (isRefreshing) {
        progress.coerceIn(0f, 1f)
    } else {
        state.distanceFraction.coerceIn(0f, 1f)
    }
    val progressText = "${(indicatorProgress * 100).toInt()}%"

    PullToRefreshDefaults.IndicatorBox(
        state = state,
        isRefreshing = isRefreshing,
        modifier = modifier,
        containerColor = WHITE,
        elevation = 6.dp,
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                progress = { indicatorProgress },
                modifier = Modifier.size(36.dp),
                color = R500,
                trackColor = R100,
                strokeWidth = 4.dp,
            )
            Text(
                text = progressText,
                style = SSUType.Caption2SemiBold,
                color = R500,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun TodoList(
    modifier: Modifier = Modifier,
    todos: List<TodoInfo>,
    aiSummaryStates: Map<String, AiSummaryUiState> = emptyMap(),
    emptyStateMinHeight: Dp = 240.dp,
    onClickSubmitted: () -> Unit,
    onExpandTodo: (TodoInfo) -> Unit = {},
    submittedSize: Int
) {
    val sortedTodos = todos.sortedForMainDisplay()
    val immediateTodos = sortedTodos.filter { getRemainingDays(it.due_date) <= 1 }
    val freeTodos = sortedTodos.filter { getRemainingDays(it.due_date) > 1 }

    Column(
        modifier = modifier,
    ) {
        if (immediateTodos.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.main_urgent_tasks_title),
                    style = SSUType.H3SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .border(width = 1.dp, color = N300, shape = RoundedCornerShape(8.dp))
                        .clickable { onClickSubmitted() }
                        .padding(8.dp),
                    text = stringResource(R.string.main_submitted_count, submittedSize),
                    style = SSUType.Caption1SemiBold,
                    color = N500,
                )
            }

            immediateTodos.forEach {
                key(it.todoId) {
                    Spacer(Modifier.height(8.dp))
                    TodoItem(
                        todoInfo = it,
                        aiSummaryState = aiSummaryStates[it.aiSummaryKey()],
                        onExpandTodo = onExpandTodo,
                    )
                }
            }

            if (freeTodos.isNotEmpty()) {
                Spacer(Modifier.height(28.dp))

                Text(
                    text = stringResource(R.string.main_relaxed_tasks_title),
                    style = SSUType.H5SemiBold
                )

                freeTodos.forEach {
                    key(it.todoId) {
                        Spacer(Modifier.height(8.dp))
                        TodoItem(
                            todoInfo = it,
                            aiSummaryState = aiSummaryStates[it.aiSummaryKey()],
                            onExpandTodo = onExpandTodo,
                        )
                    }
                }
            }
        } else if (freeTodos.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.main_relaxed_tasks_title),
                    style = SSUType.H5SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .border(width = 1.dp, color = N300, shape = RoundedCornerShape(8.dp))
                        .clickable { onClickSubmitted() }
                        .padding(8.dp),
                    text = stringResource(R.string.main_submitted_count, submittedSize),
                    style = SSUType.Caption1SemiBold,
                    color = N500,
                )
            }
            freeTodos.forEach {
                key(it.todoId) {
                    Spacer(Modifier.height(8.dp))
                    TodoItem(
                        todoInfo = it,
                        aiSummaryState = aiSummaryStates[it.aiSummaryKey()],
                        onExpandTodo = onExpandTodo,
                    )
                }
            }
        } else {
            val density = LocalDensity.current
            var emptyHeaderHeightPx by remember { mutableIntStateOf(0) }
            val emptyHeaderHeight = with(density) { emptyHeaderHeightPx.toDp() }
            val emptyContentMinHeight = maxOf(180.dp, emptyStateMinHeight - emptyHeaderHeight)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onSizeChanged { emptyHeaderHeightPx = it.height },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.main_todo_list_title),
                    style = SSUType.H3SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .border(width = 1.dp, color = N300, shape = RoundedCornerShape(8.dp))
                        .clickable { onClickSubmitted() }
                        .padding(8.dp),
                    text = stringResource(R.string.main_submitted_count, submittedSize),
                    style = SSUType.Caption1SemiBold,
                    color = N500,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = emptyContentMinHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.main_empty_todos),
                    style = SSUType.H3Medium,
                )
            }
        }
    }
}

@Composable
fun TodoItem(
    todoInfo: TodoInfo,
    aiSummaryState: AiSummaryUiState? = null,
    onExpandTodo: (TodoInfo) -> Unit = {},
) {
    val uriHandler = LocalUriHandler.current
    var expanded by remember { mutableStateOf(false) }
    var isLate by remember { mutableStateOf(false) }

    // 1초마다 갱신되는 기준 시간 상태 (시스템 클럭의 000ms에 맞춰 갱신되도록 보정)
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            // 다음 1초 정각까지 남은 밀리초만큼 대기 (누적 오차 방지)
            val sleepTime = 1000L - (System.currentTimeMillis() % 1000L)
            delay(sleepTime)
        }
    }

    val leftDay = getRemainingDays(todoInfo.due_date, now)
    val remainingSeconds = ChronoUnit.SECONDS.between(
        now,
        todoInfo.due_date.toTodoDeadlineInstant(),
    ).coerceAtLeast(0L)

//    Log.d("리컴포지션", "${todoInfo.todoId} 리컴포지션 발생 (남은시간: ${
//        getRemainingTimeText(
//            todoInfo.due_date,
//            now
//        )
//    })")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp, 50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WHITE),
                    contentAlignment = Alignment.Center
                ) {
                    var res by remember { mutableIntStateOf(-1) }
                    res = when(leftDay) {
                        3L -> R.drawable.day3
                        2L -> R.drawable.day2
                        1L -> R.drawable.day_red2
                        0L -> {
                            val txt = getRemainingTimeText(
                                todoInfo.due_date,
                                now
                            )
                            if(remainingSeconds == 0L) {
                                isLate = true
                                R.drawable.late
                            } else if(remainingSeconds < 60L)
                                R.drawable.seconds
                            else
                                R.drawable.timer
                        }
                        else -> {
                            -1
                        }
                    }

                    if(res > 0)
                        Image(
                            painter = painterResource(res),
                            contentDescription = "day-$res"
                        )

                    if(!isLate) {
                        Text(
                            text = if (leftDay > 0) "D-${leftDay}" else getRemainingTimeText(
                                todoInfo.due_date,
                                now
                            ),
                            style = if (leftDay > 1) SSUType.H4ExtraBold else SSUType.H4ExtraBold.copy(
                                color = WHITE
                            )
                        )
                    }
                }

                Spacer(Modifier.size(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if(!isLate) {
                            Text(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when (todoInfo.type) {
                                            TodoType.COMMONS -> Color(0xFFF7DBF7)
                                            TodoType.QUIZ -> Color(0xFFFFD7C2)
                                            TodoType.ASSIGNMENT -> Color(0xFFD8E5F7)
                                            else -> Color(0xFFF7DBF7)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                text = todoInfo.type.localizedLabel(),
                                style = SSUType.Caption1SemiBold,
                                color = when(todoInfo.type) {
                                    TodoType.COMMONS -> Color(0xFFFF39D0)
                                    TodoType.QUIZ -> Color(0xFFFF5F0B)
                                    TodoType.ASSIGNMENT -> Color(0xFF007BFF)
                                    else -> Color(0xFFFF39D0)
                                }
                            )
                            Spacer(Modifier.width(6.dp))

                            Text(
                                modifier = Modifier.fillMaxWidth(0.9f),
                                maxLines = 1,
                                text = todoInfo.subject?.name ?: stringResource(R.string.common_unknown_subject),
                                style = SSUType.H5SemiBold,
                                color = N500
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.main_late_submission_available),
                                style = SSUType.H5SemiBold.copy(color = R500)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        maxLines = 1,
                        text = todoInfo.title,
                        style = SSUType.H4SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))

                Image(
                    modifier = Modifier.clickable {
                        val nextExpanded = !expanded
                        if (nextExpanded) {
                            Analytics.taskDetailExpand(
                                todo = todoInfo,
                                dDay = leftDay.toInt(),
                                hasAiSummary = aiSummaryState is AiSummaryUiState.Success,
                            )
                            onExpandTodo(todoInfo)
                        } else {
                            Analytics.taskDetailCollapse()
                        }
                        expanded = nextExpanded
                    },
                    painter = if (!expanded) painterResource(R.drawable.icon_collapsed) else painterResource(R.drawable.icon_expand),
                    contentDescription = stringResource(R.string.main_expand_task_content_description)
                )
            }

            AnimatedVisibility(expanded) {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(top = 12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.main_deadline_label),
                            style = SSUType.H5SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(
                                R.string.main_due_until,
                                getStringDateWithTime(todoInfo.due_date),
                            ),
                            style = SSUType.H5SemiBold
                        )
                    }

                    if (todoInfo.canRequestAiSummary()) {
                        AiSummaryBlock(aiSummaryState = aiSummaryState)
                    }
                    Spacer(Modifier.height(5.dp))
                    if(todoInfo.url.startsWith("http")) {
                        Text(
                            text = "브라우저에서 확인하기",
                            style = SSUType.Body1Regular,
                            color = Color.Blue,
                            modifier = Modifier.clickable {
                                uriHandler.openUri(todoInfo.url)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AiSummaryBlock(
    aiSummaryState: AiSummaryUiState?,
) {
    val estimatedDurationText = when (aiSummaryState) {
        is AiSummaryUiState.Success -> aiSummaryState.estimatedDurationMinutes
            ?.takeIf { it > 0 }
            ?.let { stringResource(R.string.ai_estimated_duration, it) }
            ?: stringResource(R.string.ai_estimated_duration_unknown)
        else -> stringResource(R.string.ai_estimated_duration_unknown)
    }

    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WHITE)
            .padding(14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(R.drawable.ai),
                contentDescription = null,
                tint = Color.Black,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.ai_summary_title),
                style = SSUType.H5SemiBold,
                color = Color.Black,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = estimatedDurationText,
                style = SSUType.Caption1SemiBold,
                color = N500,
            )
        }

        Spacer(Modifier.height(12.dp))

        Crossfade(
            targetState = aiSummaryState,
            label = "AiSummaryText",
        ) { state ->
            Text(
                text = when (state) {
                    is AiSummaryUiState.Success -> state.summary
                    AiSummaryUiState.Loading -> stringResource(R.string.ai_summary_loading)
                    AiSummaryUiState.Analyzing -> stringResource(R.string.ai_summary_analyzing)
                    AiSummaryUiState.Empty -> stringResource(R.string.ai_summary_empty)
                    AiSummaryUiState.Error -> stringResource(R.string.ai_summary_error)
                    null -> stringResource(R.string.ai_summary_loading)
                },
                style = SSUType.Body1Medium,
                color = N500,
            )
        }
    }
}

@Composable
fun SubmittedItem(
    todoInfo: TodoInfo
) {
//    Log.d("리컴포지션", "${todoInfo.todoId} 리컴포지션 발생")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            maxLines = 1,
                            text = todoInfo.subject?.name ?: stringResource(R.string.common_unknown_subject),
                            style = SSUType.Caption1SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        maxLines = 1,
                        text = todoInfo.title,
                        style = SSUType.H5SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))

                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (todoInfo.type == TodoType.SUBMITTED_LATE) R100 else G100)
                        .padding(6.dp),
                    text = todoInfo.type.localizedLabel(),
                    style = SSUType.Caption2Medium.copy(color = if(todoInfo.type == TodoType.SUBMITTED_LATE) R400 else G400)
                )
            }
        }
    }
}

@Composable
@Preview
fun SSUTimeTopBar(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier.height(18.dp),
            painter = painterResource(R.drawable.logo_red),
            contentDescription = stringResource(R.string.app_name)
        )

        Spacer(Modifier.weight(1f))

        Image(
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .clickable { onProfileClick() }
            ,
            painter = painterResource(R.drawable.ic_user),
            contentDescription = stringResource(R.string.my_avatar_content_description)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallingAlertBottomSheet(
    onConfirmClick: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        properties = ModalBottomSheetProperties(
            shouldDismissOnClickOutside = false,
            shouldDismissOnBackPress = false,
        ),
        sheetGesturesEnabled = false,
        dragHandle = null,
        onDismissRequest = {},
        sheetState = sheetState,
        containerColor = WHITE,
    ) {
        CallingAlertBody(
            onConfirmClick = onConfirmClick
        )
    }
}

@Composable
@Preview
fun CallingAlertBody(
    modifier: Modifier = Modifier,
    onConfirmClick: (Long) -> Unit = {}
) {

    val radioOptions = listOf(
        stringResource(R.string.call_alert_option_1h),
        stringResource(R.string.call_alert_option_2h),
        stringResource(R.string.call_alert_option_6h),
    )
    val (selectedOption, onOptionSelected) = remember { mutableStateOf("") }

    var enableCallingAlert = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                text = stringResource(R.string.call_alert_setup_title),
                style = SSUType.H2SemiBold
            )
            Text(
                text = stringResource(R.string.call_alert_setup_description),
                style = SSUType.Body1Medium
            )
        }

        Column(
            modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            radioOptions.forEach { text ->
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (text == selectedOption && !enableCallingAlert.value),
                            onClick = {
                                onOptionSelected(text)
                                enableCallingAlert.value = false
                            },
                            role = Role.RadioButton
                        ),
                    labelText = text,
                    selected = (text == selectedOption && !enableCallingAlert.value)
                )
            }
        }
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SCheckBox(
                labelText = stringResource(R.string.call_alert_disable),
                checked = enableCallingAlert,
                onCheckedChanged = {
                    if(it)
                        onOptionSelected("")
                    enableCallingAlert.value = it
                }
            )
        }
        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = stringResource(R.string.common_confirm),
            onClick = {
                onConfirmClick(
                    if(enableCallingAlert.value)
                        -1L
                    else when (radioOptions.indexOf(selectedOption)) {
                        0 -> 60L
                        1 -> 120L
                        else -> 360L
                    }
                )
            },
            enable = enableCallingAlert.value || radioOptions.any { it == selectedOption }
        )
    }
}

private fun Context.canUseFullScreenIntent(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    return getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}

private fun Context.fullScreenIntentSettingsIntent(): Intent? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return null
    }

    val packageUri = Uri.parse("package:$packageName")
    val fullScreenIntentSettings = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = packageUri
    }

    return fullScreenIntentSettings.takeIf { it.resolveActivity(packageManager) != null }
        ?: Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = packageUri
        }.takeIf { it.resolveActivity(packageManager) != null }
}
