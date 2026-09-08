package com.yourssu.ssutime.desktop.screen.calendar

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.dueDate
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.calendar_empty_events
import com.yourssu.ssutime.desktop.ui.resources.common_close
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_right
import com.yourssu.ssutime.desktop.ui.resources.icon_collapsed
import com.yourssu.ssutime.desktop.ui.resources.main_deadline_label
import com.yourssu.ssutime.desktop.ui.resources.notice_new
import com.yourssu.ssutime.desktop.ui.resources.todo_type_assignment
import com.yourssu.ssutime.desktop.ui.resources.todo_type_lecture
import com.yourssu.ssutime.desktop.ui.resources.todo_type_quiz
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted_late
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N400
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.N600
import com.yourssu.ssutime.desktop.ui.theme.N700
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit

private val KOREA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")

fun AppTodoType.badgeBackgroundColor(): Color = when (this) {
    AppTodoType.COMMONS -> Color(0xFFF7DBF7)
    AppTodoType.QUIZ -> Color(0xFFFFD7C2)
    AppTodoType.ASSIGNMENT -> Color(0xFFD8E5F7)
    else -> Color(0xFFF7DBF7)
}

fun AppTodoType.badgeTextColor(): Color = when (this) {
    AppTodoType.COMMONS -> Color(0xFFFF39D0)
    AppTodoType.QUIZ -> Color(0xFFFF5F0B)
    AppTodoType.ASSIGNMENT -> Color(0xFF007BFF)
    else -> Color(0xFFFF39D0)
}

fun AppTodo.toLocalDate(): LocalDate? = runCatching {
    Instant.parse(dueDate).atZone(KOREA_ZONE_ID).toLocalDate()
}.getOrNull()

fun AppTodo.toDueTimeText(): String = runCatching {
    val zdt = Instant.parse(dueDate).atZone(KOREA_ZONE_ID)
    "${zdt.hour}시 ${zdt.minute}분까지"
}.getOrDefault("")

@Composable
fun DesktopCalendarPanel(
    todos: List<AppTodo>,
    noticeCount: Int,
    onNoticeClick: () -> Unit,
    onTodoClick: (AppTodo) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var currentYearMonth by remember { mutableStateOf(YearMonth.now(KOREA_ZONE_ID)) }
    val today = remember { LocalDate.now(KOREA_ZONE_ID) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    val eventsByDate = remember(todos) {
        todos.mapNotNull { todo ->
            val date = todo.toLocalDate() ?: return@mapNotNull null
            date to todo
        }.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second },
        )
    }

    val totalMonthEventCount = remember(currentYearMonth, todos) {
        todos.count { todo ->
            val date = todo.toLocalDate() ?: return@count false
            YearMonth.from(date) == currentYearMonth
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .verticalScroll(scrollState)
            .padding(16.dp),
    ) {
        CalendarNoticeBanner(
            noticeCount = noticeCount,
            onClick = onNoticeClick,
        )

        Spacer(modifier = Modifier.height(20.dp))

        CalendarMonthHeader(
            currentYearMonth = currentYearMonth,
            totalCount = totalMonthEventCount,
            onPreviousMonth = { currentYearMonth = currentYearMonth.minusMonths(1) },
            onNextMonth = { currentYearMonth = currentYearMonth.plusMonths(1) },
        )

        Spacer(modifier = Modifier.height(16.dp))

        CalendarWeekHeader()

        Spacer(modifier = Modifier.height(12.dp))

        CalendarMonthGrid(
            yearMonth = currentYearMonth,
            today = today,
            eventsByDate = eventsByDate,
            onDayClick = { date -> selectedDate = date },
        )
    }

    selectedDate?.let { date ->
        val selectedDayTodos = eventsByDate[date] ?: emptyList()
        DesktopCalendarDateDetailDialog(
            date = date,
            today = today,
            todos = selectedDayTodos,
            onDismiss = { selectedDate = null },
            onTodoClick = { todo ->
                selectedDate = null
                onTodoClick(todo)
            },
        )
    }
}

@Composable
private fun CalendarNoticeBanner(
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
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = stringResource(Res.string.notice_new),
                style = SSUType.Caption1SemiBold,
                color = N500,
            )
            Text(
                text = "$noticeCount",
                style = SSUType.Caption1SemiBold,
                color = R400,
            )
        }

        Icon(
            painter = painterResource(Res.drawable.icon_collapsed),
            contentDescription = null,
            tint = N500,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
private fun CalendarMonthHeader(
    currentYearMonth: YearMonth,
    totalCount: Int,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${currentYearMonth.monthValue}월",
                style = SSUType.H3SemiBold,
                color = N500,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, N200, CircleShape)
                        .clickable(onClick = onPreviousMonth),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_back),
                        contentDescription = "이전 달",
                        tint = N500,
                        modifier = Modifier.size(14.dp),
                    )
                }

                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .border(1.dp, N200, CircleShape)
                        .clickable(onClick = onNextMonth),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_arrow_right),
                        contentDescription = "다음 달",
                        tint = N500,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        Text(
            text = "${totalCount}건",
            style = SSUType.H4SemiBold,
            color = N700,
        )
    }
}

@Composable
private fun CalendarWeekHeader(
    modifier: Modifier = Modifier,
) {
    val weekDays = listOf("일", "월", "화", "수", "목", "금", "토")

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        weekDays.forEach { day ->
            Text(
                text = day,
                style = SSUType.Label2Medium,
                color = N400,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun CalendarMonthGrid(
    yearMonth: YearMonth,
    today: LocalDate?,
    eventsByDate: Map<LocalDate, List<AppTodo>>,
    onDayClick: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val firstDayOfMonth = yearMonth.atDay(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
    val totalCells = firstDayOfWeekOffset + daysInMonth
    val totalWeeks = (totalCells + 6) / 7

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (week in 0 until totalWeeks) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
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
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    isToday: Boolean,
    events: List<AppTodo>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 2.dp)
            .heightIn(min = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (isToday) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(R500),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$day",
                    style = SSUType.Label2Bold,
                    color = WHITE,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "$day",
                    style = SSUType.Label2Medium,
                    color = N500,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        val visibleEvents = events.take(2)
        val moreCount = events.size - 2

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            visibleEvents.forEach { todo ->
                CalendarEventChip(todo = todo)
            }

            if (moreCount > 0) {
                Text(
                    text = "+외 ${moreCount}건",
                    style = SSUType.Caption3Regular,
                    color = N400,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun CalendarEventChip(
    todo: AppTodo,
    modifier: Modifier = Modifier,
) {
    val title = todo.subject?.name?.takeIf(String::isNotBlank) ?: todo.title

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(3.dp))
            .background(todo.type.badgeBackgroundColor())
            .padding(horizontal = 3.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
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

@Composable
fun DesktopCalendarDateDetailDialog(
    date: LocalDate,
    today: LocalDate,
    todos: List<AppTodo>,
    onDismiss: () -> Unit,
    onTodoClick: (AppTodo) -> Unit,
) {
    val dDayDays = ChronoUnit.DAYS.between(today, date)
    val dDayText = when {
        dDayDays == 0L -> "D-Day"
        dDayDays > 0L -> "D-$dDayDays"
        else -> "D+${-dDayDays}"
    }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(WHITE)
                .padding(24.dp),
        ) {
            Text(
                text = "${date.monthValue}월 ${date.dayOfMonth}일 ($dDayText)",
                style = SSUType.H2SemiBold,
                color = N700,
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (todos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.calendar_empty_events),
                        style = SSUType.Body1Medium,
                        color = N400,
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    todos.forEach { todo ->
                        CalendarTodoDetailItem(
                            todo = todo,
                            onClick = { onTodoClick(todo) },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            SButton(
                modifier = Modifier.fillMaxWidth(),
                labelText = stringResource(Res.string.common_close),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun CalendarTodoDetailItem(
    todo: AppTodo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(todo.type.badgeBackgroundColor())
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = when (todo.type) {
                        AppTodoType.COMMONS -> stringResource(Res.string.todo_type_lecture)
                        AppTodoType.ASSIGNMENT -> stringResource(Res.string.todo_type_assignment)
                        AppTodoType.QUIZ -> stringResource(Res.string.todo_type_quiz)
                        AppTodoType.SUBMITTED -> stringResource(Res.string.todo_type_submitted)
                        AppTodoType.SUBMITTED_LATE -> stringResource(Res.string.todo_type_submitted_late)
                    },
                    style = SSUType.Caption1SemiBold,
                    color = todo.type.badgeTextColor(),
                )
            }

            Text(
                text = todo.subject?.name.orEmpty(),
                style = SSUType.Caption1Medium,
                color = N500,
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = todo.title,
            style = SSUType.H4Medium,
            color = N600,
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(N100)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.main_deadline_label),
                style = SSUType.Label3Medium,
                color = N500,
            )

            Text(
                text = todo.toDueTimeText(),
                style = SSUType.Label3Medium,
                color = N500,
            )
        }
    }
}
