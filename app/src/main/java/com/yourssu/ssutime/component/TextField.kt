package com.yourssu.ssutime.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicSecureTextField
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.ui.theme.N100
import com.yourssu.ssutime.ui.theme.N400
import com.yourssu.ssutime.ui.theme.N700
import com.yourssu.ssutime.ui.theme.SSUType

@Composable
fun STextField(
    state: TextFieldState = rememberTextFieldState(),
    placeholder: String,
) {
    BasicTextField(
        modifier = Modifier
            .clip(
                shape = RoundedCornerShape(10.dp)
            )
            .background(N100)
            .padding(
                vertical = 20.dp,
                horizontal = 22.dp
            )
            .height(20.dp),
        lineLimits = TextFieldLineLimits.SingleLine,
        textStyle = SSUType.Label2Medium.copy(color = N700),
        state = state,
        decorator = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = SSUType.Label2Medium.copy(color = N400)
                    )
                }

                innerTextField()
            }
        }
    )
}

@Composable
fun SSecureTextField(
    state: TextFieldState = rememberTextFieldState(),
    placeholder: String,
) {
    BasicSecureTextField(
        modifier = Modifier
            .clip(
                shape = RoundedCornerShape(10.dp)
            )
            .background(N100)
            .padding(
                vertical = 20.dp,
                horizontal = 22.dp
            )
            .height(20.dp),
        textStyle = SSUType.Label2Medium.copy(color = N700),
        state = state,
        decorator = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxWidth(),
                contentAlignment = Alignment.CenterStart
            ) {
                if (state.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = SSUType.Label2Medium.copy(color = N400)
                    )
                }

                innerTextField()
            }
        }
    )
}


@Preview
@Composable
fun previewSTextField() {
    STextField(rememberTextFieldState("20222908"), "유세인트 아이디를 입력하세요")
}

@Preview
@Composable
fun previewEmptySTextField() {
    STextField(placeholder = "유세인트 아이디를 입력하세요")
}