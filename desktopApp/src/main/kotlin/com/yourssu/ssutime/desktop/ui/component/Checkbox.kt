package com.yourssu.ssutime.desktop.ui.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.yourssu.ssutime.desktop.ui.theme.N500
import com.yourssu.ssutime.desktop.ui.theme.R500
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE

@Composable
fun SCheckBox(
    labelText: String,
    checked: MutableState<Boolean>,
    onCheckedChanged: (Boolean) -> Unit = {},
) {
    Row(
        modifier = Modifier
            .toggleable(
                value = checked.value,
                role = Role.Checkbox,
                onValueChange = {
                    checked.value = it
                    onCheckedChanged(it)
                },
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            colors = CheckboxDefaults.colors(
                checkedColor = R500,
                checkmarkColor = WHITE
            ),
            checked = checked.value,
            onCheckedChange = null,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = labelText,
            style = SSUType.Label2Medium.copy(color = N500),
        )
    }
}
