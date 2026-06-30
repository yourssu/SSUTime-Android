package com.yourssu.ssutime.v2.screen.main

import android.util.Log
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
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
import kotlinx.coroutines.flow.combine
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

    var gradeSummaryState = mutableStateOf<GradeSummaryUiState>(GradeSummaryUiState.Loading)
        private set
    var gradeDetailState = mutableStateOf<GradeDetailUiState>(GradeDetailUiState.Loading)
        private set

    var gradeSelectedSemesterKey = mutableStateOf("current")
    var gradeCurrentSemesterName = mutableStateOf("이번 학기")
    var gradeThisSemesterYear = mutableStateOf<String?>(null)
    var gradeThisSemesterType = mutableStateOf<io.github.chlwhdtn03.data.Lms.Semester?>(null)

    var isGradeLoading = mutableStateOf(false)
        private set

    var chapelState = mutableStateOf<ChapelUiState>(ChapelUiState.Loading)
        private set

    var chapelSelectedSemesterKey = mutableStateOf("current")
    var chapelCurrentSemesterName = mutableStateOf("이번 학기")
    var chapelThisSemesterYear = mutableStateOf<String?>(null)
    var chapelThisSemesterType = mutableStateOf<io.github.chlwhdtn03.data.Lms.Semester?>(null)

    var isChapelLoading = mutableStateOf(false)
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
            combine(
                mainRepository.gradeData,
                snapshotFlow { gradeSelectedSemesterKey.value },
                snapshotFlow { isGradeLoading.value }
            ) { localGrade, selectedKey, isLoading ->
                Triple(localGrade, selectedKey, isLoading)
            }.collect { (localGrade, selectedKey, isLoading) ->
                // 0. 이번 학기 메타 정보가 캐시되어 있으면 최신 상태 복원
                val cachedYear = localGrade.thisSemesterYear
                val cachedTypeStr = localGrade.thisSemesterType
                if (cachedYear != null && cachedTypeStr != null) {
                    val cachedType = runCatching { io.github.chlwhdtn03.data.Lms.Semester.valueOf(cachedTypeStr) }.getOrNull()
                    if (cachedType != null) {
                        if (gradeThisSemesterYear.value == null || gradeThisSemesterType.value == null) {
                            gradeThisSemesterYear.value = cachedYear
                            gradeThisSemesterType.value = cachedType
                            gradeCurrentSemesterName.value = "$cachedYear ${cachedType.nameKor}"
                        }
                    }
                }

                if (localGrade.summaryItems.isNotEmpty()) {
                    gradeSummaryState.value = GradeSummaryUiState.Success(localGrade.toDomainSummary())
                } else {
                    if (!isLoading) {
                        gradeSummaryState.value = GradeSummaryUiState.Empty
                    }
                }

                val targetKey = if (selectedKey == "current") {
                    val curYear = gradeThisSemesterYear.value ?: cachedYear
                    val curType = gradeThisSemesterType.value?.name ?: cachedTypeStr
                    if (curYear != null && curType != null) "$curYear-$curType" else null
                } else {
                    selectedKey
                }

                val cachedTable = if (targetKey != null) localGrade.details[targetKey] else null
                if (cachedTable != null && cachedTable.items.isNotEmpty()) {
                    gradeDetailState.value = GradeDetailUiState.Success(cachedTable.toDomain())
                } else {
                    if (!isLoading) {
                        gradeDetailState.value = GradeDetailUiState.Empty
                    } else if (gradeDetailState.value is GradeDetailUiState.Success) {
                        gradeDetailState.value = GradeDetailUiState.Loading
                    }
                }
            }
        }

        viewModelScope.launch {
            combine(
                mainRepository.chapelData,
                snapshotFlow { chapelSelectedSemesterKey.value },
                snapshotFlow { isChapelLoading.value }
            ) { localChapel, selectedKey, isLoading ->
                Triple(localChapel, selectedKey, isLoading)
            }.collect { (localChapel, selectedKey, isLoading) ->
                val cachedYear = localChapel.thisSemesterYear
                val cachedTypeStr = localChapel.thisSemesterType
                if (cachedYear != null && cachedTypeStr != null) {
                    val cachedType = runCatching { io.github.chlwhdtn03.data.Lms.Semester.valueOf(cachedTypeStr) }.getOrNull()
                    if (cachedType != null) {
                        if (chapelThisSemesterYear.value == null || chapelThisSemesterType.value == null) {
                            chapelThisSemesterYear.value = cachedYear
                            chapelThisSemesterType.value = cachedType
                            chapelCurrentSemesterName.value = "$cachedYear ${cachedType.nameKor}"
                        }
                    }
                }

                val targetKey = if (selectedKey == "current") {
                    val curYear = chapelThisSemesterYear.value ?: cachedYear
                    val curType = chapelThisSemesterType.value?.name ?: cachedTypeStr
                    if (curYear != null && curType != null) "$curYear-$curType" else null
                } else {
                    selectedKey
                }

                val cachedTable = if (targetKey != null) localChapel.details[targetKey] else null
                if (cachedTable != null) {
                    val domainTable = cachedTable.toDomain()
                    val isEmpty = domainTable.seatStatusTable.items.isEmpty() &&
                            domainTable.attendanceTable.items.isEmpty() &&
                            domainTable.absenceTable.items.isEmpty()
                    if (isEmpty) {
                        chapelState.value = ChapelUiState.Empty
                    } else {
                        chapelState.value = ChapelUiState.Success(domainTable)
                    }
                } else {
                    if (!isLoading) {
                        chapelState.value = ChapelUiState.Empty
                    } else {
                        chapelState.value = ChapelUiState.Loading
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
                val timetable = lmsRefreshRepository.fetchTimetable(forceLogin = forceRefresh)
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
                val table = lmsRefreshRepository.fetchScholarshipTable(forceLogin = forceRefresh)
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
                val table = lmsRefreshRepository.fetchTuitionTable(forceLogin = forceRefresh)
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
                val table = lmsRefreshRepository.fetchGraduateTable(forceLogin = forceRefresh)
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

       fun loadAllGrades(forceRefresh: Boolean = false) {
        if (isGradeLoading.value) return
        isGradeLoading.value = true

        val currentSummaryState = gradeSummaryState.value
        val currentDetailState = gradeDetailState.value

        if (currentSummaryState !is GradeSummaryUiState.Success) {
            gradeSummaryState.value = GradeSummaryUiState.Loading
        }
        if (currentDetailState !is GradeDetailUiState.Success) {
            gradeDetailState.value = GradeDetailUiState.Loading
        }

        viewModelScope.launch {
            val jobs = mutableListOf<kotlinx.coroutines.Job>()
            try {
                // 1. 이번 학기 성적을 먼저 조회하여 최신 학기가 무엇인지 확인
                val currentCache = mainRepository.getGradeData()
                val thisYear = gradeThisSemesterYear.value ?: currentCache.thisSemesterYear
                val thisSem = gradeThisSemesterType.value ?: runCatching {
                    io.github.chlwhdtn03.data.Lms.Semester.valueOf(currentCache.thisSemesterType.orEmpty())
                }.getOrNull()

                val currentTable = if (thisYear != null && thisSem != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        lmsRefreshRepository.fetchGradeTable(thisYear, thisSem, forceLogin = forceRefresh)
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        lmsRefreshRepository.fetchGradeTable(forceLogin = forceRefresh)
                    }
                }

                // 2. 요약 테이블 조회
                val summaryTable = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    lmsRefreshRepository.fetchSemesterGradeSummaryTable(forceLogin = forceRefresh)
                }

                // 3. 요약 및 이번 학기 상세 성적 1차로 데이터스토어 저장 (빠른 화면 노출)
                mainRepository.updateGradeData { latestCache ->
                    val updatedDetails = latestCache.details.toMutableMap().apply {
                        put("${currentTable.year}-${currentTable.semester.name}", currentTable.toLocal())
                    }
                    latestCache.copy(
                        summaryItems = summaryTable.items.map { it.toLocal() },
                        details = updatedDetails,
                        thisSemesterYear = currentTable.year,
                        thisSemesterType = currentTable.semester.name
                    )
                }

                // 이번 학기 상태 값 갱신
                gradeThisSemesterYear.value = currentTable.year
                gradeThisSemesterType.value = currentTable.semester
                val semName = runCatching { currentTable.semester.nameKor }.getOrDefault("")
                if (currentTable.year.isNotBlank() && semName.isNotBlank()) {
                    gradeCurrentSemesterName.value = "${currentTable.year} $semName"
                }

                // 4. 나머지 과거 학기에 대해 병렬 비동기 조회 및 점진적 개별 업데이트
                summaryTable.items.forEach { cell ->
                    val yearKey = cell.year
                    val sem = cell.semester
                    if (yearKey != null && sem != null && (yearKey != currentTable.year || sem != currentTable.semester)) {
                        val job = launch {
                            runCatching {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    lmsRefreshRepository.fetchGradeTable(yearKey, sem, forceLogin = forceRefresh)
                                }
                            }.onSuccess { table ->
                                mainRepository.updateGradeData { latestCache ->
                                    val updatedDetails = latestCache.details.toMutableMap().apply {
                                        put("${table.year}-${table.semester.name}", table.toLocal())
                                    }
                                    latestCache.copy(details = updatedDetails)
                                }
                            }.onFailure { e ->
                                Log.e(javaClass.name, "${yearKey}학년도 ${sem.nameKor} 상세 성적을 백그라운드 로드하지 못했습니다.", e)
                            }
                        }
                        jobs.add(job)
                    }
                }

                // 모든 과거 학기 비동기 조회가 완전히 끝날 때까지 대기
                jobs.forEach { it.join() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(javaClass.name, "최신 성적 데이터를 불러오지 못했습니다.", e)
                if (gradeSummaryState.value !is GradeSummaryUiState.Success) {
                    gradeSummaryState.value = GradeSummaryUiState.Error(e.localizedMessage ?: "성적 조회를 불러오지 못했어요.")
                }
                if (gradeDetailState.value !is GradeDetailUiState.Success) {
                    gradeDetailState.value = GradeDetailUiState.Error(e.localizedMessage ?: "성적 세부 정보를 불러오지 못했어요.")
                }
            } finally {
                isGradeLoading.value = false
            }
        }
    }

    fun loadGradeSummary(forceRefresh: Boolean = false) {
        loadAllGrades(forceRefresh)
    }

    fun loadGradeDetail(year: String?, semester: io.github.chlwhdtn03.data.Lms.Semester?, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentCache = mainRepository.getGradeData()
            val targetYear = year ?: currentCache.thisSemesterYear ?: gradeThisSemesterYear.value
            val targetSem = semester?.name ?: currentCache.thisSemesterType ?: gradeThisSemesterType.value?.name
            val targetKey = if (targetYear != null && targetSem != null) "$targetYear-$targetSem" else null
            val cachedTable = if (targetKey != null) currentCache.details[targetKey] else null

            if (cachedTable != null && cachedTable.items.isNotEmpty()) {
                gradeDetailState.value = GradeDetailUiState.Success(cachedTable.toDomain())
                // 캐시가 존재하더라도, 백그라운드 업데이트를 실행하여 최신 성적 정보를 업데이트합니다.
                loadAllGrades(forceRefresh = forceRefresh)
            } else {
                // 캐시가 전혀 없을 때는 화면에 로딩 인디케이터가 명확히 돌도록 세팅하고 패치합니다.
                gradeDetailState.value = GradeDetailUiState.Loading
                loadAllGrades(forceRefresh = forceRefresh)
            }
        }
    }

    fun loadLargeScreenData(forceRefresh: Boolean = false) {
        loadTimetable(forceRefresh)
        loadScholarship(forceRefresh)
        loadTuition(forceRefresh)
        loadGraduate(forceRefresh)
        loadGradeSummary(forceRefresh)
        loadAllChapel(forceRefresh)
    }

    fun loadChapelDetail(year: String?, semester: io.github.chlwhdtn03.data.Lms.Semester?, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            val currentCache = mainRepository.getChapelData()
            val targetYear = year ?: currentCache.thisSemesterYear ?: chapelThisSemesterYear.value
            val targetSem = semester ?: runCatching {
                io.github.chlwhdtn03.data.Lms.Semester.valueOf(currentCache.thisSemesterType.orEmpty())
            }.getOrNull() ?: chapelThisSemesterType.value

            if (targetSem != null && targetSem != io.github.chlwhdtn03.data.Lms.Semester.FIRST && targetSem != io.github.chlwhdtn03.data.Lms.Semester.SECOND) {
                chapelState.value = ChapelUiState.Empty
                return@launch
            }

            val targetKey = if (targetYear != null && targetSem != null) "$targetYear-${targetSem.name}" else null
            val cachedTable = if (targetKey != null) currentCache.details[targetKey] else null

            if (cachedTable != null) {
                val domainTable = cachedTable.toDomain()
                val isEmpty = domainTable.seatStatusTable.items.isEmpty() &&
                        domainTable.attendanceTable.items.isEmpty() &&
                        domainTable.absenceTable.items.isEmpty()
                if (isEmpty) {
                    chapelState.value = ChapelUiState.Empty
                } else {
                    chapelState.value = ChapelUiState.Success(domainTable)
                }
                // 캐시가 존재하더라도, 백그라운드 업데이트를 실행하여 최신 채플 정보를 동기화합니다.
                loadAllChapel(forceRefresh = forceRefresh)
            } else {
                // 캐시가 전혀 없을 때는 화면에 로딩 인디케이터가 명확히 돌도록 세팅하고 패치합니다.
                chapelState.value = ChapelUiState.Loading
                loadAllChapel(forceRefresh = forceRefresh)
            }
        }
    }

    fun loadAllChapel(forceRefresh: Boolean = false) {
        if (isChapelLoading.value) return
        isChapelLoading.value = true

        val currentChapelState = chapelState.value

        if (currentChapelState !is ChapelUiState.Success) {
            chapelState.value = ChapelUiState.Loading
        }

        viewModelScope.launch {
            val jobs = mutableListOf<kotlinx.coroutines.Job>()
            try {
                val currentCache = mainRepository.getChapelData()
                val thisYear = chapelThisSemesterYear.value ?: currentCache.thisSemesterYear
                val thisSem = chapelThisSemesterType.value ?: runCatching {
                    io.github.chlwhdtn03.data.Lms.Semester.valueOf(currentCache.thisSemesterType.orEmpty())
                }.getOrNull()

                val currentTable = if (thisYear != null && thisSem != null) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        lmsRefreshRepository.fetchChapelTable(thisYear, thisSem, forceLogin = forceRefresh)
                    }
                } else {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        lmsRefreshRepository.fetchChapelTable(forceLogin = forceRefresh)
                    }
                }

                val summaryTable = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    lmsRefreshRepository.fetchSemesterGradeSummaryTable(forceLogin = forceRefresh)
                }

                mainRepository.updateChapelData { latestCache ->
                    val updatedDetails = latestCache.details.toMutableMap().apply {
                        put("${currentTable.year}-${currentTable.semester.name}", currentTable.toLocalTable())
                    }
                    latestCache.copy(
                        details = updatedDetails,
                        thisSemesterYear = currentTable.year,
                        thisSemesterType = currentTable.semester.name
                    )
                }

                chapelThisSemesterYear.value = currentTable.year
                chapelThisSemesterType.value = currentTable.semester
                val semName = runCatching { currentTable.semester.nameKor }.getOrDefault("")
                if (currentTable.year.isNotBlank() && semName.isNotBlank()) {
                    chapelCurrentSemesterName.value = "${currentTable.year} $semName"
                }

                summaryTable.items.forEach { cell ->
                    val yearKey = cell.year
                    val sem = cell.semester
                    val isRegular = sem == io.github.chlwhdtn03.data.Lms.Semester.FIRST || sem == io.github.chlwhdtn03.data.Lms.Semester.SECOND
                    if (yearKey != null && sem != null && isRegular && (yearKey != currentTable.year || sem != currentTable.semester)) {
                        val job = launch {
                            runCatching {
                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    lmsRefreshRepository.fetchChapelTable(yearKey, sem, forceLogin = forceRefresh)
                                }
                            }.onSuccess { table ->
                                mainRepository.updateChapelData { latestCache ->
                                    val updatedDetails = latestCache.details.toMutableMap().apply {
                                        put("${table.year}-${table.semester.name}", table.toLocalTable())
                                    }
                                    latestCache.copy(details = updatedDetails)
                                }
                            }.onFailure { e ->
                                Log.e(javaClass.name, "${yearKey}학년도 ${sem.nameKor} 상세 채플을 백그라운드 로드하지 못했습니다.", e)
                            }
                        }
                        jobs.add(job)
                    }
                }

                jobs.forEach { it.join() }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e(javaClass.name, "최신 채플 데이터를 불러오지 못했습니다.", e)
                if (chapelState.value !is ChapelUiState.Success) {
                    chapelState.value = ChapelUiState.Error(e.localizedMessage ?: "채플 조회를 불러오지 못했어요.")
                }
            } finally {
                isChapelLoading.value = false
            }
        }
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

sealed interface GradeSummaryUiState {
    data object Loading : GradeSummaryUiState
    data class Success(val table: io.github.chlwhdtn03.data.Lms.SemesterGradeSummaryTable) : GradeSummaryUiState
    data class Error(val message: String) : GradeSummaryUiState
    data object Empty : GradeSummaryUiState
}

sealed interface GradeDetailUiState {
    data object Loading : GradeDetailUiState
    data class Success(val table: io.github.chlwhdtn03.data.Lms.GradeTable) : GradeDetailUiState
    data class Error(val message: String) : GradeDetailUiState
    data object Empty : GradeDetailUiState
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

sealed interface ChapelUiState {
    data object Loading : ChapelUiState
    data class Success(val table: io.github.chlwhdtn03.data.Lms.ChapelInformation) : ChapelUiState
    data class Error(val message: String) : ChapelUiState
    data object Empty : ChapelUiState
}
