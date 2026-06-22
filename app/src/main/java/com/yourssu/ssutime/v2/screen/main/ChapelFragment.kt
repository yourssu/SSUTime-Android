package com.yourssu.ssutime.v2.screen.main

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.ui.theme.G500
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import io.github.chlwhdtn03.data.Lms.ChapelAbsenceCell
import io.github.chlwhdtn03.data.Lms.ChapelAttendanceCell
import io.github.chlwhdtn03.data.Lms.ChapelInformation
import io.github.chlwhdtn03.data.Lms.ChapelSeatStatusCell
import io.github.chlwhdtn03.data.Lms.Semester

@Composable
fun ChapelHistoryView(
    summaryState: GradeSummaryUiState,
    chapelState: ChapelUiState,
    selectedSemesterKey: String,
    onSelectedSemesterKeyChange: (String) -> Unit,
    currentSemesterName: String,
    onCurrentSemesterNameChange: (String) -> Unit,
    thisSemesterYear: String?,
    onThisSemesterYearChange: (String?) -> Unit,
    thisSemesterType: Semester?,
    onThisSemesterTypeChange: (Semester?) -> Unit,
    onLoadDetail: (String?, Semester?, Boolean) -> Unit,
    onRefreshSummary: () -> Unit,
    modifier: Modifier = Modifier,
    isLargeScreen: Boolean = false,
) {
    LaunchedEffect(Unit) {
        onRefreshSummary()
        onLoadDetail(null, null, false)
    }

    LaunchedEffect(chapelState) {
        if (selectedSemesterKey == "current" && chapelState is ChapelUiState.Success) {
            val table = chapelState.table
            val semName = runCatching { table.semester.nameKor }.getOrDefault("")
            if (table.year.isNotBlank() && semName.isNotBlank()) {
                onCurrentSemesterNameChange("${table.year} $semName")
                if (thisSemesterYear == null || thisSemesterType == null) {
                    onThisSemesterYearChange(table.year)
                    onThisSemesterTypeChange(table.semester)
                }
            }
        }
    }

    if (isLargeScreen) {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(WHITE)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. 왼쪽 1/3 영역: 학기 목록 (List Pane)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "학기 목록",
                    style = SSUType.H2SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        val isSelected = selectedSemesterKey == "current"
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) R500 else N100)
                                .border(
                                    1.dp,
                                    if (isSelected) R500 else N200,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    onSelectedSemesterKeyChange("current")
                                    onLoadDetail(null, null, false)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Text(
                                text = currentSemesterName,
                                style = SSUType.Caption1SemiBold,
                                color = if (isSelected) WHITE else N500
                            )
                        }
                    }

                    if (summaryState is GradeSummaryUiState.Success) {
                        val displayCells = summaryState.table.items.filter { cell ->
                            !(thisSemesterYear != null && thisSemesterType != null &&
                                    cell.year == thisSemesterYear && cell.semester == thisSemesterType)
                        }
                        items(displayCells.size) { index ->
                            val cell = displayCells[index]
                            val key = "${cell.year}-${cell.semester?.name ?: ""}"
                            val isSelected = selectedSemesterKey == key
                            val semesterName = cell.semester?.nameKor ?: ""

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) R500 else N100)
                                    .border(
                                        1.dp,
                                        if (isSelected) R500 else N200,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable {
                                        onSelectedSemesterKeyChange(key)
                                        cell.semester?.let { sem ->
                                            onLoadDetail(cell.year, sem, false)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = "${cell.year}학년도 $semesterName",
                                    style = SSUType.Caption1SemiBold,
                                    color = if (isSelected) WHITE else N500
                                )
                            }
                        }
                    }
                }
            }

            // 2. 오른쪽 2/3 영역: 선택된 학기 상세 정보 (Detail Pane)
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "채플 상세",
                    style = SSUType.H2SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                Crossfade(
                    targetState = chapelState,
                    label = "ChapelStateCrossfadeLarge",
                    modifier = Modifier.weight(1f)
                ) { state ->
                    when (state) {
                        is ChapelUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = R500, trackColor = R100)
                            }
                        }
                        is ChapelUiState.Success -> {
                            ChapelContentList(table = state.table)
                        }
                        is ChapelUiState.Empty -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "채플 정보가 없습니다.", style = SSUType.Body2Regular, color = N500)
                            }
                        }
                        is ChapelUiState.Error -> {
                            val summaryCells = (summaryState as? GradeSummaryUiState.Success)?.table?.items ?: emptyList()
                            LargeScreenEmptyState(
                                message = state.message,
                                onRetry = {
                                    if (selectedSemesterKey == "current") {
                                        onLoadDetail(null, null, true)
                                    } else {
                                        val cell = summaryCells.firstOrNull { "${it.year}-${it.semester?.name ?: ""}" == selectedSemesterKey }
                                        cell?.let { c ->
                                            c.semester?.let { sem ->
                                                onLoadDetail(c.year, sem, true)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(WHITE)
                .padding(16.dp),
        ) {
            Text(
                text = "채플 조회",
                style = SSUType.H1SemiBold,
            )

            Spacer(Modifier.height(16.dp))

            // 1. 학기 선택 칩 목록 (LazyRow)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelected = selectedSemesterKey == "current"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) R500 else N100)
                            .border(1.dp, if (isSelected) R500 else N200, RoundedCornerShape(20.dp))
                            .clickable {
                                onSelectedSemesterKeyChange("current")
                                onLoadDetail(null, null, false)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = currentSemesterName,
                            style = SSUType.Caption1SemiBold,
                            color = if (isSelected) WHITE else N500
                        )
                    }
                }

                if (summaryState is GradeSummaryUiState.Success) {
                    val displayCells = summaryState.table.items.filter { cell ->
                        !(thisSemesterYear != null && thisSemesterType != null &&
                                cell.year == thisSemesterYear && cell.semester == thisSemesterType)
                    }

                    items(displayCells.size) { index ->
                        val cell = displayCells[index]
                        val key = "${cell.year}-${cell.semester?.name ?: ""}"
                        val isSelected = selectedSemesterKey == key
                        val semesterName = cell.semester?.nameKor ?: ""
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) R500 else N100)
                                .border(
                                    1.dp,
                                    if (isSelected) R500 else N200,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    onSelectedSemesterKeyChange(key)
                                    cell.semester?.let { sem ->
                                        onLoadDetail(cell.year, sem, false)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "${cell.year} $semesterName",
                                style = SSUType.Caption1SemiBold,
                                color = if (isSelected) WHITE else N500
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Crossfade(
                targetState = chapelState,
                label = "ChapelStateCrossfade",
                modifier = Modifier.weight(1f)
            ) { state ->
                when (state) {
                    is ChapelUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = R500, trackColor = R100)
                        }
                    }
                    is ChapelUiState.Success -> {
                        ChapelContentList(table = state.table)
                    }
                    is ChapelUiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "채플 정보가 없습니다.", style = SSUType.Body2Regular, color = N500)
                        }
                    }
                    is ChapelUiState.Error -> {
                        val summaryCells = (summaryState as? GradeSummaryUiState.Success)?.table?.items ?: emptyList()
                        LargeScreenEmptyState(
                            message = state.message,
                            onRetry = {
                                if (selectedSemesterKey == "current") {
                                    onLoadDetail(null, null, true)
                                } else {
                                    val cell = summaryCells.firstOrNull { "${it.year}-${it.semester?.name ?: ""}" == selectedSemesterKey }
                                    cell?.let { c ->
                                        c.semester?.let { sem ->
                                            onLoadDetail(c.year, sem, true)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapelContentList(table: ChapelInformation) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val seatItems = table.seatStatusTable.items
        if (seatItems.isNotEmpty()) {
            item {
                Text(text = "좌석 정보", style = SSUType.H3SemiBold)
                Spacer(Modifier.height(8.dp))
                seatItems.forEach { seat ->
                    ChapelSeatCard(cell = seat)
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        val attendanceItems = table.attendanceTable.items
        if (attendanceItems.isNotEmpty()) {
            item {
                Text(text = "출결 현황", style = SSUType.H3SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            items(attendanceItems.size) { index ->
                ChapelAttendanceCard(cell = attendanceItems[index])
            }
        }

        val absenceItems = table.absenceTable.items
        if (absenceItems.isNotEmpty()) {
            item {
                Text(text = "결석 사유 상세", style = SSUType.H3SemiBold)
                Spacer(Modifier.height(8.dp))
            }
            items(absenceItems.size) { index ->
                ChapelAbsenceCard(cell = absenceItems[index])
            }
        }
    }
}

@Composable
private fun ChapelSeatCard(cell: ChapelSeatStatusCell) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${cell.classGroup}분반",
                style = SSUType.H4SemiBold,
                color = R500
            )
            if (cell.gradeResult.isNotBlank()) {
                Text(
                    text = cell.gradeResult,
                    style = SSUType.Caption1SemiBold,
                    color = if (cell.gradeResult == "P" || cell.gradeResult.contains("이수")) G500 else R500
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "강의실", style = SSUType.Caption1Medium, color = N500)
                Spacer(Modifier.height(4.dp))
                Text(text = cell.classroom.ifBlank { "-" }, style = SSUType.Label2SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "좌석번호", style = SSUType.Caption1Medium, color = N500)
                Spacer(Modifier.height(4.dp))
                Text(text = cell.seatNo.ifBlank { "-" }, style = SSUType.Label2SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "결석횟수", style = SSUType.Caption1Medium, color = N500)
                Spacer(Modifier.height(4.dp))
                Text(text = cell.absenceCount.ifBlank { "0" }, style = SSUType.Label2SemiBold)
            }
        }
        if (cell.timetable.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = "시간표: ${cell.timetable}",
                style = SSUType.Caption2Medium,
                color = N500
            )
        }
    }
}

@Composable
private fun ChapelAttendanceCard(cell: ChapelAttendanceCell) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WHITE)
            .border(1.dp, N200, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = cell.date,
                style = SSUType.Caption2Medium,
                color = N500
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "${cell.rawValues["강사"] ?: ""} ${cell.rawValues["소속"] ?: ""} ${cell.rawValues["제목"] ?: ""}",
                style = SSUType.Label2SemiBold,
            )
        }

        Spacer(Modifier.width(12.dp))

        Text(
            text = cell.status,
            style = SSUType.H4SemiBold,
            color = when (cell.status) {
                "출석" -> G500
                "결석" -> R500
                else -> N500
            }
        )
    }
}

@Composable
private fun ChapelAbsenceCard(cell: ChapelAbsenceCell) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(WHITE)
            .border(1.dp, N200, RoundedCornerShape(12.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "${cell.year} ${cell.semester}",
                    style = SSUType.Caption2Medium,
                    color = N500
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = cell.rawValues["결석사유(국문)"] ?: "",
                    style = SSUType.Label2SemiBold,
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = "${cell.rawValues["결석시작일자"] ?: ""} ~ ${cell.rawValues["결석종료일자"] ?: ""}",
                style = SSUType.Label2SemiBold,
            )
        }
    }
}
