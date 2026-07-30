package com.yourssu.ssutime.desktop.screen.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.logo
import com.yourssu.ssutime.desktop.ui.resources.splash_logo_content_description
import com.yourssu.ssutime.desktop.ui.resources.splash_tagline
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopSplashScreen(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFFE2B27)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            painter = painterResource(Res.drawable.logo),
            contentDescription = stringResource(
                Res.string.splash_logo_content_description,
            ),
            modifier = Modifier.size(width = 200.dp, height = 50.dp),
        )
        Text(
            text = stringResource(Res.string.splash_tagline),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
        )
    }
}
