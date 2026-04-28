package com.yourssu.ssutime.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.ui.theme.N300
import com.yourssu.ssutime.ui.theme.R500
import com.yourssu.ssutime.ui.theme.SSUType
import com.yourssu.ssutime.ui.theme.WHITE

@Composable
fun SButton(
    modifier: Modifier = Modifier,
    labelText: String,
    onClick: () -> Unit = {},
    enable: Boolean = true,
    textStyle: TextStyle = SSUType.Label2Medium
) {
    Button(
        colors = ButtonDefaults.buttonColors(
            disabledContentColor = WHITE,
            disabledContainerColor = N300,
            containerColor = R500
        ),
        enabled = enable,
        modifier = modifier,
        onClick = onClick,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            modifier = Modifier
                .padding(10.dp),
            text = labelText,
            style = textStyle.copy(color = WHITE)
        )
    }
}

@Preview
@Composable
fun previewEnableButton() {
    SButton(
        labelText = "로그인"
    )
}

@Preview
@Composable
fun previewDisableButton() {
    SButton(
        labelText = "로그인",
        enable = false
    )
}