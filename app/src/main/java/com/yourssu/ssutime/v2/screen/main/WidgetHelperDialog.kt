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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.component.SButton
import com.yourssu.ssutime.v2.ui.theme.N100
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
                text = "위젯을 추가하면 볼 수 있어요",
                style = SSUType.H2SemiBold
            )

            Text(
                text = "홈화면 길게 누르고 위젯 버튼 눌러서\n슈타임 위젯을 추가하세요",
                style = SSUType.Body1Medium,
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
                text = "위젯 미리보기",
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
                    contentDescription = "위젯 미리보기 1"
                )

                Image(
                    modifier = Modifier
                        .weight(1.18f)
                        .fillMaxHeight(),
                    painter = painterResource(R.drawable.widget3),
                    contentDescription = "위젯 미리보기 2"
                )

                Image(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    painter = painterResource(R.drawable.widget2),
                    contentDescription = "위젯 미리보기 3"
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        SButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            labelText = "확인",
            onClick = { onDismissClick() }
        )
    }
}