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

import com.breadwallet.databinding.ControllerFingerprintSettingsBinding
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.checked
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.settings.fingerprint.FingerprintSettings.E
import com.breadwallet.ui.settings.fingerprint.FingerprintSettings.F
import com.breadwallet.ui.settings.fingerprint.FingerprintSettings.M
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach

class FingerprintSettingsController : BaseMobiusController<M, E, F>() {

    override val defaultModel = M()
    override val update = FingerprintSettingsUpdate
    override val init = FingerprintSettingsInit
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createFingerprintSettingsHandler()

    private val binding by viewBinding(ControllerFingerprintSettingsBinding::inflate)

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            modelFlow.map { it.unlockApp }
                .onEach { switchUnlockApp.isChecked = it }
                .launchIn(uiBindScope)

            modelFlow.map { it.sendMoney }
                .onEach { switchSendMoney.isChecked = it }
                .launchIn(uiBindScope)

            modelFlow.map { it.sendMoneyEnable }
                .onEach { switchSendMoney.isEnabled = it }
                .launchIn(uiBindScope)

            merge(
                faqBtn.clicks().map { E.OnFaqClicked },
                backBtn.clicks().map { E.OnBackClicked },
                switchSendMoney.checked().map { E.OnSendMoneyChanged(it) },
                switchUnlockApp.checked().map { E.OnAppUnlockChanged(it) }
            )
        }
    }
}
