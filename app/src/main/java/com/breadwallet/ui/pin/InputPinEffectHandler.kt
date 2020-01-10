/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 9/23/19.
 * Copyright (c) 2019 breadwallet LLC
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

import android.content.Context
import com.breadwallet.tools.security.BRKeyStore
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class InputPinEffectHandler(
    private val output: Consumer<InputPinEvent>,
    private val context: Context,
    private val errorShake: () -> Unit,
    private val showPinFailed: () -> Unit
) : Connection<InputPinEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    override fun accept(effect: InputPinEffect) {
        when (effect) {
            is InputPinEffect.SetupPin -> setupPin(effect)
            is InputPinEffect.ErrorShake -> errorShake()
            is InputPinEffect.CheckIfPinExists -> checkIfPinExists()
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun setupPin(effect: InputPinEffect.SetupPin) {
        if (BRKeyStore.putPinCode(effect.pin, context)) {
            output.accept(InputPinEvent.OnPinSaved)
        } else {
            launch(Dispatchers.Main) { showPinFailed.invoke() }
            output.accept(InputPinEvent.OnPinSaveFailed)
        }
    }

    private fun checkIfPinExists() {
        output.accept(
            InputPinEvent.OnPinCheck(BRKeyStore.getPinCode(context).isNotEmpty())
        )
    }
}
