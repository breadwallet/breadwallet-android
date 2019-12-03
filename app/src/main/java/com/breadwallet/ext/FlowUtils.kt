package com.breadwallet.ext

import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flattenMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.transform

/** Dispatch each item emitted by this flow to [consumer], launching in [scope]. */
fun <T> Flow<T>.bindConsumerIn(consumer: Consumer<T>, scope: CoroutineScope) =
    onEach { consumer.accept(it) }.launchIn(scope)

/** Collect each [Flow] in [flows] and emit all values in the returned [Flow]. */
@Suppress("SpreadOperator")
fun <T> merge(vararg flows: Flow<T>): Flow<T> =
    flowOf(*flows).flattenMerge(concurrency = flows.size)
