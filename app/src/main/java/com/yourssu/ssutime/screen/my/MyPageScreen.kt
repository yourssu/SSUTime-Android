package com.yourssu.ssutime.screen.my

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourssu.ssutime.ui.theme.N100
import com.yourssu.ssutime.ui.theme.N200
import com.yourssu.ssutime.ui.theme.R400
import com.yourssu.ssutime.ui.theme.SSUType
import com.yourssu.ssutime.ui.theme.WHITE
import com.yourssu.ssutime.v2.R
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    viewModel: MyViewModel = koinViewModel(),
    onPressBack: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val loginInfo = viewModel.loginInfo.value
    val isLogout by remember { viewModel.isLogout }
    var showLogoutPopup by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState(
        isPersistent = true
    )
    val coroutine = rememberCoroutineScope()

    LaunchedEffect(isLogout) {
        if(isLogout)
            onLogout()
    }

    if (showLogoutPopup) {
        Dialog(onDismissRequest = { showLogoutPopup = false }) {
            LogoutPopup(
                onCancel = { showLogoutPopup = false },
                onConfirm = { viewModel.logout() }
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Row {
            IconButton(onClick = onPressBack) {
                Image(
                    imageVector = Icons.Outlined.ArrowBackIosNew,
                    contentDescription = "back"
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.avatar_container),
                contentDescription = "avatar",
                modifier = Modifier.size(100.dp, 100.dp)
            )
            Text(
                text = loginInfo?.user_name ?: "불러오는 중",
                style = SSUType.H3SemiBold,
            )
            Text(
                text = loginInfo?.dept_name ?: "",
                style = SSUType.H4SemiBold,
            )
        }

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "알림 설정"
                )

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
                    tooltip = {
                        Card(
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            NotificationTooltip()
                        }
                    },
                    state = tooltipState
                ) {
                    IconButton(onClick = { coroutine.launch { tooltipState.show() } }) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "About Notification"
                        )
                    }
                }
            }

            ToggleOption(
                text = "시스템 알림",
                value = true, // TODO: Bind to ViewModel state
                onValueChanged = { /* TODO: Update ViewModel */ }
            )
            ToggleOption(
                text = "전화 알림",
                value = true, // TODO: Bind to ViewModel state
                onValueChanged = { /* TODO: Update ViewModel */ }
            )
        }

        Spacer(Modifier.height(28.dp))

        OptionButton(
            text = "로그아웃"
        ) {
            showLogoutPopup = true
        }
    }
}

@Composable
fun ToggleOption(
    text: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = N100)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold
        )
        Spacer(Modifier.weight(1f))
        Switch(
            modifier = Modifier.height(0.dp),
            checked = value,
            onCheckedChange = onValueChanged,
            colors = SwitchDefaults.colors(
                checkedTrackColor = R400
            )
        )
    }
}

@Composable
fun OptionButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color = N100)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold
        )
    }
}

@Composable
fun PopupButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color = color)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold,
            color = textColor
        )
    }
}


@Composable
@Preview(showBackground = true)
fun previewToggleOption() {
    ToggleOption(
        "시스템 알림", true
    ) { }
}

@Composable
@Preview
fun NotificationTooltip() {
    Column(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WHITE)
            .padding(16.dp)
    ) {
        Text(
            text = "시스템 푸시?",
            style = SSUType.Caption1SemiBold
        )
        Text(
            text = "앱 푸시 메시지로 간편하게 알려드려요",
            style = SSUType.Body2Medium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "전화 알림?",
            style = SSUType.Caption1SemiBold
        )
        Text(
            text = "과제 마감 당일 설정한 시간까지 완료하지 않았다면, 전화 알림을 드려요! * 진짜 전화는 아니니 놀라지 않으셔도 돼요!",
            style = SSUType.Body2Medium
        )
    }
}

@Composable
@Preview
fun LogoutPopup(
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    Column(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WHITE)
            .padding(top = 18.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "로그아웃",
            style = SSUType.H4SemiBold
        )
        Text(
            text = "정말 로그아웃 하시겠어요?",
            style = SSUType.Body1Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PopupButton(
                modifier = Modifier
                    .weight(1f),
                text = "취소",
                color = N200,
                onClick = onCancel
            )
            PopupButton(
                modifier = Modifier
                    .weight(1f),
                text = "확인",
                color = R400,
                textColor = WHITE,
                onClick = onConfirm
            )
        }
    }
}
