package com.yourssu.ssutime.desktop.screen.cyber

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.component.DesktopBackButton
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.component.SSecureTextField
import com.yourssu.ssutime.desktop.ui.component.STextField
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.cyber_login_find_id
import com.yourssu.ssutime.desktop.ui.resources.cyber_login_id_placeholder
import com.yourssu.ssutime.desktop.ui.resources.cyber_login_pw_placeholder
import com.yourssu.ssutime.desktop.ui.resources.cyber_login_title
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.login_button
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_back
import com.yourssu.ssutime.desktop.ui.theme.BLACK
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.N600
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopCyberLoginScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onBack: () -> Unit,
    onLoginClick: (id: String, pw: String) -> Unit,
    onFindIdClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val idState = rememberTextFieldState()
    val pwState = rememberTextFieldState()
    val canLogin = !isLoading && idState.text.isNotEmpty() && pwState.text.isNotEmpty()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .onPreviewKeyEvent { event ->
                    if (
                        event.key == Key.Enter &&
                        event.type == KeyEventType.KeyUp &&
                        canLogin
                    ) {
                        onLoginClick(idState.text.toString(), pwState.text.toString())
                        true
                    } else {
                        false
                    }
                },
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                DesktopBackButton(onClick = onBack)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            ) {
                Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(Res.string.cyber_login_title),
                style = SSUType.H2SemiBold,
                color = BLACK,
            )

            Spacer(Modifier.height(36.dp))

            STextField(
                state = idState,
                placeholder = stringResource(Res.string.cyber_login_id_placeholder),
            )

            Spacer(Modifier.height(12.dp))

            SSecureTextField(
                state = pwState,
                placeholder = stringResource(Res.string.cyber_login_pw_placeholder),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(Res.string.cyber_login_find_id),
                    style = SSUType.Label3Medium.copy(textDecoration = TextDecoration.Underline),
                    color = N500,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onFindIdClick,
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                )
            }

            if (!errorMessage.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = errorMessage,
                    style = SSUType.Caption1SemiBold.copy(color = R500),
                )
            }

            Spacer(Modifier.height(16.dp))

            SButton(
                modifier = Modifier.fillMaxWidth(),
                labelText = stringResource(Res.string.login_button),
                textStyle = SSUType.H4SemiBold,
                enable = canLogin,
                onClick = {
                    onLoginClick(idState.text.toString(), pwState.text.toString())
                },
            )
            }
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
