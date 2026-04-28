package com.yourssu.ssutime.screen.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.R
import com.yourssu.ssutime.component.SButton
import com.yourssu.ssutime.ui.theme.SSUType
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun OnBoardingScreen(
    viewModel: OnBoardingViewModel = koinViewModel(),
) {
    var isGranted by remember { viewModel.isGranted }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        isGranted = granted
    }

    if(isGranted) {
        TipFragment()
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
            .fillMaxSize(),
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
            .fillMaxSize(),
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
fun TipFragment() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "성적에 반영되는 할 일의\n마감일을 알려줄게요",
            style = SSUType.H2SemiBold,
            textAlign = TextAlign.Center
        )
    }
}