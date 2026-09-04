package com.yourssu.ssutime.v2.screen.main

import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.todo.localizedLabel
import com.yourssu.ssutime.v2.ui.theme.G100
import com.yourssu.ssutime.v2.ui.theme.G400
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N400
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE

@Composable
fun SubmittedScreen(
    modifier: Modifier = Modifier,
    submitted: List<TodoInfo> = emptyList(),
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = stringResource(R.string.main_submitted_title),
                style = SSUType.H3SemiBold
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = submitted.size.toString(),
                style = SSUType.H3SemiBold.copy(color = R400)
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.main_submitted_sort_latest),
                style = SSUType.Caption1SemiBold,
                color = N400,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (submitted.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = submitted,
                    key = { item -> item.todoId }
                ) {
                    SubmittedItem(it)
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    modifier = Modifier.padding(vertical = 50.dp),
                    text = stringResource(R.string.main_submitted_empty),
                    style = SSUType.H3Medium
                )
            }
        }
    }
}

@Composable
fun SubmittedItem(
    todoInfo: TodoInfo
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            maxLines = 1,
                            text = todoInfo.subject?.name ?: stringResource(R.string.common_unknown_subject),
                            style = SSUType.Caption1SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        maxLines = 1,
                        text = todoInfo.title,
                        style = SSUType.H5SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))

                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (todoInfo.type == TodoType.SUBMITTED_LATE) R100 else G100)
                        .padding(6.dp),
                    text = todoInfo.type.localizedLabel(),
                    style = SSUType.Caption2Medium.copy(color = if (todoInfo.type == TodoType.SUBMITTED_LATE) R400 else G400)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SubmittedScreenPreview() {
    SubmittedScreen(
        submitted = listOf(
            TodoInfo(
                todoId = 1,
                title = "과제 1 제출 완료",
                due_date = "2026-09-04T00:00:00Z",
                type = TodoType.SUBMITTED,
                subject = SubjectInfo(1, "컴퓨터네트워크", "")
            ),
            TodoInfo(
                todoId = 2,
                title = "지각 제출 과제",
                due_date = "2026-09-03T00:00:00Z",
                type = TodoType.SUBMITTED_LATE,
                subject = SubjectInfo(2, "운영체제", "")
            )
        )
    )
}

@Preview(showBackground = true)
@Composable
private fun SubmittedScreenEmptyPreview() {
    SubmittedScreen(
        submitted = emptyList()
    )
}
