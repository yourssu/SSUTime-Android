package com.yourssu.ssutime.desktop.screen.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.component.SButton
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.common_next
import com.yourssu.ssutime.desktop.ui.resources.onboarding_tip_assignment
import com.yourssu.ssutime.desktop.ui.resources.onboarding_tip_cyber_excluded
import com.yourssu.ssutime.desktop.ui.resources.onboarding_tip_lecture
import com.yourssu.ssutime.desktop.ui.resources.onboarding_tip_quiz
import com.yourssu.ssutime.desktop.ui.resources.onboarding_tip_title
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.R100
import com.yourssu.ssutime.desktop.ui.theme.R300
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import org.jetbrains.compose.resources.stringResource

@Composable
fun DesktopOnBoardingScreen(
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 50.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(Res.string.onboarding_tip_title),
                style = SSUType.H2SemiBold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            TipItem(stringResource(Res.string.onboarding_tip_assignment))
            Spacer(Modifier.height(12.dp))
            TipItem(stringResource(Res.string.onboarding_tip_quiz))
            Spacer(Modifier.height(12.dp))
            TipItem(stringResource(Res.string.onboarding_tip_lecture))
            Spacer(Modifier.height(12.dp))
            TipItem(
                text = stringResource(Res.string.onboarding_tip_cyber_excluded),
                isWarning = true,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            SButton(
                modifier = Modifier.fillMaxWidth(),
                labelText = stringResource(Res.string.common_next),
                onClick = onConfirmClick,
            )
        }
    }
}

@Composable
private fun TipItem(
    text: String,
    isWarning: Boolean = false,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isWarning) R100 else N100)
            .then(
                if (isWarning) {
                    Modifier.border(1.dp, R300, RoundedCornerShape(10.dp))
                } else {
                    Modifier
                },
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            if (!isWarning) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(N500),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "1",
                        style = SSUType.Caption1Medium.copy(color = WHITE),
                    )
                }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                modifier = Modifier.fillMaxWidth(),
                text = text,
                textAlign = if (isWarning) TextAlign.Center else TextAlign.Start,
                style = if (isWarning) {
                    SSUType.Body1Medium.copy(color = R500)
                } else {
                    SSUType.H4Medium
                },
            )
        }
    }
}
