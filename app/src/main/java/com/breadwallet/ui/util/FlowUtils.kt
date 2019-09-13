package com.breadwallet.ui.util

import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Dispatch each item emitted by this flow to [consumer], launching in [scope]. */
fun <T> Flow<T>.bindConsumerIn(consumer: Consumer<T>, scope: CoroutineScope) =
    onEach { consumer.accept(it) }.launchIn(scope)