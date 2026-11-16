package com.yourssu.ssutime.v2.screen.my

import android.content.Context
import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AlertData
import com.yourssu.data.CyberLoginData
import com.yourssu.data.LabsData
import com.yourssu.data.TodoInfo
import com.yourssu.data.UiState
import com.yourssu.ssutime.v2.accessToken
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import com.yourssu.ssutime.v2.component.openExternalUrl
import com.yourssu.ssutime.v2.lms.ensureLmsLoggedIn
import com.yourssu.ssutime.v2.lms.getLmsLoginInfo
import com.yourssu.ssutime.v2.lms.getLmsTerms
import com.yourssu.ssutime.v2.network.ApiRepository
import com.yourssu.ssutime.v2.screen.cyber.CyberRepository
import com.yourssu.ssutime.v2.screen.login.LoginRepository
import com.yourssu.ssutime.v2.screen.main.MainRepository
import com.yourssu.ssutime.v2.screen.main.TermSelectionStore
import com.yourssu.ssutime.v2.screen.main.currentTermAt
import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class MyViewModel(
    private val loginRepository: LoginRepository,
    private val mainRepository: MainRepository,
    private val termSelectionStore: TermSelectionStore,
    private val cyberRepository: CyberRepository,
    private val apiRepository: ApiRepository,
) : ViewModel() {
    var loginInfo = mutableStateOf<Info?>(null)
    var terms = mutableStateOf<List<Term>>(emptyList())
    var currentTerm = mutableStateOf<Term?>(null)
    var isLogout = mutableStateOf(false)
    val selectedTerm = termSelectionStore.selectedTerm

    private val _uiState = MutableStateFlow<UiState<AlertData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AlertData>> = _uiState.asStateFlow()

    val cyberLoginData: StateFlow<CyberLoginData> = cyberRepository.cyberLoginData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CyberLoginData(),
        )

    fun logoutCyber() {
        viewModelScope.launch {
            cyberRepository.logout()
        }
    }

    val hiddenTodos: StateFlow<List<TodoInfo>> = mainRepository.todoData
        .map { it.hiddenTodos }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )


    val labsData: StateFlow<LabsData> = mainRepository.labsData
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LabsData(),
        )

    fun updateLabsData(labsData: LabsData) {
        viewModelScope.launch {
            mainRepository.updateLabsData(labsData)
        }
    }

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
                    fetchAndApplyLmsData()
                    true
                } else {
                    false
                }
            }.getOrDefault(false)

            // 세션 만료 등으로 실패했고 자동 로그인 정보가 있는 경우 강제 재로그인 후 1회 재시도
            if (!loadSuccess && loginData.hasAutoLoginCredentials) {
                runCatching {
                    if (ensureLmsLoggedIn(loginData.id, loginData.pw, force = true)) {
                        fetchAndApplyLmsData()
                    }
                }.onFailure { exception ->
                    SentryExceptionReporter.capture(exception)
                    Log.e(TAG, "마이페이지 LMS 정보 로드 재시도 실패", exception)
                }
            }
        }
    }

    private suspend fun fetchAndApplyLmsData() {
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
        Analytics.applicationScope.launch {
            termSelectionStore.clear()
            cyberRepository.logout()
            withTimeoutOrNull(3_000L) {
                suspendCancellableCoroutine { continuation ->
                    LmsApi.logout {
                        if (continuation.isActive) {
                            continuation.resume(Unit)
                        }
                    }
                }
            }
            mainRepository.clearTodoData()
            mainRepository.clearLabsData()
            loginRepository.logout()
            accessToken = ""
            Analytics.resetUser()
            Analytics.flush()
            withContext(Dispatchers.Main) {
                isLogout.value = true
            }
        }
    }

    private var isWithdrawing = false

    fun withdrawAccount(reason: String = "string") {
        Analytics.applicationScope.launch {
            if (isWithdrawing) return@launch
            isWithdrawing = true
            runCatching {
                apiRepository.requestAccountWithdrawal(reason)
            }.onSuccess {
                logout()
            }.onFailure { exception ->
                isWithdrawing = false
                SentryExceptionReporter.capture(exception)
                Log.e(TAG, "계정 탈퇴 요청 실패", exception)
            }
        }
    }

    fun openKakaoTalkQA(context: Context) {
        Analytics.kakaoClick()
        openExternalLink(context, KAKAO_TALK_QA_URL, "문의하기 링크를 열지 못했습니다.")
    }

    fun openURL(context: Context, url: String) {
        openExternalLink(context, url, "링크를 열지 못했습니다.")
    }

    private fun openExternalLink(context: Context, url: String, failureMessage: String) {
        context.openExternalUrl(
            url = url,
            showNoAppToast = false,
            onFailure = { exception ->
                SentryExceptionReporter.capture(exception)
                Log.e(TAG, failureMessage, exception)
            },
        )
    }

    private companion object {
        const val TAG = "MyViewModel"
        const val KAKAO_TALK_QA_URL = "https://open.kakao.com/o/gFKdBhxi"
    }
}
