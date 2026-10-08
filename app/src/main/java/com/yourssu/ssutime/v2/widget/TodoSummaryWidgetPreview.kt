package com.yourssu.ssutime.v2.widget

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.glance.LocalContext
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import java.util.Locale

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun TodoMediumMultipleLatePreview() {
    TodoSummaryPreview(TodoSummarySize.Medium)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 180)
@Composable
private fun TodoMediumSecondaryLatePreview() {
    TodoSummaryPreview(TodoSummarySize.Medium, primaryIsLate = false)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 360)
@Composable
private fun TodoLargeMultipleLatePreview() {
    TodoSummaryPreview(TodoSummarySize.Large)
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 320, heightDp = 280)
@Composable
private fun TodoLargeCompactMultipleLatePreview() {
    TodoSummaryPreview(TodoSummarySize.Large)
}

@Composable
private fun TodoSummaryPreview(size: TodoSummarySize, primaryIsLate: Boolean = true) {
    val context = LocalContext.current
    val koreanContext = remember(context) {
        context.createConfigurationContext(
            Configuration(context.resources.configuration).apply { setLocale(Locale.KOREAN) },
        )
    }
    val items = listOf(
        TodoSummaryItem(
            subjectName = "벤처중소기업경영사례연구",
            title = "경영사례 분석 보고서",
            type = "과제",
            countdownText = "11:32:00",
            countdownTargetEpochMillis = null,
            dDayText = "D-0",
            isLate = primaryIsLate,
        ),
        TodoSummaryItem(
            subjectName = "벤처중소기업경영사례연구",
            title = "경영사례 퀴즈",
            type = "퀴즈",
            countdownText = "D-1",
            countdownTargetEpochMillis = null,
            dDayText = "D-1",
            isLate = true,
        ),
        TodoSummaryItem(
            subjectName = "현대사회와윤리",
            title = "윤리 사례 보고서",
            type = "과제",
            countdownText = "D-2",
            countdownTargetEpochMillis = null,
            dDayText = "D-2",
            isLate = true,
        ),
        TodoSummaryItem(
            subjectName = "데이터분석입문",
            title = "데이터 분석 실습",
            type = "과제",
            countdownText = "D-3",
            countdownTargetEpochMillis = null,
            dDayText = "D-3",
            isLate = true,
        ),
        TodoSummaryItem(
            subjectName = "컴퓨팅적사고",
            title = "온라인 강의",
            type = "강의",
            countdownText = "D-4",
            countdownTargetEpochMillis = null,
            dDayText = "D-4",
            isLate = false,
        ),
    )
    CompositionLocalProvider(LocalContext provides koreanContext) {
        TodoSummaryContent(
            uiState = TodoSummaryUiState(
                primary = items.first(),
                items = items.take(size.visibleTodoCount),
                hiddenCount = items.size - size.visibleTodoCount,
                updatedAtText = "업데이트 오후 12시 21분",
                isRefreshing = false,
                refreshProgressMessage = null,
                refreshErrorMessage = null,
            ),
            size = size,
        )
    }
}
