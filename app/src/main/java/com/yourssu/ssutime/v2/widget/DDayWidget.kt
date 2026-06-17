package com.yourssu.ssutime.v2.widget

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.getStringSimpleDate
import com.yourssu.ssutime.v2.screen.main.LmsRefreshRepository
import com.yourssu.ssutime.v2.screen.main.RefreshSource
import com.yourssu.ssutime.v2.screen.main.TodoRefreshResult
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import com.yourssu.ssutime.v2.todo.sortedByDeadlineThenName
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstant
import com.yourssu.ssutime.v2.ui.theme.SSUType
import kotlinx.coroutines.flow.first
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.math.abs

private const val TAG = "DDayWidget"
private const val SECONDS_PER_DAY = 24 * 60 * 60L
private const val WIDGET_REFRESH_TIMEOUT_MILLIS = 30_000L
private const val EMPTY_DDAY_TEXT = "모든 할 일을 수행했어요!"
private val WidgetRefreshSizeKey = ActionParameters.Key<String>("widget_size")

private val widgetWhite = ColorProvider(day = Color.White, night = Color.White)
private val widgetTextColor = ColorProvider(day = Color.White, night = Color.White)
private val widgetErrorTextColor = ColorProvider(day = Color(0xFFFE4F4C), night = Color(0xFFFE4F4C))

internal fun widgetRefreshAction(widgetSize: WidgetAnalyticsSize) = actionRunCallback<DDayWidgetRefreshAction>(
    actionParametersOf(WidgetRefreshSizeKey to widgetSize.value),
)

class DDayWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        context.captureWidgetDisplayOnceDaily(WidgetAnalyticsSize.Small)
        val initialTodoData = context.todoDataStore.data.first()

        provideContent {
            val todoData by context.todoDataStore.data.collectAsState(initial = initialTodoData)
            DDayContent(
                uiState = todoData.toWidgetUiState(context),
                packageName = context.packageName,
            )
        }
    }
}

@Composable
@GlanceComposable
private fun DDayContent(
    uiState: DDayWidgetUiState,
    packageName: String = "com.yourssu.ssutime.v2",
) {
    val availableSize = LocalSize.current
    val squareSize = availableSize.toSquareSize()
    val openAppAction = actionStartActivity(
        Intent().setClassName(
            packageName,
            MainActivity::class.java.name,
        ).apply {
            putExtra(MainActivity.EXTRA_ENTRY_SOURCE, MainActivity.ENTRY_SOURCE_WIDGET)
            putExtra(MainActivity.EXTRA_WIDGET_SIZE, WidgetAnalyticsSize.Small.value)
        },
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
            val backgroundResId = uiState.backgroundResId
            when {
                uiState.isRefreshing -> {
                    DDayRefreshInProgressContent()
                }
                uiState.refreshErrorMessage != null -> {
                    DDayRefreshErrorContent(message = uiState.refreshErrorMessage)
                }
                uiState.hasTodo && backgroundResId != null -> {
                    DDayTodoContent(
                        uiState = uiState,
                        backgroundResId = backgroundResId,
                    )
                }
                else -> {
                    DDayEmptyContent()
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun DDayTodoContent(
    uiState: DDayWidgetUiState,
    backgroundResId: Int,
) {
    Image(
        provider = ImageProvider(backgroundResId),
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
            style = SSUType.G_Caption1SemiBold.copy(color = widgetTextColor),
        )
        Text(
            text = uiState.type,
            maxLines = 1,
            style = SSUType.G_H3SemiBold.copy(color = widgetTextColor),
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
                style = SSUType.G_Caption2Medium.copy(color = widgetTextColor),
            )
            if (uiState.isRemainingTimeText) {
                LiveCountdownText(
                    targetEpochMillis = uiState.countdownTargetEpochMillis,
                    fallbackText = uiState.dDayText,
                    fontSizeSp = 18,
                    color = LiveCountdownColor.White,
                    modifier = GlanceModifier
                        .width(96.dp)
                        .height(24.dp),
                )
            } else {
                Text(
                    text = uiState.dDayText,
                    maxLines = 1,
                    style = SSUType.G_H1SemiBold.copy(color = widgetTextColor),
                )
            }

            Text(
                text = uiState.updatedAtText,
                maxLines = 1,
                style = SSUType.G_Caption3Regular.copy(color = widgetTextColor),
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
                .clickable(widgetRefreshAction(WidgetAnalyticsSize.Small)),
        )
    }
}

@Composable
@GlanceComposable
private fun DDayEmptyContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetWhite),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.doneall),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = GlanceModifier.size(80.dp, 75.dp),
        )
        Spacer(modifier = GlanceModifier.height(7.dp))
        Text(
            text = EMPTY_DDAY_TEXT,
            maxLines = 1,
            style = SSUType.G_Caption2SemiBold.copy(color = widgetTextColor),
        )
    }
}

@Composable
@GlanceComposable
private fun DDayRefreshInProgressContent() {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetWhite)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = GlanceModifier.size(28.dp),
            color = widgetErrorTextColor,
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "새로고침 중",
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 1,
            style = SSUType.G_Caption1SemiBold.copy(
                color = widgetTextColor,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "LMS에서 할 일을 불러오고 있어요.",
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 2,
            style = SSUType.G_Caption2Medium.copy(
                color = widgetTextColor,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
@GlanceComposable
private fun DDayRefreshErrorContent(message: String) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(widgetWhite)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text = "새로고침 실패",
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 1,
            style = SSUType.G_Caption1SemiBold.copy(
                color = widgetTextColor,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = message,
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 2,
            style = SSUType.G_Caption2Medium.copy(
                color = widgetTextColor,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Image(
            provider = ImageProvider(R.drawable.ic_widget_refresh),
            contentDescription = "새로고침",
            modifier = GlanceModifier
                .size(28.dp)
                .clickable(widgetRefreshAction(WidgetAnalyticsSize.Small)),
        )
    }
}

private data class DDayWidgetUiState(
    val hasTodo: Boolean,
    val subjectName: String,
    val type: String,
    val dDayText: String,
    val isRemainingTimeText: Boolean,
    val updatedAtText: String,
    val backgroundResId: Int?,
    val countdownTargetEpochMillis: Long?,
    val isRefreshing: Boolean,
    val refreshErrorMessage: String?,
)

private fun TodoData.toWidgetUiState(context: Context): DDayWidgetUiState {
    val selectedTodo = todos.selectMostUrgentTodo()
    val now = Instant.now()
    val targetInstant = selectedTodo?.let {
        it.due_date.toTodoDeadlineInstant()
    }
    val remainingDays = selectedTodo?.let {
        getRemainingDays(it.due_date, now)
    } ?: Long.MAX_VALUE
    val remainingSeconds = targetInstant?.let {
        ChronoUnit.SECONDS.between(now, it)
    } ?: Long.MAX_VALUE
    val displayRemainingSeconds = remainingSeconds.coerceAtLeast(0L)
    val isRemainingTimeText = selectedTodo != null && displayRemainingSeconds <= SECONDS_PER_DAY

    return DDayWidgetUiState(
        hasTodo = selectedTodo != null,
        subjectName = selectedTodo?.subject?.name ?: "표시할 과제가 없어요",
        type = selectedTodo?.type?.kor
            ?: "앱에서 새로고침해 주세요",
        dDayText = selectedTodo.toDdayText(remainingDays, displayRemainingSeconds),
        isRemainingTimeText = isRemainingTimeText,
        updatedAtText = loadedAt.toUpdatedAtText(context),
        backgroundResId = selectedTodo?.let {
            backgroundFor(
                todo = it,
                loadedAt = loadedAt,
                remainingDays = remainingDays,
                remainingSeconds = remainingSeconds,
            )
        },
        countdownTargetEpochMillis = if (remainingSeconds in 1..SECONDS_PER_DAY) {
            targetInstant?.toEpochMilli()
        } else {
            null
        },
        isRefreshing = isWidgetRefreshing,
        refreshErrorMessage = widgetRefreshErrorMessage,
    )
}

private fun List<TodoInfo>.selectMostUrgentTodo(): TodoInfo? =
    sortedByDeadlineThenName().firstOrNull()

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

private fun Long.toWidgetRemainingTimeText(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return "%02d:%02d:%02d".format(hours, minutes, seconds)
}

private fun String.toUpdatedAtText(context: Context): String {
    if (isBlank()) {
        return "업데이트 전"
    }
    return "업데이트 ${getStringSimpleDate(context, this)}"
}

private fun backgroundFor(
    todo: TodoInfo,
    loadedAt: String,
    remainingDays: Long,
    remainingSeconds: Long,
): Int {
    if (remainingSeconds < 0) {
        return R.drawable.dlate
    }

    if (remainingSeconds <= SECONDS_PER_DAY) {
        return R.drawable.d0
    }

    val urgentBackgrounds = intArrayOf(R.drawable.d1_1, R.drawable.d1_2)
    val relaxedBackgrounds = intArrayOf(R.drawable.oth_1, R.drawable.oth_2)
    val candidates = if (remainingDays <= 1) urgentBackgrounds else relaxedBackgrounds
    val seed = todo.todoId * 31 + loadedAt.hashCode()
    val index = abs(seed) % candidates.size
    return candidates[index]
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 110)
@Composable
private fun DDayWidgetEmptyPreview() {
    DDayContent(
        uiState = DDayWidgetUiState(
            hasTodo = false,
            subjectName = "",
            type = "",
            dDayText = "",
            isRemainingTimeText = false,
            updatedAtText = "",
            backgroundResId = null,
            countdownTargetEpochMillis = null,
            isRefreshing = false,
            refreshErrorMessage = null,
        ),
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 110)
@Composable
private fun DDayWidgetDdayPreview() {
    DDayContent(
        uiState = DDayWidgetUiState(
            hasTodo = true,
            subjectName = "모바일프로그래밍",
            type = "과제",
            dDayText = "D-3",
            isRemainingTimeText = false,
            updatedAtText = "업데이트 05.24 10:30",
            backgroundResId = R.drawable.oth_1,
            countdownTargetEpochMillis = null,
            isRefreshing = false,
            refreshErrorMessage = null,
        ),
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 110)
@Composable
private fun DDayWidgetRemainingTimePreview() {
    DDayContent(
        uiState = DDayWidgetUiState(
            hasTodo = true,
            subjectName = "운영체제",
            type = "강의",
            dDayText = "03:24:16",
            isRemainingTimeText = true,
            updatedAtText = "업데이트 05.24 10:30",
            backgroundResId = R.drawable.d1_1,
            countdownTargetEpochMillis = null,
            isRefreshing = false,
            refreshErrorMessage = null,
        ),
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 110)
@Composable
private fun DDayWidgetRefreshErrorPreview() {
    DDayContent(
        uiState = DDayWidgetUiState(
            hasTodo = true,
            subjectName = "운영체제",
            type = "과제",
            dDayText = "D-1",
            isRemainingTimeText = false,
            updatedAtText = "업데이트 05.24 10:30",
            backgroundResId = R.drawable.d1_1,
            countdownTargetEpochMillis = null,
            isRefreshing = false,
            refreshErrorMessage = "네트워크 연결을 확인해 주세요.",
        ),
    )
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 110, heightDp = 110)
@Composable
private fun DDayWidgetRefreshingPreview() {
    DDayContent(
        uiState = DDayWidgetUiState(
            hasTodo = true,
            subjectName = "운영체제",
            type = "과제",
            dDayText = "D-1",
            isRemainingTimeText = false,
            updatedAtText = "업데이트 05.24 10:30",
            backgroundResId = R.drawable.d1_1,
            countdownTargetEpochMillis = null,
            isRefreshing = true,
            refreshErrorMessage = null,
        ),
    )
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
        val widgetSize = parameters[WidgetRefreshSizeKey] ?: WidgetAnalyticsSize.Small.value
        var refreshSucceeded = false

        context.markWidgetRefreshRunning()
        updateAllTodoWidgets(context)

        runCatching {
            when (val result = lmsRefreshRepository.refreshTodos(
                source = RefreshSource.WIDGET,
                timeoutMillis = WIDGET_REFRESH_TIMEOUT_MILLIS,
                forceLogin = true,
            )) {
                is TodoRefreshResult.Success -> {
                    refreshSucceeded = true
                    Log.i(TAG, "위젯 새로고침이 완료되었습니다.")
                }
                is TodoRefreshResult.Skipped -> {
                    context.markWidgetRefreshFailed(result.reason)
                    Log.i(TAG, result.reason)
                }
                is TodoRefreshResult.Failure -> {
                    context.markWidgetRefreshFailed(result.message)
                    Log.e(TAG, result.message, result.throwable)
                }
            }
        }.onFailure { exception ->
            context.markWidgetRefreshFailed(
                exception.message?.takeIf { it.isNotBlank() }
                    ?: "위젯 새로고침을 실행하지 못했어요.",
            )
            Log.e(TAG, "위젯 새로고침을 실행하지 못했습니다.", exception)
        }

        Analytics.widgetRefreshTap(
            refreshResult = refreshSucceeded,
            widgetSize = widgetSize,
        )
        updateAllTodoWidgets(context)
    }
}
