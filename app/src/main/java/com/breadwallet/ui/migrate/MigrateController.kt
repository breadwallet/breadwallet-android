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
import com.breadwallet.tools.security.BrdUserManager
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.keystore.KeyStoreController
import com.breadwallet.ui.login.LoginController
import com.breadwallet.ui.onboarding.IntroController
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.kodein.di.direct
import org.kodein.di.erased.instance

class MigrateController(
    args: Bundle? = null
) : BaseController(args) {

    override val layoutId: Int = R.layout.controller_login

    private val userManager: BrdUserManager by instance()

    private val mutex = Mutex()

    override fun onAttach(view: View) {
        super.onAttach(view)
        BreadApp.applicationScope.launch(Main) {
            mutex.withLock<Unit> {
                if (userManager.isMigrationRequired()) {
                    try {
                        migrateAccount()
                    } catch (e: UserNotAuthenticatedException) {
                        waitUntilAttached()
                        activity?.finish()
                    }
                } else if (isAttached) {
                    redirect()
                }
            }
        }
    }

    private fun redirect() {
        val target = if (userManager.isInitialized()) {
            LoginController()
        } else {
            IntroController()
        }
        router.replaceTopController(RouterTransaction.with(target))
    }

    private suspend fun migrateAccount() {
        if (userManager.migrateKeystoreData()) {
            val context = (BreadApp.getBreadContext().applicationContext as BreadApp)
            // The one case where we need to invoke this outside of BreadApp, need to set the migrate flag
            context.startWithInitializedWallet(direct.instance(), true)

            waitUntilAttached()
            router.replaceTopController(RouterTransaction.with(LoginController()))
        } else {
            waitUntilAttached()
            router.replaceTopController(RouterTransaction.with(KeyStoreController()))
        }
    }

    private suspend fun waitUntilAttached() {
        while (!isAttached) delay(1_000)
    }
}
