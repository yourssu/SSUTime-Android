package com.yourssu.ssutime.desktop.screen.notice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourssu.data.DiscussionAttachment
import com.yourssu.data.DiscussionInfo
import com.yourssu.ssutime.desktop.core.model.AppSubject
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.icon_collapsed
import com.yourssu.ssutime.desktop.ui.resources.icon_expand
import com.yourssu.ssutime.desktop.ui.resources.notice_empty
import com.yourssu.ssutime.desktop.ui.resources.notice_title
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_back
import com.yourssu.ssutime.desktop.ui.theme.BLACK
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N400
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.N600
import com.yourssu.ssutime.desktop.ui.theme.N700
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import com.yourssu.ssutime.desktop.ui.util.parseHtmlToPlainText
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatDiscussionDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return runCatching {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd. a h:mm", Locale.KOREA)
        instant.atZone(ZoneId.of("Asia/Seoul")).format(formatter)
    }.getOrElse { dateString }
}

@Composable
fun DesktopNoticeScreen(
    subjects: List<AppSubject>,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onDiscussionExpanded: (DiscussionInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubjectIndex by remember { mutableIntStateOf(0) }
    val validIndex = selectedSubjectIndex.coerceIn(0, (subjects.size - 1).coerceAtLeast(0))
    val currentSubject = subjects.getOrNull(validIndex)

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

            Text(
                text = stringResource(Res.string.notice_title),
                style = SSUType.H2SemiBold,
                color = BLACK,
            )

            Spacer(modifier = Modifier.height(14.dp))

            if (subjects.isNotEmpty()) {
                NoticeSubjectDropdown(
                    subjects = subjects,
                    selectedIndex = validIndex,
                    onSelectSubject = { selectedSubjectIndex = it },
                )
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            if (currentSubject != null && currentSubject.discussions.isNotEmpty()) {
                itemsIndexed(
                    items = currentSubject.discussions,
                    key = { index, item -> "${item.id}_${item.title}_${item.createdAt}_$index" },
                ) { index, discussion ->
                    NoticeAccordionItem(
                        discussion = discussion,
                        onExpanded = { onDiscussionExpanded(discussion) },
                        onAttachmentClick = {
                            val targetUrl = discussion.attachments.firstOrNull()?.url?.takeIf(String::isNotBlank)
                                ?: discussion.url
                            if (targetUrl.isNotBlank()) {
                                onOpenUrl(targetUrl)
                            }
                        },
                    )

                    if (index < currentSubject.discussions.lastIndex) {
                        HorizontalDivider(
                            color = N200,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 14.dp),
                        )
                    } else {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = stringResource(Res.string.notice_empty),
                            style = SSUType.Body1Medium,
                            color = N400,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NoticeSubjectDropdown(
    subjects: List<AppSubject>,
    selectedIndex: Int,
    onSelectSubject: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentSubject = subjects.getOrNull(selectedIndex)
    val selectedSubjectName = currentSubject?.name.orEmpty()
    val selectedUnreadCount = currentSubject?.discussions
        ?.count { it.readState.equals("unread", ignoreCase = true) } ?: 0

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = N200, shape = RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = selectedSubjectName,
                    style = SSUType.H4Medium,
                    color = N600,
                )

                if (selectedUnreadCount > 0) {
                    Text(
                        text = "$selectedUnreadCount",
                        style = SSUType.Caption1SemiBold,
                        color = R400,
                    )
                }
            }

            Icon(
                painter = painterResource(if (expanded) Res.drawable.icon_expand else Res.drawable.icon_collapsed),
                contentDescription = null,
                tint = N400,
                modifier = Modifier.size(18.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(WHITE),
        ) {
            subjects.forEachIndexed { index, subject ->
                val unreadCount = subject.discussions.count { it.readState.equals("unread", ignoreCase = true) }
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                text = subject.name,
                                style = SSUType.Body1Regular,
                                color = if (index == selectedIndex) N700 else N500,
                            )

                            if (unreadCount > 0) {
                                Text(
                                    text = "$unreadCount",
                                    style = SSUType.Caption1SemiBold,
                                    color = R400,
                                )
                            }
                        }
                    },
                    onClick = {
                        onSelectSubject(index)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoticeAccordionItem(
    discussion: DiscussionInfo,
    onExpanded: () -> Unit,
    onAttachmentClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember(discussion.id) { mutableStateOf(false) }
    val formattedDate = remember(discussion.createdAt) { formatDiscussionDate(discussion.createdAt) }
    val plainContent = remember(discussion.message) { parseHtmlToPlainText(discussion.message) }
    val isNew = discussion.readState.equals("unread", ignoreCase = true)
    val hasAttachment = discussion.attachments.isNotEmpty()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val nextExpanded = !isExpanded
                    isExpanded = nextExpanded
                    if (nextExpanded && isNew) {
                        onExpanded()
                    }
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                FlowRow(
                    verticalArrangement = Arrangement.Center,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = discussion.title,
                        style = SSUType.H4Medium,
                        color = N700,
                    )

                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(R400)
                                .align(Alignment.CenterVertically),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "N",
                                style = SSUType.Caption2SemiBold,
                                color = WHITE,
                            )
                        }
                    }
                }

                if (formattedDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        style = SSUType.Caption1Medium,
                        color = N400,
                    )
                }
            }

            Icon(
                painter = painterResource(if (isExpanded) Res.drawable.icon_expand else Res.drawable.icon_collapsed),
                contentDescription = null,
                tint = N400,
                modifier = Modifier.size(20.dp),
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            ) {
                if (plainContent.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(N100)
                            .padding(16.dp),
                    ) {
                        Text(
                            text = plainContent,
                            style = SSUType.Body1Regular,
                            color = N500,
                        )
                    }
                }

                if (hasAttachment) {
                    DesktopDiscussionAttachmentButtonGroup(
                        attachments = discussion.attachments,
                        onOpenUrl = { onAttachmentClick() },
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DesktopDiscussionAttachmentButtonGroup(
    attachments: List<DiscussionAttachment>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        attachments.forEachIndexed { index, attachment ->
            val buttonText = if (attachment.name.isNotBlank()) {
                "첨부파일: ${attachment.name}"
            } else {
                "첨부파일 ${index + 1}"
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(N600)
                    .clickable { onOpenUrl(attachment.url) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = buttonText,
                    style = SSUType.Caption1SemiBold,
                    color = WHITE,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
