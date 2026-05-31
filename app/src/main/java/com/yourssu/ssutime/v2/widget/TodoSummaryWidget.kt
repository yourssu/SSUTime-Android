package com.yourssu.ssutime.v2.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceComposable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.getStringSimpleDate
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import com.yourssu.ssutime.v2.ui.theme.SSUType
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val SUMMARY_EMPTY_TEXT = "모든 할 일을 수행했어요!"
private const val SUMMARY_SECONDS_PER_DAY = 24 * 60 * 60L

private val summaryBackground = ColorProvider(
    day = Color.White,
    night = Color.Black
)
private val summaryCardBackground = ColorProvider(
    day = Color(0xFFFFEAE9),
    night = Color(0xFF222222)
)
private val headlineTextColor = ColorProvider(
    day = Color(0xFFFE4F4C),
    night = Color(0xFFFE4F4C)
)
private val summaryAccent = ColorProvider(
    day = Color(0xFFFE4F4C),
    night = Color(0xFFF6F7F8)
)
private val summaryAccentSoft = ColorProvider(
    day = Color(0xFFFFA8A6),
    night = Color(0xFFADB5BD)
)
private val summaryPrimaryText = ColorProvider(
    day = Color(0xFF4B515B),
    night = Color(0xFFF2F2F7)
)
private val summarySecondaryText = ColorProvider(
    day = Color(0xFF9BA5B1),
    night = Color(0xFF9A9AA0)
)

class TodoMediumWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        context.captureWidgetDisplayOnceDaily(WidgetAnalyticsSize.Medium)
        val initialTodoData = context.todoDataStore.data.first()

        provideContent {
            val todoData by context.todoDataStore.data.collectAsState(initial = initialTodoData)
            TodoSummaryContent(
                uiState = todoData.toTodoSummaryUiState(context, TodoSummarySize.Medium),
                size = TodoSummarySize.Medium,
                packageName = context.packageName,
            )
        }
    }
}

class TodoLargeWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        context.captureWidgetDisplayOnceDaily(WidgetAnalyticsSize.Large)
        val initialTodoData = context.todoDataStore.data.first()

        provideContent {
            val todoData by context.todoDataStore.data.collectAsState(initial = initialTodoData)
            TodoSummaryContent(
                uiState = todoData.toTodoSummaryUiState(context, TodoSummarySize.Large),
                size = TodoSummarySize.Large,
                packageName = context.packageName,
            )
        }
    }
}

class TodoMediumWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = TodoMediumWidget()
}

class TodoLargeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget
        get() = TodoLargeWidget()
}

internal suspend fun updateAllTodoWidgets(context: Context) {
    DDayWidget().updateAll(context)
    TodoMediumWidget().updateAll(context)
    TodoLargeWidget().updateAll(context)
}

@Composable
@GlanceComposable
private fun TodoSummaryContent(
    uiState: TodoSummaryUiState,
    size: TodoSummarySize,
    packageName: String = "com.yourssu.ssutime.v2",
) {
    val availableSize = LocalSize.current
    val surfaceHeight = when {
        size == TodoSummarySize.Medium && availableSize.height >= 190.dp -> availableSize.height - 40.dp
        size == TodoSummarySize.Medium && availableSize.height >= 170.dp -> availableSize.height - 22.dp
        else -> availableSize.height
    }
    val openAppAction = actionStartActivity(
        Intent().setClassName(
            packageName,
            MainActivity::class.java.name,
        ).apply {
            putExtra(MainActivity.EXTRA_ENTRY_SOURCE, MainActivity.ENTRY_SOURCE_WIDGET)
            putExtra(MainActivity.EXTRA_WIDGET_SIZE, size.analyticsSize.value)
        },
    )

    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(openAppAction),
        contentAlignment = if (size == TodoSummarySize.Medium) {
            Alignment.Center
        } else {
            Alignment.TopCenter
        },
    ) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(surfaceHeight)
                .background(summaryBackground)
                .appWidgetBackground()
                .cornerRadius(32.dp)
                .clickable(openAppAction),
            contentAlignment = Alignment.Center,
        ) {
            when {
                uiState.isRefreshing -> {
                    TodoSummaryRefreshInProgressContent(size = size)
                }
                uiState.refreshErrorMessage != null -> {
                    TodoSummaryRefreshErrorContent(
                        size = size,
                        message = uiState.refreshErrorMessage,
                    )
                }
                uiState.primary == null -> {
                    TodoSummaryEmptyContent(size = size)
                }
                else -> {
                    when (size) {
                        TodoSummarySize.Medium -> TodoMediumContent(
                            uiState = uiState,
                            availableHeight = surfaceHeight,
                        )
                        TodoSummarySize.Large -> TodoLargeContent(uiState = uiState)
                    }
                }
            }
        }
    }
}

@Composable
@GlanceComposable
private fun TodoMediumContent(
    uiState: TodoSummaryUiState,
    availableHeight: Dp,
) {
    val primary = uiState.primary ?: return
    val availableSize = LocalSize.current
    val useCompactHeight = availableHeight < 165.dp
    val verticalPadding = if (useCompactHeight) 10.dp else 14.dp
    val horizontalPadding = if (availableSize.width < 280.dp) 12.dp else 20.dp
    val gap = if (availableSize.width < 280.dp) 10.dp else 14.dp

    Row(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                horizontal = horizontalPadding,
                vertical = verticalPadding,
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
        ) {
            PrimaryCountdown(
                item = primary,
                countdownSize = if (availableSize.width < 280.dp) 26 else 30,
            )
            Spacer(modifier = GlanceModifier.height(6.dp))
            PrimaryTodoCard(
                item = primary,
                height = if (useCompactHeight) 52.dp else 58.dp,
                showSubject = false,
                showTitle = true,
                subjectFontSize = 8,
                titleFontSize = 11,
                typeFontSize = 11,
                horizontalPadding = 4.dp,
                verticalPadding = 5.dp,
            )
            Spacer(modifier = GlanceModifier.height(5.dp))
            WidgetUpdatedAt(
                text = uiState.updatedAtText,
                widgetSize = WidgetAnalyticsSize.Medium,
            )
        }

        Spacer(modifier = GlanceModifier.width(gap))

        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            CompactTodoSlot(
                item = uiState.items.getOrNull(1),
                height = 44.dp,
                subjectFontSize = 9,
                typeFontSize = 14,
                dDayFontSize = 16,
                dDayContainerWidth = 52.dp,
                dDayTextWidth = 50.dp,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            CompactTodoSlot(
                item = uiState.items.getOrNull(2),
                height = 44.dp,
                subjectFontSize = 9,
                typeFontSize = 14,
                dDayFontSize = 16,
                dDayContainerWidth = 52.dp,
                dDayTextWidth = 50.dp,
            )
            Spacer(modifier = GlanceModifier.height(8.dp))
            MoreTodosText(
                count = uiState.hiddenCount,
                height = 14.dp,
                fontSize = 10,
                barHeight = 12.dp,
            )
        }
    }
}

@Composable
@GlanceComposable
private fun TodoLargeContent(uiState: TodoSummaryUiState) {
    val primary = uiState.primary ?: return
    val availableSize = LocalSize.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        PrimaryCountdown(
            item = primary,
            countdownSize = if (availableSize.width < 280.dp) 26 else 30,
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        PrimaryTodoCard(
            item = primary,
            height = 66.dp,
            showSubject = true,
            showTitle = true,
            subjectFontSize = 10,
            titleFontSize = 12,
            typeFontSize = 12,
            horizontalPadding = 4.dp,
            verticalPadding = 5.dp,
        )
        Spacer(modifier = GlanceModifier.height(2.dp))
        WidgetUpdatedAt(
            text = uiState.updatedAtText,
            widgetSize = WidgetAnalyticsSize.Large,
            fontSize = 9,
            iconSize = 24.dp,
        )
        Spacer(modifier = GlanceModifier.height(2.dp))

        Column(modifier = GlanceModifier.fillMaxWidth()) {
            repeat(4) { index ->
                val item = uiState.items.getOrNull(index + 1)
                CompactTodoSlot(
                    item = item,
                    height = 36.dp,
                    subjectFontSize = 9,
                    typeFontSize = 14,
                    dDayFontSize = 16,
                    dDayContainerWidth = if (item?.isLate == true) {
                        74.dp
                    } else {
                        52.dp
                    },
                    dDayTextWidth = 50.dp,
                )
                if (index != 3) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        MoreTodosText(
            count = uiState.hiddenCount,
            height = 14.dp,
            fontSize = 10,
            barHeight = 12.dp,
        )
    }
}

@Composable
@GlanceComposable
private fun TodoSummaryEmptyContent(size: TodoSummarySize) {
    Column(
        modifier = GlanceModifier.fillMaxSize(),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Image(
            provider = ImageProvider(R.drawable.doneall),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = when (size) {
                TodoSummarySize.Medium -> GlanceModifier.size(84.dp, 79.dp)
                TodoSummarySize.Large -> GlanceModifier.size(102.dp, 96.dp)
            },
        )
        Spacer(modifier = GlanceModifier.height(12.dp))
        Text(
            text = SUMMARY_EMPTY_TEXT,
            maxLines = 1,
            style = SSUType.G_Caption2SemiBold
                .copy(color = summaryPrimaryText),
        )
    }
}

@Composable
@GlanceComposable
private fun TodoSummaryRefreshInProgressContent(size: TodoSummarySize) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = if (size == TodoSummarySize.Medium) 16.dp else 24.dp,
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            modifier = GlanceModifier.size(30.dp),
            color = headlineTextColor,
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "새로고침 중",
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 1,
            style = SSUType.G_Caption1SemiBold.copy(
                color = headlineTextColor,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = "LMS에서 할 일을 불러오고 있어요.",
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = if (size == TodoSummarySize.Medium) 1 else 2,
            style = SSUType.G_Caption2Medium.copy(
                color = summaryPrimaryText,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

@Composable
@GlanceComposable
private fun TodoSummaryRefreshErrorContent(
    size: TodoSummarySize,
    message: String,
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(
                horizontal = 24.dp,
                vertical = if (size == TodoSummarySize.Medium) 16.dp else 24.dp,
            ),
        verticalAlignment = Alignment.Vertical.CenterVertically,
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
    ) {
        Text(
            text = "새로고침 실패",
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 1,
            style = SSUType.G_Caption1SemiBold.copy(
                color = headlineTextColor,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(6.dp))
        Text(
            text = message,
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = if (size == TodoSummarySize.Medium) 1 else 2,
            style = SSUType.G_Caption2Medium.copy(
                color = summaryPrimaryText,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(10.dp))
        Image(
            provider = ImageProvider(R.drawable.refreshbtn),
            contentDescription = "새로고침",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(summarySecondaryText),
            modifier = GlanceModifier
                .size(24.dp)
                .clickable(widgetRefreshAction(size.analyticsSize)),
        )
    }
}

@Composable
@GlanceComposable
private fun PrimaryCountdown(
    item: TodoSummaryItem,
    countdownSize: Int,
) {
    Column {
        Row(
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            if (item.isLate) {
                LateBadge(
                    text = "지각 제출",
                    fontSizeSp = 9,
                    modifier = GlanceModifier.size(52.dp, 18.dp),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
            Text(
                text = "마감까지",
                maxLines = 1,
                style = SSUType.G_Caption2Medium
                    .copy(color = headlineTextColor),
            )
        }
        LiveCountdownText(
            targetEpochMillis = item.countdownTargetEpochMillis,
            fallbackText = item.countdownText,
            fontSizeSp = countdownSize,
            color = LiveCountdownColor.Accent,
            modifier = GlanceModifier
                .width(if (countdownSize <= 30) 132.dp else 190.dp)
                .height((countdownSize + 4).dp),
        )
    }
}

@Composable
@GlanceComposable
private fun PrimaryTodoCard(
    item: TodoSummaryItem,
    height: Dp,
    showSubject: Boolean,
    showTitle: Boolean,
    subjectFontSize: Int,
    titleFontSize: Int,
    typeFontSize: Int,
    horizontalPadding: Dp,
    verticalPadding: Dp,
) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height)
            .background(summaryCardBackground)
            .cornerRadius(10.dp)
            .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width((1.5).dp)
                .height(height.minus(verticalPadding.times(5)))
                .background(summaryAccent)
                .cornerRadius(2.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            if (showSubject) {
                Text(
                    text = item.subjectName,
                    maxLines = 1,
                    style = SSUType.G_Caption2Medium
                        .copy(color = summaryPrimaryText),
                )
            }
            if (showTitle) {
                Text(
                    text = item.title,
                    maxLines = 1,
                    style = SSUType.G_Caption2Medium
                        .copy(color = summaryPrimaryText),
                )
            }
            Text(
                text = item.type,
                maxLines = 1,
                style = SSUType.G_Caption1SemiBold
                    .copy(color = summaryAccent),
            )
        }
    }
}

@Composable
@GlanceComposable
private fun CompactTodoSlot(
    item: TodoSummaryItem?,
    height: Dp,
    subjectFontSize: Int,
    typeFontSize: Int,
    dDayFontSize: Int,
    dDayContainerWidth: Dp,
    dDayTextWidth: Dp,
) {
    if (item == null) {
        Spacer(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(height),
        )
        return
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width((1.5).dp)
                .height(height.minus(15.dp))
                .background(summaryAccentSoft)
                .cornerRadius(2.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Column(
            modifier = GlanceModifier.defaultWeight(),
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            Text(
                text = item.subjectName,
                maxLines = 1,
                style = SSUType.G_Caption2Medium
                    .copy(color = summaryPrimaryText),
            )
            Text(
                text = item.type,
                maxLines = 1,
                style = SSUType.G_Caption1SemiBold
                    .copy(color = summaryPrimaryText),
            )
        }
        Row(
            modifier = GlanceModifier.width(dDayContainerWidth),
            verticalAlignment = Alignment.Vertical.CenterVertically,
            horizontalAlignment = Alignment.Horizontal.End,
        ) {
            Text(
                text = item.dDayText,
                modifier = GlanceModifier.width(dDayTextWidth),
                maxLines = 1,
                style = SSUType.G_H4SemiBold
                    .copy(color = summaryPrimaryText, textAlign = TextAlign.End),
            )
            if (item.isLate) {
                Spacer(modifier = GlanceModifier.width(4.dp))
                LateBadge(
                    text = "지각",
                    fontSizeSp = 9,
                    modifier = GlanceModifier.size(30.dp, 18.dp),
                )
            }
        }
    }
}

@Composable
@GlanceComposable
private fun WidgetUpdatedAt(
    text: String,
    widgetSize: WidgetAnalyticsSize,
    fontSize: Int = 9,
    iconSize: Dp = 24.dp,
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Horizontal.End,
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Text(
            text = text,
            maxLines = 1,
            style = SSUType.G_Caption3Regular
                .copy(color = summaryPrimaryText),
        )
        Spacer(modifier = GlanceModifier.width(4.dp))
        Image(
            provider = ImageProvider(R.drawable.refreshbtn),
            contentDescription = "새로고침",
            contentScale = ContentScale.Fit,
            colorFilter = ColorFilter.tint(summarySecondaryText),
            modifier = GlanceModifier
                .size(iconSize)
                .clickable(widgetRefreshAction(widgetSize)),
        )
    }
}

@Composable
@GlanceComposable
private fun MoreTodosText(
    count: Int,
    height: Dp,
    fontSize: Int,
    barHeight: Dp,
) {
    if (count <= 0) {
        Spacer(modifier = GlanceModifier.height(height))
        return
    }

    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .height(height),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        Box(
            modifier = GlanceModifier
                .width((1.5).dp)
                .height(barHeight)
                .background(summaryAccentSoft)
                .cornerRadius(2.dp),
        ) {}
        Spacer(modifier = GlanceModifier.width(8.dp))
        Text(
            text = "그 외 ${count}건의 할 일이 있어요",
            maxLines = 1,
            style = SSUType.G_Caption2Medium
                .copy(color = summarySecondaryText),
        )
    }
}

private data class TodoSummaryUiState(
    val primary: TodoSummaryItem?,
    val items: List<TodoSummaryItem>,
    val hiddenCount: Int,
    val updatedAtText: String,
    val isRefreshing: Boolean,
    val refreshErrorMessage: String?,
)

private data class TodoSummaryItem(
    val subjectName: String,
    val title: String,
    val type: String,
    val countdownText: String,
    val countdownTargetEpochMillis: Long?,
    val dDayText: String,
    val isLate: Boolean,
)

private enum class TodoSummarySize(
    val visibleTodoCount: Int,
    val analyticsSize: WidgetAnalyticsSize,
) {
    Medium(3, WidgetAnalyticsSize.Medium),
    Large(5, WidgetAnalyticsSize.Large),
}

private fun TodoData.toTodoSummaryUiState(
    context: Context,
    size: TodoSummarySize,
): TodoSummaryUiState {
    val now = Instant.now()
    val sortedTodos = todos
        .sortedBy { it.widgetDueInstant() }
        .map { it.toTodoSummaryItem(now) }

    return TodoSummaryUiState(
        primary = sortedTodos.firstOrNull(),
        items = sortedTodos.take(size.visibleTodoCount),
        hiddenCount = (sortedTodos.size - size.visibleTodoCount).coerceAtLeast(0),
        updatedAtText = loadedAt.toSummaryUpdatedAtText(context),
        isRefreshing = isWidgetRefreshing,
        refreshErrorMessage = widgetRefreshErrorMessage,
    )
}

private fun TodoInfo.toTodoSummaryItem(now: Instant): TodoSummaryItem {
    val dueInstant = widgetDueInstant()
    val rawRemainingSeconds = ChronoUnit.SECONDS.between(now, dueInstant)
    val remainingSeconds = rawRemainingSeconds.coerceAtLeast(0L)
    val remainingDays = ChronoUnit.DAYS.between(now, dueInstant).coerceAtLeast(0L)
    val dDayText = "D-$remainingDays"

    return TodoSummaryItem(
        subjectName = subject?.name.toWidgetSubjectName(),
        title = title.takeIf { it.isNotBlank() } ?: type.kor,
        type = type.kor,
        countdownText = if (remainingSeconds <= SUMMARY_SECONDS_PER_DAY) {
            remainingSeconds.toSummaryCountdownText()
        } else {
            dDayText
        },
        countdownTargetEpochMillis = dueInstant
            .toEpochMilli()
            .takeIf { remainingSeconds in 1..SUMMARY_SECONDS_PER_DAY },
        dDayText = dDayText,
        isLate = rawRemainingSeconds < 0L,
    )
}

private fun String?.toWidgetSubjectName(): String =
    this
        ?.substringBefore(" (")
        ?.substringBefore("(")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "과목명 없음"

private fun TodoInfo.widgetDueInstant(): Instant {
    val parsedTime = DateTimeFormatter.ISO_DATE_TIME.parseBest(
        due_date,
        ZonedDateTime::from,
        OffsetDateTime::from,
        LocalDateTime::from,
    )

    return when (parsedTime) {
        is ZonedDateTime -> parsedTime.toInstant()
        is OffsetDateTime -> parsedTime.toInstant()
        is LocalDateTime -> parsedTime.atZone(ZoneId.of("Asia/Seoul")).toInstant()
        else -> error("Unsupported target time format: $due_date")
    }
}

private fun Long.toSummaryCountdownText(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun String.toSummaryUpdatedAtText(context: Context): String {
    if (isBlank()) {
        return "업데이트 전"
    }
    return "업데이트 ${getStringSimpleDate(context, this)} 기준"
}

private fun summaryTextStyle(
    fontSize: Int,
    fontWeight: FontWeight,
    color: androidx.glance.unit.ColorProvider,
    textAlign: TextAlign? = null,
): TextStyle = TextStyle(
    fontSize = fontSize.sp,
    fontWeight = fontWeight,
    color = color,
    textAlign = textAlign,
)
