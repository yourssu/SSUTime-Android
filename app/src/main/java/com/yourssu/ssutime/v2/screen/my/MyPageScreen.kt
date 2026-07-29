@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.yourssu.ssutime.v2.screen.my

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIosNew
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourssu.data.AlertData
import com.yourssu.data.UiState
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.ui.theme.BLACK
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N200
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.N400
import com.yourssu.ssutime.v2.ui.theme.N500
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyPageScreen(
    viewModel: MyViewModel = koinViewModel(),
    onPressBack: () -> Unit = {},
    onLogout: () -> Unit = {},
) {
    val context = LocalContext.current
    val loginInfo = viewModel.loginInfo.value
    val terms = viewModel.terms.value
    val currentTerm = viewModel.currentTerm.value
    val selectedTerm by viewModel.selectedTerm.collectAsStateWithLifecycle()
    val isLogout by remember { viewModel.isLogout }
    var showLogoutPopup by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState(
        isPersistent = true
    )
    val coroutine = rememberCoroutineScope()
    val alertState by viewModel.uiState.collectAsStateWithLifecycle()
    var alertData by remember { mutableStateOf<AlertData>(AlertData(valid = false, false, false, -1)) }
    var pendingNotificationSettingsRequest by remember {
        mutableStateOf<NotificationSettingsRequest?>(null)
    }
    var pendingCallAlertThresholdMinutes by remember {
        mutableStateOf<Long?>(null)
    }

    fun updateAlertData(nextAlertData: AlertData) {
        alertData = nextAlertData
        viewModel.updateAlertData(nextAlertData)
    }

    fun enableSystemAlert() {
        Analytics.settingSystemAlarm(isEnabled = true)
        updateAlertData(
            alertData.copy(allowSystemAlert = true)
        )
    }

    fun disableSystemAlert() {
        Analytics.settingSystemAlarm(isEnabled = false)
        updateAlertData(
            alertData.copy(allowSystemAlert = false)
        )
    }

    fun applyCallAlertSetting(
        enabled: Boolean,
        thresholdMinutes: Long = alertData.callingAlertThresholdMinutes,
    ) {
        val selectedTime = if (enabled) {
            Analytics.selectedTimeFromMinutes(thresholdMinutes) ?: return
        } else {
            "reject"
        }
        Analytics.settingCallAlarm(
            isEnabled = enabled,
            selectedTime = selectedTime,
        )
        updateAlertData(
            alertData.copy(
                allowCallAlert = enabled,
                callingAlertThresholdMinutes = thresholdMinutes,
            )
        )
    }

    fun enableCallAlert(thresholdMinutes: Long = alertData.callingAlertThresholdMinutes) {
        applyCallAlertSetting(
            enabled = true,
            thresholdMinutes = thresholdMinutes,
        )
    }

    fun disableCallAlert() {
        applyCallAlertSetting(
            enabled = false,
        )
    }

    lateinit var notificationSettingsLauncher: androidx.activity.result.ActivityResultLauncher<Intent>

    fun openNotificationSettings(request: NotificationSettingsRequest) {
        pendingNotificationSettingsRequest = request
        val intent = when (request) {
            NotificationSettingsRequest.SystemAlert,
            NotificationSettingsRequest.CallAlertPostNotifications -> context.notificationSettingsIntent()
            NotificationSettingsRequest.CallAlertFullScreenIntent -> context.fullScreenIntentSettingsIntent()
        }
        notificationSettingsLauncher.launch(intent)
    }

    fun requestEnableSystemAlert() {
        if (context.canPostNotifications()) {
            enableSystemAlert()
        } else {
            openNotificationSettings(NotificationSettingsRequest.SystemAlert)
        }
    }

    fun requestEnableCallAlert(thresholdMinutes: Long = alertData.callingAlertThresholdMinutes) {
        pendingCallAlertThresholdMinutes = thresholdMinutes
        when {
            !context.canPostNotifications() -> {
                openNotificationSettings(NotificationSettingsRequest.CallAlertPostNotifications)
            }

            !context.canUseFullScreenIntent() -> {
                openNotificationSettings(NotificationSettingsRequest.CallAlertFullScreenIntent)
            }

            else -> {
                enableCallAlert(thresholdMinutes)
                pendingCallAlertThresholdMinutes = null
            }
        }
    }

    notificationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        when (pendingNotificationSettingsRequest) {
            NotificationSettingsRequest.SystemAlert -> {
                if (context.canPostNotifications()) {
                    enableSystemAlert()
                }
            }

            NotificationSettingsRequest.CallAlertPostNotifications -> {
                if (context.canPostNotifications()) {
                    val thresholdMinutes = pendingCallAlertThresholdMinutes
                        ?: alertData.callingAlertThresholdMinutes
                    if (context.canUseFullScreenIntent()) {
                        enableCallAlert(thresholdMinutes)
                    } else {
                        openNotificationSettings(NotificationSettingsRequest.CallAlertFullScreenIntent)
                        return@rememberLauncherForActivityResult
                    }
                }
            }

            NotificationSettingsRequest.CallAlertFullScreenIntent -> {
                if (context.canPostNotifications() && context.canUseFullScreenIntent()) {
                    enableCallAlert(
                        pendingCallAlertThresholdMinutes ?: alertData.callingAlertThresholdMinutes
                    )
                }
            }

            null -> Unit
        }
        pendingNotificationSettingsRequest = null
        pendingCallAlertThresholdMinutes = null
    }

    LaunchedEffect(Unit) {
        Analytics.viewMyPage()
    }

    LaunchedEffect(alertState) {
        val state = alertState
        if (state is UiState.Success) {
            alertData = state.data
        }
    }

    LaunchedEffect(isLogout) {
        if(isLogout)
            onLogout()
    }

    if (showLogoutPopup) {
        Dialog(onDismissRequest = { showLogoutPopup = false }) {
            LogoutPopup(
                onCancel = {
                    Analytics.logoutCancel()
                    showLogoutPopup = false
                },
                onConfirm = {
                    Analytics.logoutConfirm()
                    viewModel.logout()
                }
            )
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .safeDrawingPadding()
            .verticalScroll(scrollState)
    ) {
        Row {
            IconButton(onClick = onPressBack) {
                Image(
                    imageVector = Icons.Outlined.ArrowBackIosNew,
                    contentDescription = stringResource(R.string.my_back_content_description)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 28.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.avatar_container),
                contentDescription = stringResource(R.string.my_avatar_content_description),
                modifier = Modifier.size(100.dp, 100.dp)
            )
            Text(
                text = loginInfo?.user_name ?: stringResource(R.string.common_loading),
                style = SSUType.H3SemiBold,
            )
            Text(
                text = loginInfo?.dept_name ?: "",
                style = SSUType.H4SemiBold,
            )
            TermDropdown(
                terms = terms,
                selectedTerm = selectedTerm ?: currentTerm,
                onTermSelected = viewModel::selectTerm,
            )
        }

        Spacer(Modifier.height(28.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.my_notification_settings),
                    style = SSUType.H5SemiBold,
                    color = N500,
                )

                Spacer(Modifier.width(5.dp))

                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below
                    ),
                    tooltip = {
                        Card(
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 6.dp
                            )
                        ) {
                            NotificationTooltip()
                        }
                    },
                    state = tooltipState
                ) {
                    Icon(
                        modifier = Modifier.clickable {
                            coroutine.launch { tooltipState.show() }
                        },
                        painter = painterResource(R.drawable.ic_alret),
                        tint = N400,
                        contentDescription = stringResource(R.string.my_notification_info_content_description)
                    )
                }
            }
            ToggleOption(
                text = stringResource(R.string.my_system_alert),
                value = alertData.allowSystemAlert,
                onValueChanged = { enabled ->
                    if (enabled) {
                        requestEnableSystemAlert()
                    } else {
                        disableSystemAlert()
                    }
                },
                childOption = null
            )
            ToggleOption(
                text = stringResource(R.string.my_call_alert),
                value = alertData.allowCallAlert,
                onValueChanged = { enabled ->
                    if (enabled) {
                        requestEnableCallAlert()
                    } else {
                        disableCallAlert()
                    }
                },
                childOption = {
                    ComboOption(
                        text = stringResource(R.string.my_time),
                        value = stringResource(
                            R.string.my_hours_before,
                            (alertData.callingAlertThresholdMinutes / 60).toInt(),
                        )
                    ) { hours ->
                        val thresholdMinutes = hours * 60L
                        if (alertData.allowCallAlert) {
                            applyCallAlertSetting(
                                enabled = true,
                                thresholdMinutes = thresholdMinutes,
                            )
                        } else {
                            requestEnableCallAlert(thresholdMinutes)
                        }
                    }
                }
            )

            OptionButton(
                text = stringResource(R.string.my_contact)
            ) {
                viewModel.openKakaoTalkQA(context)
            }

            OptionButton(
                text = stringResource(R.string.my_terms)
            ) {
                viewModel.openURL(context, "https://chlwhdtn03.github.io/ssutime/term.html")
            }

            OptionButton(
                text = stringResource(R.string.my_privacy_policy)
            ) {
                viewModel.openURL(context, "https://chlwhdtn03.github.io/ssutime/privacy.html")
            }

        }

        Spacer(Modifier.height(28.dp))

        OptionButton(
            text = stringResource(R.string.my_logout)
        ) {
            Analytics.logoutClick()
            showLogoutPopup = true
        }

        Spacer(Modifier.height(12.dp))

//        OptionButton(
//            text = "디버그: 10초 후 전화알림"
//        ) {
//            viewModel.sendDebugCallAlertAfterDelay(context)
//        }

//        OptionButton(
//            text = "디버그: 푸시알림"
//        ) {
//            context.showDebugNotification()
//        }
    }
}

@Composable
private fun TermDropdown(
    terms: List<Term>,
    selectedTerm: Term?,
    onTermSelected: (Term) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 0.5.dp,
                    shape = RoundedCornerShape(8.dp),
                    color = N300,
                )
                .background(WHITE)
                .clickable(enabled = terms.isNotEmpty()) {
                    expanded = true
                }
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedTerm?.name.orEmpty().ifBlank {
                    stringResource(R.string.my_no_term_info)
                },
                style = SSUType.Caption1SemiBold,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Outlined.KeyboardArrowDown,
                contentDescription = stringResource(R.string.my_term_dropdown_content_description),
                tint = N400,
            )
        }

        DropdownMenu(
            containerColor = WHITE,
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            terms.forEach { term ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = term.name.orEmpty(),
                            style = SSUType.Label3Medium,
                            color = BLACK,
                        )
                    },
                    onClick = {
                        onTermSelected(term)
                        expanded = false
                    },
                )
            }
        }
    }
}

private enum class NotificationSettingsRequest {
    SystemAlert,
    CallAlertPostNotifications,
    CallAlertFullScreenIntent,
}

private fun Context.canPostNotifications(): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED

private fun Context.canUseFullScreenIntent(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return true
    }

    return getSystemService(NotificationManager::class.java).canUseFullScreenIntent()
}

private fun Context.notificationSettingsIntent(): Intent {
    val notificationSettingsIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
    } else {
        null
    }

    return notificationSettingsIntent?.takeIf { it.resolveActivity(packageManager) != null }
        ?: appDetailsSettingsIntent()
}

private fun Context.fullScreenIntentSettingsIntent(): Intent {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        return appDetailsSettingsIntent()
    }

    val packageUri = Uri.parse("package:$packageName")
    val fullScreenIntentSettings = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
        data = packageUri
    }

    return fullScreenIntentSettings.takeIf { it.resolveActivity(packageManager) != null }
        ?: appDetailsSettingsIntent()
}

private fun Context.appDetailsSettingsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.parse("package:$packageName")
    }

@Composable
fun ToggleOption(
    text: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
    childOption: (@Composable () -> Unit)?,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color = N100)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = SSUType.H5SemiBold
            )
            Spacer(Modifier.weight(1f))
            Switch(
                modifier = Modifier.height(0.dp),
                checked = value,
                onCheckedChange = onValueChanged,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = R400
                )
            )
        }
        if(childOption != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                childOption()
            }
        }
    }
}

@Composable
fun ComboOption(
    modifier: Modifier = Modifier,
    text: String,
    value: String,
    onValueChanged: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold
        )

        Spacer(Modifier.weight(1f))

        Box(
            modifier = Modifier
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(width = (0.5).dp, shape = RoundedCornerShape(8.dp), color = N300)
                    .background(WHITE)
                    .clickable {
                        expanded = true
                    }
                    .padding(start = 12.dp, top = 4.dp, bottom = 4.dp, end = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = value,
                    style = SSUType.Label3Medium
                )
                Spacer(
                    modifier = Modifier.weight(1f)
                )
                Box {
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.call_alert_time_dropdown_content_description)
                    )
                }
            }
            DropdownMenu(
                containerColor = WHITE,
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.call_alert_time_1h), style = SSUType.Label3Medium, color = BLACK) },
                    onClick = { onValueChanged(1); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.call_alert_time_2h), style = SSUType.Label3Medium, color = BLACK) },
                    onClick = { onValueChanged(2); expanded = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.call_alert_time_6h), style = SSUType.Label3Medium, color = BLACK) },
                    onClick = { onValueChanged(6); expanded = false }
                )
            }
        }
    }
}

@Composable
fun OptionButton(
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color = N100)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold
        )
    }
}

@Composable
fun PopupButton(
    modifier: Modifier = Modifier,
    text: String,
    color: Color = Color.Unspecified,
    textColor: Color = Color.Unspecified,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .background(color = color)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold,
            color = textColor
        )
    }
}


@Composable
@Preview(showBackground = true)
fun previewToggleOption() {
    ToggleOption(
        "시스템 알림", true, onValueChanged = {},
    ) { }
}

@Composable
@Preview
fun NotificationTooltip() {
    Column(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WHITE)
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.my_notification_tooltip_system_title),
            style = SSUType.Caption1SemiBold
        )
        Text(
            text = stringResource(R.string.my_notification_tooltip_system_desc),
            style = SSUType.Body2Medium
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.my_notification_tooltip_call_title),
            style = SSUType.Caption1SemiBold
        )
        Text(
            text = stringResource(R.string.my_notification_tooltip_call_desc),
            style = SSUType.Body2Medium
        )
    }
}

@Composable
@Preview
fun LogoutPopup(
    onCancel: () -> Unit = {},
    onConfirm: () -> Unit = {}
) {
    Column(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WHITE)
            .padding(top = 18.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = stringResource(R.string.my_logout),
            style = SSUType.H4SemiBold
        )
        Text(
            text = stringResource(R.string.my_logout_message),
            style = SSUType.Body1Medium
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PopupButton(
                modifier = Modifier
                    .weight(1f),
                text = stringResource(R.string.common_cancel),
                color = N200,
                onClick = onCancel
            )
            PopupButton(
                modifier = Modifier
                    .weight(1f),
                text = stringResource(R.string.common_confirm),
                color = R400,
                textColor = WHITE,
                onClick = onConfirm
            )
        }
    }
}
