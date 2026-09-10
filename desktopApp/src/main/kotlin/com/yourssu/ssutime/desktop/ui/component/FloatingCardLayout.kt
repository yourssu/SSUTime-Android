package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.fire_left_1
import com.yourssu.ssutime.desktop.ui.resources.fire_left_2
import com.yourssu.ssutime.desktop.ui.resources.fire_left_3
import com.yourssu.ssutime.desktop.ui.resources.fire_left_4
import com.yourssu.ssutime.desktop.ui.resources.fire_left_5
import com.yourssu.ssutime.desktop.ui.resources.fire_right_1
import com.yourssu.ssutime.desktop.ui.resources.fire_right_2
import com.yourssu.ssutime.desktop.ui.resources.fire_right_3
import com.yourssu.ssutime.desktop.ui.resources.fire_right_4
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.painterResource

val DesktopCardWidth: Dp = 480.dp

val LeftFireDrawables = listOf(
    Res.drawable.fire_left_1,
    Res.drawable.fire_left_2,
    Res.drawable.fire_left_3,
    Res.drawable.fire_left_4,
    Res.drawable.fire_left_5,
)

val RightFireDrawables = listOf(
    Res.drawable.fire_right_4,
    Res.drawable.fire_right_3,
    Res.drawable.fire_right_2,
    Res.drawable.fire_right_1,
)

@Composable
fun DesktopFloatingCardLayout(
    modifier: Modifier = Modifier,
    cardWidth: Dp = DesktopCardWidth,
    cardModifier: Modifier = Modifier,
    showFireCharacters: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(N100),
        contentAlignment = Alignment.Center,
    ) {
        val actualCardWidth = if (maxWidth < cardWidth) maxWidth else cardWidth
        val hasEnoughSideSpace = showFireCharacters && (maxWidth - actualCardWidth) >= 120.dp

        // Layer 1: 좌측 5개, 우측 4개 불꽃 캐릭터를 화면 하단부에 서로 겹치며(-44dp) 밀어넣어 배치
        if (hasEnoughSideSpace) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 좌측 불꽃 캐릭터 컬럼: 동적 사이징 없이 이미지끼리 겹치도록(-44dp) 화면 하단부에 밀착
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .offset(x = 2.dp)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy((-44).dp, Alignment.Bottom),
                ) {
                    LeftFireDrawables.forEach { res ->
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                        )
                    }
                }

                // 중앙 카드 너비만큼의 공간 확보 (캐릭터들이 카드 뒤에 가려지도록)
                Spacer(modifier = Modifier.width(actualCardWidth))

                // 우측 불꽃 캐릭터 컬럼: 동적 사이징 없이 이미지끼리 겹치도록(-44dp) 화면 하단부에 밀착 (fire_right_1이 맨 아래)
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .offset(x = (-2).dp)
                        .padding(bottom = 8.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy((-44).dp, Alignment.Bottom),
                ) {
                    RightFireDrawables.forEach { res ->
                        Image(
                            painter = painterResource(res),
                            contentDescription = null,
                        )
                    }
                }
            }
        }

        // Layer 2: 중앙 플로팅 카드 (Layer 1 위에 렌더링되어 양쪽 캐릭터의 절단면을 2dp 가리고 그림자가 자연스럽게 드리워짐)
        Box(
            modifier = Modifier
                .width(actualCardWidth)
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
