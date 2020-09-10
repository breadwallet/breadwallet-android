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
package com.breadwallet.ui.writedownkey

import com.spotify.mobius.Next

interface WriteDownKeyUpdateSpec {
    fun patch(model: WriteDownKey.M, event: WriteDownKey.E): Next<WriteDownKey.M, WriteDownKey.F> = when (event) {
        WriteDownKey.E.OnCloseClicked -> onCloseClicked(model)
        WriteDownKey.E.OnFaqClicked -> onFaqClicked(model)
        WriteDownKey.E.OnWriteDownClicked -> onWriteDownClicked(model)
        WriteDownKey.E.OnGetPhraseFailed -> onGetPhraseFailed(model)
        WriteDownKey.E.OnUserAuthenticated -> onUserAuthenticated(model)
        is WriteDownKey.E.OnPhraseRecovered -> onPhraseRecovered(model, event)
    }

    fun onCloseClicked(model: WriteDownKey.M): Next<WriteDownKey.M, WriteDownKey.F>

    fun onFaqClicked(model: WriteDownKey.M): Next<WriteDownKey.M, WriteDownKey.F>

    fun onWriteDownClicked(model: WriteDownKey.M): Next<WriteDownKey.M, WriteDownKey.F>

    fun onGetPhraseFailed(model: WriteDownKey.M): Next<WriteDownKey.M, WriteDownKey.F>

    fun onUserAuthenticated(model: WriteDownKey.M): Next<WriteDownKey.M, WriteDownKey.F>

    fun onPhraseRecovered(model: WriteDownKey.M, event: WriteDownKey.E.OnPhraseRecovered): Next<WriteDownKey.M, WriteDownKey.F>
}