package com.yourssu.ssutime.v2.screen.main.todo

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.component.SButton_Small
import com.yourssu.ssutime.v2.getStringDateWithTime
import com.yourssu.ssutime.v2.screen.my.PopupButton
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstant
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.N600
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant

@Composable
fun TodoDetailScreen(
    modifier: Modifier = Modifier,
    viewModel: TodoDetailViewModel = koinViewModel(),
    onPreviousClick: () -> Unit = {},
    todo: TodoInfo,
) {
    var showHidePopup by remember { mutableStateOf(false) }

    if (showHidePopup) {
        Dialog(onDismissRequest = { showHidePopup = false }) {
            HideTodoPopup(
                onCancel = { showHidePopup = false },
                onConfirm = {
                    showHidePopup = false
                    viewModel.hideTodo(todo) {
                        onPreviousClick()
                    }
                }
            )
        }
    }

    TodoDetailContent(
        modifier = modifier,
        aiSummaryState = viewModel.aiSummaryState,
        onLoadAiSummary = { viewModel.loadAiSummary(todo) },
        onPreviousClick = onPreviousClick,
        onHideClick = { showHidePopup = true },
        todo = todo
    )
}

@Composable
fun TodoDetailContent(
    modifier: Modifier = Modifier,
    aiSummaryState: AiSummaryUiState?,
    onLoadAiSummary: () -> Unit,
    onPreviousClick: () -> Unit,
    onHideClick: () -> Unit = {},
    todo: TodoInfo,
) {
    LaunchedEffect(todo.aiSummaryKey()) {
        onLoadAiSummary()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Icon(
                modifier = Modifier.clickable { onPreviousClick() },
                imageVector = Icons.Filled.ArrowBackIosNew,
                contentDescription = "뒤로가기"
            )

            TodoOverView(
                todo = todo,
                aiSummaryState = aiSummaryState,
                onHideClick = onHideClick,
            )
        }
        Spacer(Modifier.fillMaxWidth().height(12.dp).background(color = N100))

        TodoDetailTabArea(
            todo = todo,
            aiSummaryState = aiSummaryState,
        )

        HorizontalDivider(color = N200)
    }

}

@Composable
fun TodoDetailTabArea(
    todo: TodoInfo,
    aiSummaryState: AiSummaryUiState?,
) {
    val shouldShowAiSummaryTab = todo.canRequestAiSummary() && when (aiSummaryState) {
        is AiSummaryUiState.Success -> true
        AiSummaryUiState.Loading, AiSummaryUiState.Analyzing -> true
        AiSummaryUiState.Empty, AiSummaryUiState.Error, null -> false
    }

    val availableTabs = remember(shouldShowAiSummaryTab) {
        if (shouldShowAiSummaryTab) {
            listOf(TodoDetailTab.DESCRIPTION, TodoDetailTab.AI_SUMMARY)
        } else {
            listOf(TodoDetailTab.DESCRIPTION)
        }
    }

    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        var selectedDestination by rememberSaveable { mutableIntStateOf(0) }
        val safeIndex = selectedDestination.coerceIn(0, availableTabs.lastIndex)
        val currentTab = availableTabs[safeIndex]

        PrimaryTabRow(
            selectedTabIndex = safeIndex,
            indicator = {
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(safeIndex),
                    color = N500,
                    height = 2.dp
                )
            },
        ) {
            availableTabs.forEachIndexed { index, destination ->
                val isSelected = safeIndex == index
                Tab(
                    modifier = Modifier.background(WHITE),
                    selected = isSelected,
                    unselectedContentColor = WHITE,
                    selectedContentColor = WHITE,
                    onClick = {
                        selectedDestination = index
                    },
                    text = {
                        Text(
                            text = destination.label,
                            style = SSUType.Label2SemiBold.copy(color = if (isSelected) N500 else N300),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                )
            }
        }
        TodoDetailTabContent(
            destination = currentTab,
            todo = todo,
            aiSummaryState = aiSummaryState,
        )
    }
}

@Composable
fun DescriptionScreen(
    description: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = description,
            style = SSUType.Body1Regular
        )
    }
}

@Composable
fun SummaryScreen(
    aiSummaryState: AiSummaryUiState? = null,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(N100)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ai_sparkle),
                contentDescription = "AI 요약 아이콘"
            )
            Text(
                text = "첨부된 파일 내용을 ai가 요약했어요.",
                style = SSUType.Caption1SemiBold.copy(color = N500)
            )
        }

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

/**
 * 할일 유형별 LMS 상세 페이지 URL 생성
 */
fun TodoInfo.toLmsUrl(): String {
    val currentSubjectId = subject?.id ?: subjectId
    return when (type) {
        TodoType.QUIZ -> "https://canvas.ssu.ac.kr/courses/$currentSubjectId/quizzes/$componentId"
        TodoType.ASSIGNMENT -> "https://canvas.ssu.ac.kr/courses/$currentSubjectId/assignments/$todoId"
        TodoType.COMMONS -> "https://canvas.ssu.ac.kr/courses/$currentSubjectId/modules/items/$moduleItemId"
        else -> url.ifBlank { "https://canvas.ssu.ac.kr/courses/$currentSubjectId" }
    }
}

/**
 * LMS 외부 브라우저 이동
 */
fun openTodoLmsUrl(context: Context, todo: TodoInfo) {
    val lmsUrl = todo.toLmsUrl()
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(lmsUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "LMS 링크를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun TodoDetailTabContent(
    destination: TodoDetailTab,
    todo: TodoInfo,
    aiSummaryState: AiSummaryUiState?,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.padding(16.dp),
    ) {
        when (destination) {
            TodoDetailTab.DESCRIPTION -> DescriptionScreen(todo.description)
            TodoDetailTab.AI_SUMMARY -> SummaryScreen(aiSummaryState)
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.TopEnd
        ) {
            SButton_Small(
                labelText = "LMS 바로가기",
                textStyle = SSUType.Label3Medium,
                onClick = {
                    openTodoLmsUrl(context, todo)
                }
            )
        }
    }
}

@Preview
@Composable
fun previewSummaryScreen() {
    SummaryScreen()
}

/**
 * 영상(초 단위) 재생 시간 포맷팅
 */
fun formatVideoDuration(secondsDouble: Double): String {
    val totalSeconds = secondsDouble.toInt()
    if (totalSeconds <= 0) return ""
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60

    return buildString {
        if (hours > 0) {
            append("${hours}시간")
        }
        if (minutes > 0) {
            if (isNotEmpty()) append(" ")
            append("${minutes}분")
        }
        if (seconds > 0 || isEmpty()) {
            if (isNotEmpty()) append(" ")
            append("${seconds}초")
        }
    }
}

@Composable
fun TodoOverView(
    todo: TodoInfo,
    aiSummaryState: AiSummaryUiState?,
    onHideClick: () -> Unit = {},
) {
    val estimatedDurationText = when {
        todo.type == TodoType.COMMONS && todo.duration > 0 -> {
            formatVideoDuration(todo.duration)
        }
        else -> {
            (aiSummaryState as? AiSummaryUiState.Success)
                ?.estimatedDurationMinutes
                ?.takeIf { it > 0 }
                ?.let { stringResource(R.string.ai_estimated_duration_minutes, it) }
                ?: stringResource(R.string.ai_estimated_duration_value_unknown)
        }
    }

    Box(
        modifier = Modifier
            .padding(top = 20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = todo.subject?.name ?: stringResource(R.string.common_unknown_subject),
                style = SSUType.H5SemiBold.copy(color = N500),
            )

            Text(
                text = todo.title,
                style = SSUType.H2SemiBold.copy(color = N600),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .border(color = N200, width = 1.dp, shape = RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row {
                        Text(
                            text = stringResource(R.string.main_deadline_label),
                            style = SSUType.Caption1SemiBold.copy(N500)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = stringResource(
                                R.string.main_due_until,
                                getStringDateWithTime(todo.due_date),
                            ),
                            style = SSUType.Caption1SemiBold.copy(N500)
                        )
                    }

                    Row {
                        Text(
                            text = stringResource(R.string.ai_estimated_duration),
                            style = SSUType.Caption1SemiBold.copy(N500)
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = estimatedDurationText,
                            style = SSUType.Caption1SemiBold.copy(N500)
                        )
                    }
                }
            }

            if(todo.submittedAt.isBlank() && todo.due_date.toTodoDeadlineInstant().isBefore(Instant.now())) {
                Text(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(R100)
                        .padding(14.dp),
                    textAlign = TextAlign.Center,
                    text = "마감이 지났지만 지각 제출이 가능한 과제에요!",
                    style = SSUType.Caption1SemiBold.copy(color = R400)
                )
            }

            Box(
                modifier = Modifier
                    .padding(top = 5.dp, end = 5.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    modifier = Modifier
                        .clickable { onHideClick() },
                    text = stringResource(R.string.todo_hide_from_list),
                    style = SSUType.Label3Medium.copy(N600),
                    textDecoration = TextDecoration.Underline
                )
            }

        }
    }
}

@Composable
fun HideTodoPopup(
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {},
) {
    Column(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WHITE)
            .padding(top = 18.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.todo_hide_popup_title),
            style = SSUType.H4SemiBold
        )
        Text(
            text = stringResource(R.string.todo_hide_popup_message),
            style = SSUType.Body1Medium,
            textAlign = TextAlign.Center
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PopupButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_cancel),
                color = N200,
                onClick = onCancel
            )
            PopupButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_confirm),
                color = R400,
                textColor = WHITE,
                onClick = onConfirm
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun previewTodoDetailScreen() {
    TodoDetailContent(
        aiSummaryState = AiSummaryUiState.Success(
            summary = "이 과제는 컴퓨터개론의 레포트 과제로, 수업시간에 다루었던 데이터와 케이스를 중심으로 개념과 해석을 정리해야 합니다. 계산기가 필요할 수 있습니다.",
            estimatedDurationMinutes = 30
        ),
        onLoadAiSummary = {},
        onPreviousClick = {},
        todo = TodoInfo(
            todoId = 12345,
            title = "컴퓨터 Report",
            due_date = "2026-05-29T18:00:00Z",
            type = TodoType.QUIZ,
            subject = SubjectInfo(123456, "컴퓨터개론", "유어슈"),
            submittedAt = "",
            aiSummary = "",
            description = "여러분께,\n" +
                    "내일 중간시험 관련해서 알려드립니다\n" +
                    "\n" +
                    "말씀 드린데로 수업시간에 다루었던 데이터와 케이스를 중심으로 개념, 해석 및 등이 출제될 예정입니다.\n" +
                    "\n" +
                    "복잡한 수식 관련 문제는 출제하지 않았지만 간단한 계산을 위해서는 계산기가 필요할 수 있습니다",
            url = "https://naver.com"
        )
    )
}
