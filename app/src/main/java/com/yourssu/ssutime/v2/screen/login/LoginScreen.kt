package com.yourssu.ssutime.v2.screen.login

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.component.SButton
import com.yourssu.ssutime.v2.component.SCheckBox
import com.yourssu.ssutime.v2.component.SSecureTextField
import com.yourssu.ssutime.v2.component.STextField
import com.yourssu.ssutime.v2.previewModule
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen(
    successLogin: () -> Unit = {},
    viewModel: LoginViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    coroutine: CoroutineScope = rememberCoroutineScope()
    ) {
    val idState = remember { viewModel.idState }
    val pwState = remember { viewModel.pwState }
    val errorMessage = remember { viewModel.errorMessage }
    val isLoading by remember { viewModel.isLoading }

    LaunchedEffect(Unit) {
        Analytics.viewLogin()
    }

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
            contentDescription = stringResource(R.string.login_logo_content_description)
        )

        Spacer(Modifier.height(24.dp))

        STextField(
            state = idState,
            stringResource(R.string.login_id_placeholder)
        )
        Spacer(Modifier.height(10.dp))

        SSecureTextField(
            state = pwState,
            stringResource(R.string.login_password_placeholder)
        )

        Spacer(Modifier.height(12.dp))

        SCheckBox(
            labelText = stringResource(R.string.login_auto_login),
            checked = viewModel.autoLoginState,
            onCheckedChanged = {}
        )

        Spacer(Modifier.height(30.dp))

        if (errorMessage.value.isNotEmpty()) {
            Text(
                text = errorMessage.value,
                style = SSUType.Caption1SemiBold.copy(color = R500)
            )
        }

        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = stringResource(R.string.login_button),
            enable = idState.text.isNotEmpty() && pwState.text.isNotEmpty(),
            onClick = {
                coroutine.launch {
                    Analytics.loginAttempt(autoLogin = viewModel.autoLoginState.value)
                    if (viewModel.login()) {
                        Analytics.identifyUser(idState.text.toString())
                        Analytics.loginSuccess()
                        registerFCMTokenAndContinue(viewModel, coroutine, successLogin)
                    } else {
                        Analytics.loginFailIfKnown(errorMessage.value)
                    }
                }
            }
        )
    }

    if(!isLoading)
        return

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Color(0x80000000)
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }

}

private fun registerFCMTokenAndContinue(
    viewModel: LoginViewModel,
    coroutine: CoroutineScope,
    onComplete: () -> Unit,
) {
    FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener { task ->
        if (!task.isSuccessful) {
            Log.w("FCM", "Fetching FCM registration token failed", task.exception)
            onComplete()
            return@OnCompleteListener
        }

        coroutine.launch {
            runCatching {
                viewModel.registerFCMToken(task.result)
            }.onFailure { exception ->
                SentryExceptionReporter.capture(exception)
                Log.e("FCM", "FCM token registration failed", exception)
            }
            onComplete()
        }
    })
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
