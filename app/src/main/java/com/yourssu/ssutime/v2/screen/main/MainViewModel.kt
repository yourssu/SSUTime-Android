package com.yourssu.ssutime.v2.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.data.AiSummaryCache
import com.yourssu.data.AlertData
import com.yourssu.data.TodoData
import com.yourssu.data.TodoInfo
import com.yourssu.data.TodoType
import com.yourssu.data.network.ReportedTodoResponse
import com.yourssu.data.network.matches
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private val REFRESH_DATE_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
private val SUBMITTED_VISIBLE_WINDOW: Duration = Duration.ofHours(24)
private const val AI_SUMMARY_POLL_ATTEMPTS = 8
private const val AI_SUMMARY_POLL_INTERVAL_MILLIS = 2_000L
private const val TODO_STATUS_PROVISIONAL = "PROVISIONAL"
private const val TODO_STATUS_CONFIRMED = "CONFIRMED"

class MainViewModel(
    private val mainRepository: MainRepository,
    private val lmsRefreshRepository: LmsRefreshRepository,
) : ViewModel() {
    var todos = mutableStateListOf<TodoInfo>()
    var submitted = mutableStateListOf<TodoInfo>()
    var isLoading = mutableStateOf(false)
    var showLoading = mutableStateOf(false)
    var loadingProgress = mutableFloatStateOf(0f)
    var loadedAt = mutableStateOf("")
    var showNetworkError = mutableStateOf(false)
    var showNetworkCause = mutableStateOf("")
    var showWidgetBadge = mutableStateOf(false)
    var onboardingInitialRefreshInProgress = mutableStateOf(false)
    val aiSummaryStates = mutableStateMapOf<String, AiSummaryUiState>()

    var requiredShowAlertBottomSheet = mutableStateOf(false)
    private var handledHomeEntryVersion: Int? = null

    var timetableState = mutableStateOf<TimetableUiState>(TimetableUiState.Loading)
        private set
    var scholarshipState = mutableStateOf<ScholarshipUiState>(ScholarshipUiState.Loading)
        private set
    var tuitionState = mutableStateOf<TuitionUiState>(TuitionUiState.Loading)
        private set
    var graduateState = mutableStateOf<GraduateUiState>(GraduateUiState.Loading)
        private set

    private var isTimetableLoading = false
    private var isScholarshipLoading = false
    private var isTuitionLoading = false
    private var isGraduateLoading = false

    init {
        viewModelScope.launch {
            val alertData = mainRepository.getAlertData()
            requiredShowAlertBottomSheet.value = !alertData.valid
            showWidgetBadge.value = alertData.showWidgetHelperBadge
        }

        viewModelScope.launch {
            mainRepository.todoData.collect { todoData ->
                updateTodoState(todoData)
            }
        }

        viewModelScope.launch {
            mainRepository.timetableData.collect { localTimetable ->
                if (localTimetable.items.isNotEmpty()) {
                    timetableState.value = TimetableUiState.Success(localTimetable.toDomain())
                } else {
                    if (timetableState.value !is TimetableUiState.Loading) {
                        timetableState.value = TimetableUiState.Empty
                    }
                }
            }
        }

        viewModelScope.launch {
            mainRepository.scholarshipData.collect { localData ->
                if (localData.items.isNotEmpty()) {
                    scholarshipState.value = ScholarshipUiState.Success(localData.toDomain())
                } else {
                    if (scholarshipState.value !is ScholarshipUiState.Loading) {
                        scholarshipState.value = ScholarshipUiState.Empty
                    }
                }
            }
        }

        viewModelScope.launch {
            mainRepository.tuitionData.collect { localData ->
                if (localData.items.isNotEmpty()) {
                    tuitionState.value = TuitionUiState.Success(localData.toDomain())
                } else {
                    if (tuitionState.value !is TuitionUiState.Loading) {
                        tuitionState.value = TuitionUiState.Empty
                    }
                }
            }
        }

        viewModelScope.launch {
            mainRepository.graduateData.collect { localData ->
                if (localData.items.isNotEmpty()) {
                    graduateState.value = GraduateUiState.Success(localData.toDomain())
                } else {
                    if (graduateState.value !is GraduateUiState.Loading) {
                        graduateState.value = GraduateUiState.Empty
                    }
                }
            }
        }

        viewModelScope.launch {
            lmsRefreshRepository.onboardingInitialRefreshInProgress.collect { isRefreshing ->
                onboardingInitialRefreshInProgress.value = isRefreshing
            }
        }
    }

    fun shouldRunInitialLoad(homeEntryVersion: Int): Boolean {
        if (handledHomeEntryVersion == homeEntryVersion) {
            return false
        }

        handledHomeEntryVersion = homeEntryVersion
        return true
    }

    fun updateAlertState(alertData: AlertData) {
        viewModelScope.launch {
            mainRepository.updateAlertData(alertData)
        }
        requiredShowAlertBottomSheet.value = false
    }

    fun showNetworkErrorScreen() {
        showNetworkError.value = true
    }

    fun dismissWidgetHelperBadge() {
        showWidgetBadge.value = false
        viewModelScope.launch {
            mainRepository.dismissWidgetHelperBadge()
        }
    }

    fun loadAiSummary(todo: TodoInfo) {
        if (!todo.canRequestAiSummary()) {
            return
        }

        val key = todo.aiSummaryKey()
        when (aiSummaryStates[key]) {
            AiSummaryUiState.Loading,
            AiSummaryUiState.Analyzing -> return
            is AiSummaryUiState.Success,
            AiSummaryUiState.Empty,
            AiSummaryUiState.Error,
            null -> Unit
        }

        viewModelScope.launch {
            val fallbackSuccess = (aiSummaryStates[key] as? AiSummaryUiState.Success)
                ?: mainRepository.getCachedAiSummary(key)?.toAiSummarySuccessOrNull()

            if (fallbackSuccess != null) {
                aiSummaryStates[key] = fallbackSuccess
            } else {
                aiSummaryStates[key] = AiSummaryUiState.Loading
            }

            runCatching {
                requestAiSummary(todo)
            }.onSuccess {
                pollAndCacheAiSummary(todo, key, fallbackSuccess)
                return@launch
            }.onFailure { exception ->
                Log.e(javaClass.name, "AI 요약 요청에 실패했습니다: ${todo.title}", exception)
                if (fallbackSuccess != null) {
                    return@launch
                }
            }

            val reportedTodoResult = runCatching {
                findReportedTodo(todo)
            }
            val reportedTodo = reportedTodoResult.getOrNull()
            reportedTodo?.toAiSummarySuccessOrNull()?.let { success ->
                cacheAndShowAiSummary(key, success)
                return@launch
            }

            if (reportedTodoResult.isFailure) {
                aiSummaryStates[key] = AiSummaryUiState.Error
                return@launch
            }

            if (reportedTodo?.isProvisional == true) {
                pollAndCacheAiSummary(todo, key)
                return@launch
            }

            if (reportedTodo?.isConfirmed == true) {
                aiSummaryStates[key] = AiSummaryUiState.Empty
                return@launch
            }

            aiSummaryStates[key] = AiSummaryUiState.Error
        }
    }

    suspend fun loadTodos(
        forceRefresh: Boolean = false,
        forceLogin: Boolean = false,
        allowRefresh: Boolean = true,
        showBlockingLoading: Boolean = true,
        source: RefreshSource = RefreshSource.APP_START,
    ): TodoData? {
        if(isLoading.value) {
            return null
        }

        isLoading.value = true
        loadingProgress.value = 0f
        showNetworkError.value = false

        return try {
            val cachedTodoData = mainRepository.getTodoData()
            val hasCachedTodoData = cachedTodoData.loadedAt.isNotEmpty()
            if(hasCachedTodoData) {
                updateTodoState(cachedTodoData)
            }

            if (!allowRefresh && hasCachedTodoData) {
                cachedTodoData
            } else if (!forceRefresh && !shouldRefreshOnOpen(cachedTodoData)) {
                cachedTodoData
            } else {
                showLoading.value = showBlockingLoading
                when (val refreshResult = lmsRefreshRepository.refreshTodos(
                    source = source,
                    forceLogin = forceLogin,
                    loadingState = {
                        viewModelScope.launch {
                            loadingProgress.value = it
                        }
                    }
                )) {
                    is TodoRefreshResult.Success -> {
                        loadingProgress.value = 1f
                        showNetworkError.value = false
                        updateTodoState(refreshResult.todoData)
                        refreshResult.todoData
                    }

                    is TodoRefreshResult.Skipped -> {
                        Log.i(javaClass.name, refreshResult.reason)
                        cachedTodoData.takeIf { hasCachedTodoData }
                    }

                    is TodoRefreshResult.Failure -> {
                        Log.e(javaClass.name, refreshResult.message, refreshResult.throwable)
                        showNetworkError.value = true
                        showNetworkCause.value = refreshResult.message
                        null
                    }
                }
            }
        } catch(e: Exception) {
            if(e is CancellationException) throw e
            Log.e(javaClass.name, "과제 정보를 갱신하지 못했습니다.", e)
            showNetworkError.value = true
            showNetworkCause.value = e.localizedMessage ?: "알 수 없는 에러"
            null
        } finally {
            isLoading.value = false
            showLoading.value = false
        }
    }

    private fun updateTodoState(todoData: TodoData) {
        todos.apply {
            clear()
            addAll(todoData.todos)
        }

        submitted.apply {
            clear()
            addAll(todoData.submitted.filterRecentlySubmitted())
        }

        loadedAt.value = todoData.loadedAt

        todoData.aiSummaryCache.forEach { (key, cache) ->
            cache.toAiSummarySuccessOrNull()?.let { success ->
                aiSummaryStates[key] = success
            }
        }
    }

    private suspend fun findReportedTodo(todo: TodoInfo): ReportedTodoResponse? =
        mainRepository.getReportedTodos()
            .firstOrNull { response -> response.todo.matches(todo) }
            ?.todo

    private suspend fun requestAiSummary(todo: TodoInfo) {
        val lmsSession = lmsRefreshRepository.getLmsSessionRequest()
        mainRepository.reportTodoWithAnalysis(
            todo = todo,
            lmsSession = lmsSession,
        )
    }

    private suspend fun pollAndCacheAiSummary(
        todo: TodoInfo,
        key: String,
        fallbackSuccess: AiSummaryUiState.Success? = null,
    ) {
        if (fallbackSuccess == null) {
            aiSummaryStates[key] = AiSummaryUiState.Analyzing
        }

        repeat(AI_SUMMARY_POLL_ATTEMPTS) { attempt ->
            val success = runCatching {
                findReportedTodo(todo)?.toAiSummarySuccessOrNull()
            }.getOrNull()

            if (success != null) {
                cacheAndShowAiSummary(key, success)
                return
            }

            if (attempt < AI_SUMMARY_POLL_ATTEMPTS - 1) {
                delay(AI_SUMMARY_POLL_INTERVAL_MILLIS)
            }
        }

        aiSummaryStates[key] = fallbackSuccess ?: AiSummaryUiState.Empty
    }

    private suspend fun cacheAndShowAiSummary(
        key: String,
        success: AiSummaryUiState.Success,
    ) {
        mainRepository.cacheAiSummary(
            key = key,
            summary = success.summary,
            estimatedDurationMinutes = success.estimatedDurationMinutes,
        )
        aiSummaryStates[key] = success
    }

    private fun shouldRefreshOnOpen(todoData: TodoData): Boolean {
        if (todoData.submitted.any { it.submittedAt.isBlank() && it.todoId != -1 }) {
            return true
        }

        val loadedDate = todoData.loadedAt.toLocalDateOrNull()
            ?: return true
        val today = LocalDate.now(REFRESH_DATE_ZONE_ID)
        return loadedDate.isBefore(today)
    }

    private fun String.toLocalDateOrNull(): LocalDate? = runCatching {
        Instant.parse(this)
            .atZone(REFRESH_DATE_ZONE_ID)
            .toLocalDate()
    }.getOrNull()

    private fun Throwable.isNetworkOrAuthError(): Boolean {
        val name = this.javaClass.name
        val msg = this.message.orEmpty()
        return this is java.io.IOException ||
               name.contains("Connect") ||
               name.contains("Timeout") ||
               name.contains("Host") ||
               name.contains("Http") ||
               msg.contains("로그인") ||
               msg.contains("세션") ||
               msg.contains("인증") ||
               msg.contains("Unauthorized")
    }


    fun loadTimetable(forceRefresh: Boolean = false) {
        if (isTimetableLoading) return
        viewModelScope.launch {
            isTimetableLoading = true
            val currentState = timetableState.value
            if (currentState !is TimetableUiState.Success) {
                timetableState.value = TimetableUiState.Loading
            }
            try {
                val timetable = lmsRefreshRepository.fetchTimetable()
                if (timetable.items.isEmpty()) {
                    if (currentState is TimetableUiState.Success) {
                        Log.w(javaClass.name, "새로 불러온 시간표가 비어있어 기존 데이터를 유지합니다.")
                    } else {
                        mainRepository.updateTimetableData(timetable.toLocal())
                        timetableState.value = TimetableUiState.Empty
                    }
                } else {
                    mainRepository.updateTimetableData(timetable.toLocal())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(javaClass.name, "시간표 정보를 갱신하지 못했습니다.", e)
                if (currentState !is TimetableUiState.Success) {
                    if (e.isNetworkOrAuthError()) {
                        timetableState.value = TimetableUiState.Error(e.localizedMessage ?: "시간표를 불러오지 못했어요.")
                    } else {
                        timetableState.value = TimetableUiState.Empty
                    }
                }
            } finally {
                isTimetableLoading = false
            }
        }
    }


    fun loadScholarship(forceRefresh: Boolean = false) {
        if (isScholarshipLoading) return
        viewModelScope.launch {
            isScholarshipLoading = true
            val currentState = scholarshipState.value
            if (currentState !is ScholarshipUiState.Success) {
                scholarshipState.value = ScholarshipUiState.Loading
            }
            try {
                val table = lmsRefreshRepository.fetchScholarshipTable()
                if (table.items.isEmpty()) {
                    if (currentState is ScholarshipUiState.Success) {
                        Log.w(javaClass.name, "새로 불러온 장학 내역이 비어있어 기존 데이터를 유지합니다.")
                    } else {
                        mainRepository.updateScholarshipData(table.toLocal())
                        scholarshipState.value = ScholarshipUiState.Empty
                    }
                } else {
                    mainRepository.updateScholarshipData(table.toLocal())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(javaClass.name, "장학 정보를 갱신하지 못했습니다.", e)
                if (currentState !is ScholarshipUiState.Success) {
                    if (e.isNetworkOrAuthError()) {
                        scholarshipState.value = ScholarshipUiState.Error(e.localizedMessage ?: "장학 정보를 불러오지 못했어요.")
                    } else {
                        scholarshipState.value = ScholarshipUiState.Empty
                    }
                }
            } finally {
                isScholarshipLoading = false
            }
        }
    }

    fun loadTuition(forceRefresh: Boolean = false) {
        if (isTuitionLoading) return
        viewModelScope.launch {
            isTuitionLoading = true
            val currentState = tuitionState.value
            if (currentState !is TuitionUiState.Success) {
                tuitionState.value = TuitionUiState.Loading
            }
            try {
                val table = lmsRefreshRepository.fetchTuitionTable()
                if (table.items.isEmpty()) {
                    if (currentState is TuitionUiState.Success) {
                        Log.w(javaClass.name, "새로 불러온 등록금 내역이 비어있어 기존 데이터를 유지합니다.")
                    } else {
                        mainRepository.updateTuitionData(table.toLocal())
                        tuitionState.value = TuitionUiState.Empty
                    }
                } else {
                    mainRepository.updateTuitionData(table.toLocal())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(javaClass.name, "등록금 정보를 갱신하지 못했습니다.", e)
                if (currentState !is TuitionUiState.Success) {
                    if (e.isNetworkOrAuthError()) {
                        tuitionState.value = TuitionUiState.Error(e.localizedMessage ?: "등록금 정보를 불러오지 못했어요.")
                    } else {
                        tuitionState.value = TuitionUiState.Empty
                    }
                }
            } finally {
                isTuitionLoading = false
            }
        }
    }

    fun loadGraduate(forceRefresh: Boolean = false) {
        if (isGraduateLoading) return
        viewModelScope.launch {
            isGraduateLoading = true
            val currentState = graduateState.value
            if (currentState !is GraduateUiState.Success) {
                graduateState.value = GraduateUiState.Loading
            }
            try {
                val table = lmsRefreshRepository.fetchGraduateTable()
                if (table.items.isEmpty()) {
                    if (currentState is GraduateUiState.Success) {
                        Log.w(javaClass.name, "새로 불러온 졸업 사정 내역이 비어있어 기존 데이터를 유지합니다.")
                    } else {
                        mainRepository.updateGraduateData(table.toLocal())
                        graduateState.value = GraduateUiState.Empty
                    }
                } else {
                    mainRepository.updateGraduateData(table.toLocal())
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(javaClass.name, "졸업 사정 정보를 갱신하지 못했습니다.", e)
                if (currentState !is GraduateUiState.Success) {
                    if (e.isNetworkOrAuthError()) {
                        graduateState.value = GraduateUiState.Error(e.localizedMessage ?: "졸업 사정 정보를 불러오지 못했어요.")
                    } else {
                        graduateState.value = GraduateUiState.Empty
                    }
                }
            } finally {
                isGraduateLoading = false
            }
        }
    }

    fun loadLargeScreenData(forceRefresh: Boolean = false) {
        loadTimetable(forceRefresh)
        loadScholarship(forceRefresh)
        loadTuition(forceRefresh)
        loadGraduate(forceRefresh)
    }

}

sealed interface TimetableUiState {
    data object Loading : TimetableUiState
    data class Success(val timetable: io.github.chlwhdtn03.data.Lms.Timetable) : TimetableUiState
    data class Error(val message: String) : TimetableUiState
    data object Empty : TimetableUiState
}

sealed interface ScholarshipUiState {
    data object Loading : ScholarshipUiState
    data class Success(val table: io.github.chlwhdtn03.data.Lms.ScholarshipHistoryTable) : ScholarshipUiState
    data class Error(val message: String) : ScholarshipUiState
    data object Empty : ScholarshipUiState
}

sealed interface TuitionUiState {
    data object Loading : TuitionUiState
    data class Success(val table: io.github.chlwhdtn03.data.Lms.TuitionTable) : TuitionUiState
    data class Error(val message: String) : TuitionUiState
    data object Empty : TuitionUiState
}

sealed interface GraduateUiState {
    data object Loading : GraduateUiState
    data class Success(val table: io.github.chlwhdtn03.data.Lms.GraduateTable) : GraduateUiState
    data class Error(val message: String) : GraduateUiState
    data object Empty : GraduateUiState
}

sealed interface AiSummaryUiState {
    data object Loading : AiSummaryUiState
    data object Analyzing : AiSummaryUiState
    data object Empty : AiSummaryUiState
    data class Success(
        val summary: String,
        val estimatedDurationMinutes: Int?,
    ) : AiSummaryUiState
    data object Error : AiSummaryUiState
}

fun TodoInfo.aiSummaryKey(): String =
    "${subject?.id ?: subjectId}:$todoId:${type.name}"

fun TodoInfo.canRequestAiSummary(): Boolean =
    type == TodoType.ASSIGNMENT && description.isNotBlank()

private fun ReportedTodoResponse.toAiSummarySuccessOrNull(): AiSummaryUiState.Success? {
    val summary = aiSummary.orEmpty().trim()
        .takeIf { isConfirmed && it.isNotBlank() }
        ?: return null

    return AiSummaryUiState.Success(
        summary = summary,
        estimatedDurationMinutes = estimatedDurationMinutes,
    )
}

private fun AiSummaryCache.toAiSummarySuccessOrNull(): AiSummaryUiState.Success? =
    summary.trim()
        .takeIf { it.isNotBlank() }
        ?.let { summary ->
            AiSummaryUiState.Success(
                summary = summary,
                estimatedDurationMinutes = estimatedDurationMinutes,
            )
        }

private val ReportedTodoResponse.isProvisional: Boolean
    get() = status.equals(TODO_STATUS_PROVISIONAL, ignoreCase = true)

private val ReportedTodoResponse.isConfirmed: Boolean
    get() = status.equals(TODO_STATUS_CONFIRMED, ignoreCase = true)

private fun List<TodoInfo>.filterRecentlySubmitted(
    now: Instant = Instant.now(),
): List<TodoInfo> = filter { todo ->
    if (todo.type != TodoType.SUBMITTED && todo.type != TodoType.SUBMITTED_LATE) {
        return@filter false
    }
    val submittedAt = todo.submittedAt.toInstantOrNull() ?: return@filter false
    Duration.between(submittedAt, now) <= SUBMITTED_VISIBLE_WINDOW
}

private fun String.toInstantOrNull(): Instant? = runCatching {
    Instant.parse(this)
}.getOrNull()
