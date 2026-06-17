package com.yourssu.ssutime.v2.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.component.SButton
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.SSUType

@Composable
@Preview
fun WidgetHelperDialogContent(
    onDismissClick: () -> Unit = {}
) {
    Column {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.widget_helper_title),
                style = SSUType.H2SemiBold
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = stringResource(R.string.widget_helper_description),
                style = SSUType.Body1Medium,
                color = N500,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(8.dp))
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(N100)
                .padding(horizontal = 10.dp, vertical = 14.dp)
        ) {
            Text(
                text = stringResource(R.string.widget_helper_preview_title),
                style = SSUType.H5SemiBold
            )
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .height(200.dp)
            ) {
                Image(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    painter = painterResource(R.drawable.widget1),
                    contentDescription = stringResource(R.string.widget_helper_preview_content_description_1)
                )

                Image(
                    modifier = Modifier
                        .weight(1.18f)
                        .fillMaxHeight(),
                    painter = painterResource(R.drawable.widget3),
                    contentDescription = stringResource(R.string.widget_helper_preview_content_description_2)
                )

                Image(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    painter = painterResource(R.drawable.widget2),
                    contentDescription = stringResource(R.string.widget_helper_preview_content_description_3)
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        SButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            labelText = stringResource(R.string.common_confirm),
            onClick = { onDismissClick() }
        )
    }
}
