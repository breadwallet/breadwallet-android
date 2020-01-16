/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/7/20.
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
package com.breadwallet.ui.migrate

import android.os.Bundle
import android.security.keystore.UserNotAuthenticatedException
import android.view.View
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.app.BreadApp
import com.breadwallet.crypto.Account
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.security.BRKeyStore
import com.breadwallet.tools.security.KeyStore
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.login.LoginController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.util.Date

class MigrateController(
    args: Bundle? = null
) : BaseController(args) {

    private val keyStore by instance<KeyStore>()

    override val layoutId: Int = R.layout.controller_login

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        controllerScope.launch {
            val phrase = try {
                checkNotNull(keyStore.getPhrase())
            } catch (e: UserNotAuthenticatedException) {
                null
            }
            withContext(Dispatchers.Main) {
                phrase?.apply(::migrateAccount) ?: activity?.finish()
            }
        }
    }

    private fun migrateAccount(phrase: ByteArray) {
        val timestamp = Date(BRKeyStore.getWalletCreationTime(applicationContext).toLong())
        val account = Account.createFromPhrase(phrase, timestamp, BRSharedPrefs.getDeviceId())
        BRKeyStore.putAccount(account, applicationContext)
        BRKeyStore.deleteMasterPublicKey(applicationContext)

        (applicationContext as BreadApp).startWithInitializedWallet(direct.instance(), true)

        BreadApp.initialize()

        router.replaceTopController(RouterTransaction.with(LoginController()))
    }
}
