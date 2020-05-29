package com.breadwallet.tools.security

import java.lang.Exception

sealed class SetupResult {
    object Success : SetupResult()
    object PhraseAlreadyExists : SetupResult()
    data class FailedToGeneratePhrase(val exception: Exception?) : SetupResult()
    object FailedToPersistPhrase : SetupResult()
    object FailedToCreateAccount : SetupResult()
    object FailedToCreateApiKey : SetupResult()
    object FailedToCreateValidWallet : SetupResult()
    data class UnknownFailure(val exception: Exception) : SetupResult()
}