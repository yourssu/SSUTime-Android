package com.yourssu.ssutime.screen.login

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.R
import com.yourssu.ssutime.component.STextField
import com.yourssu.ssutime.previewModule
import com.yourssu.ssutime.ui.theme.WHITE
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    ) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(horizontal = 16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.logo_red),
            contentDescription = "logo"
        )
        Spacer(Modifier.height(24.dp))
        STextField(
            state = viewModel.idState,
            "유세인트 아이디를 입력하세요"
        )
        Spacer(Modifier.height(10.dp))
        STextField(
            state = viewModel.pwState,
            "유세인트 비밀번호를 입력하세요"
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginRoutePreview() {
    KoinApplicationPreview(
        application = {
            modules(previewModule)
        }
    ) {
        LoginScreen()
    }
}