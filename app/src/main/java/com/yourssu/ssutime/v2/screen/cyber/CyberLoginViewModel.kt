package com.yourssu.ssutime.v2.screen.cyber

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.ssutime.v2.R
import com.yourssu.ssutime.v2.analytics.Analytics
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "CyberLoginViewModel"

class CyberLoginViewModel(
    private val cyberRepository: CyberRepository,
) : ViewModel() {

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _errorMessageResId = MutableStateFlow<Int?>(null)
    val errorMessageResId: StateFlow<Int?> = _errorMessageResId.asStateFlow()

    fun login(id: String, pw: String, onSuccess: () -> Unit) {
        if (_isLoading.value) return

        val loginDeferred = Analytics.applicationScope.async {
            try {
                val success = cyberRepository.login(id.trim(), pw.trim())
                if (success) {
                    Analytics.cyberLoginSuccess()
                    // 로그인 성공 시 사이버대학교 Todo 목록 즉시 동기화
                    runCatching {
                        cyberRepository.syncCyberTodos()
                    }.onFailure { exception ->
                        Log.e(TAG, "사이버대 Todo 동기화 중 오류 발생", exception)
                    }
                } else {
                    Analytics.cyberLoginFail()
                }
                Result.success(success)
            } catch (e: Exception) {
                if (e !is CancellationException) {
                    Analytics.cyberLoginFail()
                    SentryExceptionReporter.capture(e)
                    Log.e(TAG, "사이버대학교 로그인 중 오류 발생", e)
                }
                Result.failure(e)
            }
        }

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            _errorMessageResId.value = null

            try {
                val result = loginDeferred.await()
                result.onSuccess { success ->
                    if (success) {
                        onSuccess()
                    } else {
                        _errorMessageResId.value = R.string.cyber_login_error_credentials
                    }
                }.onFailure { e ->
                    val msg = e.message?.takeIf { it.isNotBlank() }
                    if (msg != null) {
                        _errorMessage.value = msg
                    } else {
                        _errorMessageResId.value = R.string.cyber_login_error_general
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
        _errorMessageResId.value = null
    }
}
