package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.theme.N300
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE

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

@Composable
fun SButton_Small(
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
        contentPadding = PaddingValues(vertical = 0.dp, horizontal = 12.dp),
        onClick = onClick,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            modifier = Modifier.padding(0.dp),
            text = labelText,
            style = textStyle.copy(color = WHITE)
        )
    }
}
