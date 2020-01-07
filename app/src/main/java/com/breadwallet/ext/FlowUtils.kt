package com.breadwallet.ext

import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Dispatch each item emitted by this flow to [consumer], launching in [scope]. */
fun <T> Flow<T>.bindConsumerIn(consumer: Consumer<T>, scope: CoroutineScope) =
    onEach { consumer.accept(it) }.launchIn(scope)

fun <T> Flow<T>.throttleFirst(windowDuration: Long): Flow<T> = flow {
    var lastEmissionMs = 0L
    collect { value ->
        val currentMs = System.currentTimeMillis()
        if (currentMs - lastEmissionMs > windowDuration) {
            lastEmissionMs = currentMs
            emit(value)
        }
    }
}
