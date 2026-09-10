package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.WHITE

val DesktopCardWidth: Dp = 480.dp

@Composable
fun DesktopFloatingCardLayout(
    modifier: Modifier = Modifier,
    cardWidth: Dp = DesktopCardWidth,
    cardModifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(N100),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = cardWidth)
                .fillMaxWidth()
                .fillMaxHeight()
                .then(cardModifier)
                .shadow(elevation = 4.dp)
                .background(WHITE)
                .drawWithContent {
                    drawContent()
                    val strokeWidth = 1.dp.toPx()
                    // 좌우에만 카드 경계선(Divider) 적용
                    drawLine(
                        color = N200,
                        start = Offset(strokeWidth / 2, 0f),
                        end = Offset(strokeWidth / 2, size.height),
                        strokeWidth = strokeWidth,
                    )
                    drawLine(
                        color = N200,
                        start = Offset(size.width - strokeWidth / 2, 0f),
                        end = Offset(size.width - strokeWidth / 2, size.height),
                        strokeWidth = strokeWidth,
                    )
                },
            content = content,
        )
    }
}
