package com.yourssu.ssutime.desktop.screen.todo

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.aiSummaryKey
import com.yourssu.ssutime.desktop.core.model.canRequestAiSummary
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.screen.main.DesktopAiSummaryUiState
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.ai_estimated_duration
import com.yourssu.ssutime.desktop.ui.resources.ai_estimated_duration_unknown
import com.yourssu.ssutime.desktop.ui.resources.ai_sparkle
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_analyzing
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_empty
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_error
import com.yourssu.ssutime.desktop.ui.resources.ai_summary_loading
import com.yourssu.ssutime.desktop.ui.resources.common_cancel
import com.yourssu.ssutime.desktop.ui.resources.common_confirm
import com.yourssu.ssutime.desktop.ui.resources.common_unknown_subject
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.main_deadline_label
import com.yourssu.ssutime.desktop.ui.resources.main_due_until
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_ai_sparkle_desc
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_ai_summary_tab
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_back
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_description_tab
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_late_notice
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_lms_link
import com.yourssu.ssutime.desktop.ui.resources.todo_hide_from_list
import com.yourssu.ssutime.desktop.ui.resources.todo_hide_popup_message
import com.yourssu.ssutime.desktop.ui.resources.todo_hide_popup_title
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N300
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.N600
import com.yourssu.ssutime.desktop.ui.theme.R100
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import com.yourssu.ssutime.desktop.ui.util.formatMonthDayWithTime
import com.yourssu.ssutime.desktop.ui.util.remainingSeconds
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

private enum class TodoDetailTab {
    DESCRIPTION,
    AI_SUMMARY,
}

@Composable
fun DesktopTodoDetailScreen(
    todo: AppTodo,
    aiSummaryState: DesktopAiSummaryUiState?,
    onLoadAiSummary: (AppTodo) -> Unit,
    onBack: () -> Unit,
    onHideTodo: (AppTodo) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showHideDialog by remember { mutableStateOf(false) }

    LaunchedEffect(todo.aiSummaryKey()) {
        if (todo.canRequestAiSummary) {
            onLoadAiSummary(todo)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .verticalScroll(scrollState),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onBack)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.todo_detail_back),
                    tint = N600,
                    modifier = Modifier.size(20.dp),
                )
            }

            TodoDetailOverview(
                todo = todo,
                aiSummaryState = aiSummaryState,
                onHideClick = { showHideDialog = true },
            )
        }

        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .background(N100),
        )

        TodoDetailTabSection(
            todo = todo,
            aiSummaryState = aiSummaryState,
            onOpenUrl = {
                val targetUrl = todo.url.ifBlank {
                    todo.subject?.id?.let { "https://smartid.ssu.ac.kr" } ?: "https://smartid.ssu.ac.kr"
                }
                onOpenUrl(targetUrl)
            },
        )
    }

    if (showHideDialog) {
        Dialog(onDismissRequest = { showHideDialog = false }) {
            DesktopHideTodoPopup(
                onCancel = { showHideDialog = false },
                onConfirm = {
                    showHideDialog = false
                    onHideTodo(todo)
                },
            )
        }
    }
}

@Composable
private fun TodoDetailOverview(
    todo: AppTodo,
    aiSummaryState: DesktopAiSummaryUiState?,
    onHideClick: () -> Unit,
) {
    val estimatedDurationText = (aiSummaryState as? DesktopAiSummaryUiState.Success)
        ?.estimatedDurationMinutes
        ?.takeIf { it > 0 }
        ?.let { stringResource(Res.string.ai_estimated_duration, it) }
        ?: stringResource(Res.string.ai_estimated_duration_unknown)

    val seconds = runCatching { remainingSeconds(todo.dueDate) }.getOrDefault(0L)
    val isDeadlinePassed = seconds == 0L

    Box(modifier = Modifier.padding(top = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = todo.subject?.name
                    ?: stringResource(Res.string.common_unknown_subject),
                style = SSUType.H5SemiBold,
                color = N500,
            )

            Text(
                text = todo.title,
                style = SSUType.H2SemiBold,
                color = N600,
            )

            Spacer(modifier = Modifier.height(14.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(color = N200, width = 1.dp, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = stringResource(Res.string.main_deadline_label),
                            style = SSUType.Caption1SemiBold,
                            color = N500,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(
                                Res.string.main_due_until,
                                runCatching {
                                    formatMonthDayWithTime(todo.dueDate)
                                }.getOrDefault(todo.dueDate),
                            ),
                            style = SSUType.Caption1SemiBold,
                            color = N500,
                        )
                    }

                    if (todo.canRequestAiSummary) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(Res.string.ai_estimated_duration),
                                style = SSUType.Caption1SemiBold,
                                color = N500,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = estimatedDurationText,
                                style = SSUType.Caption1SemiBold,
                                color = N500,
                            )
                        }
                    }
                }
            }

            if (todo.submittedAt.isBlank() && isDeadlinePassed) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(R100)
                        .padding(14.dp),
                    textAlign = TextAlign.Center,
                    text = stringResource(Res.string.todo_detail_late_notice),
                    style = SSUType.Caption1SemiBold,
                    color = R400,
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 6.dp, end = 4.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    modifier = Modifier.clickable(onClick = onHideClick),
                    text = stringResource(Res.string.todo_hide_from_list),
                    style = SSUType.Label3Medium,
                    color = N600,
                    textDecoration = TextDecoration.Underline,
                )
            }
        }
    }
}

@Composable
private fun TodoDetailTabSection(
    todo: AppTodo,
    aiSummaryState: DesktopAiSummaryUiState?,
    onOpenUrl: () -> Unit,
) {
    val shouldShowAiSummaryTab = todo.canRequestAiSummary && when (aiSummaryState) {
        is DesktopAiSummaryUiState.Success -> true
        DesktopAiSummaryUiState.Loading, DesktopAiSummaryUiState.Analyzing -> true
        DesktopAiSummaryUiState.Empty, DesktopAiSummaryUiState.Error, null -> false
    }

    val availableTabs = remember(shouldShowAiSummaryTab) {
        if (shouldShowAiSummaryTab) {
            listOf(TodoDetailTab.DESCRIPTION, TodoDetailTab.AI_SUMMARY)
        } else {
            listOf(TodoDetailTab.DESCRIPTION)
        }
    }

    var selectedTabIndex by remember(availableTabs) { mutableIntStateOf(0) }
    val safeIndex = selectedTabIndex.coerceIn(0, availableTabs.lastIndex)
    val currentTab = availableTabs[safeIndex]

    Column(modifier = Modifier.padding(16.dp)) {
        PrimaryTabRow(
            selectedTabIndex = safeIndex,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(safeIndex),
                    color = N500,
                    height = 2.dp,
                )
            },
            divider = {
                HorizontalDivider(color = N200, thickness = 1.dp)
            },
        ) {
            availableTabs.forEachIndexed { index, tab ->
                val isSelected = safeIndex == index
                Tab(
                    modifier = Modifier.background(WHITE),
                    selected = isSelected,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            text = when (tab) {
                                TodoDetailTab.DESCRIPTION -> stringResource(Res.string.todo_detail_description_tab)
                                TodoDetailTab.AI_SUMMARY -> stringResource(Res.string.todo_detail_ai_summary_tab)
                            },
                            style = SSUType.Label2SemiBold,
                            color = if (isSelected) N500 else N300,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (currentTab) {
            TodoDetailTab.DESCRIPTION -> {
                Text(
                    text = todo.description.ifBlank { "상세 설명이 없습니다." },
                    style = SSUType.Body1Regular,
                    color = N600,
                )
            }

            TodoDetailTab.AI_SUMMARY -> {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(N100)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ai_sparkle),
                            contentDescription = null,
                            tint = Color.Unspecified,
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(Res.string.todo_detail_ai_sparkle_desc),
                            style = SSUType.Caption1SemiBold,
                            color = N500,
                        )
                    }

                    Crossfade(targetState = aiSummaryState) { state ->
                        Text(
                            text = when (state) {
                                is DesktopAiSummaryUiState.Success -> state.summary
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
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd,
        ) {
            SButton(
                labelText = stringResource(Res.string.todo_detail_lms_link),
                onClick = onOpenUrl,
            )
        }
    }
}

@Composable
fun DesktopHideTodoPopup(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(320.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WHITE)
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.todo_hide_popup_title),
            style = SSUType.H4SemiBold,
            color = N600,
        )
        Text(
            text = stringResource(Res.string.todo_hide_popup_message),
            style = SSUType.Body1Medium,
            color = N500,
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(N200)
                    .clickable(onClick = onCancel)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.common_cancel),
                    style = SSUType.H5SemiBold,
                    color = N600,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(R400)
                    .clickable(onClick = onConfirm)
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.common_confirm),
                    style = SSUType.H5SemiBold,
                    color = WHITE,
                )
            }
        }
    }
}
