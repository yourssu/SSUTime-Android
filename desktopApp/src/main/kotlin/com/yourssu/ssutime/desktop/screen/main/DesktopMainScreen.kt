package com.yourssu.ssutime.desktop.screen.main

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.aiSummaryKey
import com.yourssu.ssutime.desktop.core.model.canRequestAiSummary
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.ai
import com.yourssu.ssutime.desktop.ui.resources.ai_estimated_duration
import com.yourssu.ssutime.desktop.ui.resources.ai_estimated_duration_unknown
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_analyzing
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_empty
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_error
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_loading
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_title
import com.yourssu.ssutime.desktop.ui.resources.app_name
import com.yourssu.ssutime.desktop.ui.resources.common_close
import com.yourssu.ssutime.desktop.ui.resources.common_refresh
import com.yourssu.ssutime.desktop.ui.resources.common_retry
import com.yourssu.ssutime.desktop.ui.resources.common_unknown_subject
import com.yourssu.ssutime.desktop.ui.resources.day2
import com.yourssu.ssutime.desktop.ui.resources.day3
import com.yourssu.ssutime.desktop.ui.resources.day_red2
import com.yourssu.ssutime.desktop.ui.resources.done
import com.yourssu.ssutime.desktop.ui.resources.ic_user
import com.yourssu.ssutime.desktop.ui.resources.icon_collapsed
import com.yourssu.ssutime.desktop.ui.resources.icon_expand
import com.yourssu.ssutime.desktop.ui.resources.late
import com.yourssu.ssutime.desktop.ui.resources.logo_red
import com.yourssu.ssutime.desktop.ui.resources.main_completed_auto_disappear
import com.yourssu.ssutime.desktop.ui.resources.main_date_base
import com.yourssu.ssutime.desktop.ui.resources.main_date_placeholder
import com.yourssu.ssutime.desktop.ui.resources.main_deadline_label
import com.yourssu.ssutime.desktop.ui.resources.main_due_until
import com.yourssu.ssutime.desktop.ui.resources.main_empty_todos
import com.yourssu.ssutime.desktop.ui.resources.main_expand_task_content_description
import com.yourssu.ssutime.desktop.ui.resources.main_late_submission_available
import com.yourssu.ssutime.desktop.ui.resources.main_relaxed_tasks_title
import com.yourssu.ssutime.desktop.ui.resources.main_submitted_count
import com.yourssu.ssutime.desktop.ui.resources.main_submitted_empty
import com.yourssu.ssutime.desktop.ui.resources.main_submitted_title
import com.yourssu.ssutime.desktop.ui.resources.main_todo_count
import com.yourssu.ssutime.desktop.ui.resources.main_todo_list_title
import com.yourssu.ssutime.desktop.ui.resources.main_update_info_none
import com.yourssu.ssutime.desktop.ui.resources.main_updated_at
import com.yourssu.ssutime.desktop.ui.resources.main_urgent_tasks_title
import com.yourssu.ssutime.desktop.ui.resources.my_avatar_content_description
import com.yourssu.ssutime.desktop.ui.resources.network_error_cause
import com.yourssu.ssutime.desktop.ui.resources.network_error_description
import com.yourssu.ssutime.desktop.ui.resources.network_error_title
import com.yourssu.ssutime.desktop.ui.resources.refreshbtn
import com.yourssu.ssutime.desktop.ui.resources.seconds
import com.yourssu.ssutime.desktop.ui.resources.timer
import com.yourssu.ssutime.desktop.ui.resources.todo_type_assignment
import com.yourssu.ssutime.desktop.ui.resources.todo_type_lecture
import com.yourssu.ssutime.desktop.ui.resources.todo_type_quiz
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted_late
import com.yourssu.ssutime.desktop.ui.theme.G100
import com.yourssu.ssutime.desktop.ui.theme.G400
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N300
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.R100
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import com.yourssu.ssutime.desktop.ui.util.currentEpochMilliseconds
import com.yourssu.ssutime.desktop.ui.util.formatMonthDay
import com.yourssu.ssutime.desktop.ui.util.formatMonthDayWithTime
import com.yourssu.ssutime.desktop.ui.util.formatUpdatedTime
import com.yourssu.ssutime.desktop.ui.util.remainingDays
import com.yourssu.ssutime.desktop.ui.util.remainingSeconds
import com.yourssu.ssutime.desktop.ui.util.remainingTimeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

sealed interface DesktopAiSummaryUiState {
    data object Loading : DesktopAiSummaryUiState
    data object Analyzing : DesktopAiSummaryUiState
    data object Empty : DesktopAiSummaryUiState
    data class Success(
        val summary: String,
        val estimatedDurationMinutes: Int?,
    ) : DesktopAiSummaryUiState
    data object Error : DesktopAiSummaryUiState
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMainScreen(
    todoData: AppTodoData,
    aiSummaryStates: Map<String, DesktopAiSummaryUiState>,
    isLoading: Boolean,
    loadingProgress: Float,
    errorMessage: String?,
    onRefresh: () -> Unit,
    onProfileClick: () -> Unit,
    onExpandTodo: (AppTodo) -> Unit,
    modifier: Modifier = Modifier,
    showBlockingLoading: Boolean = false,
    onSubmittedClick: () -> Unit = {},
) {
    var showSubmitted by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = WHITE,
        topBar = {
            SsuTimeTopBar(onProfileClick = onProfileClick)
        },
    ) { innerPadding ->
        if (errorMessage != null) {
            NetworkErrorContent(
                modifier = Modifier.padding(innerPadding),
                errorCause = errorMessage,
                onRetry = onRefresh,
            )
        } else {
            HomeList(
                innerPadding = innerPadding,
                todoData = todoData,
                aiSummaryStates = aiSummaryStates,
                isLoading = isLoading && !showBlockingLoading,
                loadingProgress = loadingProgress,
                onRefresh = onRefresh,
                onSubmittedClick = {
                    onSubmittedClick()
                    showSubmitted = true
                },
                onExpandTodo = onExpandTodo,
            )
        }

        if (showSubmitted) {
            ModalBottomSheet(
                modifier = Modifier.fillMaxWidth(),
                containerColor = WHITE,
                onDismissRequest = { showSubmitted = false },
                sheetState = sheetState,
            ) {
                SubmittedSheet(
                    todoData = todoData,
                    onClose = {
                        scope.launch {
                            sheetState.hide()
                            showSubmitted = false
                        }
                    },
                )
            }
        }

        if (showBlockingLoading) {
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
    }
}

@Composable
private fun HomeList(
    innerPadding: PaddingValues,
    todoData: AppTodoData,
    aiSummaryStates: Map<String, DesktopAiSummaryUiState>,
    isLoading: Boolean,
    loadingProgress: Float,
    onRefresh: () -> Unit,
    onSubmittedClick: () -> Unit,
    onExpandTodo: (AppTodo) -> Unit,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
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
                    text = stringResource(Res.string.main_completed_auto_disappear),
                    style = SSUType.H4SemiBold,
                    color = N500,
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(
                        Res.string.main_todo_count,
                        todoData.todos.size,
                    ),
                    style = SSUType.H1SemiBold,
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (todoData.loadedAt.isNotBlank()) {
                            stringResource(
                                Res.string.main_updated_at,
                                runCatching {
                                    formatUpdatedTime(todoData.loadedAt)
                                }.getOrDefault("-"),
                            )
                        } else {
                            stringResource(Res.string.main_update_info_none)
                        },
                        style = SSUType.Caption1Medium,
                        color = N500,
                    )
                    Spacer(Modifier.width(5.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(enabled = !isLoading, onClick = onRefresh),
                        contentAlignment = Alignment.Center,
                    ) {
                        Image(
                            painter = painterResource(Res.drawable.refreshbtn),
                            contentDescription = stringResource(Res.string.common_refresh),
                            modifier = Modifier.size(13.dp),
                        )
                    }
                    if (isLoading) {
                        Spacer(Modifier.width(7.dp))
                        CircularProgressIndicator(
                            progress = { loadingProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.size(18.dp),
                            color = R500,
                            trackColor = R100,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "${(loadingProgress.coerceIn(0f, 1f) * 100).toInt()}%",
                            style = SSUType.Caption2SemiBold,
                            color = R500,
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                Spacer(Modifier.height(12.dp))
            }

            TodoList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                todos = todoData.todos,
                submittedSize = todoData.submitted.size,
                aiSummaryStates = aiSummaryStates,
                emptyStateMinHeight = emptyStateMinHeight,
                onClickSubmitted = onSubmittedClick,
                onExpandTodo = onExpandTodo,
            )
        }
    }
}

@Composable
private fun TodoList(
    modifier: Modifier = Modifier,
    todos: List<AppTodo>,
    submittedSize: Int,
    aiSummaryStates: Map<String, DesktopAiSummaryUiState>,
    emptyStateMinHeight: Dp,
    onClickSubmitted: () -> Unit,
    onExpandTodo: (AppTodo) -> Unit,
) {
    val sortedTodos = remember(todos) {
        todos.sortedWith(
            compareBy<AppTodo> { todo ->
                runCatching { remainingSeconds(todo.dueDate) }.getOrDefault(Long.MAX_VALUE)
            }.thenBy { it.subject?.name.orEmpty() }
                .thenBy(AppTodo::title),
        )
    }
    val immediate = sortedTodos.filter {
        runCatching { remainingDays(it.dueDate) <= 1L }.getOrDefault(false)
    }
    val relaxed = sortedTodos - immediate.toSet()

    Column(modifier) {
        TodoListHeader(
            title = when {
                immediate.isNotEmpty() -> stringResource(Res.string.main_urgent_tasks_title)
                relaxed.isNotEmpty() -> stringResource(Res.string.main_relaxed_tasks_title)
                else -> stringResource(Res.string.main_todo_list_title)
            },
            submittedSize = submittedSize,
            onClickSubmitted = onClickSubmitted,
            emphasized = immediate.isNotEmpty() || todos.isEmpty(),
        )

        immediate.forEach { todo ->
            key(todo.aiSummaryKey()) {
                Spacer(Modifier.height(8.dp))
                TodoItem(
                    todo = todo,
                    aiSummaryState = aiSummaryStates[todo.aiSummaryKey()],
                    onExpand = onExpandTodo,
                )
            }
        }

        if (immediate.isNotEmpty() && relaxed.isNotEmpty()) {
            Spacer(Modifier.height(28.dp))
            Text(
                text = stringResource(Res.string.main_relaxed_tasks_title),
                style = SSUType.H5SemiBold,
            )
        }

        relaxed.forEach { todo ->
            key(todo.aiSummaryKey()) {
                Spacer(Modifier.height(8.dp))
                TodoItem(
                    todo = todo,
                    aiSummaryState = aiSummaryStates[todo.aiSummaryKey()],
                    onExpand = onExpandTodo,
                )
            }
        }

        if (todos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = emptyStateMinHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.done),
                    contentDescription = null,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(Res.string.main_empty_todos),
                    style = SSUType.H3Medium,
                )
            }
        }
    }
}

@Composable
private fun TodoListHeader(
    title: String,
    submittedSize: Int,
    onClickSubmitted: () -> Unit,
    emphasized: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = if (emphasized) SSUType.H3SemiBold else SSUType.H5SemiBold,
        )
        Spacer(Modifier.weight(1f))
        Text(
            text = stringResource(Res.string.main_submitted_count, submittedSize),
            style = SSUType.Caption1SemiBold,
            color = N500,
            modifier = Modifier
                .border(1.dp, N300, RoundedCornerShape(8.dp))
                .clickable(onClick = onClickSubmitted)
                .padding(8.dp),
        )
    }
}

@Composable
private fun TodoItem(
    todo: AppTodo,
    aiSummaryState: DesktopAiSummaryUiState?,
    onExpand: (AppTodo) -> Unit,
) {
    var expanded by remember(todo.aiSummaryKey()) { mutableStateOf(false) }
    val now by produceState(initialValue = currentEpochMilliseconds()) {
        while (true) {
            value = currentEpochMilliseconds()
            delay(1_000L - (value % 1_000L))
        }
    }
    val seconds = runCatching { remainingSeconds(todo.dueDate, now) }.getOrDefault(0L)
    val days = runCatching { remainingDays(todo.dueDate, now) }.getOrDefault(0L)
    val isLate = seconds == 0L
    val deadlineBackground: DrawableResource? = when (days) {
        3L -> Res.drawable.day3
        2L -> Res.drawable.day2
        1L -> Res.drawable.day_red2
        0L -> when {
            isLate -> Res.drawable.late
            seconds < 60L -> Res.drawable.seconds
            else -> Res.drawable.timer
        }
        else -> null
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100),
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WHITE),
                    contentAlignment = Alignment.Center,
                ) {
                    deadlineBackground?.let { background ->
                        Image(
                            painter = painterResource(background),
                            contentDescription = null,
                        )
                    }
                    if (!isLate) {
                        Text(
                            text = if (days > 0L) {
                                "D-$days"
                            } else {
                                remainingTimeText(todo.dueDate, now)
                            },
                            style = if (days > 1L) {
                                SSUType.H4ExtraBold
                            } else {
                                SSUType.H4ExtraBold.copy(color = WHITE)
                            },
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    if (!isLate) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TodoTypeBadge(todo.type)
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = todo.subject?.name
                                    ?: stringResource(Res.string.common_unknown_subject),
                                style = SSUType.H5SemiBold,
                                color = N500,
                                maxLines = 1,
                            )
                        }
                    } else {
                        Text(
                            text = stringResource(Res.string.main_late_submission_available),
                            style = SSUType.H5SemiBold.copy(color = R500),
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = todo.title,
                        style = SSUType.H4SemiBold,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            expanded = !expanded
                            if (expanded) onExpand(todo)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Image(
                        painter = painterResource(
                            if (expanded) {
                                Res.drawable.icon_expand
                            } else {
                                Res.drawable.icon_collapsed
                            },
                        ),
                        contentDescription = stringResource(
                            Res.string.main_expand_task_content_description,
                        ),
                    )
                }
            }

            AnimatedVisibility(expanded) {
                Column {
                    Row(Modifier.padding(top = 12.dp)) {
                        Text(
                            text = stringResource(Res.string.main_deadline_label),
                            style = SSUType.H5SemiBold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(
                                Res.string.main_due_until,
                                runCatching {
                                    formatMonthDayWithTime(todo.dueDate)
                                }.getOrDefault(todo.dueDate),
                            ),
                            style = SSUType.H5SemiBold,
                        )
                    }
                    if (todo.canRequestAiSummary) {
                        AiSummaryBlock(aiSummaryState)
                    }
                }
            }
        }
    }
}

@Composable
private fun TodoTypeBadge(type: AppTodoType) {
    val background = when (type) {
        AppTodoType.COMMONS -> Color(0xFFF7DBF7)
        AppTodoType.QUIZ -> Color(0xFFFFD7C2)
        AppTodoType.ASSIGNMENT -> Color(0xFFD8E5F7)
        else -> Color(0xFFF7DBF7)
    }
    val foreground = when (type) {
        AppTodoType.COMMONS -> Color(0xFFFF39D0)
        AppTodoType.QUIZ -> Color(0xFFFF5F0B)
        AppTodoType.ASSIGNMENT -> Color(0xFF007BFF)
        else -> Color(0xFFFF39D0)
    }
    Text(
        text = type.localizedLabel(),
        style = SSUType.Caption1SemiBold,
        color = foreground,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(background)
            .padding(horizontal = 6.dp, vertical = 3.dp),
    )
}

@Composable
private fun AppTodoType.localizedLabel(): String = stringResource(
    when (this) {
        AppTodoType.COMMONS -> Res.string.todo_type_lecture
        AppTodoType.ASSIGNMENT -> Res.string.todo_type_assignment
        AppTodoType.QUIZ -> Res.string.todo_type_quiz
        AppTodoType.SUBMITTED -> Res.string.todo_type_submitted
        AppTodoType.SUBMITTED_LATE -> Res.string.todo_type_submitted_late
    },
)

@Composable
private fun AiSummaryBlock(state: DesktopAiSummaryUiState?) {
    val estimatedDuration = (state as? DesktopAiSummaryUiState.Success)
        ?.estimatedDurationMinutes
        ?.takeIf { it > 0 }
        ?.let { stringResource(Res.string.ai_estimated_duration, it) }
        ?: stringResource(Res.string.ai_estimated_duration_unknown)

    Column(
        modifier = Modifier
            .padding(top = 14.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(WHITE)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(Res.drawable.ai),
                contentDescription = null,
                tint = Color.Black,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.ai_summary_title),
                style = SSUType.H5SemiBold,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = estimatedDuration,
                style = SSUType.Caption1SemiBold,
                color = N500,
            )
        }
        Spacer(Modifier.height(12.dp))
        Crossfade(targetState = state) { current ->
            Text(
                text = when (current) {
                    is DesktopAiSummaryUiState.Success -> current.summary
                    DesktopAiSummaryUiState.Loading -> stringResource(Res.string.ai_summary_loading)
                    DesktopAiSummaryUiState.Analyzing -> stringResource(Res.string.ai_summary_analyzing)
                    DesktopAiSummaryUiState.Empty -> stringResource(Res.string.ai_summary_empty)
                    DesktopAiSummaryUiState.Error -> stringResource(Res.string.ai_summary_error)
                    null -> stringResource(Res.string.ai_summary_loading)
                },
                style = SSUType.Body1Medium,
                color = N500,
            )
        }
    }
}

@Composable
private fun SubmittedSheet(
    todoData: AppTodoData,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.72f)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.main_submitted_title),
                style = SSUType.H3SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = todoData.submitted.size.toString(),
                style = SSUType.H3SemiBold.copy(color = R400),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = if (todoData.loadedAt.isNotBlank()) {
                    stringResource(
                        Res.string.main_date_base,
                        runCatching {
                            formatMonthDay(todoData.loadedAt)
                        }.getOrDefault("-"),
                    )
                } else {
                    stringResource(Res.string.main_date_placeholder)
                },
                style = SSUType.Caption1SemiBold,
            )
        }
        Spacer(Modifier.height(16.dp))
        if (todoData.submitted.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.main_submitted_empty),
                    style = SSUType.H3Medium,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(
                    items = todoData.submitted,
                    key = AppTodo::aiSummaryKey,
                ) { todo ->
                    SubmittedItem(todo)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = stringResource(Res.string.common_close),
            onClick = onClose,
        )
    }
}

@Composable
private fun SubmittedItem(todo: AppTodo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = todo.subject?.name
                    ?: stringResource(Res.string.common_unknown_subject),
                style = SSUType.Caption1SemiBold,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = todo.title,
                style = SSUType.H5SemiBold,
                maxLines = 1,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = todo.type.localizedLabel(),
            style = SSUType.Caption2Medium.copy(
                color = if (todo.type == AppTodoType.SUBMITTED_LATE) R400 else G400,
            ),
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                    if (todo.type == AppTodoType.SUBMITTED_LATE) R100 else G100,
                )
                .padding(6.dp),
        )
    }
}

@Composable
private fun SsuTimeTopBar(onProfileClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo_red),
            contentDescription = stringResource(Res.string.app_name),
            modifier = Modifier.height(18.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_user),
                contentDescription = stringResource(
                    Res.string.my_avatar_content_description,
                ),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun NetworkErrorContent(
    errorCause: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.network_error_title),
            style = SSUType.H3Medium,
        )
        Text(
            text = stringResource(Res.string.network_error_description),
            style = SSUType.Body2Regular,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(Res.string.network_error_cause, errorCause),
            style = SSUType.Body1Medium,
            color = R400,
        )
        Spacer(Modifier.height(20.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(onClick = onRetry)
                .background(N200)
                .padding(horizontal = 24.dp, vertical = 8.dp),
        ) {
            Text(
                text = stringResource(Res.string.common_retry),
                style = SSUType.Label2Medium,
            )
        }
    }
}
