package com.yourssu.ssutime.v2.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
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
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
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
import com.yourssu.ssutime.v2.todo.localizedLabel
import com.yourssu.ssutime.v2.todo.sortedByDeadlineThenName
import com.yourssu.ssutime.v2.todo.toTodoDeadlineInstant
import com.yourssu.ssutime.v2.ui.theme.SSUType
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

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
    DDayWidget().updateAllForReceiver<DDayWidgetReceiver>(context)
    TodoMediumWidget().updateAllForReceiver<TodoMediumWidgetReceiver>(context)
    TodoLargeWidget().updateAllForReceiver<TodoLargeWidgetReceiver>(context)
}

private suspend inline fun <reified R : GlanceAppWidgetReceiver> GlanceAppWidget.updateAllForReceiver(
    context: Context,
) {
    val appWidgetManager = AppWidgetManager.getInstance(context)
    val glanceManager = GlanceAppWidgetManager(context)
    val receiver = ComponentName(context, R::class.java)

    appWidgetManager.getAppWidgetIds(receiver).forEach { appWidgetId ->
        update(context, glanceManager.getGlanceIdBy(appWidgetId))
    }
}

@Composable
@GlanceComposable
internal fun TodoSummaryContent(
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
                        TodoSummarySize.Large -> TodoLargeContent(
                            uiState = uiState,
                            availableHeight = surfaceHeight,
                        )
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
private fun TodoLargeContent(
    uiState: TodoSummaryUiState,
    availableHeight: Dp,
) {
    val primary = uiState.primary ?: return
    val availableSize = LocalSize.current
    val visibleSecondarySlots = if (availableHeight < 340.dp) 3 else 4
    val verticalPadding = if (availableHeight < 430.dp) 10.dp else 14.dp
    val hiddenByCompactLayout = uiState.items
        .drop(1 + visibleSecondarySlots)
        .size
    val moreCount = uiState.hiddenCount + hiddenByCompactLayout

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = 18.dp, vertical = verticalPadding),
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
            repeat(visibleSecondarySlots) { index ->
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
                if (index != visibleSecondarySlots - 1) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
        }
        Spacer(modifier = GlanceModifier.height(2.dp))
        MoreTodosText(
            count = moreCount,
            height = 14.dp,
            fontSize = 10,
            barHeight = 12.dp,
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
    }
}

@Composable
@GlanceComposable
private fun TodoSummaryEmptyContent(size: TodoSummarySize) {
    val context = LocalContext.current
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
            text = context.getString(R.string.widget_empty_text),
            maxLines = 1,
            style = SSUType.G_Caption2SemiBold
                .copy(color = summaryPrimaryText),
        )
    }
}

@Composable
@GlanceComposable
private fun TodoSummaryRefreshInProgressContent(size: TodoSummarySize) {
    val context = LocalContext.current
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
            text = context.getString(R.string.widget_refreshing_title),
            modifier = GlanceModifier.fillMaxWidth(),
            maxLines = 1,
            style = SSUType.G_Caption1SemiBold.copy(
                color = headlineTextColor,
                textAlign = TextAlign.Center,
            ),
        )
        Spacer(modifier = GlanceModifier.height(4.dp))
        Text(
            text = context.getString(R.string.widget_refreshing_description),
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
    val context = LocalContext.current
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
            text = context.getString(R.string.widget_refresh_failed_title),
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
            contentDescription = context.getString(R.string.common_refresh),
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
    val context = LocalContext.current
    Column {
        Row(
            verticalAlignment = Alignment.Vertical.CenterVertically,
        ) {
            if (item.isLate) {
                LateBadge(
                    text = context.getString(R.string.widget_late_submission),
                    fontSizeSp = 9,
                    modifier = GlanceModifier.size(52.dp, 18.dp),
                )
                Spacer(modifier = GlanceModifier.width(4.dp))
            }
            Text(
                text = context.getString(R.string.widget_deadline_until),
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
    val context = LocalContext.current
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
                    text = context.getString(R.string.widget_late_short),
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
    val context = LocalContext.current
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
            contentDescription = context.getString(R.string.common_refresh),
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
    val context = LocalContext.current
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
            text = context.getString(R.string.widget_more_todos, count),
            maxLines = 1,
            style = SSUType.G_Caption2Medium
                .copy(color = summarySecondaryText),
        )
    }
}

internal data class TodoSummaryUiState(
    val primary: TodoSummaryItem?,
    val items: List<TodoSummaryItem>,
    val hiddenCount: Int,
    val updatedAtText: String,
    val isRefreshing: Boolean,
    val refreshErrorMessage: String?,
)

internal data class TodoSummaryItem(
    val subjectName: String,
    val title: String,
    val type: String,
    val countdownText: String,
    val countdownTargetEpochMillis: Long?,
    val dDayText: String,
    val isLate: Boolean,
)

internal enum class TodoSummarySize(
    val visibleTodoCount: Int,
    val analyticsSize: WidgetAnalyticsSize,
) {
    Medium(3, WidgetAnalyticsSize.Medium),
    Large(5, WidgetAnalyticsSize.Large),
}

internal fun TodoData.toTodoSummaryUiState(
    context: Context,
    size: TodoSummarySize,
): TodoSummaryUiState {
    val now = Instant.now()
    val sortedTodos = todos
        .sortedByDeadlineThenName()
        .map { it.toTodoSummaryItem(context, now) }

    return TodoSummaryUiState(
        primary = sortedTodos.firstOrNull(),
        items = sortedTodos.take(size.visibleTodoCount),
        hiddenCount = (sortedTodos.size - size.visibleTodoCount).coerceAtLeast(0),
        updatedAtText = loadedAt.toSummaryUpdatedAtText(context),
        isRefreshing = isWidgetRefreshing,
        refreshErrorMessage = widgetRefreshErrorMessage,
    )
}

private fun TodoInfo.toTodoSummaryItem(context: Context, now: Instant): TodoSummaryItem {
    val dueInstant = widgetDueInstant()
    val rawRemainingSeconds = ChronoUnit.SECONDS.between(now, dueInstant)
    val remainingSeconds = rawRemainingSeconds.coerceAtLeast(0L)
    val remainingDays = ChronoUnit.DAYS.between(now, dueInstant).coerceAtLeast(0L)
    val dDayText = "D-$remainingDays"

    return TodoSummaryItem(
        subjectName = subject?.name.toWidgetSubjectName(context),
        title = title.takeIf { it.isNotBlank() } ?: type.localizedLabel(context),
        type = type.localizedLabel(context),
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

private fun String?.toWidgetSubjectName(context: Context): String =
    this
        ?.substringBefore(" (")
        ?.substringBefore("(")
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: context.getString(R.string.widget_no_subject)

private fun TodoInfo.widgetDueInstant(): Instant {
    return due_date.toTodoDeadlineInstant()
}

private fun Long.toSummaryCountdownText(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

private fun String.toSummaryUpdatedAtText(context: Context): String {
    if (isBlank()) {
        return context.getString(R.string.widget_updated_before)
    }
    return context.getString(R.string.widget_updated_at_base, getStringSimpleDate(context, this))
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
