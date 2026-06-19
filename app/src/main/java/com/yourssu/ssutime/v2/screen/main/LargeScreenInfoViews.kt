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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.ui.theme.G400
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
                        table = uiState.table
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
                    ScholarshipContent(table = defaultTable)
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
private fun ScholarshipContent(table: ScholarshipHistoryTable) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(table.items) { cell ->
            ScholarshipCellCard(cell = cell)
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
                Text(
                    text = cell.processStatus,
                    style = SSUType.Body2Medium,
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
                        style = SSUType.Body2Medium,
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
                        style = SSUType.Body2Medium,
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
                        table = uiState.table
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
private fun TuitionContent(table: TuitionTable) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(table.items.reversed()) { cell ->
            TuitionCellCard(cell = cell)
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
                        table = uiState.table
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
private fun GraduateContent(table: GraduateTable) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(table.items) { cell ->
            GraduateCellCard(cell = cell)
        }
    }
}

@Composable
private fun GraduateCellCard(cell: GraduateTableCell) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .border(1.dp, N200, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1.3f)) {
            Text(
                text = cell.classification,
                style = SSUType.H4SemiBold,
            )
            if (cell.requirement.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = cell.requirement,
                    style = SSUType.Caption1Medium,
                    color = N500,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        Row(
            modifier = Modifier.weight(2f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.graduate_header_earned) + " ",
                        style = SSUType.Caption1Medium,
                        color = N500
                    )
                    Text(
                        text = cell.calculatedValue,
                        style = SSUType.Label2SemiBold,
                    )
                    Text(
                        text = " / " + cell.standardValue,
                        style = SSUType.Caption1SemiBold,
                        color = N500
                    )
                }
                if (cell.difference.isNotBlank() && cell.difference != "0") {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.graduate_header_diff) + ": " + cell.difference,
                        style = SSUType.Caption1Medium,
                        color = if (cell.difference.startsWith("-")) R500 else G400
                    )
                }
            }

            if (cell.result.isNotBlank()) {
                Spacer(Modifier.width(16.dp))
                val isPass = cell.result == "충족" || cell.result.lowercase() == "pass" || cell.result.lowercase() == "y"
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPass) G400.copy(alpha = 0.15f) else R500.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = cell.result,
                        style = SSUType.Caption1SemiBold,
                        color = if (isPass) G400 else R500
                    )
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
