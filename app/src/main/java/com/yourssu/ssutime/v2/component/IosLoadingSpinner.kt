package com.yourssu.ssutime.v2.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin

private const val SPOKE_COUNT = 12
private val DEFAULT_SPINNER_COLOR = Color(0xFF8E8E93)

/**
 * iOS 스타일의 UIActivityIndicatorView (로딩 스피너)
 *
 * @param modifier Modifier
 * @param size 스피너 크기 (기본값: 24.dp)
 * @param color 스피너 색상 (기본값: iOS 회색 0xFF8E8E93)
 * @param isRefreshing 새로고침 진행 중 여부 (true면 회전 애니메이션 실행)
 * @param pullProgress 당기는 진행도 (0f ~ 1f, isRefreshing = false일 때 spokes 순차 노출)
 */
@Composable
fun IosLoadingSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    color: Color = DEFAULT_SPINNER_COLOR,
    isRefreshing: Boolean = true,
    pullProgress: Float = 0f,
) {
    var step by remember { mutableIntStateOf(0) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            while (true) {
                delay(80L)
                step = (step + 1) % SPOKE_COUNT
            }
        }
    }

    Canvas(
        modifier = modifier.size(size)
    ) {
        val center = Offset(this.size.width / 2f, this.size.height / 2f)
        val outerRadius = this.size.width * 0.46f
        val innerRadius = this.size.width * 0.25f
        val strokeWidth = this.size.width * 0.085f

        val visibleSpokes = if (isRefreshing) {
            SPOKE_COUNT
        } else {
            (pullProgress.coerceIn(0f, 1f) * SPOKE_COUNT).toInt().coerceIn(0, SPOKE_COUNT)
        }

        for (i in 0 until visibleSpokes) {
            val angleDeg = i * (360f / SPOKE_COUNT) - 90f // 12시 방향부터 시작
            val angleRad = Math.toRadians(angleDeg.toDouble())

            val startX = center.x + (innerRadius * cos(angleRad)).toFloat()
            val startY = center.y + (innerRadius * sin(angleRad)).toFloat()
            val endX = center.x + (outerRadius * cos(angleRad)).toFloat()
            val endY = center.y + (outerRadius * sin(angleRad)).toFloat()

            val alpha = if (isRefreshing) {
                val diff = (step - i + SPOKE_COUNT) % SPOKE_COUNT
                1.0f - (diff / SPOKE_COUNT.toFloat()) * 0.82f
            } else {
                0.45f
            }

            drawLine(
                color = color.copy(alpha = alpha.coerceIn(0.15f, 1f)),
                start = Offset(startX, startY),
                end = Offset(endX, endY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}
