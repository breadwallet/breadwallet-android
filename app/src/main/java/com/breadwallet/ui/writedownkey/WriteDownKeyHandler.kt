/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/10/19.
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
package com.breadwallet.ui.writedownkey

import com.breadwallet.tools.security.BRAccountManager
import com.breadwallet.ui.writedownkey.WriteDownKey.E
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class WriteDownKeyHandler(
    private val output: Consumer<E>,
    private val controllerScope: CoroutineScope,
    private val accountManager: BRAccountManager,
    private val showBrdAuthPrompt: () -> Unit
) : Connection<WriteDownKey.F>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    override fun accept(value: WriteDownKey.F) {
        when (value) {
            WriteDownKey.F.GetPhrase -> getPhrase()
            WriteDownKey.F.ShowAuthPrompt -> launch(Dispatchers.Main) { showBrdAuthPrompt() }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun getPhrase() {
        controllerScope.launch {
            val rawPhrase = accountManager.getPhrase()
            if (rawPhrase == null || rawPhrase.isEmpty()) {
                output.accept(E.OnGetPhraseFailed)
            } else {
                val phrase = String(rawPhrase).split(" ")
                output.accept(E.OnPhraseRecovered(phrase))
            }
        }
    }
}
