package com.yourssu.ssutime.v2.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.R300
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE

@Composable
fun SCheckBox(
    labelText: String,
    checked: MutableState<Boolean>,
    onCheckedChanged: (Boolean) -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            colors = CheckboxDefaults.colors(
                checkedColor = R300,
                checkmarkColor = WHITE
            ),
            checked = checked.value,
            onCheckedChange = null,
            modifier = Modifier
                .padding(end = 12.dp)
                .toggleable(
                    value = checked.value,
                    role = Role.Checkbox,
                    onValueChange = {
                        checked.value = it
                        onCheckedChanged(it)
                    }
                )
        )
        Text(
            modifier = Modifier.clickable {
                checked.value = !checked.value
            },
            text = labelText,
            style = SSUType.Label2Medium.copy(color = N500)
        )
    }

}

@Preview
@Composable
fun previewSCheckBox() {
    val checked = remember { mutableStateOf(false) }
    SCheckBox("자동 로그인 하기", checked)
}

@Preview
@Composable
fun previewSCheckBox2() {
    val checked = remember { mutableStateOf(true) }
    SCheckBox("자동 로그인 하기", checked)
}