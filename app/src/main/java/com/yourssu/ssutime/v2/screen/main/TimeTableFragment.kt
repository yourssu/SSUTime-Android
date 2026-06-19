package com.yourssu.ssutime.v2.screen.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import io.github.chlwhdtn03.data.Lms.TimetableCell
import java.time.LocalDate
import io.github.chlwhdtn03.data.Lms.DayOfWeek as LmsDayOfWeek
import java.time.DayOfWeek as JDayOfWeek

sealed interface TimetableItemUiModel {
    data class Course(val cell: TimetableCell) : TimetableItemUiModel
    data class EmptyTime(val minutes: Int) : TimetableItemUiModel
}

@Composable
fun TimeTableFragment(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier,
) {
    val timetableState = viewModel.timetableState.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.timetable_title),
            style = SSUType.H1SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        Crossfade(
            targetState = timetableState,
            label = "TimetableStateCrossfade",
            modifier = Modifier.weight(1f),
        ) { state ->
            when (state) {
                is TimetableUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = R500,
                            trackColor = R100,
                        )
                    }
                }

                is TimetableUiState.Success -> {
                    val timetable = state.timetable
                    TimetableContent(
                        timetable = timetable,
                        onRefresh = { viewModel.loadTimetable(forceRefresh = true) }
                    )
                }

                is TimetableUiState.Empty -> {
                    TimetableEmptyState(
                        message = stringResource(R.string.timetable_empty_message),
                        onRetry = { viewModel.loadTimetable(forceRefresh = true) }
                    )
                }

                is TimetableUiState.Error -> {
                    TimetableEmptyState(
                        message = state.message,
                        onRetry = { viewModel.loadTimetable(forceRefresh = true) }
                    )
                }
            }
        }
    }
}

@Composable
private fun TimetableContent(
    timetable: io.github.chlwhdtn03.data.Lms.Timetable,
    onRefresh: () -> Unit,
) {
    var selectedDay by remember { mutableStateOf(getCurrentLmsDayOfWeek()) }
    val filteredCells = remember(timetable.items, selectedDay) {
        timetable.items
            .filter { it.dayOfWeek == selectedDay }
            .sortedBy { cell ->
                cell.period.toDoubleOrNull() ?: cell.period.replace(Regex("[^0-9.]"), "").toDoubleOrNull() ?: Double.MAX_VALUE
            }
    }

    val uiItems = remember(filteredCells) {
        val list = mutableListOf<TimetableItemUiModel>()
        for (i in filteredCells.indices) {
            val current = filteredCells[i]
            if (i > 0) {
                val prev = filteredCells[i - 1]
                val prevEnd = parseEndMinutes(prev)
                val currentStart = parseStartMinutes(current)
                if (prevEnd != null && currentStart != null) {
                    val diff = currentStart - prevEnd
                    if (diff > 0) {
                        list.add(TimetableItemUiModel.EmptyTime(diff))
                    }
                }
            }
            list.add(TimetableItemUiModel.Course(current))
        }
        list
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${timetable.year} ${timetable.semester}",
                style = SSUType.Caption1Medium,
                color = N500,
            )

//            Text(
//                text = stringResource(R.string.common_refresh),
//                style = SSUType.Caption1SemiBold,
//                color = R500,
//                modifier = Modifier
//                    .clip(RoundedCornerShape(4.dp))
//                    .clickable { onRefresh() }
//                    .padding(horizontal = 4.dp, vertical = 2.dp)
//            )
        }

        Spacer(Modifier.height(12.dp))

        // Weekday selection horizontal list
        val scrollState = rememberScrollState()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LmsDayOfWeek.values().forEach { day ->
                val isSelected = day == selectedDay
                val backgroundColor = if (isSelected) R500 else N100
                val textColor = if (isSelected) WHITE else N500
                val borderModifier = if (isSelected) Modifier else Modifier.border(1.dp, N200, RoundedCornerShape(20.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(backgroundColor)
                        .then(borderModifier)
                        .clickable { selectedDay = day }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = day.toLocalizedName(),
                        style = SSUType.Label2Medium,
                        color = textColor,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        if (uiItems.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiItems) { item ->
                    when (item) {
                        is TimetableItemUiModel.Course -> {
                            TimetableCellCard(cell = item.cell)
                        }
                        is TimetableItemUiModel.EmptyTime -> {
                            EmptyTimeCard(minutes = item.minutes)
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.timetable_no_class_msg, selectedDay.toLocalizedName()),
                    style = SSUType.H4Medium,
                    color = N500,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun TimetableCellCard(cell: TimetableCell) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cell.subject,
                style = SSUType.H4SemiBold,
            )

            Spacer(Modifier.height(4.dp))

            Text(
                text = "${cell.period} ${cell.periodTime}",
                style = SSUType.Caption1SemiBold,
                color = R500,
            )

            if (cell.classroom.isNotBlank() || cell.professor.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                val infoText = buildString {
                    if (cell.classroom.isNotBlank()) append(cell.classroom)
                    if (cell.classroom.isNotBlank() && cell.professor.isNotBlank()) append("  |  ")
                    if (cell.professor.isNotBlank()) append(cell.professor)
                }
                Text(
                    text = infoText,
                    style = SSUType.Caption1SemiBold,
                    color = N500,
                )
            }
        }
    }
}

@Composable
private fun EmptyTimeCard(minutes: Int) {
    val text = when {
        minutes < 60 -> {
            stringResource(R.string.timetable_empty_period, minutes)
        }
        minutes % 60 == 0 -> {
            val hours = minutes / 60
            stringResource(R.string.timetable_empty_period_hours, hours)
        }
        else -> {
            val hours = minutes / 60
            val remainingMinutes = minutes % 60
            stringResource(R.string.timetable_empty_period_hours_minutes, hours, remainingMinutes)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
            .padding(vertical = 8.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = SSUType.Body1Medium,
            color = N500,
        )
    }
}

@Composable
private fun TimetableEmptyState(
    message: String,
    onRetry: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = message,
            style = SSUType.Body2Regular,
            color = N500,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(N200)
                .clickable { onRetry() }
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(R.string.common_retry),
                style = SSUType.Label2Medium,
            )
        }
    }
}

private fun getCleanTimeRange(cell: TimetableCell): String {
    val rawTime = cell.periodTime.takeIf { it.isNotBlank() } ?: cell.time
    return rawTime.replace(Regex("[^0-9:~-]"), "").trim()
}

private fun parseStartMinutes(cell: TimetableCell): Int? {
    val timeRange = getCleanTimeRange(cell)
    val parts = timeRange.split(Regex("[-~]"))
    if (parts.isEmpty()) return null
    return timeToMinutes(parts[0].trim())
}

private fun parseEndMinutes(cell: TimetableCell): Int? {
    val timeRange = getCleanTimeRange(cell)
    val parts = timeRange.split(Regex("[-~]"))
    if (parts.size < 2) return null
    return timeToMinutes(parts[1].trim())
}

private fun timeToMinutes(timeStr: String): Int? {
    val timeParts = timeStr.split(":")
    if (timeParts.size < 2) return null
    val hour = timeParts[0].toIntOrNull() ?: return null
    val minute = timeParts[1].toIntOrNull() ?: return null
    return hour * 60 + minute
}

private fun getCurrentLmsDayOfWeek(): LmsDayOfWeek {
    return when (LocalDate.now().dayOfWeek) {
        JDayOfWeek.MONDAY -> LmsDayOfWeek.MONDAY
        JDayOfWeek.TUESDAY -> LmsDayOfWeek.TUESDAY
        JDayOfWeek.WEDNESDAY -> LmsDayOfWeek.WEDNESDAY
        JDayOfWeek.THURSDAY -> LmsDayOfWeek.THURSDAY
        JDayOfWeek.FRIDAY -> LmsDayOfWeek.FRIDAY
        JDayOfWeek.SATURDAY -> LmsDayOfWeek.SATURDAY
        JDayOfWeek.SUNDAY -> LmsDayOfWeek.SUNDAY
        null -> LmsDayOfWeek.MONDAY
    }
}

@Composable
private fun LmsDayOfWeek.toLocalizedName(): String {
    val resId = when (this) {
        LmsDayOfWeek.MONDAY -> R.string.day_mon
        LmsDayOfWeek.TUESDAY -> R.string.day_tue
        LmsDayOfWeek.WEDNESDAY -> R.string.day_wed
        LmsDayOfWeek.THURSDAY -> R.string.day_thu
        LmsDayOfWeek.FRIDAY -> R.string.day_fri
        LmsDayOfWeek.SATURDAY -> R.string.day_sat
        LmsDayOfWeek.SUNDAY -> R.string.day_sun
    }
    return stringResource(resId)
}