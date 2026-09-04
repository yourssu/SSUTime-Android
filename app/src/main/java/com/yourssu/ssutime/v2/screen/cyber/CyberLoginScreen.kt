package com.yourssu.ssutime.v2.screen.cyber

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.component.SButton
import com.yourssu.ssutime.v2.component.SSUTimeTopBar
import com.yourssu.ssutime.v2.component.SSecureTextField
import com.yourssu.ssutime.v2.component.STextField
import com.yourssu.ssutime.v2.ui.theme.BLACK
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE

@Composable
fun CyberLoginScreen(
    modifier: Modifier = Modifier,
    onBackClick: () -> Unit = {},
    onLoginClick: (id: String, pw: String) -> Unit = { _, _ -> },
    onFindIdClick: () -> Unit = {},
) {
    val idState = rememberTextFieldState()
    val pwState = rememberTextFieldState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE),
    ) {
        SSUTimeTopBar(
            onLogoClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.cyber_login_title),
                style = SSUType.H2SemiBold,
                color = BLACK,
            )

            Spacer(Modifier.height(36.dp))

            STextField(
                state = idState,
                placeholder = stringResource(R.string.cyber_login_id_placeholder),
            )

            Spacer(Modifier.height(12.dp))

            SSecureTextField(
                state = pwState,
                placeholder = stringResource(R.string.cyber_login_pw_placeholder),
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    text = stringResource(R.string.cyber_login_find_id),
                    style = SSUType.Label3Medium.copy(textDecoration = TextDecoration.Underline),
                    color = N500,
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFindIdClick,
                    ),
                )
            }

            Spacer(Modifier.height(12.dp))

            SButton(
                modifier = Modifier.fillMaxWidth(),
                labelText = stringResource(R.string.login_button),
                textStyle = SSUType.H4SemiBold,
                enable = idState.text.isNotEmpty() && pwState.text.isNotEmpty(),
                onClick = {
                    onLoginClick(idState.text.toString(), pwState.text.toString())
                },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CyberLoginScreenPreview() {
    CyberLoginScreen()
}
