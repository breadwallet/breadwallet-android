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
package com.breadwallet.ui.settings.segwit

import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import dev.zacsweers.redacted.annotations.Redacted

object LegacyAddress {

    data class M(
        /** The network compatible address for transactions. */
        @Redacted val receiveAddress: String = "",
        /** The address without network specific decoration. */
        @Redacted val sanitizedAddress: String = "",
        /** The name of the Wallet's currency. */
        val walletName: String = ""
    )

    sealed class E {
        object OnShareClicked : E()
        object OnAddressClicked : E()
        object OnCloseClicked : E()

        data class OnAddressUpdated(
            @Redacted val receiveAddress: String,
            @Redacted val sanitizedAddress: String
        ) : E()

        data class OnWalletNameUpdated(
            val name: String
        ) : E()
    }

    sealed class F {
        object LoadAddress : F()
        object GoBack : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }

        data class CopyAddressToClipboard(@Redacted val address: String) : F()

        data class ShareAddress(@Redacted val address: String, val walletName: String) : F()
    }
}
