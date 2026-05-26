package com.yourssu.ssutime.v2.screen.main

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.yourssu.data.AlertData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.component.OutlinedButton
import com.yourssu.ssutime.v2.component.SButton
import com.yourssu.ssutime.v2.component.SCheckBox
import com.yourssu.ssutime.v2.getRemainingDays
import com.yourssu.ssutime.v2.getRemainingTimeText
import com.yourssu.ssutime.v2.getStringDate
import com.yourssu.ssutime.v2.getStringDateWithTime
import com.yourssu.ssutime.v2.getStringSimpleDate
import com.yourssu.ssutime.v2.ui.theme.G100
import com.yourssu.ssutime.v2.ui.theme.G400
import com.yourssu.ssutime.v2.ui.theme.N100
import com.yourssu.ssutime.v2.ui.theme.N300
import com.yourssu.ssutime.v2.ui.theme.R100
import com.yourssu.ssutime.v2.ui.theme.R400
import com.yourssu.ssutime.v2.ui.theme.R500
import com.yourssu.ssutime.v2.ui.theme.SSUType
import com.yourssu.ssutime.v2.ui.theme.WHITE
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel(),
    coroutine: CoroutineScope = rememberCoroutineScope(),
    skipInitialLmsRefresh: Boolean = false,
    onInitialLmsRefreshSkipConsumed: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {
    val context = LocalContext.current
    var showSubmittedBottomSheet by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        viewModel.loadTodos(allowRefresh = !skipInitialLmsRefresh)
        if (skipInitialLmsRefresh) {
            onInitialLmsRefreshSkipConsumed()
        }
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false,
    )


    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding(),
        containerColor = WHITE,
        topBar = {
            SSUTimeTopBar(
                modifier = Modifier.statusBarsPadding(),
                onProfileClick = onProfileClick
            )
        }
    ) { innerPadding ->
        MainFragment(
            innerPadding = innerPadding,
            todos = viewModel.todos,
            submitted = viewModel.submitted,
            loadedAt = viewModel.loadedAt.value,
            onClickRefresh = {
                coroutine.launch {
                    viewModel.loadTodos(forceRefresh = true)
                }
            },
            onClickSubmitted = {
                showSubmittedBottomSheet = true
            }
        )

        if(viewModel.requiredShowAlertBottomSheet.value) {
            CallingAlertBottomSheet(
                onConfirmClick = {
                    val allowSystem = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                    var allowCalling = false
                    if  (it > 0)
                        allowCalling = true

                    viewModel.updateAlertState(
                        AlertData(
                            valid = true,
                            allowSystemAlert = allowSystem,
                            allowCallAlert = allowCalling,
                            callingAlertThresholdMinutes = it
                        )
                    )

                }
            )
        }

        if (showSubmittedBottomSheet) {
            ModalBottomSheet(
                modifier = Modifier
                    .fillMaxWidth(),
                containerColor = WHITE,
                onDismissRequest = { showSubmittedBottomSheet = false },
                sheetState = sheetState

            ) {
                Column(
                    modifier = Modifier
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "제출한 과제",
                            style = SSUType.H3SemiBold
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = viewModel.submitted.size.toString(),
                            style = SSUType.H3SemiBold.copy(color = R400)
                        )
                        Spacer(Modifier.weight(1f))
                        Text( //TODO
                            text = if(viewModel.loadedAt.value.isNotEmpty()) {
                                "${getStringDate(viewModel.loadedAt.value)} 기준"
                            } else {
                                "00월 00일 기준"
                            },
                            style = SSUType.Caption1SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    if(viewModel.submitted.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(
                                items = viewModel.submitted,
                                key = { item -> item.todoId }
                            ) {
                                SubmittedItem(it)
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                modifier = Modifier
                                    .padding(vertical = 50.dp),
                                text = "아직 제출한 과제가 없어요",
                                style = SSUType.H3Medium
                            )
                        }
                    }

                    SButton(
                        modifier = Modifier.fillMaxWidth(),
                        labelText = "닫기",
                        onClick = {
                            coroutine.launch {
                                sheetState.hide()
                                showSubmittedBottomSheet = false
                            }
                        }
                    )

                }
            }
        }

        if(viewModel.showLoading.value)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x80000000)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    LinearProgressIndicator(
                        progress = { viewModel.loadingProgress.value },
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "정보를 불러오는 중이에요... ${(viewModel.loadingProgress.value * 100).toInt()}%",
                        style = SSUType.H2Medium,
                        color = WHITE
                    )
                }
            }

    }
}

@Composable
@Preview
fun MainFragment(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    todos: List<TodoInfo> = emptyList(),
    submitted: List<TodoInfo> = emptyList(),
    loadedAt: String = "",
    onClickRefresh: () -> Unit = {},
    onClickSubmitted: () -> Unit = {},
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 32.dp, horizontal = 16.dp),
        ) {
            Text(
                text = "완료하면 자동으로 사라져요",
                style = SSUType.H4SemiBold
            )

            Text(
                text = "${todos.size}건의 할 일이 있어요",
                style = SSUType.H1SemiBold
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if(loadedAt.isNotEmpty()) {
                        "업데이트 ${
                            getStringSimpleDate(
                                context,
                                loadedAt
                            )
                        } 기준"
                    } else {
                        "업데이트 정보 없음"
                    },
                    style = SSUType.Caption1Medium
                )

                Image(
                    modifier = Modifier
                        .height(13.dp)
                        .clickable { onClickRefresh() },
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "새로고침"
                )
            }

            Spacer(Modifier.height(28.dp))

            TodoList(
                modifier = Modifier
                    .fillMaxWidth(),
                todos = todos,
                onClickSubmitted = onClickSubmitted,
                submittedSize = submitted.size,
            )
        }
    }
}

@Composable
fun TodoList(
    modifier: Modifier = Modifier,
    todos: List<TodoInfo>,
    onClickSubmitted: () -> Unit,
    submittedSize: Int
) {
    val immediateTodos = todos.filter { getRemainingDays(it.due_date) <= 1 }
    val freeTodos = todos.filter { getRemainingDays(it.due_date) > 1 }

    Column (
        modifier = modifier
    ) {

        if (immediateTodos.isNotEmpty()) {
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
                        .clickable { onClickSubmitted() }
                        .padding(8.dp),
                    text = "제출 완료 $submittedSize",
                    style = SSUType.Caption1SemiBold
                )
            }

            immediateTodos.forEach {
                key(it.todoId) {
                    Spacer(Modifier.height(8.dp))
                    TodoItem(todoInfo = it)
                }
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "여유가 있는 할 일 리스트",
                style = SSUType.H5SemiBold
            )

            freeTodos.forEach {
                key(it.todoId) {
                    Spacer(Modifier.height(8.dp))
                    TodoItem(todoInfo = it)
                }
            }
        } else if (freeTodos.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "여유가 있는 할 일 리스트",
                    style = SSUType.H5SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .border(width = 1.dp, color = N300, shape = RoundedCornerShape(8.dp))
                        .clickable { onClickSubmitted() }
                        .padding(8.dp),
                    text = "제출 완료 $submittedSize",
                    style = SSUType.Caption1SemiBold
                )
            }
            freeTodos.forEach {
                key(it.todoId) {
                    Spacer(Modifier.height(8.dp))
                    TodoItem(todoInfo = it)
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "과제 목록",
                    style = SSUType.H3SemiBold
                )
                Spacer(Modifier.weight(1f))
                Text(
                    modifier = Modifier
                        .border(width = 1.dp, color = N300, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .clickable { onClickSubmitted() },
                    text = "제출 완료 $submittedSize",
                    style = SSUType.Caption1SemiBold
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.done),
                    contentDescription = "Done All Assignment"
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "제출할 과제가 없어요",
                    style = SSUType.H3Medium,
                )
            }
        }
    }
}

@Composable
fun TodoItem(
    todoInfo: TodoInfo
) {
    var expanded by remember { mutableStateOf(false) }
    var isLate by remember { mutableStateOf(false) }

    // 1초마다 갱신되는 기준 시간 상태 (시스템 클럭의 000ms에 맞춰 갱신되도록 보정)
    val now by produceState(initialValue = Instant.now()) {
        while (true) {
            value = Instant.now()
            // 다음 1초 정각까지 남은 밀리초만큼 대기 (누적 오차 방지)
            val sleepTime = 1000L - (System.currentTimeMillis() % 1000L)
            delay(sleepTime)
        }
    }

    val leftDay = getRemainingDays(todoInfo.due_date, now)

//    Log.d("리컴포지션", "${todoInfo.todoId} 리컴포지션 발생 (남은시간: ${
//        getRemainingTimeText(
//            todoInfo.due_date,
//            now
//        )
//    })")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(50.dp, 50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(WHITE),
                    contentAlignment = Alignment.Center
                ) {
                    var res by remember { mutableIntStateOf(-1) }
                    res = when(leftDay) {
                        3L -> R.drawable.day3
                        2L -> R.drawable.day2
                        1L -> R.drawable.day_red2
                        0L -> {
                            val txt = getRemainingTimeText(
                                todoInfo.due_date,
                                now
                            )
                            if(txt == "0초") {
                                isLate = true
                                R.drawable.late
                            } else if(txt.contains("초"))
                                R.drawable.seconds
                            else
                                R.drawable.timer
                        }
                        else -> {
                            -1
                        }
                    }

                    if(res > 0)
                        Image(
                            painter = painterResource(res),
                            contentDescription = "day-$res"
                        )

                    if(!isLate) {
                        Text(
                            text = if (leftDay > 0) "D-${leftDay}" else getRemainingTimeText(
                                todoInfo.due_date,
                                now
                            ),
                            style = if (leftDay > 1) SSUType.H4ExtraBold else SSUType.H4ExtraBold.copy(
                                color = WHITE
                            )
                        )
                    }
                }

                Spacer(Modifier.size(12.dp))

                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if(!isLate) {
                            Text(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        when(todoInfo.type) {
                                            TodoType.COMMONS -> Color(0xFFF7DBF7)
                                            TodoType.QUIZ -> Color(0xFFFFD7C2)
                                            TodoType.ASSIGNMENT -> Color(0xFFD8E5F7)
                                            else -> Color(0xFFF7DBF7)
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                text = todoInfo.type.kor,
                                style = SSUType.Caption1SemiBold
                            )
                            Spacer(Modifier.width(6.dp))

                            Text(
                                modifier = Modifier.fillMaxWidth(0.8f),
                                maxLines = 1,
                                text = todoInfo.subject?.name ?: "알 수 없는 과목",
                                style = SSUType.H5SemiBold
                            )
                        } else {
                            Text(
                                text = "지각 제출 가능해요",
                                style = SSUType.H5SemiBold.copy(color = R500)
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        maxLines = 1,
                        text = todoInfo.title,
                        style = SSUType.H4SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))

                Image(
                    modifier = Modifier.clickable {
                        expanded = !expanded
                    },
                    imageVector = if (!expanded) Icons.Outlined.KeyboardArrowDown else Icons.Outlined.KeyboardArrowUp,
                    contentDescription = "과제 정보 확장"
                )
            }

            AnimatedVisibility(expanded) {
                Row(
                    modifier = Modifier
                        .padding(top = 12.dp)
                ) {
                    Text(
                        text = "마감기한",
                        style = SSUType.H5SemiBold
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = getStringDateWithTime(todoInfo.due_date) + "까지",
                        style = SSUType.H5SemiBold
                    )
                }

                // TODO AI 요약
            }
        }
    }
}

@Composable
fun SubmittedItem(
    todoInfo: TodoInfo
) {
//    Log.d("리컴포지션", "${todoInfo.todoId} 리컴포지션 발생")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(N100)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            modifier = Modifier.fillMaxWidth(0.8f),
                            maxLines = 1,
                            text = todoInfo.subject?.name ?: "알 수 없는 과목",
                            style = SSUType.Caption1SemiBold
                        )
                    }

                    Spacer(Modifier.height(4.dp))

                    Text(
                        modifier = Modifier.fillMaxWidth(0.8f),
                        maxLines = 1,
                        text = todoInfo.title,
                        style = SSUType.H5SemiBold
                    )
                }
                Spacer(Modifier.weight(1f))

                Text(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (todoInfo.type == TodoType.SUBMITTED_LATE) R100 else G100)
                        .padding(6.dp),
                    text = todoInfo.type.kor,
                    style = SSUType.Caption2Medium.copy(color = if(todoInfo.type == TodoType.SUBMITTED_LATE) R400 else G400)
                )
            }
        }
    }
}

@Composable
@Preview
fun SSUTimeTopBar(
    modifier: Modifier = Modifier,
    onProfileClick: () -> Unit = {}
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
            modifier = Modifier
                .height(IntrinsicSize.Max)
                .clickable { onProfileClick() }
            ,
            painter = painterResource(R.drawable.ic_user),
            contentDescription = "User"
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallingAlertBottomSheet(
    onConfirmClick: (Long) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
    )

    ModalBottomSheet(
        properties = ModalBottomSheetProperties(
            shouldDismissOnClickOutside = false,
            shouldDismissOnBackPress = false,
        ),
        sheetGesturesEnabled = false,
        dragHandle = null,
        onDismissRequest = {},
        sheetState = sheetState,
        containerColor = WHITE,
    ) {
        CallingAlertBody(
            onConfirmClick = onConfirmClick
        )
    }
}

@Composable
@Preview
fun CallingAlertBody(
    modifier: Modifier = Modifier,
    onConfirmClick: (Long) -> Unit = {}
) {

    val radioOptions = listOf("마감 당일 1시간 전", "마감 당일 2시간 전", "마감 당일 6시간 전")
    val (selectedOption, onOptionSelected) = remember { mutableStateOf("") }

    var enableCallingAlert = remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Column {
            Text(
                text = "전화 알림 설정만 하면 끝이에요!",
                style = SSUType.H2SemiBold
            )
            Text(
                text = "설정한 시간 기준으로 교수님한테 전화 알림을 받을 수 있어요\n(진짜 전화 연결이 되는 것은 아니에요!)",
                style = SSUType.Body1Medium
            )
        }

        Column(
            modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            radioOptions.forEach { text ->
                OutlinedButton(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = (text == selectedOption && !enableCallingAlert.value),
                            onClick = {
                                onOptionSelected(text)
                                enableCallingAlert.value = false
                            },
                            role = Role.RadioButton
                        ),
                    labelText = text,
                    selected = (text == selectedOption && !enableCallingAlert.value)
                )
            }
        }
        Box(
            modifier = modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            SCheckBox(
                labelText = "전화 알림 받지 않기",
                checked = enableCallingAlert,
                onCheckedChanged = {
                    if(it)
                        onOptionSelected("")
                    enableCallingAlert.value = it
                }
            )
        }
        SButton(
            modifier = Modifier.fillMaxWidth(),
            labelText = "확인",
            onClick = {
                onConfirmClick(
                    if(enableCallingAlert.value)
                        -1L
                    else when (radioOptions.indexOf(selectedOption)) {
                        0 -> 60L
                        1 -> 120L
                        else -> 360L
                    }
                )
            },
            enable = enableCallingAlert.value || radioOptions.any { it == selectedOption }
        )
    }
}
