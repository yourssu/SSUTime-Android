package com.yourssu.data

interface UiState<out T> {
    object Loading: UiState<Nothing>
    data class Success<out T>(
        val data:T,
    ) : UiState<T>
    data class Failure(
        val error: Throwable,
    ) : UiState<Nothing>
}