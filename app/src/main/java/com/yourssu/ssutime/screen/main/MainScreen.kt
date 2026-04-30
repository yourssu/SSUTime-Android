package com.yourssu.ssutime.screen.main

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.R
import com.yourssu.ssutime.ui.theme.N300
import com.yourssu.ssutime.ui.theme.SSUType
import com.yourssu.ssutime.ui.theme.WHITE
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Preview
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize(),
        containerColor = WHITE,
        topBar = {
            SSUTimeTopBar(
                modifier = Modifier.statusBarsPadding()
            )
        }
    ) { innerPadding ->
        MainFragment(
            innerPadding = innerPadding
        )
    }
}

@Composable
@Preview
fun MainFragment(
    innerPadding: PaddingValues = PaddingValues(0.dp)
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(vertical = 32.dp, horizontal = 16.dp)
        ) {

            Text(
                text = "완료하면 자동으로 사라져요",
                style = SSUType.H4SemiBold
            )

            Text(
                text = "N건의 할 일이 있어요",
                style = SSUType.H1SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "업데이트 TIME 기준",
                    style = SSUType.Caption1Medium
                )

                Image(
                    modifier = Modifier.height(13.dp),
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "새로고침"
                )
            }

            Spacer(Modifier.height(28.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "가장 급한 과제에요",
                    style = SSUType.H3SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .border(width = 1.dp, color = N300, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp),
                    text = "제출 완료 N",
                    style = SSUType.Caption1SemiBold
                )
            }

        }
    }
}
@Composable
@Preview
fun SSUTimeTopBar(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(R.drawable.logo_red),
            contentDescription = "App Icon"
        )

        Spacer(Modifier.weight(1f))

        Image(
            modifier = Modifier.height(IntrinsicSize.Max),
            painter = painterResource(R.drawable.ic_user),
            contentDescription = "User"
        )
    }
}