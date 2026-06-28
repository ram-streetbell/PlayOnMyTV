package com.playonmytv.util

sealed class ResultState<out T> {
    data class Success<T>(val data: T) : ResultState<T>()
    data class Error(val throwable: Throwable) : ResultState<Nothing>()
    data object Loading : ResultState<Nothing>()
}

