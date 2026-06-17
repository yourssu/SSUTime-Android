package com.yourssu.ssutime.v2.screen.splash

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun SplashScreen(
    viewModel: SplashViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    navigateToLogin: () -> Unit = {},
    navigateToMain: () -> Unit = {},
) {
    val destination by remember { viewModel.destination }

    SplashContent(modifier = modifier)

    LaunchedEffect(destination) {
        when (destination) {
            SplashDestination.Login -> navigateToLogin()
            SplashDestination.Main -> navigateToMain()
            null -> Unit
        }
    }
}

@Composable
private fun SplashContent(
    modifier: Modifier = Modifier,
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
            contentDescription = stringResource(R.string.splash_logo_content_description),
            modifier = Modifier.size(width = 200.dp, height = 50.dp)
        )
        Text(
            text = stringResource(R.string.splash_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}

@Preview
@Composable
private fun SplashContentPreview() {
    SplashContent()
}
