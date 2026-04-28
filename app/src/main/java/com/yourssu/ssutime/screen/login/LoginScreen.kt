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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.R
import com.yourssu.ssutime.component.SButton
import com.yourssu.ssutime.component.SCheckBox
import com.yourssu.ssutime.component.SSecureTextField
import com.yourssu.ssutime.component.STextField
import com.yourssu.ssutime.previewModule
import com.yourssu.ssutime.ui.theme.R500
import com.yourssu.ssutime.ui.theme.SSUType
import com.yourssu.ssutime.ui.theme.WHITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    coroutine: CoroutineScope = rememberCoroutineScope()
    ) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(horizontal = 16.dp)
    ) {

        val idState = remember { viewModel.idState }
        val pwState = remember { viewModel.pwState }
        val errorMessage = remember { viewModel.errorMessage }

        Image(
            painter = painterResource(R.drawable.logo_red),
            contentDescription = "logo"
        )

        Spacer(Modifier.height(24.dp))

        STextField(
            state = idState,
            "유세인트 아이디를 입력하세요"
        )
        Spacer(Modifier.height(10.dp))

        SSecureTextField(
            state = pwState,
            "유세인트 비밀번호를 입력하세요"
        )

        Spacer(Modifier.height(12.dp))

        SCheckBox(
            labelText = "자동 로그인 하기",
            checked = viewModel.autoLoginState,
            onCheckedChanged = {}
        )

        Spacer(Modifier.height(30.dp))

        if(errorMessage.value.isNotEmpty()) {
            Text(
                text = errorMessage.value,
                style = SSUType.Caption1SemiBold.copy(color = R500)
            )
        }

        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = "로그인",
            enable = idState.text.isNotEmpty() && pwState.text.isNotEmpty(),
            onClick = {
                coroutine.launch {
                    var errorMessage = ""
                    val isLogined = withContext(Dispatchers.IO) {
                        // 무거운 작업 + 네트워크 작업은 I/O쓰레드에서 따로 실행
                        return@withContext try {
                            viewModel.login()
                        } catch (e: Exception) {
                            errorMessage = e.message ?: ""
                            false
                        }
                    }

                    // 다시 메인쓰레드에서 나머지 작업 처리
                    viewModel.processLogin(isLogined, errorMessage)
                }
            }
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