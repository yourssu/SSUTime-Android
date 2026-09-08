@file:OptIn(kotlin.time.ExperimentalTime::class)

package com.yourssu.ssutime.desktop.screen.my

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.yourssu.ssutime.desktop.core.model.AppProfile
import com.yourssu.ssutime.desktop.ui.resources.Res
import com.yourssu.ssutime.desktop.ui.resources.avatar_container
import com.yourssu.ssutime.desktop.ui.resources.common_cancel
import com.yourssu.ssutime.desktop.ui.resources.common_confirm
import com.yourssu.ssutime.desktop.ui.resources.common_loading
import com.yourssu.ssutime.desktop.ui.resources.ic_alret
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_back
import com.yourssu.ssutime.desktop.ui.resources.icon_collapsed
import com.yourssu.ssutime.desktop.ui.resources.icon_expand
import com.yourssu.ssutime.desktop.ui.resources.my_avatar_content_description
import com.yourssu.ssutime.desktop.ui.resources.my_back_content_description
import com.yourssu.ssutime.desktop.ui.resources.my_contact
import com.yourssu.ssutime.desktop.ui.resources.my_hidden_todos
import com.yourssu.ssutime.desktop.ui.resources.my_logout
import com.yourssu.ssutime.desktop.ui.resources.my_logout_message
import com.yourssu.ssutime.desktop.ui.resources.my_no_term_info
import com.yourssu.ssutime.desktop.ui.resources.my_notification_info_content_description
import com.yourssu.ssutime.desktop.ui.resources.my_notification_settings
import com.yourssu.ssutime.desktop.ui.resources.my_notification_tooltip_desktop_desc
import com.yourssu.ssutime.desktop.ui.resources.my_notification_tooltip_system_title
import com.yourssu.ssutime.desktop.ui.resources.my_privacy_policy
import com.yourssu.ssutime.desktop.ui.resources.my_settings
import com.yourssu.ssutime.desktop.ui.resources.my_system_alert
import com.yourssu.ssutime.desktop.ui.resources.my_terms
import com.yourssu.ssutime.desktop.ui.theme.BLACK
import com.yourssu.ssutime.desktop.ui.theme.N100
import com.yourssu.ssutime.desktop.ui.theme.N200
import com.yourssu.ssutime.desktop.ui.theme.N300
import com.yourssu.ssutime.desktop.ui.theme.N400
import com.yourssu.ssutime.desktop.ui.theme.N500
import androidx.compose.ui.text.style.TextDecoration
import com.yourssu.ssutime.desktop.ui.resources.cyber_connect_title
import com.yourssu.ssutime.desktop.ui.resources.cyber_connected
import com.yourssu.ssutime.desktop.ui.resources.cyber_disconnect
import com.yourssu.ssutime.desktop.ui.resources.cyber_title
import com.yourssu.ssutime.desktop.ui.resources.ic_arrow_right
import com.yourssu.ssutime.desktop.ui.resources.my_labs_enable_submitted_file
import com.yourssu.ssutime.desktop.ui.resources.my_labs_title
import com.yourssu.ssutime.desktop.ui.resources.my_labs_tooltip_desc
import com.yourssu.ssutime.desktop.ui.resources.my_labs_tooltip_title
import com.yourssu.ssutime.desktop.ui.theme.R100
import com.yourssu.ssutime.desktop.ui.theme.R400
import com.yourssu.ssutime.desktop.ui.theme.SSUType
import com.yourssu.ssutime.desktop.ui.theme.WHITE
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

const val CONTACT_URL = "https://open.kakao.com/o/gFKdBhxi"
const val TERMS_URL = "https://chlwhdtn03.github.io/ssutime/term.html"
const val PRIVACY_URL = "https://chlwhdtn03.github.io/ssutime/privacy.html"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopMyPageScreen(
    profile: AppProfile?,
    isLoading: Boolean,
    errorMessage: String?,
    terms: List<Term> = emptyList(),
    selectedTerm: Term? = null,
    onTermSelected: (Term) -> Unit = {},
    onNavigateToHiddenTodos: () -> Unit = {},
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    showSystemNotificationSetting: Boolean = false,
    systemNotificationsEnabled: Boolean = false,
    onSystemNotificationsChanged: (Boolean) -> Unit = {},
    isCyberConnected: Boolean = false,
    cyberUserId: String = "",
    onNavigateToCyberLogin: () -> Unit = {},
    onDisconnectCyber: () -> Unit = {},
    isEnableSubmittedFile: Boolean = false,
    onEnableSubmittedFileChanged: (Boolean) -> Unit = {},
) {
    var showLogoutDialog by remember { mutableStateOf(false) }
    val tooltipState = rememberTooltipState(isPersistent = true)
    val labsTooltipState = rememberTooltipState(isPersistent = true)
    val scope = rememberCoroutineScope()

    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            LogoutPopup(
                onCancel = { showLogoutDialog = false },
                onConfirm = {
                    showLogoutDialog = false
                    onLogout()
                },
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WHITE)
            .padding(horizontal = 24.dp)
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(modifier = Modifier.padding(top = 8.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(Res.drawable.ic_arrow_back),
                    contentDescription = stringResource(
                        Res.string.my_back_content_description,
                    ),
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(Res.drawable.avatar_container),
                contentDescription = stringResource(
                    Res.string.my_avatar_content_description,
                ),
                modifier = Modifier.size(100.dp),
            )
            Text(
                text = profile?.name
                    ?: errorMessage?.takeIf(String::isNotBlank)
                    ?: stringResource(Res.string.common_loading),
                style = SSUType.H3SemiBold,
                color = if (profile == null && !errorMessage.isNullOrBlank()) {
                    R400
                } else {
                    Color.Unspecified
                },
                textAlign = TextAlign.Center,
            )
            Text(
                text = profile?.department.orEmpty(),
                style = SSUType.H4SemiBold,
            )

            if (terms.isNotEmpty()) {
                TermDropdown(
                    terms = terms,
                    selectedTerm = selectedTerm,
                    fallbackTermName = profile?.termName.orEmpty(),
                    onTermSelected = onTermSelected,
                )
            } else {
                Text(
                    text = profile?.termName
                        ?.takeIf(String::isNotBlank)
                        ?: if (isLoading) {
                            ""
                        } else {
                            stringResource(Res.string.my_no_term_info)
                        },
                    style = SSUType.Caption1SemiBold,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (isCyberConnected) {
            SSUCyberAccountConnectedBadge(
                cyberId = cyberUserId,
                onDisconnect = onDisconnectCyber,
            )
        } else {
            SSUCyberAccountHelperBadge(
                onClickBadge = onNavigateToCyberLogin,
            )
        }

        Spacer(Modifier.height(20.dp))

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (showSystemNotificationSetting) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(Res.string.my_notification_settings),
                        style = SSUType.H5SemiBold,
                        color = N500,
                    )
                    Spacer(Modifier.width(5.dp))
                    TooltipBox(
                        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                            TooltipAnchorPosition.Below,
                        ),
                        tooltip = {
                            Card(
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 6.dp,
                                ),
                            ) {
                                NotificationTooltip()
                            }
                        },
                        state = tooltipState,
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_alret),
                            contentDescription = stringResource(
                                Res.string.my_notification_info_content_description,
                            ),
                            tint = N400,
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .clickable {
                                    scope.launch { tooltipState.show() }
                                }
                                .padding(4.dp),
                        )
                    }
                }
                ToggleOption(
                    text = stringResource(Res.string.my_system_alert),
                    value = systemNotificationsEnabled,
                    onValueChanged = onSystemNotificationsChanged,
                )
                Spacer(Modifier.height(4.dp))
            }

            Text(
                text = stringResource(Res.string.my_settings),
                style = SSUType.H5SemiBold,
                color = N500,
            )

            OptionButton(
                text = stringResource(Res.string.my_hidden_todos),
                onClick = onNavigateToHiddenTodos,
            )
            OptionButton(
                text = stringResource(Res.string.my_contact),
                onClick = { onOpenUrl(CONTACT_URL) },
            )
            OptionButton(
                text = stringResource(Res.string.my_terms),
                onClick = { onOpenUrl(TERMS_URL) },
            )
            OptionButton(
                text = stringResource(Res.string.my_privacy_policy),
                onClick = { onOpenUrl(PRIVACY_URL) },
            )

            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.my_labs_title),
                    style = SSUType.H5SemiBold,
                    color = N500,
                )
                Spacer(Modifier.width(5.dp))
                TooltipBox(
                    positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
                        TooltipAnchorPosition.Below,
                    ),
                    tooltip = {
                        Card(
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 6.dp,
                            ),
                        ) {
                            LabsTooltip()
                        }
                    },
                    state = labsTooltipState,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.ic_alret),
                        contentDescription = stringResource(Res.string.my_labs_title),
                        tint = N400,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable {
                                scope.launch { labsTooltipState.show() }
                            }
                            .padding(4.dp),
                    )
                }
            }
            ToggleOption(
                text = stringResource(Res.string.my_labs_enable_submitted_file),
                value = isEnableSubmittedFile,
                onValueChanged = onEnableSubmittedFileChanged,
            )
        }

        Spacer(Modifier.height(28.dp))
        OptionButton(
            text = stringResource(Res.string.my_logout),
            onClick = { showLogoutDialog = true },
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun TermDropdown(
    terms: List<Term>,
    selectedTerm: Term?,
    fallbackTermName: String,
    onTermSelected: (Term) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .border(width = 0.5.dp, shape = RoundedCornerShape(8.dp), color = N300)
                .background(WHITE)
                .clickable(enabled = terms.isNotEmpty()) { expanded = true }
                .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = selectedTerm?.name?.takeIf(String::isNotBlank)
                    ?: fallbackTermName.takeIf(String::isNotBlank)
                    ?: stringResource(Res.string.my_no_term_info),
                style = SSUType.Caption1SemiBold,
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                painter = painterResource(if (expanded) Res.drawable.icon_expand else Res.drawable.icon_collapsed),
                contentDescription = null,
                tint = N400,
                modifier = Modifier.size(16.dp),
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(WHITE),
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

@Composable
private fun ToggleOption(
    text: String,
    value: Boolean,
    onValueChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(N100)
            .clickable { onValueChanged(!value) }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = value,
            onCheckedChange = onValueChanged,
            colors = SwitchDefaults.colors(checkedTrackColor = R400),
        )
    }
}

@Composable
private fun OptionButton(
    text: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(N100)
            .clickable(onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = text, style = SSUType.H5SemiBold)
    }
}

@Composable
private fun NotificationTooltip() {
    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WHITE)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.my_notification_tooltip_system_title),
            style = SSUType.Caption1SemiBold,
        )
        Text(
            text = stringResource(Res.string.my_notification_tooltip_desktop_desc),
            style = SSUType.Body2Medium,
        )
    }
}

@Composable
private fun LogoutPopup(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(WHITE)
            .padding(top = 18.dp, start = 12.dp, end = 12.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.my_logout),
            style = SSUType.H4SemiBold,
        )
        Text(
            text = stringResource(Res.string.my_logout_message),
            style = SSUType.Body1Medium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            PopupButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.common_cancel),
                color = N200,
                onClick = onCancel,
            )
            PopupButton(
                modifier = Modifier.weight(1f),
                text = stringResource(Res.string.common_confirm),
                color = R400,
                textColor = WHITE,
                onClick = onConfirm,
            )
        }
    }
}

@Composable
private fun PopupButton(
    text: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    textColor: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick)
            .padding(14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = SSUType.H5SemiBold,
            color = textColor,
        )
    }
}

@Composable
fun SSUCyberAccountHelperBadge(
    onClickBadge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFFFF0F0))
            .border(1.dp, Color(0xFFFFD2D2), RoundedCornerShape(10.dp))
            .clickable(onClick = onClickBadge)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = stringResource(Res.string.cyber_connect_title),
            style = SSUType.H4SemiBold,
            color = R400,
        )

        Icon(
            painter = painterResource(Res.drawable.ic_arrow_right),
            contentDescription = null,
            tint = N500,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun SSUCyberAccountConnectedBadge(
    cyberId: String,
    onDisconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(Res.string.cyber_title),
                style = SSUType.Label2SemiBold,
                color = BLACK,
            )
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(7.dp))
                    .background(R100)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(Res.string.cyber_connected),
                    style = SSUType.Caption2Medium,
                    color = R400,
                )
            }
        }

        Text(
            text = stringResource(Res.string.cyber_disconnect),
            style = SSUType.Label3Regular,
            textDecoration = TextDecoration.Underline,
            color = N500,
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(onClick = onDisconnect)
                .padding(horizontal = 8.dp, vertical = 6.dp),
        )
    }
}

@Composable
fun LabsTooltip() {
    Column(
        Modifier
            .width(300.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(WHITE)
            .padding(16.dp),
    ) {
        Text(
            text = stringResource(Res.string.my_labs_tooltip_title),
            style = SSUType.Caption1SemiBold,
            color = BLACK,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(Res.string.my_labs_tooltip_desc),
            style = SSUType.Body2Medium,
            color = N500,
        )
    }
}
