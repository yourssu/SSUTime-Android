package com.yourssu.ssutime.desktop.screen.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE

enum class DesktopNavTab {
    TODO,      // 1분할: Main Todo
    CALENDAR,  // 1분할: Calendar
    MY_PAGE,   // Profile
}

const val DEFAULT_WINDOW_WIDTH_DP = 520
const val DEFAULT_WINDOW_HEIGHT_DP = 760

private val KakaoSideBg = Color(0xFFF2F3F6)

@Composable
fun DesktopVerticalNavBar(
    currentTab: DesktopNavTab,
    onTabSelect: (DesktopNavTab) -> Unit,
    modifier: Modifier = Modifier,
    unreadNoticeCount: Int = 0,
) {
    Row(
        modifier = modifier
            .width(56.dp)
            .fillMaxHeight(),
    ) {
        // 카카오톡 PC 스타일 세로 네비게이션 바 본체
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(KakaoSideBg)
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 할 일 메뉴
            NavBarItem(
                icon = Icons.Default.CheckCircle,
                tooltipText = "할 일",
                isSelected = currentTab == DesktopNavTab.TODO,
                onClick = { onTabSelect(DesktopNavTab.TODO) },
            )

            Spacer(Modifier.height(12.dp))

            // 캘린더 메뉴
            NavBarItem(
                icon = Icons.Default.CalendarMonth,
                tooltipText = "캘린더",
                isSelected = currentTab == DesktopNavTab.CALENDAR,
                onClick = { onTabSelect(DesktopNavTab.CALENDAR) },
                hasBadge = unreadNoticeCount > 0,
            )

            Spacer(Modifier.height(12.dp))

            // 마이페이지 메뉴
            NavBarItem(
                icon = Icons.Default.Person,
                tooltipText = "마이페이지",
                isSelected = currentTab == DesktopNavTab.MY_PAGE,
                onClick = { onTabSelect(DesktopNavTab.MY_PAGE) },
            )
        }

        // 우측 1dp 구분선 (본문과의 경계)
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(N200),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NavBarItem(
    icon: ImageVector,
    tooltipText: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasBadge: Boolean = false,
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(),
        tooltip = {
            PlainTooltip(
                containerColor = Color(0xEE333333),
                contentColor = WHITE,
                shape = RoundedCornerShape(4.dp),
            ) {
                Text(
                    text = tooltipText,
                    style = SSUType.Caption1Medium,
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        val shape = RoundedCornerShape(10.dp)
        Box(
            modifier = modifier
                .size(42.dp)
                .then(
                    if (isSelected) {
                        Modifier
                            .shadow(2.dp, shape = shape, clip = false)
                            .background(WHITE, shape = shape)
                            .border(1.dp, N200, shape = shape)
                    } else {
                        Modifier
                    },
                )
                .clip(shape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = tooltipText,
                tint = if (isSelected) R500 else N500,
                modifier = Modifier.size(24.dp),
            )

            if (hasBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 4.dp, end = 4.dp)
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(R500),
                )
            }
        }
    }
}
