package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.todo_detail_back
import com.yourssu.ssutime.desktop.ui.theme.N600
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopBackButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = stringResource(Res.string.todo_detail_back),
) {
    Box(
        modifier = modifier
            .size(36.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_arrow_back),
            contentDescription = contentDescription,
            tint = N600,
            modifier = Modifier.size(20.dp),
        )
    }
}
