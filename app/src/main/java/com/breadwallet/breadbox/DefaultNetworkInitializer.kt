/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 04/28/20.
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
package com.breadwallet.breadbox

import com.breadwallet.crypto.Network
import com.breadwallet.crypto.System
import com.breadwallet.crypto.errors.AccountInitializationCantCreateError
import com.breadwallet.crypto.errors.AccountInitializationError
import com.breadwallet.crypto.errors.AccountInitializationMultipleHederaAccountsError
import com.breadwallet.tools.security.BrdUserManager

class DefaultNetworkInitializer(private val userManager: BrdUserManager) : NetworkInitializer {
    override fun isSupported(currencyId: String) = true

    override suspend fun initialize(
        system: System,
        network: Network,
        createIfNeeded: Boolean
    ): NetworkState {
        if (system.accountIsInitialized(system.account, network)) {
            return NetworkState.Initialized
        }

        return try {
            val data = system.accountInitialize(system.account, network, createIfNeeded)
            userManager.updateAccount(data)
            NetworkState.Initialized
        } catch (e: AccountInitializationCantCreateError) {
            NetworkState.ActionNeeded
        } catch (e: AccountInitializationMultipleHederaAccountsError) {
            val account = e.accounts.sortedBy { it.balance.or(0) }[0]
            val data =
                system.accountInitializeUsingHedera(system.account, network, account).orNull()
            if (data == null) {
                NetworkState.Error(message = "Initialization failed using one of multiple Hedera accounts.")
            } else {
                userManager.updateAccount(data)
                NetworkState.Initialized
            }
        } catch (e: AccountInitializationError) {
            NetworkState.Error(e)
        }
    }
}
