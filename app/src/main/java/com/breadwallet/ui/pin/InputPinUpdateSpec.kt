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
package com.breadwallet.ui.pin

import com.spotify.mobius.Next

interface InputPinUpdateSpec {
    fun patch(model: InputPin.M, event: InputPin.E): Next<InputPin.M, InputPin.F> = when (event) {
        InputPin.E.OnFaqClicked -> onFaqClicked(model)
        InputPin.E.OnPinLocked -> onPinLocked(model)
        InputPin.E.OnPinSaved -> onPinSaved(model)
        InputPin.E.OnPinSaveFailed -> onPinSaveFailed(model)
        is InputPin.E.OnPinEntered -> onPinEntered(model, event)
        is InputPin.E.OnPinCheck -> onPinCheck(model, event)
    }

    fun onFaqClicked(model: InputPin.M): Next<InputPin.M, InputPin.F>

    fun onPinLocked(model: InputPin.M): Next<InputPin.M, InputPin.F>

    fun onPinSaved(model: InputPin.M): Next<InputPin.M, InputPin.F>

    fun onPinSaveFailed(model: InputPin.M): Next<InputPin.M, InputPin.F>

    fun onPinEntered(model: InputPin.M, event: InputPin.E.OnPinEntered): Next<InputPin.M, InputPin.F>

    fun onPinCheck(model: InputPin.M, event: InputPin.E.OnPinCheck): Next<InputPin.M, InputPin.F>
}