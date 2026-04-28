package com.yourssu.ssutime.screen.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.yourssu.ssutime.ui.theme.SSUType

@Composable
@Preview
fun OnBoardingScreen() {
    NotificationFragment()
}

@Composable
@Preview
fun NotificationFragment() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "과제 알림을 제공받기 위해\n허용을 눌러주세요",
            style = SSUType.H2SemiBold,
            textAlign = TextAlign.Center
        )
    }
}