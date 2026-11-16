package com.yourssu.ssutime.v2.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE

@Composable
fun ConfirmPopup(
    title: String,
    message: String,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    messageTextAlign: TextAlign = TextAlign.Unspecified,
) {
    Column(
        modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WHITE)
            .padding(top = 18.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = title,
            style = SSUType.H4SemiBold,
        )
        Text(
            text = message,
            style = SSUType.Body1Medium,
            textAlign = messageTextAlign,
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PopupButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_cancel),
                color = N200,
                onClick = onCancel,
            )
            PopupButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.common_confirm),
                color = R400,
                textColor = WHITE,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
fun PopupButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color = color)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold,
            color = textColor,
        )
    }
}
