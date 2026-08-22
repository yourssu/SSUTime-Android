package com.yourssu.ssutime.v2.screen.my

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourssu.data.SubjectInfo
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.component.SSUTimeTopBar
import com.yourssu.ssutime.v2.screen.calendar.badgeBackgroundColor
import com.yourssu.ssutime.v2.screen.calendar.badgeTextColor
import com.yourssu.ssutime.v2.todo.labelResId
import com.yourssu.ssutime.v2.ui.theme.BLACK
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.N700
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HiddenTodosScreen(
    modifier: Modifier = Modifier,
    viewModel: MyViewModel = koinViewModel(),
) {
    val hiddenTodos by viewModel.hiddenTodos.collectAsStateWithLifecycle()

    HiddenTodosContent(
        modifier = modifier,
        hiddenTodos = hiddenTodos,
        onRestoreClick = { todo -> viewModel.restoreTodo(todo) }
    )
}

@Composable
fun HiddenTodosContent(
    modifier: Modifier = Modifier,
    hiddenTodos: List<TodoInfo>,
    onRestoreClick: (TodoInfo) -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
    ) {
        SSUTimeTopBar()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.my_hidden_todos),
                style = SSUType.H2SemiBold,
                color = BLACK
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "${hiddenTodos.size}",
                style = SSUType.H2SemiBold,
                color = R400
            )
        }

        if (hiddenTodos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.done),
                    contentDescription = null
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.hidden_todos_empty),
                    style = SSUType.H3Medium,
                    color = BLACK
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = hiddenTodos,
                    key = { it.todoId.takeIf { id -> id > 0 }?.toString() ?: "${it.title}_${it.due_date}" }
                ) { todo ->
                    HiddenTodoItem(
                        todo = todo,
                        onRestoreClick = { onRestoreClick(todo) }
                    )
                }
            }
        }
    }
}

@Composable
fun HiddenTodoItem(
    modifier: Modifier = Modifier,
    todo: TodoInfo,
    onRestoreClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .padding(horizontal = 20.dp, vertical = 18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(todo.type.badgeBackgroundColor())
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(todo.type.labelResId()),
                            style = SSUType.Caption1SemiBold,
                            color = todo.type.badgeTextColor()
                        )
                    }
                    Text(
                        text = todo.subject?.name ?: stringResource(R.string.common_unknown_subject),
                        style = SSUType.H5SemiBold,
                        color = N500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = todo.title,
                    style = SSUType.H4SemiBold,
                    color = N700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(N500)
                    .clickable { onRestoreClick() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.hidden_todos_restore),
                    style = SSUType.Label3Medium,
                    color = WHITE
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun previewHiddenTodosScreenNotEmpty() {
    HiddenTodosContent(
        hiddenTodos = listOf(
            TodoInfo(
                todoId = 1,
                title = "레포트34",
                due_date = "2026-05-29T18:00:00Z",
                type = TodoType.QUIZ,
                subject = SubjectInfo(1, "컴학", "")
            ),
            TodoInfo(
                todoId = 2,
                title = "레포트34",
                due_date = "2026-05-29T18:00:00Z",
                type = TodoType.QUIZ,
                subject = SubjectInfo(1, "컴학", "")
            )
        ),
        onRestoreClick = {}
    )
}

@Preview(showBackground = true)
@Composable
fun previewHiddenTodosScreenEmpty() {
    HiddenTodosContent(
        hiddenTodos = emptyList(),
        onRestoreClick = {}
    )
}
