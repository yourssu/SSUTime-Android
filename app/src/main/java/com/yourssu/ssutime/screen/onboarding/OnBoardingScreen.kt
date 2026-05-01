package com.yourssu.ssutime.screen.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.R
import com.yourssu.ssutime.component.SButton
import com.yourssu.ssutime.ui.theme.N100
import com.yourssu.ssutime.ui.theme.N500
import com.yourssu.ssutime.ui.theme.R100
import com.yourssu.ssutime.ui.theme.R300
import com.yourssu.ssutime.ui.theme.R500
import com.yourssu.ssutime.ui.theme.SSUType
import com.yourssu.ssutime.ui.theme.WHITE
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnBoardingScreen(
    viewModel: OnBoardingViewModel = koinViewModel(),
    onConfirmClick: () -> Unit = {}
) {
    var isGranted by remember { viewModel.isGranted }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
    }

    if(isGranted) {
        TipFragment(onConfirmClick)
    } else {
        NotificationFragment() {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

@Composable
@Preview
fun NotificationFragment(
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "과제 알림을 제공받기 위해\n허용을 눌러주세요",
            style = SSUType.H2SemiBold,
            textAlign = TextAlign.Center
        )

        Image(
            modifier = Modifier.fillMaxWidth(),
            painter = painterResource(R.drawable.noti_guide),
            contentDescription = "알림 권한 요청 예시"
        )
    }
    Box(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = "다음",
            onClick = onClick
        )
    }
}

@Preview
@Composable
fun TipFragment(
    onClick: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 50.dp)
            .fillMaxSize()
            .safeDrawingPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "성적에 반영되는 할 일의\n마감일을 알려줄게요",
            style = SSUType.H2SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(30.dp))
        TipItem(
            key = "1",
            tipText = "LearningX [과제]"
        )
        Spacer(Modifier.height(12.dp))
        TipItem(
            key = "1",
            tipText = "LearningX [퀴즈]"
        )
        Spacer(Modifier.height(12.dp))
        TipItem(
            key = "1",
            tipText = "LearningX [강의]"
        )
        Spacer(Modifier.height(12.dp))
        TipItem(
            tipText = "숭실 사이버 강의는 포함되지 않아요",
            isWarning = true
        )
    }

    Box(
        modifier = Modifier
            .padding(20.dp)
            .fillMaxSize()
            .safeDrawingPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = "다음",
            onClick = onClick
        )
    }
}

@Composable
fun TipItem(
    key: String = "",
    tipText: String,
    isWarning: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if(isWarning) R100 else N100)
            .run {
                if(isWarning)
                    this.border(width = 1.dp, color = R300, shape = RoundedCornerShape(10.dp))
                else
                    this
            }

    ) {
        Row(
            modifier = Modifier
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
            ) {
            if(key.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(20.dp, 20.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(N500),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "1",
                        style = SSUType.Caption1Medium.copy(color = WHITE)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))
            }

            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = if(isWarning) TextAlign.Center else TextAlign.Unspecified,
                text = tipText,
                style = if(isWarning) SSUType.Body1Medium.copy(color = R500) else SSUType.H4Medium
            )
        }
    }
}

@Composable
@Preview
fun previewTipItem() {
    TipItem(key = "1", tipText = "LearningX [과제]")
}

@Composable
@Preview
fun previewTipItemWarning() {
    TipItem(tipText = "숭실 사이버대학 강의는 포함되지 않아요", isWarning = true)
}