package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.theme.N300
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE


@Composable
fun OutlinedButton(
    modifier: Modifier = Modifier.fillMaxWidth(),
    labelText: String,
    textStyle: TextStyle = SSUType.Body1Medium,
    selected: Boolean = false,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(WHITE)
            .border(width = 1.dp, shape = RoundedCornerShape(8.dp), color = if(selected) R400 else N300)
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = labelText,
            style = textStyle
        )
    }
}
