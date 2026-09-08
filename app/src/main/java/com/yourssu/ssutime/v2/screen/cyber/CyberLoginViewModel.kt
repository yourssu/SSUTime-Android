package com.yourssu.ssutime.v2.screen.cyber

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourssu.ssutime.v2.analytics.SentryExceptionReporter
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

    fun login(id: String, pw: String, onSuccess: () -> Unit) {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val success = cyberRepository.login(id.trim(), pw.trim())
                if (success) {
                    // 로그인 성공 시 사이버대학교 Todo 목록 즉시 동기화
                    runCatching {
                        cyberRepository.syncCyberTodos()
                    }.onFailure { exception ->
                        Log.e(TAG, "사이버대 Todo 동기화 중 오류 발생", exception)
                    }
                    onSuccess()
                } else {
                    _errorMessage.value = "로그인에 실패했습니다. 학번과 비밀번호를 확인해주세요."
                }
            } catch (e: Exception) {
                SentryExceptionReporter.capture(e)
                Log.e(TAG, "사이버대학교 로그인 중 오류 발생", e)
                _errorMessage.value = e.message?.takeIf { it.isNotBlank() }
                    ?: "로그인 중 오류가 발생했습니다. 다시 시도해주세요."
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }
}
