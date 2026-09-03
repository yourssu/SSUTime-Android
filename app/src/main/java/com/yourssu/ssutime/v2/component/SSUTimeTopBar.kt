package com.yourssu.ssutime.v2.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R

@Composable
fun SSUTimeTopBar(
    modifier: Modifier = Modifier,
    onLogoClick: (() -> Unit)? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val logoModifier = Modifier
            .height(18.dp)
            .then(
                if (onLogoClick != null) Modifier.clickable(onClick = onLogoClick)
                else Modifier
            )
        Image(
            modifier = logoModifier,
            painter = painterResource(R.drawable.logo_red),
            contentDescription = stringResource(R.string.app_name)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun previewSSUTimeTopBar() {
    SSUTimeTopBar()
}
