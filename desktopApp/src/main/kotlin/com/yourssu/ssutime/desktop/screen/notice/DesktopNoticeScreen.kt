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
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourssu.data.DiscussionAttachment
import com.yourssu.data.DiscussionInfo
import com.yourssu.ssutime.desktop.analytics.DesktopAnalytics
import com.yourssu.ssutime.desktop.core.model.AppSubject
import com.yourssu.ssutime.desktop.ui.component.DesktopBackButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.common_attachment
import com.yourssu.ssutime.desktop.ui.resources.common_attachment_numbered
import com.yourssu.ssutime.desktop.ui.resources.common_download
import com.yourssu.ssutime.desktop.ui.resources.notice_collapse
import com.yourssu.ssutime.desktop.ui.resources.notice_empty
import com.yourssu.ssutime.desktop.ui.resources.notice_expand
import com.yourssu.ssutime.desktop.ui.resources.notice_has_attachment
import com.yourssu.ssutime.desktop.ui.resources.notice_inline_attachment_tag
import com.yourssu.ssutime.desktop.ui.resources.notice_select_subject
import com.yourssu.ssutime.desktop.ui.resources.notice_title
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
import com.yourssu.ssutime.desktop.ui.util.parseHtmlToAnnotatedString
import org.jetbrains.compose.resources.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val INLINE_ATTACHMENT_ID = "inline_attachment"
private const val INLINE_NEW_BADGE_ID = "inline_new_badge"

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
    var selectedSubjectIndex by rememberSaveable { mutableIntStateOf(0) }
    val validIndex = selectedSubjectIndex.coerceIn(0, (subjects.size - 1).coerceAtLeast(0))
    val currentSubject = subjects.getOrNull(validIndex)

    LaunchedEffect(Unit) {
        DesktopAnalytics.viewNotice()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    DesktopBackButton(onClick = onBack)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(Res.string.notice_title),
                        style = SSUType.H2SemiBold,
                        color = BLACK,
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (subjects.isNotEmpty()) {
                    NoticeSubjectDropdown(
                        subjects = subjects,
                        selectedIndex = validIndex,
                        onSelectSubject = { selectedSubjectIndex = it },
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentSubject != null && currentSubject.discussions.isNotEmpty()) {
                itemsIndexed(
                    items = currentSubject.discussions,
                    key = { _, item -> item.id },
                ) { index, discussion ->
                    NoticeAccordionItem(
                        discussion = discussion,
                        onExpanded = { onDiscussionExpanded(discussion) },
                        onOpenUrl = onOpenUrl,
                    )

                    if (index < currentSubject.discussions.lastIndex) {
                        HorizontalDivider(
                            color = N200,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 16.dp),
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
                .padding(horizontal = 16.dp, vertical = 14.dp),
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
                        style = SSUType.H4Medium,
                        color = R400,
                    )
                }
            }

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = stringResource(Res.string.notice_select_subject),
                tint = N400,
                modifier = Modifier.size(24.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(WHITE)
                .fillMaxWidth(0.9f),
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

@Composable
private fun NoticeAccordionItem(
    discussion: DiscussionInfo,
    onExpanded: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable(discussion.id) { mutableStateOf(false) }
    val formattedDate = remember(discussion.createdAt) { formatDiscussionDate(discussion.createdAt) }
    val annotatedContent = remember(discussion.message) { parseHtmlToAnnotatedString(discussion.message) }
    val isNew = discussion.readState.equals("unread", ignoreCase = true)
    val hasAttachment = discussion.attachments.isNotEmpty()

    val inlineAttachmentTag = stringResource(Res.string.notice_inline_attachment_tag)
    val annotatedTitle = remember(discussion.title, hasAttachment, isNew, inlineAttachmentTag) {
        buildAnnotatedString {
            append(discussion.title)
            if (hasAttachment) {
                append("\u00A0")
                appendInlineContent(INLINE_ATTACHMENT_ID, inlineAttachmentTag)
            }
            if (isNew) {
                append("\u00A0")
                appendInlineContent(INLINE_NEW_BADGE_ID, "[N]")
            }
        }
    }

    val inlineContentMap = remember(hasAttachment, isNew) {
        mapOf(
            INLINE_ATTACHMENT_ID to InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Icon(
                    imageVector = Icons.Default.AttachFile,
                    contentDescription = stringResource(Res.string.notice_has_attachment),
                    tint = N400,
                    modifier = Modifier.size(16.dp),
                )
            },
            INLINE_NEW_BADGE_ID to InlineTextContent(
                Placeholder(
                    width = 16.sp,
                    height = 16.sp,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                ),
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(R400),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "N",
                        style = SSUType.Caption2SemiBold,
                        color = WHITE,
                    )
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    val nextExpanded = !isExpanded
                    isExpanded = nextExpanded
                    if (nextExpanded) {
                        DesktopAnalytics.noticeExpand(isUnread = isNew)
                    }
                    if (nextExpanded && isNew) {
                        onExpanded()
                    }
                }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = annotatedTitle,
                    inlineContent = inlineContentMap,
                    style = SSUType.H4Medium,
                    color = N700,
                    modifier = Modifier.fillMaxWidth(),
                )

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
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) stringResource(Res.string.notice_collapse) else stringResource(Res.string.notice_expand),
                tint = N400,
                modifier = Modifier.size(24.dp),
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
                if (annotatedContent.text.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(N100)
                            .padding(16.dp),
                    ) {
                        SelectionContainer {
                            Text(
                                text = annotatedContent,
                                style = SSUType.Body1Regular,
                                color = N500,
                            )
                        }
                    }
                }

                if (hasAttachment) {
                    DiscussionAttachmentSection(
                        attachments = discussion.attachments,
                        onOpenUrl = onOpenUrl,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DiscussionAttachmentSection(
    attachments: List<DiscussionAttachment>,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (attachments.isEmpty()) return

    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(Res.string.common_attachment),
            style = SSUType.Caption1SemiBold,
            color = N400,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            attachments.forEachIndexed { index, attachment ->
                val fileName = attachment.name.takeIf { it.isNotBlank() }
                    ?: stringResource(Res.string.common_attachment_numbered, index + 1)
                DiscussionAttachmentItem(
                    fileName = fileName,
                    onClick = {
                        onOpenUrl(attachment.url)
                    },
                )
            }
        }
    }
}

@Composable
fun DiscussionAttachmentItem(
    fileName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, N200, RoundedCornerShape(8.dp))
            .background(WHITE)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Outlined.AttachFile,
            contentDescription = stringResource(Res.string.common_attachment),
            tint = N400,
            modifier = Modifier.size(20.dp),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = fileName,
            style = SSUType.Label2Medium,
            color = N500,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        Spacer(modifier = Modifier.width(8.dp))

        Icon(
            imageVector = Icons.Outlined.FileDownload,
            contentDescription = stringResource(Res.string.common_download),
            tint = N400,
            modifier = Modifier.size(20.dp),
        )
    }
}
