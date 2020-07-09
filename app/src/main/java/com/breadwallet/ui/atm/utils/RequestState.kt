package com.breadwallet.ui.atm.utils

sealed class RequestState {
    open class State : RequestState()
    open class Result : RequestState()

    open class Error(val throwable: Throwable, message: String? = null) : Result()
    open class Success(val result: Any) : Result()

    object LOADING : State()
    object CLEAR : State()

    companion object {
        fun error(throwable: Throwable): RequestState {
            return Error(throwable)
        }

        fun success(result: Any): RequestState {
            return Success(result)
        }
    }
}