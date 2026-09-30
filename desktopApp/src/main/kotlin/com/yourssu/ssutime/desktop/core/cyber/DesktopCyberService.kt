package com.yourssu.ssutime.desktop.core.cyber

import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.desktop.DesktopSessionStore
import io.github.chlwhdtn03.CyberApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DesktopCyberService(
    private val store: DesktopSessionStore,
) {
    private val loginMutex = Mutex()

    suspend fun login(id: String, pw: String): Boolean = withContext(Dispatchers.IO) {
        runCatching { CyberApi.logout() }
        val success = CyberApi.login(id, pw)
        if (!success) runCatching { CyberApi.logout() }
        if (success) {
            val currentState = store.load()
            store.saveCyber(current = currentState, cyberUserId = id, cyberPassword = pw)
        }
        success
    }

    suspend fun logout(): Unit = withContext(Dispatchers.IO) {
        runCatching { CyberApi.logout() }
        val currentState = store.load()
        store.clearCyber(current = currentState)
    }

    suspend fun ensureLoggedIn(force: Boolean = false): Boolean = loginMutex.withLock {
        val currentState = store.load()
        val credentials = store.cyberCredentials(currentState) ?: return false
        if (!force && CyberApi.isLoggined) {
            return true
        }
        return withContext(Dispatchers.IO) {
            runCatching {
                CyberApi.login(credentials.userId, credentials.password)
            }.getOrDefault(false)
        }
    }

    suspend fun fetchCyberTodos(): CyberTodoResult = withContext(Dispatchers.IO) {
        val currentState = store.load()
        if (!currentState.isCyberConnected) {
            return@withContext CyberTodoResult()
        }

        val loggedIn = ensureLoggedIn()
        if (!loggedIn) {
            return@withContext CyberTodoResult()
        }

        try {
            val cyberSubjects = CyberApi.getSubjects()
            val allTodos = mutableListOf<TodoInfo>()
            val allSubmitted = mutableListOf<TodoInfo>()
            val subjectInfos = cyberSubjects.map { CyberTodoMapper.mapToSubjectInfo(it) }

            return@withContext coroutineScope {
                val deferredList = cyberSubjects.map { subject ->
                    val subjectInfo = CyberTodoMapper.mapToSubjectInfo(subject)
                    async {
                        runCatching {
                            val weeks = CyberApi.getWeeklyLectures(subject)
                            val lectures = CyberTodoMapper.mapWeeksToTodos(subject, subjectInfo, weeks)
                            val evaluations = CyberTodoMapper.mapEvaluationsToTodos(subjectInfo, CyberApi.getQuizzesAndAssignments(subject))
                            (lectures.first + evaluations.first) to (lectures.second + evaluations.second)
                        }.getOrElse {
                            if (it is CancellationException) throw it
                            null
                        }
                    }
                }

                val results = deferredList.awaitAll()
                for (result in results) {
                    result?.let { (todos, submitted) ->
                        allTodos.addAll(todos)
                        allSubmitted.addAll(submitted)
                    }
                }
                return@coroutineScope CyberTodoResult(allTodos, allSubmitted, subjectInfos, results.all { it != null })
            }

        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (_: Exception) {
            CyberTodoResult()
        }
    }
}
