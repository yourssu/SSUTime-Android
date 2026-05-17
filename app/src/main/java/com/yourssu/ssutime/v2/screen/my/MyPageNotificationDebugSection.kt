package com.yourssu.ssutime.v2.screen.my

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.SSUType

@Composable
fun NotificationDebugSection(
    onScheduleCallAlert: () -> Unit,
    onScheduleNormalAlert: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "임시 알림 테스트",
            style = SSUType.H5SemiBold,
        )
        Text(
            text = "버튼을 누르면 10초 후 저장된 가장 임박한 할 일로 테스트해요.",
            style = SSUType.Caption1Medium,
            color = N300,
        )
        OptionButton(
            text = "10초 후 통화 알림 테스트",
            onClick = onScheduleCallAlert,
        )
        OptionButton(
            text = "10초 후 일반 알림 테스트",
            onClick = onScheduleNormalAlert,
        )
    }
}
