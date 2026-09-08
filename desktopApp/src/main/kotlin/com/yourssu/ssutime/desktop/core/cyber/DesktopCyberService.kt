package com.yourssu.ssutime.desktop.core.cyber

import com.yourssu.data.TodoInfo
import com.yourssu.ssutime.desktop.DesktopSessionStore
import io.github.chlwhdtn03.CyberApi
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
        val success = runCatching { CyberApi.login(id, pw) }.getOrDefault(false)
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

            coroutineScope {
                val deferredList = cyberSubjects.map { subject ->
                    val subjectInfo = CyberTodoMapper.mapToSubjectInfo(subject)
                    async {
                        runCatching {
                            val weeks = CyberApi.getWeeklyLectures(subject)
                            CyberTodoMapper.mapWeeksToTodos(subject, subjectInfo, weeks)
                        }.getOrElse {
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
        } catch (_: Exception) {
            CyberTodoResult()
        }
    }
}
