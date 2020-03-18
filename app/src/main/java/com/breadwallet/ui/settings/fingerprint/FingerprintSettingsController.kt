/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/25/19.
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
package com.breadwallet.ui.settings.fingerprint

import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.checked
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.settings.fingerprint.FingerprintSettings.E
import com.breadwallet.ui.settings.fingerprint.FingerprintSettings.F
import com.breadwallet.ui.settings.fingerprint.FingerprintSettings.M
import com.spotify.mobius.Connectable
import kotlinx.android.synthetic.main.controller_fingerprint_settings.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import org.kodein.di.direct
import org.kodein.di.erased.instance

class FingerprintSettingsController : BaseMobiusController<M, E, F>() {

    override val layoutId = R.layout.controller_fingerprint_settings
    override val defaultModel = M()
    override val update = FingerprintSettingsUpdate
    override val init = FingerprintSettingsInit
    override val effectHandler =
        CompositeEffectHandler.from<F, E>(
            Connectable { output ->
                FingerprintSettingsHandler(output)
            },
            nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
                when (effect) {
                    F.GoBack -> NavigationEffect.GoBack
                    F.GoToFaq -> NavigationEffect.GoToFaq(BRConstants.FAQ_ENABLE_FINGERPRINT)
                    else -> null
                }
            })
        )

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        modelFlow.map { it.unlockApp }
            .onEach { switch_unlock_app.isChecked = it }
            .launchIn(uiBindScope)

        modelFlow.map { it.sendMoney }
            .onEach { switch_send_money.isChecked = it }
            .launchIn(uiBindScope)

        modelFlow.map { it.sendMoneyEnable }
            .onEach { switch_send_money.isEnabled = it }
            .launchIn(uiBindScope)

        return merge(
            faq_btn.clicks().map { E.OnFaqClicked },
            back_btn.clicks().map { E.OnBackClicked },
            switch_send_money.checked().map { E.OnSendMoneyChanged(it) },
            switch_unlock_app.checked().map { E.OnAppUnlockChanged(it) }
        )
    }
}
