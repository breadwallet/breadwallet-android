/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/14/20.
 * Copyright (c) 2020 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.settings.nodeselector

import com.spotify.mobius.Next

interface NodeSelectorUpdateSpec {
    fun patch(model: NodeSelector.M, event: NodeSelector.E): Next<NodeSelector.M, NodeSelector.F> = when (event) {
        NodeSelector.E.OnSwitchButtonClicked -> onSwitchButtonClicked(model)
        is NodeSelector.E.OnConnectionStateUpdated -> onConnectionStateUpdated(model, event)
        is NodeSelector.E.OnConnectionInfoLoaded -> onConnectionInfoLoaded(model, event)
        is NodeSelector.E.SetCustomNode -> setCustomNode(model, event)
    }

    fun onSwitchButtonClicked(model: NodeSelector.M): Next<NodeSelector.M, NodeSelector.F>

    fun onConnectionStateUpdated(model: NodeSelector.M, event: NodeSelector.E.OnConnectionStateUpdated): Next<NodeSelector.M, NodeSelector.F>

    fun onConnectionInfoLoaded(model: NodeSelector.M, event: NodeSelector.E.OnConnectionInfoLoaded): Next<NodeSelector.M, NodeSelector.F>

    fun setCustomNode(model: NodeSelector.M, event: NodeSelector.E.SetCustomNode): Next<NodeSelector.M, NodeSelector.F>
}