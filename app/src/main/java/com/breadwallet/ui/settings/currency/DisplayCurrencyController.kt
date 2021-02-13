/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 1/7/20.
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
package com.breadwallet.ui.settings.currency

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.breadwallet.R
import com.breadwallet.databinding.ControllerDisplayCurrencyBinding
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.settings.currency.DisplayCurrency.E
import com.breadwallet.ui.settings.currency.DisplayCurrency.F
import com.breadwallet.ui.settings.currency.DisplayCurrency.M
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance

class DisplayCurrencyController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args) {

    override val defaultModel = M.createDefault()
    override val init = DisplayCurrencyInit
    override val update = CurrencyUpdate
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createDisplayCurrencyHandler(
            checkNotNull(applicationContext),
            direct.instance()
        )

    private val binding by viewBinding(ControllerDisplayCurrencyBinding::inflate)

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            currencyList.layoutManager = LinearLayoutManager(checkNotNull(activity))
            merge(
                backButton.clicks().map { E.OnBackClicked },
                faqButton.clicks().map { E.OnFaqClicked },
                bindCurrencyList(modelFlow)
            )
        }
    }

    private fun bindCurrencyList(modelFlow: Flow<M>) = callbackFlow<E> {
        binding.currencyList.adapter = FiatCurrencyAdapter(
            modelFlow
                .map { model -> model.currencies }
                .distinctUntilChanged(),
            modelFlow
                .map { model -> model.selectedCurrency }
                .distinctUntilChanged(),
            sendChannel = channel
        )
        awaitClose { binding.currencyList.adapter = null }
    }
}
