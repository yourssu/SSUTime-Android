package com.yourssu.ssutime.v2.screen.notice

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.Html
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.data.DiscussionAttachment
import com.yourssu.data.DiscussionInfo
import com.yourssu.data.SubjectInfo
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.ui.theme.BLACK
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N400
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.N600
import com.yourssu.ssutime.v2.ui.theme.N700
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * ISO-8601 형식의 날짜 문자열을 "yyyy.MM.dd. a h:mm" 형식으로 포맷팅
 */
internal fun formatDiscussionDate(dateString: String): String {
    if (dateString.isBlank()) return ""
    return runCatching {
        val instant = Instant.parse(dateString)
        val formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd. a h:mm", Locale.KOREA)
        instant.atZone(ZoneId.of("Asia/Seoul")).format(formatter)
    }.getOrElse { dateString }
}

/**
 * HTML 형식의 문자열을 일반 텍스트로 변환
 */
internal fun parseHtmlToPlainText(html: String): String {
    if (html.isBlank()) return ""
    return runCatching {
        Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT).toString().trim()
    }.getOrElse { html }
}

/**
 * 첨부파일 또는 공지 URL 열기
 */
fun openAttachmentUrl(context: Context, url: String) {
    if (url.isBlank()) {
        Toast.makeText(context, "첨부파일 경로가 유효하지 않습니다.", Toast.LENGTH_SHORT).show()
        return
    }
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }.onFailure {
        Toast.makeText(context, "링크를 열 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
    }
}

/**
 * 프리뷰 및 기본 폴백용 목업 데이터
 */
val MockSubjectInfos: List<SubjectInfo> = listOf(
    SubjectInfo(
        id = 1,
        name = "벤처경영론",
        professor = "교수님",
        discussions = listOf(
            DiscussionInfo(
                id = 1,
                title = "제목",
                message = "여러분께,\n내일 중간시험 관련해서 알려드립니다\n\n말씀 드린데로 수업시간에 다루었던 데이터와 케이스를 중심으로 개념, 해석 및 등이 출제될 예정입니다.\n\n복잡한 수식 관련 문제는 출제하지 않았지만 간단한 계산을 위해서는 계산기가 필요할 수 있습니다",
                url = "https://smartid.ssu.ac.kr",
                readState = "unread",
                createdAt = "2026-04-27T17:08:00Z",
                attachments = listOf(DiscussionAttachment(id = 1, name = "시험안내.pdf", url = "https://smartid.ssu.ac.kr"))
            ),
            DiscussionInfo(
                id = 2,
                title = "제목",
                message = "여러분께,\n내일 중간시험 관련해서 알려드립니다\n\n말씀 드린데로 수업시간에 다루었던 데이터와 케이스를 중심으로 개념, 해석 및 등이 출제될 예정입니다.\n\n복잡한 수식 관련 문제는 출제하지 않았지만 간단한 계산을 위해서는 계산기가 필요할 수 있습니다",
                url = "https://smartid.ssu.ac.kr",
                readState = "read",
                createdAt = "2026-04-27T17:08:00Z",
                attachments = listOf(DiscussionAttachment(id = 2, name = "과제안내.pdf", url = "https://smartid.ssu.ac.kr"))
            ),
            DiscussionInfo(
                id = 3,
                title = "제목",
                message = "여러분께,\n내일 중간시험 관련해서 알려드립니다\n\n말씀 드린데로 수업시간에 다루었던 데이터와 케이스를 중심으로 개념, 해석 및 등이 출제될 예정입니다.\n\n복잡한 수식 관련 문제는 출제하지 않았지만 간단한 계산을 위해서는 계산기가 필요할 수 있습니다",
                url = "https://smartid.ssu.ac.kr",
                readState = "unread",
                createdAt = "2026-04-27T17:08:00Z",
                attachments = emptyList()
            ),
        )
    ),
    SubjectInfo(
        id = 2,
        name = "컴퓨터개론",
        professor = "교수님",
        discussions = listOf(
            DiscussionInfo(
                id = 4,
                title = "중간고사 과제 안내",
                message = "중간고사 대체 레포트 과제에 대한 세부 가이드라인입니다.",
                url = "https://smartid.ssu.ac.kr",
                readState = "read",
                createdAt = "2026-04-27T17:08:00Z",
                attachments = listOf(DiscussionAttachment(id = 3, name = "레포트양식.hwp", url = "https://smartid.ssu.ac.kr"))
            ),
        )
    ),
    SubjectInfo(
        id = 3,
        name = "운영체제",
        professor = "교수님",
        discussions = listOf(
            DiscussionInfo(
                id = 5,
                title = "텀프로젝트 공지사항",
                message = "운영체제 텀프로젝트 주제 선정 및 마감 기한 안내입니다.",
                url = "https://smartid.ssu.ac.kr",
                readState = "unread",
                createdAt = "2026-04-27T17:08:00Z",
                attachments = listOf(DiscussionAttachment(id = 4, name = "프로젝트.pdf", url = "https://smartid.ssu.ac.kr"))
            ),
        )
    ),
)

@Composable
fun NoticeScreen(
    modifier: Modifier = Modifier,
    subjects: List<SubjectInfo> = emptyList(),
    onBackClick: () -> Unit = {},
    onAttachmentClick: (DiscussionInfo) -> Unit = {},
) {
    val context = LocalContext.current
    val effectiveSubjects = if (subjects.isNotEmpty()) subjects else MockSubjectInfos

    var selectedSubjectIndex by rememberSaveable { mutableIntStateOf(0) }
    val validIndex = selectedSubjectIndex.coerceIn(0, (effectiveSubjects.size - 1).coerceAtLeast(0))
    val currentSubject = effectiveSubjects.getOrNull(validIndex)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .statusBarsPadding()
    ) {
        NoticeTopBar(
            onLogoClick = onBackClick
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "공지사항",
                    style = SSUType.H2SemiBold,
                    color = BLACK
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (effectiveSubjects.isNotEmpty()) {
                    NoticeSubjectDropdown(
                        subjects = effectiveSubjects,
                        selectedIndex = validIndex,
                        onSelectSubject = { index ->
                            selectedSubjectIndex = index
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            if (currentSubject != null && currentSubject.discussions.isNotEmpty()) {
                itemsIndexed(
                    items = currentSubject.discussions,
                    key = { _, item -> item.id }
                ) { index, discussion ->
                    NoticeAccordionItem(
                        discussion = discussion,
                        defaultExpanded = (index == 1),
                        onAttachmentClick = {
                            onAttachmentClick(discussion)
                            val targetUrl = discussion.attachments.firstOrNull()?.url?.takeIf { it.isNotBlank() }
                                ?: discussion.url
                            openAttachmentUrl(context, targetUrl)
                        }
                    )

                    if (index < currentSubject.discussions.lastIndex) {
                        HorizontalDivider(
                            color = N200,
                            thickness = 0.5.dp,
                            modifier = Modifier.padding(vertical = 16.dp)
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
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "등록된 공지사항이 없습니다.",
                            style = SSUType.Body1Medium,
                            color = N400
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NoticeTopBar(
    modifier: Modifier = Modifier,
    onLogoClick: () -> Unit = {},
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            modifier = Modifier
                .height(18.dp)
                .clickable(onClick = onLogoClick),
            painter = painterResource(R.drawable.logo_red),
            contentDescription = stringResource(R.string.app_name)
        )
    }
}

@Composable
fun NoticeSubjectDropdown(
    subjects: List<SubjectInfo>,
    selectedIndex: Int,
    onSelectSubject: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedSubjectName = subjects.getOrNull(selectedIndex)?.name ?: ""

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(width = 1.dp, color = N200, shape = RoundedCornerShape(8.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = selectedSubjectName,
                style = SSUType.H4Medium,
                color = N600
            )

            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = "과목 선택",
                tint = N400,
                modifier = Modifier.size(24.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(WHITE)
                .fillMaxWidth(0.9f)
        ) {
            subjects.forEachIndexed { index, subject ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = subject.name,
                            style = SSUType.Body1Regular,
                            color = if (index == selectedIndex) N700 else N500
                        )
                    },
                    onClick = {
                        onSelectSubject(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun NoticeAccordionItem(
    discussion: DiscussionInfo,
    modifier: Modifier = Modifier,
    defaultExpanded: Boolean = false,
    onAttachmentClick: () -> Unit = {},
) {
    var isExpanded by rememberSaveable(discussion.id) { mutableStateOf(defaultExpanded) }
    val formattedDate = remember(discussion.createdAt) { formatDiscussionDate(discussion.createdAt) }
    val plainContent = remember(discussion.message) { parseHtmlToPlainText(discussion.message) }
    val isNew = discussion.readState.equals("unread", ignoreCase = true)
    val hasAttachment = discussion.attachments.isNotEmpty() || discussion.url.isNotBlank()

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // 상단 헤더 (제목, 날짜, 화살표)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = discussion.title,
                        style = SSUType.H4Medium,
                        color = N700
                    )

                    if (isNew) {
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(R400),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "N",
                                style = SSUType.Caption2SemiBold,
                                color = WHITE
                            )
                        }
                    }
                }

                if (formattedDate.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formattedDate,
                        style = SSUType.Caption1Medium,
                        color = N400
                    )
                }
            }

            Icon(
                imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                tint = N400,
                modifier = Modifier.size(24.dp)
            )
        }

        // 펼쳐진 본문 영역
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
            ) {
                if (plainContent.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(N100)
                            .padding(16.dp)
                    ) {
                        Text(
                            text = plainContent,
                            style = SSUType.Body1Regular,
                            color = N500
                        )
                    }
                }

                if (hasAttachment) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(N600)
                                .clickable(onClick = onAttachmentClick)
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "첨부파일 보기",
                                style = SSUType.Caption1SemiBold,
                                color = WHITE
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun NoticeScreenPreview() {
    NoticeScreen()
}
