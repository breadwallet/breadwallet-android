/**
 * BreadWallet
 *
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> 8/1/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.global.eventsource

import android.content.Context
import com.breadwallet.tools.manager.InternetManager
import com.breadwallet.ui.global.event.InternetEvent
import com.breadwallet.ui.util.QueuedConsumer
import com.spotify.mobius.EventSource
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer

class InternetConnectivityEventSource(
        private val context : Context
): InternetManager.ConnectionReceiverListener, EventSource<InternetEvent> {
    var eventConsumer: Consumer<InternetEvent> = QueuedConsumer()
        private set

    override fun subscribe(newConsumer: Consumer<InternetEvent>): Disposable {
        eventConsumer = newConsumer
        InternetManager.registerConnectionReceiver(context, this)

        val isConnected = InternetManager.getInstance().isConnected(context)
        eventConsumer.accept(InternetEvent.OnConnectionUpdated(isConnected))

        return Disposable {
            InternetManager.unregisterConnectionReceiver(context, this)
        }
    }

    override fun onConnectionChanged(isConnected: Boolean) {
        eventConsumer.accept(InternetEvent.OnConnectionUpdated(isConnected))
    }

}
