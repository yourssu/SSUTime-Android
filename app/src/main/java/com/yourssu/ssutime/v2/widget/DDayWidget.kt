package com.yourssu.ssutime.v2.widget

import android.content.Context
import android.content.Intent
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
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.Text
import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.v2.MainActivity
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.getStringSimpleDate
import com.yourssu.ssutime.v2.screen.main.TodoData
import com.yourssu.ssutime.v2.screen.main.todoDataStore
import com.yourssu.ssutime.v2.ui.theme.SSUType
import kotlinx.coroutines.flow.first
import kotlin.math.abs

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
                            style = SSUType.G_H1SemiBold,
                        )

                        Text(
                            text = uiState.updatedAtText,
                            maxLines = 1,
                            style = SSUType.G_Caption3Regular,
                        )
                    }
                }
            }
        }
    }
}

private data class DDayWidgetUiState(
    val subjectName: String,
    val type: String,
    val dDayText: String,
    val updatedAtText: String,
    val backgroundResId: Int,
)

private fun TodoData.toWidgetUiState(context: Context): DDayWidgetUiState {
    val selectedTodo = todos.selectMostUrgentTodo()
    val remainingDays = selectedTodo?.let {
        getRemainingDays(it.due_date)
    } ?: Long.MAX_VALUE

    return DDayWidgetUiState(
        subjectName = selectedTodo?.subject?.name ?: "표시할 과제가 없어요",
        type = selectedTodo?.type?.kor
            ?: "앱에서 새로고침해 주세요",
        dDayText = selectedTodo.toDdayText(remainingDays),
        updatedAtText = loadedAt.toUpdatedAtText(context),
        backgroundResId = backgroundFor(selectedTodo, loadedAt, remainingDays),
    )
}

private fun List<TodoInfo>.selectMostUrgentTodo(): TodoInfo? =
    minWithOrNull(
        compareBy<TodoInfo> { getRemainingDays(it.due_date) }
            .thenBy { it.due_date },
    )

private fun TodoInfo?.toDdayText(remainingDays: Long): String {
    if (this == null) {
        return "-"
    }
    return if (remainingDays > 0) {
        "D-$remainingDays"
    } else {
        "D-DAY"
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
