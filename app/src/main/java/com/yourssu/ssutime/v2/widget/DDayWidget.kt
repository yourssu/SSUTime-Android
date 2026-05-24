package com.yourssu.ssutime.v2.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.getStringSimpleDate
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.RefreshSource
import com.yourssu.ssutime.v2.screen.main.TodoRefreshResult
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import com.yourssu.ssutime.v2.ui.theme.SSUType
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlin.math.max

private const val TAG = "DDayWidget"
private const val SECONDS_PER_DAY = 24 * 60 * 60L
private const val WIDGET_REFRESH_TIMEOUT_MILLIS = 30_000L

class DDayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val initialTodoData = context.todoDataStore.data.first()

        provideContent {
            val todoData by context.todoDataStore.data.collectAsState(initial = initialTodoData)
            DDayContent(
                uiState = todoData.toWidgetUiState(context),
            )
        }
    }

    @Composable
    private fun DDayContent(uiState: DDayWidgetUiState) {
        val availableSize = LocalSize.current
        val squareSize = availableSize.toSquareSize()
        val openAppAction = actionStartActivity(
            Intent().setClassName(
                MainActivity::class.java.packageName,
                MainActivity::class.java.name,
            ),
        )

        Box(
            modifier = GlanceModifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(squareSize)
                    .clickable(openAppAction),
            ) {
                Image(
                    provider = ImageProvider(uiState.backgroundResId),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = GlanceModifier.fillMaxSize(),
                )

                Column(
                    modifier = GlanceModifier
                        .padding(18.dp),
                    verticalAlignment = Alignment.Vertical.Top,
                    horizontalAlignment = Alignment.Horizontal.Start,
                ) {
                    Text(
                        text = uiState.subjectName,
                        maxLines = 1,
                        style = SSUType.G_Caption1SemiBold,
                    )
                    Text(
                        text = uiState.type,
                        maxLines = 1,
                        style = SSUType.G_H3SemiBold,
                    )
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(start = 18.dp, bottom = 18.dp),
                    contentAlignment = Alignment.BottomStart,
                ) {
                    Column {
                        Text(
                            text = "마감까지",
                            style = SSUType.G_Caption2Medium,
                        )
                        Text(
                            text = uiState.dDayText,
                            maxLines = 1,
                            style = if (uiState.isRemainingTimeText) {
                                SSUType.G_H3SemiBold
                            } else {
                                SSUType.G_H1SemiBold
                            },
                        )

                        Text(
                            text = uiState.updatedAtText,
                            maxLines = 1,
                            style = SSUType.G_Caption3Regular,
                        )
                    }
                }

                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .padding(end = 18.dp, bottom = 18.dp),
                    contentAlignment = Alignment.BottomEnd,
                ) {
                    Image(
                        provider = ImageProvider(R.drawable.ic_widget_refresh),
                        contentDescription = "새로고침",
                        modifier = GlanceModifier
                            .size(34.dp)
                            .clickable(actionRunCallback<DDayWidgetRefreshAction>()),
                    )
                }
            }
        }
    }
}

private data class DDayWidgetUiState(
    val subjectName: String,
    val type: String,
    val dDayText: String,
    val isRemainingTimeText: Boolean,
    val updatedAtText: String,
    val backgroundResId: Int,
)

private fun TodoData.toWidgetUiState(context: Context): DDayWidgetUiState {
    val selectedTodo = todos.selectMostUrgentTodo()
    val now = Instant.now()
    val remainingDays = selectedTodo?.let {
        getRemainingDays(it.due_date, now)
    } ?: Long.MAX_VALUE
    val remainingSeconds = selectedTodo?.remainingSecondsUntil(now) ?: Long.MAX_VALUE
    val isRemainingTimeText = selectedTodo != null && remainingSeconds <= SECONDS_PER_DAY

    return DDayWidgetUiState(
        subjectName = selectedTodo?.subject?.name ?: "표시할 과제가 없어요",
        type = selectedTodo?.type?.kor
            ?: "앱에서 새로고침해 주세요",
        dDayText = selectedTodo.toDdayText(remainingDays, remainingSeconds),
        isRemainingTimeText = isRemainingTimeText,
        updatedAtText = loadedAt.toUpdatedAtText(context),
        backgroundResId = backgroundFor(selectedTodo, loadedAt, remainingDays),
    )
}

private fun List<TodoInfo>.selectMostUrgentTodo(): TodoInfo? =
    minWithOrNull(
        compareBy<TodoInfo> { getRemainingDays(it.due_date) }
            .thenBy { it.due_date },
    )

private fun TodoInfo?.toDdayText(remainingDays: Long, remainingSeconds: Long): String {
    if (this == null) {
        return "-"
    }
    return if (remainingSeconds <= SECONDS_PER_DAY) {
        remainingSeconds.toWidgetRemainingTimeText()
    } else {
        "D-$remainingDays"
    }
}

private fun TodoInfo.remainingSecondsUntil(now: Instant): Long = max(
    0L,
    ChronoUnit.SECONDS.between(now, parseWidgetTargetInstant(due_date)),
)

private fun Long.toWidgetRemainingTimeText(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun parseWidgetTargetInstant(targetTime: String): Instant {
    val parsedTime = DateTimeFormatter.ISO_DATE_TIME.parseBest(
        targetTime,
        ZonedDateTime::from,
        OffsetDateTime::from,
        LocalDateTime::from,
    )

    return when (parsedTime) {
        is ZonedDateTime -> parsedTime.toInstant()
        is OffsetDateTime -> parsedTime.toInstant()
        is LocalDateTime -> parsedTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
        else -> error("Unsupported target time format: $targetTime")
    }
}

private fun String.toUpdatedAtText(context: Context): String {
    if (isBlank()) {
        return "업데이트 전"
    }
    return "업데이트 ${getStringSimpleDate(context, this)}"
}

private fun backgroundFor(todo: TodoInfo?, loadedAt: String, remainingDays: Long): Int {
    if (todo == null) {
        return R.drawable.oth_1
    }

    val urgentBackgrounds = intArrayOf(R.drawable.d1_1, R.drawable.d1_2)
    val relaxedBackgrounds = intArrayOf(R.drawable.oth_1, R.drawable.oth_2)
    val candidates = if (remainingDays <= 1) urgentBackgrounds else relaxedBackgrounds
    val seed = todo.todoId * 31 + loadedAt.hashCode()
    val index = abs(seed) % candidates.size
    return candidates[index]
}

private fun androidx.compose.ui.unit.DpSize.toSquareSize(): Dp =
    if (width < height) width else height

class DDayWidgetRefreshAction : ActionCallback, KoinComponent {
    private val lmsRefreshRepository: LmsRefreshRepository by inject()

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters,
    ) {
        runCatching {
            when (val result = lmsRefreshRepository.refreshTodos(
                source = RefreshSource.MANUAL,
                timeoutMillis = WIDGET_REFRESH_TIMEOUT_MILLIS,
            )) {
                is TodoRefreshResult.Success -> Log.i(TAG, "위젯 새로고침이 완료되었습니다.")
                is TodoRefreshResult.Skipped -> Log.i(TAG, result.reason)
                is TodoRefreshResult.Failure -> Log.e(TAG, result.message, result.throwable)
            }
        }.onFailure { exception ->
            Log.e(TAG, "위젯 새로고침을 실행하지 못했습니다.", exception)
        }

        DDayWidget().updateAll(context)
    }
}
