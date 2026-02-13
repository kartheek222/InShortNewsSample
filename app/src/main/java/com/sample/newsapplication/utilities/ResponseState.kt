package com.sample.newsapplication.utilities

import java.lang.Exception

sealed class ResponseState<T> {

    class None<T> : ResponseState<T>()

    class Loading<T> : ResponseState<T>()

    data class Success<T>(val data: T) : ResponseState<T>()

    data class Error<T>(val error: T? = null, val exception: Throwable? = null) : ResponseState<T>()
}