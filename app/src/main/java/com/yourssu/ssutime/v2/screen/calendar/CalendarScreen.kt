package com.yourssu.ssutime.v2.screen.calendar

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.component.SSUTimeTopBar
import com.yourssu.ssutime.v2.screen.main.MainViewModel
import com.yourssu.ssutime.v2.screen.notice.NoticeScreen
import com.yourssu.ssutime.v2.todo.TODO_DEADLINE_ZONE_ID
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstantOrNull
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N400
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.N600
import com.yourssu.ssutime.v2.ui.theme.N700
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import org.koin.compose.viewmodel.koinViewModel
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

private const val CALENDAR_MAIN_ROUTE = "calendar_main"
private const val CALENDAR_NOTICE_ROUTE = "calendar_notice"

/**
 * TodoType별 뱃지 배경색 매핑 (MainScreen 기준)
 */
fun TodoType.badgeBackgroundColor(): Color = when (this) {
    TodoType.COMMONS -> Color(0xFFF7DBF7)     // 영상/강의
    TodoType.QUIZ -> Color(0xFFFFD7C2)        // 퀴즈
    TodoType.ASSIGNMENT -> Color(0xFFD8E5F7)  // 과제
    else -> Color(0xFFF7DBF7)
}

/**
 * TodoType별 뱃지 텍스트색 매핑 (MainScreen 기준)
 */
fun TodoType.badgeTextColor(): Color = when (this) {
    TodoType.COMMONS -> Color(0xFFFF39D0)     // 영상/강의
    TodoType.QUIZ -> Color(0xFFFF5F0B)        // 퀴즈
    TodoType.ASSIGNMENT -> Color(0xFF007BFF)  // 과제
    else -> Color(0xFFFF39D0)
}

/**
 * TodoInfo의 마감일을 LocalDate로 파싱
 */
fun TodoInfo.toLocalDate(): LocalDate? {
    val instant = due_date.toTodoDeadlineInstantOrNull() ?: return null
    return instant.atZone(TODO_DEADLINE_ZONE_ID).toLocalDate()
}

/**
 * TodoInfo의 마감 시각 텍스트 포맷 (예: "23시 59분까지")
 */
fun TodoInfo.toDueTimeText(): String {
    val instant = due_date.toTodoDeadlineInstantOrNull() ?: return ""
    val zdt = instant.atZone(TODO_DEADLINE_ZONE_ID)
    return "${zdt.hour}시 ${zdt.minute}분까지"
}

@Composable
fun CalendarScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = koinViewModel(),
    onNoticeClick: () -> Unit = {},
) {
    // SnapshotStateList의 변경을 감지하기 위해 toList()로 상태 전달
    val todos = viewModel.todos.toList()
    val subjects = viewModel.subjects.toList()
    val unreadNoticeCount = viewModel.unreadNoticeCount
    val calendarNavController = rememberNavController()

    NavHost(
        navController = calendarNavController,
        startDestination = CALENDAR_MAIN_ROUTE,
        modifier = modifier.fillMaxSize(),
    ) {
        composable(route = CALENDAR_MAIN_ROUTE) {
            CalendarScreenContent(
                modifier = Modifier.fillMaxSize(),
                todos = todos,
                noticeCount = unreadNoticeCount,
                onNoticeClick = {
                    onNoticeClick()
                    calendarNavController.navigate(CALENDAR_NOTICE_ROUTE)
                },
            )
        }

        composable(route = CALENDAR_NOTICE_ROUTE) {
            NoticeScreen(
                modifier = Modifier.fillMaxSize(),
                subjects = subjects,
                onBackClick = {
                    calendarNavController.popBackStack()
                },
                onDiscussionExpanded = { discussion ->
                    viewModel.markDiscussionAsRead(discussion.id)
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreenContent(
    modifier: Modifier = Modifier,
    todos: List<TodoInfo> = emptyList(),
    noticeCount: Int = 4,
    onNoticeClick: () -> Unit = {},
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now(TODO_DEADLINE_ZONE_ID)) }
    val today = remember { LocalDate.now(TODO_DEADLINE_ZONE_ID) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 날짜별 할일 매핑 (todos 리스트가 변경될 때마다 재계산)
    val eventsByDate = remember(todos) {
        todos.mapNotNull { todo ->
            val date = todo.toLocalDate() ?: return@mapNotNull null
            date to todo
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
    }

    // 해당 월의 전체 할일 수
    val totalMonthEventCount = remember(currentYearMonth, todos) {
        todos.count { todo ->
            val date = todo.toLocalDate() ?: return@count false
            YearMonth.from(date) == currentYearMonth
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
    ) {
        SSUTimeTopBar()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // 1. 공지사항 이동 박스
            item {
                Spacer(modifier = Modifier.height(8.dp))
                CalendarNoticeBanner(
                    noticeCount = noticeCount,
                    onClick = onNoticeClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. 월 네비게이션 헤더 및 건수 표시
            item {
                CalendarMonthHeader(
                    currentYearMonth = currentYearMonth,
                    totalCount = totalMonthEventCount,
                    onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
                    onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. 요일 헤더 (일 ~ 토)
            item {
                CalendarWeekHeader()
                Spacer(modifier = Modifier.height(12.dp))
            }

            // 4. 캘린더 날짜 그리드
            item {
                CalendarMonthGrid(
                    yearMonth = currentYearMonth,
                    today = today,
                    eventsByDate = eventsByDate,
                    onDayClick = { date ->
                        selectedDate = date
                    }
                )
            }
        }
    }

    // 날짜 클릭 시 바텀시트 표시
    selectedDate?.let { date ->
        val selectedDayTodos = eventsByDate[date] ?: emptyList()
        CalendarDateDetailBottomSheet(
            date = date,
            today = today,
            todos = selectedDayTodos,
            onDismissRequest = { selectedDate = null }
        )
    }
}

@Composable
fun CalendarTopBar(
    modifier: Modifier = Modifier,
) {
    SSUTimeTopBar(modifier = modifier)
}

/**
 * 상단 공지사항 이동 박스
 */
@Composable
fun CalendarNoticeBanner(
    noticeCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(N100)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "새로운 공지사항",
                style = SSUType.Caption1SemiBold,
                color = N500
            )
            if (noticeCount > 0) {
                Text(
                    text = "$noticeCount",
                    style = SSUType.Caption1SemiBold,
                    color = R400
                )
            }
        }

        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = "공지사항 바로가기",
            tint = N500,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * 월 네비게이션 & 해당 월 총 건수 헤더
 */
@Composable
fun CalendarMonthHeader(
    currentYearMonth: YearMonth,
    totalCount: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "${currentYearMonth.monthValue}월",
                style = SSUType.H3SemiBold,
                color = N500
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, N200, CircleShape)
                        .clickable(onClick = onPreviousMonth),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "이전 달",
                        tint = N500,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, N200, CircleShape)
                        .clickable(onClick = onNextMonth),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "다음 달",
                        tint = N500,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Text(
            text = "${totalCount}건",
            style = SSUType.H4SemiBold,
            color = N700
        )
    }
}

/**
 * 요일 헤더 (일 ~ 토)
 */
@Composable
fun CalendarWeekHeader(
    modifier: Modifier = Modifier,
) {
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        weekDays.forEach { day ->
            Text(
                text = day,
                style = SSUType.Label2Medium,
                color = N400,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * 캘린더 한 달 날짜 그리드
 */
@Composable
fun CalendarMonthGrid(
    yearMonth: YearMonth,
    today: LocalDate?,
    eventsByDate: Map<LocalDate, List<TodoInfo>>,
    onDayClick: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()

    // 일요일=0, 월요일=1, ... 토요일=6
    val firstDayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7

    val totalCells = firstDayOfWeekOffset + daysInMonth
    val totalWeeks = (totalCells + 6) / 7

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        for (week in 0 until totalWeeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                for (dayIndex in 0 until 7) {
                    val cellIndex = week * 7 + dayIndex
                    val dayNumber = cellIndex - firstDayOfWeekOffset + 1

                    if (dayNumber in 1..daysInMonth) {
                        val date = yearMonth.atDay(dayNumber)
                        val isToday = (date == today)
                        val events = eventsByDate[date] ?: emptyList()

                        CalendarDayCell(
                            day = dayNumber,
                            isToday = isToday,
                            events = events,
                            onClick = { onDayClick(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        // 빈 셀 (이전/다음 달 여백)
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/**
 * 개별 날짜 셀
 * - 최대 2개 할일 표시
 * - 그 이상은 아래에 "+외 N건" 표시
 */
@Composable
fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    events: List<TodoInfo>,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
            .heightIn(min = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 날짜 뱃지 / 텍스트
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(R500),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$day",
                    style = SSUType.Label2Bold,
                    color = WHITE,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "$day",
                    style = SSUType.Label2Medium,
                    color = N500,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 할일 목록 (최대 2개)
        val visibleEvents = events.take(2)
        val moreCount = events.size - 2

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            visibleEvents.forEach { todo ->
                CalendarEventChip(todo = todo)
            }

            // 2개 초과 시 외 N건 표시
            if (moreCount > 0) {
                Text(
                    text = "+외 ${moreCount}건",
                    style = SSUType.Caption3Regular,
                    color = N400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }
    }
}

/**
 * 일정/할일 태그 칩
 */
@Composable
fun CalendarEventChip(
    todo: TodoInfo,
    modifier: Modifier = Modifier,
) {
    val title = todo.subject?.name?.takeIf { it.isNotBlank() } ?: todo.title

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(todo.type.badgeBackgroundColor())
            .padding(horizontal = 2.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = SSUType.Caption2SemiBold,
            color = todo.type.badgeTextColor(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Left,
        )
    }
}

/**
 * 날짜 클릭 시 노출되는 마감일정 상세 바텀시트
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarDateDetailBottomSheet(
    date: LocalDate,
    today: LocalDate,
    todos: List<TodoInfo>,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dDayDays = ChronoUnit.DAYS.between(today, date)
    val dDayText = when {
        dDayDays == 0L -> "D-Day"
        dDayDays > 0L -> "D-$dDayDays"
        else -> "D+${-dDayDays}"
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = WHITE,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .safeDrawingPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 36.dp)
        ) {
            // 헤더: 날짜 (D-Day)
            Text(
                text = "${date.monthValue}월 ${date.dayOfMonth}일 ($dDayText)",
                style = SSUType.H2SemiBold,
                color = N700
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (todos.isEmpty()) {
                Text(
                    text = "마감 일정이 없습니다.",
                    style = SSUType.Body1Medium,
                    color = N400,
                    modifier = Modifier.padding(vertical = 24.dp)
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    todos.forEachIndexed { index, todo ->
                        CalendarTodoDetailItem(todo = todo)

                        if (index < todos.lastIndex) {
                            HorizontalDivider(
                                color = N200,
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * 바텀시트 내 개별 할일 아이템
 */
@Composable
fun CalendarTodoDetailItem(
    todo: TodoInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 상단: 타입 뱃지 + 과목명
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(todo.type.badgeBackgroundColor())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = todo.type.kor,
                    style = SSUType.Caption1SemiBold,
                    color = todo.type.badgeTextColor()
                )
            }

            Text(
                text = todo.subject?.name ?: "",
                style = SSUType.Caption1Medium,
                color = N500
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 중간: 할일 제목
        Text(
            text = todo.title,
            style = SSUType.H4Medium,
            color = N600
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 하단: 마감 기한 박스
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(N100)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "마감 기한",
                style = SSUType.Label3Medium,
                color = N500
            )

            Text(
                text = todo.toDueTimeText(),
                style = SSUType.Label3Medium,
                color = N500
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun CalendarScreenPreview() {
    CalendarScreenContent()
}


