package com.yourssu.ssutime.v2.screen.cyber

import android.util.Log
import androidx.datastore.core.DataStore
import com.yourssu.data.CyberLoginData
import com.yourssu.data.TodoInfo
import com.yourssu.data.isCyber
import com.yourssu.ssutime.v2.screen.main.MainRepository

import io.github.chlwhdtn03.CyberApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "CyberRepository"

class CyberRepository(
    private val cyberLoginDataStore: DataStore<CyberLoginData>,
    private val mainRepository: MainRepository,
) {
    val cyberLoginData: Flow<CyberLoginData> = cyberLoginDataStore.data
    private val loginMutex = Mutex()

    suspend fun getLoginData(): CyberLoginData = cyberLoginDataStore.data.first()

    suspend fun login(id: String, pw: String): Boolean = withContext(Dispatchers.IO) {
        val success = CyberApi.login(id, pw)
        if (success) {
            cyberLoginDataStore.updateData {
                CyberLoginData(id = id, pw = pw, isConnected = true)
            }
        }
        success
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            CyberApi.logout()
        }
        cyberLoginDataStore.updateData {
            CyberLoginData()
        }
        // 사이버대학교 투두 및 과목 제거
        mainRepository.updateTodoData { currentData ->
            currentData.copy(
                todos = currentData.todos.filterNot { it.isCyber() },
                submitted = currentData.submitted.filterNot { it.isCyber() },
                subjects = currentData.subjects.filterNot { it.id < 0 },
                hiddenTodos = currentData.hiddenTodos.filterNot { it.isCyber() },
            )
        }
    }

    suspend fun ensureLoggedIn(force: Boolean = false): Boolean = loginMutex.withLock {
        val loginData = getLoginData()
        if (!loginData.hasCredentials) {
            return false
        }
        if (!force && CyberApi.isLoggined) {
            return true
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                CyberApi.login(loginData.id, loginData.pw)
            }.getOrDefault(false)
        }
    }

    /**
     * 사이버대학교 할 일 목록을 불러옵니다.
     * 계정 정보가 입력(연결)되어 있을 때만 호출되며, 그렇지 않으면 빈 결과를 반환합니다.
     */
    suspend fun fetchCyberTodos(): CyberTodoResult = withContext(Dispatchers.IO) {
        val loginData = getLoginData()
        if (!loginData.hasCredentials) {
            Log.i(TAG, "사이버대학교 계정 정보가 없어 조회를 건너뜁니다.")
            return@withContext CyberTodoResult()
        }

        val loggedIn = ensureLoggedIn()
        if (!loggedIn) {
            Log.w(TAG, "사이버대학교 로그인 세션 연결에 실패했습니다.")
            return@withContext CyberTodoResult()
        }

        try {
            val cyberSubjects = CyberApi.getSubjects()
            val allTodos = mutableListOf<TodoInfo>()
            val allSubmitted = mutableListOf<TodoInfo>()
            val subjectInfos = cyberSubjects.map { CyberTodoMapper.mapToSubjectInfo(it) }

            coroutineScope {
                val deferredList = cyberSubjects.map { subject ->
                    val subjectInfo = CyberTodoMapper.mapToSubjectInfo(subject)
                    async {
                        runCatching {
                            val weeks = CyberApi.getWeeklyLectures(subject)
                            CyberTodoMapper.mapWeeksToTodos(subject, subjectInfo, weeks)
                        }.getOrElse { exception ->
                            Log.e(TAG, "과목 주차 조회 실패: ${subject.name}", exception)
                            Pair(emptyList<TodoInfo>(), emptyList<TodoInfo>())
                        }
                    }
                }

                val results = deferredList.awaitAll()
                for ((todos, submitted) in results) {
                    allTodos.addAll(todos)
                    allSubmitted.addAll(submitted)
                }
            }

            CyberTodoResult(
                todos = allTodos,
                submitted = allSubmitted,
                subjects = subjectInfos,
            )
        } catch (e: Exception) {
            Log.e(TAG, "사이버대학교 할 일 조회 중 오류 발생", e)
            CyberTodoResult()
        }
    }

    /**
     * 로그인 직후 사이버대 Todo를 즉시 동기화하여 기존 TodoData에 병합합니다.
     */
    suspend fun syncCyberTodos() {
        val result = fetchCyberTodos()
        if (result.subjects.isEmpty()) return

        mainRepository.updateTodoData { currentData ->
            // 기존 사이버대 투두/과목을 먼저 제거한 후 새로운 데이터로 병합
            val nonCyberTodos = currentData.todos.filterNot { it.isCyber() }
            val nonCyberSubmitted = currentData.submitted.filterNot { it.isCyber() }
            val nonCyberSubjects = currentData.subjects.filterNot { it.id < 0 }

            val mergedTodos = (nonCyberTodos + result.todos)
            val mergedSubmitted = (nonCyberSubmitted + result.submitted)
            val mergedSubjects = (nonCyberSubjects + result.subjects).distinctBy { it.id }

            currentData.copy(
                todos = mergedTodos,
                submitted = mergedSubmitted,
                subjects = mergedSubjects,
            )
        }
    }
}

