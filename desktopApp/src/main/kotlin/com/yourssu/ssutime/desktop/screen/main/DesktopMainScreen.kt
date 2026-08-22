package com.yourssu.ssutime.desktop.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourssu.data.DiscussionInfo
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoData
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.aiSummaryKey
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.screen.calendar.DesktopCalendarPanel
import com.yourssu.ssutime.desktop.screen.calendar.badgeBackgroundColor
import com.yourssu.ssutime.desktop.screen.calendar.badgeTextColor
import com.yourssu.ssutime.desktop.screen.notice.DesktopNoticeScreen
import com.yourssu.ssutime.desktop.screen.todo.DesktopTodoDetailScreen
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.common_close
import com.yourssu.ssutime.desktop.ui.resources.common_refresh
import com.yourssu.ssutime.desktop.ui.resources.common_retry
import com.yourssu.ssutime.desktop.ui.resources.common_unknown_subject
import com.yourssu.ssutime.desktop.ui.resources.day2
import com.yourssu.ssutime.desktop.ui.resources.day3
import com.yourssu.ssutime.desktop.ui.resources.day_red2
import com.yourssu.ssutime.desktop.ui.resources.done
import com.yourssu.ssutime.desktop.ui.resources.ic_user
import com.yourssu.ssutime.desktop.ui.resources.late
import com.yourssu.ssutime.desktop.ui.resources.logo_red
import com.yourssu.ssutime.desktop.ui.resources.main_completed_auto_disappear
import com.yourssu.ssutime.desktop.ui.resources.main_date_base
import com.yourssu.ssutime.desktop.ui.resources.main_date_placeholder
import com.yourssu.ssutime.desktop.ui.resources.main_empty_todos
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
    onHideTodo: (AppTodo) -> Unit = {},
    onDiscussionExpanded: (DiscussionInfo) -> Unit = {},
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier,
    showBlockingLoading: Boolean = false,
) {
    var showSubmitted by remember { mutableStateOf(false) }
    var selectedTodo by remember { mutableStateOf<AppTodo?>(null) }
    var showingNotice by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val scope = rememberCoroutineScope()

    val unreadNoticeCount = remember(todoData.subjects) {
        todoData.subjects.flatMap { it.discussions }.count {
            it.readState.equals("unread", ignoreCase = true)
        }
    }

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
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                // Left Column: Main Todo List or Todo Detail Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    if (selectedTodo != null) {
                        DesktopTodoDetailScreen(
                            todo = selectedTodo!!,
                            aiSummaryState = aiSummaryStates[selectedTodo!!.aiSummaryKey()],
                            onLoadAiSummary = onExpandTodo,
                            onBack = { selectedTodo = null },
                            onHideTodo = { todo ->
                                selectedTodo = null
                                onHideTodo(todo)
                            },
                            onOpenUrl = onOpenUrl,
                        )
                    } else {
                        HomeTodoListColumn(
                            todoData = todoData,
                            isLoading = isLoading && !showBlockingLoading,
                            loadingProgress = loadingProgress,
                            onRefresh = onRefresh,
                            onSubmittedClick = { showSubmitted = true },
                            onTodoClick = { todo ->
                                selectedTodo = todo
                                onExpandTodo(todo)
                            },
                        )
                    }
                }

                // Vertical Divider between columns
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(N200),
                )

                // Right Column: Calendar Panel or Notice Screen
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                ) {
                    if (showingNotice) {
                        DesktopNoticeScreen(
                            subjects = todoData.subjects,
                            onBack = { showingNotice = false },
                            onOpenUrl = onOpenUrl,
                            onDiscussionExpanded = onDiscussionExpanded,
                        )
                    } else {
                        DesktopCalendarPanel(
                            todos = todoData.todos,
                            noticeCount = unreadNoticeCount,
                            onNoticeClick = { showingNotice = true },
                            onTodoClick = { todo ->
                                selectedTodo = todo
                                onExpandTodo(todo)
                            },
                        )
                    }
                }
            }
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
private fun HomeTodoListColumn(
    todoData: AppTodoData,
    isLoading: Boolean,
    loadingProgress: Float,
    onRefresh: () -> Unit,
    onSubmittedClick: () -> Unit,
    onTodoClick: (AppTodo) -> Unit,
) {
    val density = LocalDensity.current
    val scrollState = rememberScrollState()
    var headerHeightPx by remember { mutableIntStateOf(0) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
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
                Spacer(Modifier.height(16.dp))
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
                Spacer(Modifier.height(16.dp))
            }

            TodoList(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                todos = todoData.todos,
                submittedSize = todoData.submitted.size,
                emptyStateMinHeight = emptyStateMinHeight,
                onClickSubmitted = onSubmittedClick,
                onTodoClick = onTodoClick,
            )
        }
    }
}

@Composable
private fun TodoList(
    todos: List<AppTodo>,
    submittedSize: Int,
    emptyStateMinHeight: Dp,
    onClickSubmitted: () -> Unit,
    onTodoClick: (AppTodo) -> Unit,
    modifier: Modifier = Modifier,
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
                TodoItemRow(
                    todo = todo,
                    onClick = { onTodoClick(todo) },
                )
            }
        }

        if (immediate.isNotEmpty() && relaxed.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text(
                text = stringResource(Res.string.main_relaxed_tasks_title),
                style = SSUType.H5SemiBold,
            )
        }

        relaxed.forEach { todo ->
            key(todo.aiSummaryKey()) {
                Spacer(Modifier.height(8.dp))
                TodoItemRow(
                    todo = todo,
                    onClick = { onTodoClick(todo) },
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
private fun TodoItemRow(
    todo: AppTodo,
    onClick: () -> Unit,
) {
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
            .background(N100)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
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
                            overflow = TextOverflow.Ellipsis,
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
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun TodoTypeBadge(type: AppTodoType) {
    Text(
        text = type.localizedLabel(),
        style = SSUType.Caption1SemiBold,
        color = type.badgeTextColor(),
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(type.badgeBackgroundColor())
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
private fun SsuTimeTopBar(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo_red),
            contentDescription = "SSUTime",
            modifier = Modifier.height(28.dp),
        )
        Spacer(Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(onClick = onProfileClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(Res.drawable.ic_user),
                contentDescription = stringResource(Res.string.my_avatar_content_description),
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
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
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(Res.string.network_error_title),
            style = SSUType.H3Medium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(Res.string.network_error_description),
            style = SSUType.Body2Regular,
            textAlign = TextAlign.Center,
            color = N500,
        )
        if (errorCause.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(Res.string.network_error_cause, errorCause),
                style = SSUType.Body1Medium,
                color = R400,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(Modifier.height(20.dp))
        SButton(
            labelText = stringResource(Res.string.common_retry),
            onClick = onRetry,
        )
    }
}
