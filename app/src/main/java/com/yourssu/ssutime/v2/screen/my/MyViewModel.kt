package com.yourssu.ssutime.v2.screen.my

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AlertData
import com.yourssu.data.TodoInfo
import com.yourssu.data.UiState
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.lms.ensureLmsLoggedIn
import com.yourssu.ssutime.v2.lms.getLmsLoginInfo
import com.yourssu.ssutime.v2.lms.getLmsTerms
import com.yourssu.ssutime.v2.notification.showCallAlert
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import com.yourssu.ssutime.v2.screen.main.TermSelectionStore
import com.yourssu.ssutime.v2.screen.main.currentTermAt
import com.yourssu.ssutime.v2.screen.main.sortedForMainDisplay
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MyViewModel(
    private val loginRepository: LoginRepository,
    private val mainRepository: MainRepository,
    private val termSelectionStore: TermSelectionStore,
) : ViewModel() {
    var loginInfo = mutableStateOf<Info?>(null)
    var terms = mutableStateOf<List<Term>>(emptyList())
    var currentTerm = mutableStateOf<Term?>(null)
    var isLogout = mutableStateOf(false)
    val selectedTerm = termSelectionStore.selectedTerm

    private val _uiState = MutableStateFlow<UiState<AlertData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AlertData>> = _uiState.asStateFlow()

    val hiddenTodos: StateFlow<List<TodoInfo>> = mainRepository.todoData
        .map { it.hiddenTodos }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    init {
        viewModelScope.launch {
            _uiState.value = UiState.Success(
                mainRepository.getAlertData()
            )
            loadLmsData()
        }
    }

    fun loadLmsData() {
        viewModelScope.launch {
            val loginData = loginRepository.getLoginData()
            if (loginData.hasAutoLoginCredentials) {
                runCatching {
                    ensureLmsLoggedIn(loginData.id, loginData.pw)
                }
            }

            val loadSuccess = runCatching {
                if (LmsApi.isLoggined) {
                    val info = getLmsLoginInfo()
                    loginInfo.value = info
                    val loadedTerms = getLmsTerms()
                        .sortedWith(
                            compareByDescending<Term> { it.start_at }
                                .thenByDescending { it.id }
                        )
                    terms.value = loadedTerms
                    currentTerm.value = loadedTerms.currentTermAt(now = Clock.System.now())

                    val selectedTermId = selectedTerm.value?.id
                    if (selectedTermId != null && loadedTerms.none { it.id == selectedTermId }) {
                        termSelectionStore.clear()
                    }
                    true
                } else {
                    false
                }
            }.getOrDefault(false)

            // 세션 만료 등으로 실패했고 자동 로그인 정보가 있는 경우 강제 재로그인 후 1회 재시도
            if (!loadSuccess && loginData.hasAutoLoginCredentials) {
                runCatching {
                    if (ensureLmsLoggedIn(loginData.id, loginData.pw, force = true)) {
                        loginInfo.value = getLmsLoginInfo()
                        val loadedTerms = getLmsTerms()
                            .sortedWith(
                                compareByDescending<Term> { it.start_at }
                                    .thenByDescending { it.id }
                            )
                        terms.value = loadedTerms
                        currentTerm.value = loadedTerms.currentTermAt(now = Clock.System.now())

                        val selectedTermId = selectedTerm.value?.id
                        if (selectedTermId != null && loadedTerms.none { it.id == selectedTermId }) {
                            termSelectionStore.clear()
                        }
                    }
                }.onFailure { exception ->
                    SentryExceptionReporter.capture(exception)
                    Log.e(TAG, "마이페이지 LMS 정보 로드 재시도 실패", exception)
                }
            }
        }
    }

    fun selectTerm(term: Term) {
        if (term.id == currentTerm.value?.id) {
            termSelectionStore.clear()
        } else {
            termSelectionStore.select(term)
        }
    }

    fun updateAlertData(alertData: AlertData) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            mainRepository.updateAlertData(
                alertData
            )
            _uiState.value = UiState.Success(mainRepository.getAlertData())
        }
    }

    fun restoreTodo(todo: TodoInfo) {
        viewModelScope.launch {
            mainRepository.restoreTodo(todo)
        }
    }

    fun logout() {
        viewModelScope.launch {
            termSelectionStore.clear()
            mainRepository.clearTodoData()
            loginRepository.logout()
            accessToken = ""
            Analytics.resetUser()
            isLogout.value = true
        }
    }

    fun sendDebugCallAlertAfterDelay(context: Context) {
        val appContext = context.applicationContext
        viewModelScope.launch {
            delay(10_000L)
            val todo = mainRepository.getTodoData().todos.selectDebugCallAlertTodo()
            if (todo == null) {
                Log.i(TAG, "디버그 전화 알림을 표시할 Todo가 없습니다.")
                return@launch
            }

            showCallAlert(appContext, todo)
        }
    }

    private fun List<TodoInfo>.selectDebugCallAlertTodo(): TodoInfo? =
        sortedForMainDisplay().firstOrNull()

    fun openKakaoTalkQA(context: Context) {
        Analytics.kakaoClick()
        val intent = Intent(Intent.ACTION_VIEW, KAKAO_TALK_QA_URL.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
            Log.e(TAG, "문의하기 링크를 열지 못했습니다.", exception)
        }
    }

    fun openURL(context: Context, url: String) {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(intent)
        }.onFailure { exception ->
            SentryExceptionReporter.capture(exception)
            Log.e(TAG, "링크를 열지 못했습니다.", exception)
        }
    }

    private companion object {
        const val TAG = "MyViewModel"
        const val KAKAO_TALK_QA_URL = "https://open.kakao.com/o/gFKdBhxi"
    }
}
