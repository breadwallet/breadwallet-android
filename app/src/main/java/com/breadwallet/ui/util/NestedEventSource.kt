package com.breadwallet.ui.util

// TODO: Replace with our own Mobius interfaces
import com.spotify.mobius.EventSource
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer

/**
 * Produces an Event Source that maps events produced from an inner event source
 * to a corresponding outer event that is dispatched.
 */
fun <OuterE, InnerE> nestedEventSource(
        eventSource : EventSource<InnerE>,
        mapEvent : ((InnerE) -> OuterE?)
) : EventSource<OuterE> = object : EventSource<OuterE> {

    override fun subscribe(eventConsumer: Consumer<OuterE>): Disposable {
        val mappedEventConsumer = when (mapEvent) {
            null -> Consumer {}
            else -> Consumer<InnerE> { innerEvent ->
                eventConsumer.accept(mapEvent(innerEvent) ?: return@Consumer)
            }
        }

        return eventSource.subscribe(mappedEventConsumer)
    }
}