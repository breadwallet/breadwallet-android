package com.breadwallet.ui.wallet

import com.spotify.mobius.functions.Consumer

/**
 * Collects events and passes them in order to a new consumer via [dequeueAll].
 */
class QueuedConsumer<V> : Consumer<V> {

    private val queue = arrayListOf<V>()

    override fun accept(value: V) = synchronized<Unit>(queue) {
        queue.add(value)
    }

    fun dequeueAll(target: Consumer<V>) = synchronized(queue) {
        queue.forEach(target::accept)
        queue.clear()
    }
}