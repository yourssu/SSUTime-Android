package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.WHITE

val DesktopCardWidth: Dp = 480.dp
val DesktopCardShape = RoundedCornerShape(16.dp)

@Composable
fun DesktopFloatingCardLayout(
    modifier: Modifier = Modifier,
    fillCardHeight: Boolean = true,
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
                .then(
                    if (fillCardHeight) {
                        Modifier
                            .fillMaxHeight()
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    } else {
                        Modifier
                            .wrapContentHeight()
                            .padding(vertical = 24.dp, horizontal = 16.dp)
                    },
                )
                .then(cardModifier)
                .shadow(elevation = 6.dp, shape = DesktopCardShape)
                .clip(DesktopCardShape)
                .background(WHITE)
                .border(1.dp, N200, DesktopCardShape),
            content = content,
        )
    }
}
