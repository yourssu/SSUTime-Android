package com.yourssu.ssutime.v2.lms

import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import io.github.chlwhdtn03.data.Lms.LmsSession
import io.github.chlwhdtn03.data.Lms.Subject
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime

private val lmsLoginMutex = Mutex()

suspend fun loginLms(id: String, password: String): Boolean =
    suspendCancellableCoroutine { continuation ->
        LmsApi.loginLMS(id, password) { result ->
            if (!continuation.isActive) return@loginLMS

            if (result.success) {
                continuation.resume(true)
            } else {
                continuation.resumeWithException(
                    IllegalStateException(result.errorMessage ?: "LMS 로그인에 실패했어요.")
                )
            }
        }
    }

/**
 * LMS 로그인을 단일화하여 동시 호출 시 중복 로그인을 방지하고 세션을 보장합니다.
 * @param force 이미 로그인이 되어 있더라도 강제로 재로그인할지 여부
 */
suspend fun ensureLmsLoggedIn(id: String, password: String, force: Boolean = false): Boolean =
    lmsLoginMutex.withLock {
        if (!force && LmsApi.isLoggined) {
            return true
        }
        return loginLms(id, password)
    }

@OptIn(ExperimentalTime::class)
suspend fun getLmsTerms(): List<Term> =
    suspendCancellableCoroutine { continuation ->
        LmsApi.getTerms { result ->
            if (!continuation.isActive) return@getTerms

            if (result.success) {
                continuation.resume(result.terms)
            } else {
                continuation.resumeWithException(
                    IllegalStateException(result.errorMessage ?: "학기 정보를 불러오지 못했어요.")
                )
            }
        }
    }

@OptIn(ExperimentalTime::class)
suspend fun getLmsTodoList(
    term: Term,
    loadingState: (Float) -> Unit = {},
    postHogDistinctId: String? = null,
): List<Subject> =
    suspendCancellableCoroutine { continuation ->
        LmsApi.getTodoListParallel(
            term = term,
            loadingState = loadingState,
            postHogDistinctId = postHogDistinctId,
        ) { result ->
            if (!continuation.isActive) return@getTodoListParallel

            if (result.success) {
                continuation.resume(result.subjects)
            } else {
                continuation.resumeWithException(
                    IllegalStateException(result.errorMessage ?: "todo 조회에 실패했어요.")
                )
            }
        }
    }

suspend fun getLmsLoginInfo(): Info =
    suspendCancellableCoroutine { continuation ->
        LmsApi.getLoginInfo { result ->
            if (!continuation.isActive) return@getLoginInfo

            val info = result.info
            if (result.success && info != null) {
                continuation.resume(info)
            } else {
                continuation.resumeWithException(
                    IllegalStateException(result.errorMessage ?: "로그인 정보를 불러오지 못했어요.")
                )
            }
        }
    }

suspend fun getLmsCookies(): LmsSession =
    suspendCancellableCoroutine { continuation ->
        LmsApi.getCookies { result ->
            if (!continuation.isActive) return@getCookies

            if (result.success) {
                continuation.resume(result.lmsSession)
            } else {
                continuation.resumeWithException(
                    IllegalStateException(result.errorMessage ?: "LMS 세션 정보를 불러오지 못했어요.")
                )
            }
        }
    }
