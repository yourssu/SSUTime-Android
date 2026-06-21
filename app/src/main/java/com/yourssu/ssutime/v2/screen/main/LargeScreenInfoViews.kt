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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
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
import com.yourssu.ssutime.v2.ui.theme.G400
import com.yourssu.ssutime.v2.ui.theme.G500
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import io.github.chlwhdtn03.data.Lms.GraduateTable
import io.github.chlwhdtn03.data.Lms.GraduateTableCell
import io.github.chlwhdtn03.data.Lms.ScholarshipHistoryCell
import io.github.chlwhdtn03.data.Lms.ScholarshipHistoryTable
import io.github.chlwhdtn03.data.Lms.TuitionCell
import io.github.chlwhdtn03.data.Lms.TuitionTable

@Composable
fun ScholarshipHistoryView(
    state: ScholarshipUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isLargeScreen: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.scholarship_title),
            style = SSUType.H1SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        Crossfade(
            targetState = state,
            label = "ScholarshipStateCrossfade",
            modifier = Modifier.weight(1f),
        ) { uiState ->
            when (uiState) {
                is ScholarshipUiState.Loading -> {
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

                is ScholarshipUiState.Success -> {
                    ScholarshipContent(
                        table = uiState.table,
                        isLargeScreen = isLargeScreen
                    )
                }

                is ScholarshipUiState.Empty -> {
                    val defaultTable = ScholarshipHistoryTable(
                        items = listOf(
                            ScholarshipHistoryCell(
                                year = "",
                                semester = "",
                                scholarshipName = stringResource(R.string.scholarship_no_history_name),
                                selectedAmount = stringResource(R.string.scholarship_zero_amount),
                                actualAmount = stringResource(R.string.scholarship_zero_amount),
                                paymentMethod = "",
                                processStatus = "",
                                note = "",
                                dropReason = "",
                                processDate = "",
                                redeemedAmount = "",
                                replacedAmount = "",
                                replacedScholarshipName = "",
                                workDepartment = ""
                            )
                        )
                    )
                    ScholarshipContent(
                        table = defaultTable,
                        isLargeScreen = isLargeScreen
                    )
                }

                is ScholarshipUiState.Error -> {
                    LargeScreenEmptyState(
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
private fun ScholarshipContent(
    table: ScholarshipHistoryTable,
    isLargeScreen: Boolean = false
) {
    if (isLargeScreen) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(table.items) { cell ->
                ScholarshipCellCard(cell = cell)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(table.items) { cell ->
                ScholarshipCellCard(cell = cell)
            }
        }
    }
}

@Composable
private fun ScholarshipCellCard(cell: ScholarshipHistoryCell) {
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
                text = "${cell.year} ${cell.semester}".trim(),
                style = SSUType.Caption1SemiBold,
                color = R500,
            )
            if (cell.processStatus.isNotBlank()) {
                val statusColor = when (cell.processStatus.trim()) {
                    "지급완료", "지급 완료" -> G500
                    else -> R500
                }
                Text(
                    text = cell.processStatus,
                    style = SSUType.Label1Medium,
                    color = statusColor
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = cell.scholarshipName,
            style = SSUType.H4SemiBold,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.scholarship_header_amount),
                    style = SSUType.Caption1Medium,
                    color = N500
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cell.selectedAmount.ifBlank { "0" },
                    style = SSUType.Label2SemiBold,
                )
            }

            if (cell.paymentMethod.isNotBlank()) {
                Column(modifier = Modifier.weight(1.2f)) {
                    Text(
                        text = stringResource(R.string.scholarship_header_payment_method),
                        style = SSUType.Caption1Medium,
                        color = N500
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = cell.paymentMethod,
                        style = SSUType.Label2SemiBold,
                    )
                }
            }

            if (cell.processDate.isNotBlank()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.scholarship_header_process_date),
                        style = SSUType.Caption1Medium,
                        color = N500
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = cell.processDate,
                        style = SSUType.Label2SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
fun TuitionHistoryView(
    state: TuitionUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isLargeScreen: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.tuition_title),
            style = SSUType.H1SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        Crossfade(
            targetState = state,
            label = "TuitionStateCrossfade",
            modifier = Modifier.weight(1f),
        ) { uiState ->
            when (uiState) {
                is TuitionUiState.Loading -> {
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

                is TuitionUiState.Success -> {
                    TuitionContent(
                        table = uiState.table,
                        isLargeScreen = isLargeScreen
                    )
                }

                is TuitionUiState.Empty -> {
                    LargeScreenEmptyState(
                        message = stringResource(R.string.tuition_empty_message),
                        onRetry = onRefresh
                    )
                }

                is TuitionUiState.Error -> {
                    LargeScreenEmptyState(
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
private fun TuitionContent(
    table: TuitionTable,
    isLargeScreen: Boolean = false
) {
    if (isLargeScreen) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(table.items.reversed()) { cell ->
                TuitionCellCard(cell = cell)
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(table.items.reversed()) { cell ->
                TuitionCellCard(cell = cell)
            }
        }
    }
}

@Composable
private fun TuitionCellCard(cell: TuitionCell) {
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
                text = "${cell.year} ${cell.semester}",
                style = SSUType.H4SemiBold,
            )
            Text(
                text = cell.grade,
                style = SSUType.Body1Medium,
                color = N500
            )
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = cell.registrationType,
                style = SSUType.Caption1SemiBold,
                color = N500,
            )
            if (cell.registrationDate.isNotBlank()) {
                Text(
                    text = "|",
                    style = SSUType.Caption1SemiBold,
                    color = N300,
                )
                Text(
                    text = cell.registrationDate,
                    style = SSUType.Caption1SemiBold,
                    color = N500,
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tuition_header_amount),
                    style = SSUType.Caption1Medium,
                    color = N500
                )
                Text(
                    text = cell.amount,
                    style = SSUType.Label2SemiBold,
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.scholarship_title),
                    style = SSUType.Caption1Medium,
                    color = N500
                )
                val reductionText = if (cell.reduction.isNotBlank() && cell.reduction != "0") {
                    "-${cell.reduction}"
                } else {
                    stringResource(R.string.scholarship_zero_amount)
                }
                val textColor = if (cell.reduction.isNotBlank() && cell.reduction != "0") {
                    R500
                } else {
                    N500
                }
                Text(
                    text = reductionText,
                    style = SSUType.Label2SemiBold,
                    color = textColor
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tuition_header_payment),
                    style = SSUType.Caption1Medium,
                    color = N500
                )
                Text(
                    text = cell.paymentAmount,
                    style = SSUType.Label2SemiBold,
                )
            }
        }
    }
}

@Composable
fun GraduateRequirementsView(
    state: GraduateUiState,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isLargeScreen: Boolean = false,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(R.string.graduate_title),
            style = SSUType.H1SemiBold,
        )

        Spacer(Modifier.height(16.dp))

        Crossfade(
            targetState = state,
            label = "GraduateStateCrossfade",
            modifier = Modifier.weight(1f),
        ) { uiState ->
            when (uiState) {
                is GraduateUiState.Loading -> {
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

                is GraduateUiState.Success -> {
                    GraduateContent(
                        table = uiState.table,
                        isLargeScreen = isLargeScreen
                    )
                }

                is GraduateUiState.Empty -> {
                    LargeScreenEmptyState(
                        message = stringResource(R.string.graduate_empty_message),
                        onRetry = onRefresh
                    )
                }

                is GraduateUiState.Error -> {
                    LargeScreenEmptyState(
                        message = uiState.message,
                        onRetry = onRefresh
                    )
                }
            }
        }
    }
}

@Composable
private fun GraduateContent(
    table: GraduateTable,
    isLargeScreen: Boolean
) {
    val groups = remember(table) { table.items.groupBy { it.classification } }

    if (isLargeScreen) {
        val classifications = remember(groups) { groups.keys.toList() }
        var selectedClassification by remember(groups) {
            mutableStateOf(classifications.firstOrNull() ?: "")
        }

        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Left 1/3: Classification List (List Pane)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "분류 목록",
                    style = SSUType.H2SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(classifications.size) { index ->
                        val classification = classifications[index]
                        val isSelected = selectedClassification == classification
                        val cells = groups[classification] ?: emptyList()
                        val headerCell = cells.firstOrNull()

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) R500 else N100)
                                .border(1.dp, if (isSelected) R500 else N200, RoundedCornerShape(12.dp))
                                .clickable {
                                    selectedClassification = classification
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = classification,
                                        style = SSUType.Caption1SemiBold,
                                        color = if (isSelected) WHITE else N500
                                    )
                                    if (headerCell != null && headerCell.calculatedValue.isNotBlank()) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = if (headerCell.standardValue.isNotBlank()) {
                                                "${headerCell.calculatedValue} / ${headerCell.standardValue}"
                                            } else {
                                                headerCell.calculatedValue
                                            },
                                            style = SSUType.Caption1Medium,
                                            color = if (isSelected) WHITE.copy(alpha = 0.8f) else N500
                                        )
                                    }
                                }

                                if (headerCell != null && headerCell.result.isNotBlank()) {
                                    val isPass = headerCell.result == "충족" || headerCell.result.lowercase() == "pass" || headerCell.result.lowercase() == "y"
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) {
                                                    WHITE.copy(alpha = 0.2f)
                                                } else {
                                                    if (isPass) G400.copy(alpha = 0.15f) else R500.copy(alpha = 0.15f)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = headerCell.result,
                                            style = SSUType.Caption1SemiBold,
                                            color = if (isSelected) WHITE else (if (isPass) G400 else R500)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Right 2/3: Selected Classification Details (Detail Pane)
            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxHeight()
            ) {
                Text(
                    text = "상세 정보",
                    style = SSUType.H2SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                val cells = groups[selectedClassification] ?: emptyList()
                if (cells.isNotEmpty()) {
                    val headerCell = cells.first()
                    val subCells = cells.drop(1)

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            GraduateDetailPane(
                                classification = selectedClassification,
                                headerCell = headerCell,
                                subCells = subCells
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "선택된 분류의 졸업 요건 상세 정보가 없습니다.",
                            style = SSUType.Body2Medium,
                            color = N500
                        )
                    }
                }
            }
        }
    } else {
        val expandedStates = remember { mutableStateMapOf<String, Boolean>() }

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            groups.forEach { (classification, cells) ->
                item(key = classification) {
                    val isExpanded = expandedStates[classification] ?: false
                    val headerCell = cells.first()
                    val subCells = cells.drop(1)
                    val hasSubItems = subCells.isNotEmpty()

                    GraduateGroupCard(
                        classification = classification,
                        headerCell = headerCell,
                        subCells = subCells,
                        isExpanded = isExpanded,
                        hasSubItems = hasSubItems,
                        onToggleExpand = {
                            if (hasSubItems) {
                                expandedStates[classification] = !isExpanded
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun GraduateGroupCard(
    classification: String,
    headerCell: GraduateTableCell,
    subCells: List<GraduateTableCell>,
    isExpanded: Boolean,
    hasSubItems: Boolean,
    onToggleExpand: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (hasSubItems) {
                        Modifier.clickable { onToggleExpand() }
                    } else {
                        Modifier
                    }
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerCell.classification,
                    style = SSUType.H4SemiBold,
                )
                if (headerCell.requirement.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = headerCell.requirement,
                        style = SSUType.Caption1Medium,
                        color = N500,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (headerCell.calculatedValue.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.graduate_header_earned) + " ",
                                style = SSUType.Caption1Medium,
                                color = N500
                            )
                            Text(
                                text = headerCell.calculatedValue,
                                style = SSUType.Label2SemiBold,
                            )
                            if (headerCell.standardValue.isNotBlank()) {
                                Text(
                                    text = " / " + headerCell.standardValue,
                                    style = SSUType.Caption1SemiBold,
                                    color = N500
                                )
                            }
                        }
                    }
                    if (headerCell.difference.isNotBlank() && headerCell.difference != "0") {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.graduate_header_diff) + ": " + headerCell.difference,
                            style = SSUType.Caption1Medium,
                            color = if (headerCell.difference.startsWith("-")) R500 else G400
                        )
                    }
                }

                if (headerCell.result.isNotBlank()) {
                    Spacer(Modifier.width(16.dp))
                    val isPass = headerCell.result == "충족" || headerCell.result.lowercase() == "pass" || headerCell.result.lowercase() == "y"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPass) G400.copy(alpha = 0.15f) else R500.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = headerCell.result,
                            style = SSUType.Caption1SemiBold,
                            color = if (isPass) G400 else R500
                        )
                    }
                }

                if (hasSubItems) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = N500,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        if (hasSubItems && isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                subCells.forEach { subCell ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(N200)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subCell.requirement.ifBlank { subCell.classification },
                                style = SSUType.Body2Medium,
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (subCell.calculatedValue.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.graduate_header_earned) + " ",
                                            style = SSUType.Caption1Medium,
                                            color = N500
                                        )
                                        Text(
                                            text = subCell.calculatedValue,
                                            style = SSUType.Label2SemiBold,
                                        )
                                        if (subCell.standardValue.isNotBlank()) {
                                            Text(
                                                text = " / " + subCell.standardValue,
                                                style = SSUType.Caption1SemiBold,
                                                color = N500
                                            )
                                        }
                                    }
                                }
                                if (subCell.difference.isNotBlank() && subCell.difference != "0") {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.graduate_header_diff) + ": " + subCell.difference,
                                        style = SSUType.Caption1Medium,
                                        color = if (subCell.difference.startsWith("-")) R500 else G400
                                    )
                                }
                            }

                            if (subCell.result.isNotBlank()) {
                                Spacer(Modifier.width(16.dp))
                                val isPass = subCell.result == "충족" || subCell.result.lowercase() == "pass" || subCell.result.lowercase() == "y"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPass) G400.copy(alpha = 0.15f) else R500.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = subCell.result,
                                        style = SSUType.Caption1SemiBold,
                                        color = if (isPass) G400 else R500
                                    )
                                }
                            }

                            Spacer(Modifier.width(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GraduateDetailPane(
    classification: String,
    headerCell: GraduateTableCell,
    subCells: List<GraduateTableCell>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
    ) {
        // Header Row (similar to GraduateGroupCard but no click and no arrow)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = headerCell.classification,
                    style = SSUType.H4SemiBold,
                )
                if (headerCell.requirement.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = headerCell.requirement,
                        style = SSUType.Caption1Medium,
                        color = N500,
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    if (headerCell.calculatedValue.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(R.string.graduate_header_earned) + " ",
                                style = SSUType.Caption1Medium,
                                color = N500
                            )
                            Text(
                                text = headerCell.calculatedValue,
                                style = SSUType.Label2SemiBold,
                            )
                            if (headerCell.standardValue.isNotBlank()) {
                                Text(
                                    text = " / " + headerCell.standardValue,
                                    style = SSUType.Caption1SemiBold,
                                    color = N500
                                )
                            }
                        }
                    }
                    if (headerCell.difference.isNotBlank() && headerCell.difference != "0") {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.graduate_header_diff) + ": " + headerCell.difference,
                            style = SSUType.Caption1Medium,
                            color = if (headerCell.difference.startsWith("-")) R500 else G400
                        )
                    }
                }

                if (headerCell.result.isNotBlank()) {
                    Spacer(Modifier.width(16.dp))
                    val isPass = headerCell.result == "충족" || headerCell.result.lowercase() == "pass" || headerCell.result.lowercase() == "y"
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPass) G400.copy(alpha = 0.15f) else R500.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = headerCell.result,
                            style = SSUType.Caption1SemiBold,
                            color = if (isPass) G400 else R500
                        )
                    }
                }
            }
        }

        // Subcells (always expanded)
        if (subCells.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                subCells.forEach { subCell ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(N200)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subCell.requirement.ifBlank { subCell.classification },
                                style = SSUType.Body2Medium,
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                if (subCell.calculatedValue.isNotBlank()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = stringResource(R.string.graduate_header_earned) + " ",
                                            style = SSUType.Caption1Medium,
                                            color = N500
                                        )
                                        Text(
                                            text = subCell.calculatedValue,
                                            style = SSUType.Label2SemiBold,
                                        )
                                        if (subCell.standardValue.isNotBlank()) {
                                            Text(
                                                text = " / " + subCell.standardValue,
                                                style = SSUType.Caption1SemiBold,
                                                color = N500
                                            )
                                        }
                                    }
                                }
                                if (subCell.difference.isNotBlank() && subCell.difference != "0") {
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = stringResource(R.string.graduate_header_diff) + ": " + subCell.difference,
                                        style = SSUType.Caption1Medium,
                                        color = if (subCell.difference.startsWith("-")) R500 else G400
                                    )
                                }
                            }

                            if (subCell.result.isNotBlank()) {
                                Spacer(Modifier.width(16.dp))
                                val isPass = subCell.result == "충족" || subCell.result.lowercase() == "pass" || subCell.result.lowercase() == "y"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isPass) G400.copy(alpha = 0.15f) else R500.copy(alpha = 0.15f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = subCell.result,
                                        style = SSUType.Caption1SemiBold,
                                        color = if (isPass) G400 else R500
                                    )
                                }
                            }

                            // Keep space consistent
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LargeScreenEmptyState(
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

@Composable
fun GradeHistoryView(
    summaryState: GradeSummaryUiState,
    detailState: GradeDetailUiState,
    selectedSemesterKey: String,
    onSelectedSemesterKeyChange: (String) -> Unit,
    currentSemesterName: String,
    onCurrentSemesterNameChange: (String) -> Unit,
    thisSemesterYear: String?,
    onThisSemesterYearChange: (String?) -> Unit,
    thisSemesterType: io.github.chlwhdtn03.data.Lms.Semester?,
    onThisSemesterTypeChange: (io.github.chlwhdtn03.data.Lms.Semester?) -> Unit,
    onLoadDetail: (String?, io.github.chlwhdtn03.data.Lms.Semester?, Boolean) -> Unit,
    onRefreshSummary: () -> Unit,
    modifier: Modifier = Modifier,
    isLargeScreen: Boolean = false,
) {
    LaunchedEffect(Unit) {
        onRefreshSummary()
        onLoadDetail(null, null, false)
    }

    LaunchedEffect(detailState) {
        if (selectedSemesterKey == "current" && detailState is GradeDetailUiState.Success) {
            val table = detailState.table
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
                        val currentCell = if (summaryState is GradeSummaryUiState.Success && thisSemesterYear != null && thisSemesterType != null) {
                            summaryState.table.items.firstOrNull { it.year == thisSemesterYear && it.semester == thisSemesterType }
                        } else {
                            null
                        }
                        val gpa = currentCell?.gpa?.takeIf { it.isNotBlank() }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) R500 else N100)
                                .border(1.dp, if (isSelected) R500 else N200, RoundedCornerShape(12.dp))
                                .clickable {
                                    onSelectedSemesterKeyChange("current")
                                    onLoadDetail(null, null, false)
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = currentSemesterName,
                                    style = SSUType.Caption1SemiBold,
                                    color = if (isSelected) WHITE else N500,
                                    modifier = Modifier.weight(1f)
                                )
                                if (gpa != null) {
                                    Text(
                                        text = gpa,
                                        style = SSUType.Caption1SemiBold,
                                        color = if (isSelected) WHITE.copy(alpha = 0.9f) else R500
                                    )
                                }
                            }
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
                            val gpa = cell.gpa.takeIf { it.isNotBlank() }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) R500 else N100)
                                    .border(1.dp, if (isSelected) R500 else N200, RoundedCornerShape(12.dp))
                                    .clickable {
                                        onSelectedSemesterKeyChange(key)
                                        cell.semester?.let { sem ->
                                            onLoadDetail(cell.year, sem, false)
                                        }
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${cell.year}학년도 $semesterName",
                                        style = SSUType.Caption1SemiBold,
                                        color = if (isSelected) WHITE else N500,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (gpa != null) {
                                        Text(
                                            text = gpa,
                                            style = SSUType.Caption1SemiBold,
                                            color = if (isSelected) WHITE.copy(alpha = 0.9f) else R500
                                        )
                                    }
                                }
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
                    text = "성적 상세",
                    style = SSUType.H2SemiBold,
                )
                Spacer(Modifier.height(16.dp))

                if (summaryState is GradeSummaryUiState.Success) {
                    val summaryCells = summaryState.table.items
                    val currentSummaryCell = if (selectedSemesterKey == "current") {
                        val detailTable = (detailState as? GradeDetailUiState.Success)?.table
                        if (detailTable != null) {
                            summaryCells.firstOrNull { it.year == detailTable.year && it.semester == detailTable.semester }
                        } else {
                            null
                        }
                    } else {
                        summaryCells.firstOrNull { "${it.year}-${it.semester?.name ?: ""}" == selectedSemesterKey }
                    }

                    currentSummaryCell?.let { cell ->
                        GradeSummaryCard(cell = cell)
                        Spacer(Modifier.height(16.dp))
                    }
                }

                Crossfade(
                    targetState = detailState,
                    label = "GradeDetailStateCrossfadeLarge",
                    modifier = Modifier.weight(1f)
                ) { detailUiState ->
                    when (detailUiState) {
                        is GradeDetailUiState.Loading -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = R500, trackColor = R100)
                            }
                        }
                        is GradeDetailUiState.Success -> {
                            val detailItems = detailUiState.table.items
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(detailItems.size) { index ->
                                    val gradeCell = detailItems[index]
                                    GradeCellCard(cell = gradeCell)
                                }
                            }
                        }
                        is GradeDetailUiState.Empty -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(text = "상세 성적 내역이 없습니다.", style = SSUType.Body2Regular, color = N500)
                            }
                        }
                        is GradeDetailUiState.Error -> {
                            val summaryCells = (summaryState as? GradeSummaryUiState.Success)?.table?.items ?: emptyList()
                            LargeScreenEmptyState(
                                message = detailUiState.message,
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
                text = "성적 조회",
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
                    // 이번 학기 정보(thisSemesterYear/Type)가 요약 목록에 존재하는 경우에만 요약 목록에서 해당 항목 제거 (중복 제거)
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
                                .border(1.dp, if (isSelected) R500 else N200, RoundedCornerShape(20.dp))
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

            // 2. 선택된 학기 요약 (요약 데이터가 존재할 때만)
            if (summaryState is GradeSummaryUiState.Success) {
                val summaryCells = summaryState.table.items
                val currentSummaryCell = if (selectedSemesterKey == "current") {
                    val detailTable = (detailState as? GradeDetailUiState.Success)?.table
                    if (detailTable != null) {
                        summaryCells.firstOrNull { it.year == detailTable.year && it.semester == detailTable.semester }
                    } else {
                        null
                    }
                } else {
                    summaryCells.firstOrNull { "${it.year}-${it.semester?.name ?: ""}" == selectedSemesterKey }
                }

                currentSummaryCell?.let { cell ->
                    GradeSummaryCard(cell = cell)
                    Spacer(Modifier.height(16.dp))
                }
            }

            // 3. 세부 성적 리스트
            Text(
                text = "세부 성적",
                style = SSUType.H3SemiBold,
            )

            Spacer(Modifier.height(8.dp))

            Crossfade(
                targetState = detailState,
                label = "GradeDetailStateCrossfade",
                modifier = Modifier.weight(1f)
            ) { detailUiState ->
                when (detailUiState) {
                    is GradeDetailUiState.Loading -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = R500, trackColor = R100)
                        }
                    }
                    is GradeDetailUiState.Success -> {
                        val detailItems = detailUiState.table.items
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(detailItems.size) { index ->
                                val gradeCell = detailItems[index]
                                GradeCellCard(cell = gradeCell)
                            }
                        }
                    }
                    is GradeDetailUiState.Empty -> {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(text = "상세 성적 내역이 없습니다.", style = SSUType.Body2Regular, color = N500)
                        }
                    }
                    is GradeDetailUiState.Error -> {
                        val summaryCells = (summaryState as? GradeSummaryUiState.Success)?.table?.items ?: emptyList()
                        LargeScreenEmptyState(
                            message = detailUiState.message,
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
private fun GradeSummaryCard(cell: io.github.chlwhdtn03.data.Lms.SemesterGradeSummaryCell) {
    val semesterName = cell.semester?.nameKor ?: ""
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = "${cell.year}학년도 $semesterName 요약",
            style = SSUType.H4SemiBold,
            color = R500
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "평점평균 (GPA)", style = SSUType.Caption1Medium, color = N500)
                Spacer(Modifier.height(4.dp))
                Text(text = cell.gpa.ifBlank { "-" }, style = SSUType.H3SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "취득학점", style = SSUType.Caption1Medium, color = N500)
                Spacer(Modifier.height(4.dp))
                Text(text = cell.earnedCredits.ifBlank { "-" }, style = SSUType.H3SemiBold)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text(text = "학기석차", style = SSUType.Caption1Medium, color = N500)
                Spacer(Modifier.height(4.dp))
                Text(text = cell.semesterRank.ifBlank { "-" }, style = SSUType.H3SemiBold)
            }
        }

        if (cell.academicWarning.isNotBlank() && cell.academicWarning != "N" && cell.academicWarning != "0") {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(R500.copy(alpha = 0.1f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚠️ 학사경고 대상자입니다. (${cell.academicWarning})",
                    style = SSUType.Caption1SemiBold,
                    color = R500
                )
            }
        }
    }
}

@Composable
private fun GradeCellCard(cell: io.github.chlwhdtn03.data.Lms.GradeCell) {
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
                text = "${cell.credits}학점",
                style = SSUType.Caption2Medium,
                color = N500
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = cell.subjectName,
                style = SSUType.Label2SemiBold,
            )
            if (cell.professor.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cell.professor,
                    style = SSUType.Caption1Medium,
                    color = N500
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(horizontalAlignment = Alignment.End) {
            // ABCD 성적 (gradePoint)을 크게 표시
            Text(
                text = cell.gradePoint,
                style = SSUType.H3SemiBold,
                color = if (cell.gradePoint.startsWith("A") || cell.gradePoint.startsWith("B")) G500 else R500
            )
            // 수치 점수/평점 (grade)을 작게 표시
            if (cell.grade.isNotBlank()) {
                val pointText = if (cell.grade.endsWith("점") || cell.grade.contains(".")) cell.grade else "${cell.grade}점"
                Text(
                    text = pointText,
                    style = SSUType.Caption2Medium,
                    color = N500
                )
            }
        }
    }
}
