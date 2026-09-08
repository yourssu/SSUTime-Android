package com.yourssu.ssutime.desktop.screen.my

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.aiSummaryKey
import com.yourssu.ssutime.desktop.core.model.desktopItemKey
import com.yourssu.ssutime.desktop.screen.calendar.badgeBackgroundColor
import com.yourssu.ssutime.desktop.screen.calendar.badgeTextColor
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.common_unknown_subject
import com.yourssu.ssutime.desktop.ui.resources.done
import com.yourssu.ssutime.desktop.ui.resources.hidden_todos_empty
import com.yourssu.ssutime.desktop.ui.resources.hidden_todos_restore
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.my_hidden_todos
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_back
import com.yourssu.ssutime.desktop.ui.resources.todo_type_assignment
import com.yourssu.ssutime.desktop.ui.resources.todo_type_lecture
import com.yourssu.ssutime.desktop.ui.resources.todo_type_quiz
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted_late
import com.yourssu.ssutime.desktop.ui.theme.BLACK
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.N600
import com.yourssu.ssutime.desktop.ui.theme.N700
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopHiddenTodosScreen(
    hiddenTodos: List<AppTodo>,
    onBack: () -> Unit,
    onRestoreClick: (AppTodo) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(Res.string.todo_detail_back),
                    tint = N600,
                    modifier = Modifier.size(20.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.my_hidden_todos),
                    style = SSUType.H2SemiBold,
                    color = BLACK,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "${hiddenTodos.size}",
                    style = SSUType.H2SemiBold,
                    color = R400,
                )
            }
        }

        if (hiddenTodos.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.done),
                    contentDescription = null,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.hidden_todos_empty),
                    style = SSUType.H3Medium,
                    color = BLACK,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(
                    items = hiddenTodos,
                    key = { index, item -> item.desktopItemKey(index) },
                ) { _, todo ->
                    DesktopHiddenTodoItem(
                        todo = todo,
                        onRestoreClick = { onRestoreClick(todo) },
                    )
                }
            }
        }
    }
}

@Composable
private fun DesktopHiddenTodoItem(
    todo: AppTodo,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(todo.type.badgeBackgroundColor())
                            .padding(horizontal = 6.dp, vertical = 2.dp),
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
                        text = todo.subject?.name
                            ?: stringResource(Res.string.common_unknown_subject),
                        style = SSUType.H5SemiBold,
                        color = N500,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = todo.title,
                    style = SSUType.H4SemiBold,
                    color = N700,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(N500)
                    .clickable(onClick = onRestoreClick)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.hidden_todos_restore),
                    style = SSUType.Label3Medium,
                    color = WHITE,
                )
            }
        }
    }
}
