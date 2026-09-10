package com.yourssu.ssutime.desktop.screen.login

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
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.component.SCheckBox
import com.yourssu.ssutime.desktop.ui.component.SSecureTextField
import com.yourssu.ssutime.desktop.ui.component.STextField
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.login_auto_login
import com.yourssu.ssutime.desktop.ui.resources.login_button
import com.yourssu.ssutime.desktop.ui.resources.login_id_placeholder
import com.yourssu.ssutime.desktop.ui.resources.login_logo_content_description
import com.yourssu.ssutime.desktop.ui.resources.login_password_placeholder
import com.yourssu.ssutime.desktop.ui.resources.logo_red
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopLoginScreen(
    idState: TextFieldState,
    passwordState: TextFieldState,
    autoLoginState: MutableState<Boolean>,
    message: String,
    isLoading: Boolean,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
    onAutoLoginChanged: (Boolean) -> Unit = {},
    messageColor: Color = R500,
    loginEnabled: Boolean = true,
) {
    val canLogin = loginEnabled &&
        idState.text.isNotEmpty() &&
        passwordState.text.isNotEmpty()

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (
                        event.key == Key.Enter &&
                        event.type == KeyEventType.KeyUp &&
                        canLogin
                    ) {
                        onLoginClick()
                        true
                    } else {
                        false
                    }
                }
                .padding(horizontal = 32.dp, vertical = 40.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.logo_red),
                contentDescription = stringResource(Res.string.login_logo_content_description),
            )

            Spacer(Modifier.height(24.dp))

            STextField(
                state = idState,
                placeholder = stringResource(Res.string.login_id_placeholder),
            )

            Spacer(Modifier.height(10.dp))

            SSecureTextField(
                state = passwordState,
                placeholder = stringResource(Res.string.login_password_placeholder),
            )

            Spacer(Modifier.height(12.dp))

            SCheckBox(
                labelText = stringResource(Res.string.login_auto_login),
                checked = autoLoginState,
                onCheckedChanged = onAutoLoginChanged,
            )

            Spacer(Modifier.height(30.dp))

            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    style = SSUType.Caption1SemiBold.copy(color = messageColor),
                )
                Spacer(Modifier.height(8.dp))
            }

            SButton(
                modifier = Modifier.fillMaxWidth(),
                labelText = stringResource(Res.string.login_button),
                enable = canLogin,
                onClick = onLoginClick,
            )
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
        }
    }
}
