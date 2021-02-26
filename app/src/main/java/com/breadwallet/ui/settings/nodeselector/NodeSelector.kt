/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 11/05/19.
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
package com.breadwallet.ui.settings.nodeselector

import com.breadwallet.crypto.WalletManagerState
import com.breadwallet.ui.ViewEffect
import dev.zacsweers.redacted.annotations.Redacted

object NodeSelector {

    enum class Mode { AUTOMATIC, MANUAL }

    data class M(
        val mode: Mode? = null,
        @Redacted val currentNode: String = "",
        val connected: Boolean = false
    ) {

        companion object {
            fun createDefault() = M()
        }
    }

    sealed class E {
        object OnSwitchButtonClicked : E()

        data class OnConnectionStateUpdated(
            val state: WalletManagerState
        ) : E()

        data class OnConnectionInfoLoaded(
            val mode: Mode,
            @Redacted val node: String = ""
        ) : E()

        data class SetCustomNode(@Redacted val node: String) : E()
    }

    sealed class F {
        object ShowNodeDialog : F(), ViewEffect
        object LoadConnectionInfo : F()
        object SetToAutomatic : F()
        data class SetCustomNode(@Redacted val node: String) : F()
    }
}
