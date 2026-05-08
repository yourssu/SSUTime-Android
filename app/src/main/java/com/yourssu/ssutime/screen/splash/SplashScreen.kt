package com.yourssu.ssutime.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.R
import kotlinx.coroutines.delay

@Composable
@Preview
fun SplashScreen(
    modifier: Modifier = Modifier,
    navigateToLogin: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFE2B27)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.logo),
            contentDescription = "logo",
            modifier = Modifier.size(width = 200.dp, height = 50.dp)
        )
        Text(
            text = "과제 놓쳐서 학점을 말아먹었다고?ㅏ",
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }

    LaunchedEffect(Unit) {
        delay(1000)
        navigateToLogin()
    }
}
