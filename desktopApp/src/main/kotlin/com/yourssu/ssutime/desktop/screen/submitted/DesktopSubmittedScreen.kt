package com.yourssu.ssutime.desktop.screen.submitted

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.core.model.AppTodo
import com.yourssu.ssutime.desktop.core.model.AppTodoType
import com.yourssu.ssutime.desktop.core.model.desktopItemKey
import com.yourssu.ssutime.desktop.core.model.toSimply
import com.yourssu.ssutime.desktop.ui.component.DesktopBackButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.common_unknown_subject
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.main_submitted_empty
import com.yourssu.ssutime.desktop.ui.resources.main_submitted_sort_latest
import com.yourssu.ssutime.desktop.ui.resources.main_submitted_title
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_back
import com.yourssu.ssutime.desktop.ui.resources.todo_type_assignment
import com.yourssu.ssutime.desktop.ui.resources.todo_type_lecture
import com.yourssu.ssutime.desktop.ui.resources.todo_type_quiz
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted
import com.yourssu.ssutime.desktop.ui.resources.todo_type_submitted_late
import com.yourssu.ssutime.desktop.ui.theme.G100
import com.yourssu.ssutime.desktop.ui.theme.G400
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N400
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.R100
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopSubmittedScreen(
    submitted: List<AppTodo>,
    isEnableSubmittedFile: Boolean = false,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            DesktopBackButton(onClick = onBack)
            Spacer(Modifier.width(8.dp))
            Text(
                text = stringResource(Res.string.main_submitted_title),
                style = SSUType.H3SemiBold,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = submitted.size.toString(),
                style = SSUType.H3SemiBold.copy(color = R400),
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(Res.string.main_submitted_sort_latest),
                style = SSUType.Caption1SemiBold,
                color = N400,
            )
        }

        Spacer(Modifier.height(16.dp))

        if (submitted.isNotEmpty()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                itemsIndexed(
                    items = submitted,
                    key = { index, item -> item.desktopItemKey(index) },
                ) { _, todo ->
                    DesktopSubmittedItem(
                        todo = todo,
                        isEnableSubmittedFile = isEnableSubmittedFile,
                        onOpenUrl = onOpenUrl,
                    )
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
                    modifier = Modifier.padding(vertical = 50.dp),
                    text = stringResource(Res.string.main_submitted_empty),
                    style = SSUType.H3Medium,
                    color = N400,
                )
            }
        }
    }
}

@Composable
fun DesktopSubmittedItem(
    todo: AppTodo,
    isEnableSubmittedFile: Boolean = false,
    onOpenUrl: (String) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = todo.subject?.name
                            ?: stringResource(Res.string.common_unknown_subject),
                        style = SSUType.Caption1SemiBold,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = todo.title,
                        style = SSUType.H5SemiBold,
                        maxLines = 1,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = todo.type.localizedLabel(),
                    style = SSUType.Caption2Medium.copy(
                        color = if (todo.type == AppTodoType.SUBMITTED_LATE) R400 else G400,
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (todo.type == AppTodoType.SUBMITTED_LATE) R100 else G100,
                        )
                        .padding(6.dp),
                )
            }

            if (isEnableSubmittedFile && todo.attachments.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = N200,
                    thickness = 0.5.dp,
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    todo.attachments.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, N200, RoundedCornerShape(8.dp))
                                .background(WHITE)
                                .clickable {
                                    if (item.url.isNotBlank()) {
                                        onOpenUrl(item.url)
                                    }
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "첨부파일",
                                tint = N400,
                                modifier = Modifier.size(20.dp),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = item.display_name,
                                style = SSUType.Label2Medium,
                                color = N500,
                                maxLines = 1,
                                modifier = Modifier
                                    .weight(1f)
                                    .basicMarquee(),
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = item.size.toSimply(),
                                style = SSUType.Caption1Medium,
                                color = N400,
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Icon(
                                imageVector = Icons.Outlined.FileDownload,
                                contentDescription = "${item.display_name} 다운로드",
                                tint = N400,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppTodoType.localizedLabel(): String = stringResource(
    when (this) {
        AppTodoType.COMMONS -> Res.string.todo_type_lecture
        AppTodoType.ASSIGNMENT -> Res.string.todo_type_assignment
        AppTodoType.QUIZ -> Res.string.todo_type_quiz
        AppTodoType.SUBMITTED -> Res.string.todo_type_submitted
        AppTodoType.SUBMITTED_LATE -> Res.string.todo_type_submitted_late
    },
)
