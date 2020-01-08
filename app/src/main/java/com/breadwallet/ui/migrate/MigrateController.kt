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

    override val layoutId: Int = R.layout.activity_pin

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
