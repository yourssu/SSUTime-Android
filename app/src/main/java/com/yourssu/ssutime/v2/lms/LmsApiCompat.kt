package com.yourssu.ssutime.v2.lms

import io.github.chlwhdtn03.LmsApi
import io.github.chlwhdtn03.data.Lms.Info
import io.github.chlwhdtn03.data.Lms.Subject
import io.github.chlwhdtn03.data.Lms.Term
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.ExperimentalTime

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
): List<Subject> =
    suspendCancellableCoroutine { continuation ->
        LmsApi.getTodoList(
            term = term,
            loadingState = loadingState,
        ) { result ->
            if (!continuation.isActive) return@getTodoList

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
