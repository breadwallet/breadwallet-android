/**
 * BreadWallet
 *
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 11/05/19.
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

import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_enable_segwit.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class EnableSegWitController :
    BaseMobiusController<EnableSegWitModel, EnableSegWitEvent, EnableSegWitEffect>() {

    override val defaultModel = EnableSegWitModel()
    override val update = EnableSegWitUpdate
    override val effectHandler =
        CompositeEffectHandler.from<EnableSegWitEffect, EnableSegWitEvent>(
            Connectable { output ->
                EnableSegWitEffectHandler(
                    output,
                    direct.instance(),
                    direct.instance()
                )
            },
            nestedConnectable(
                { direct.instance<RouterNavigationEffectHandler>() },
                { effect ->
                    when (effect) {
                        EnableSegWitEffect.GoBack -> NavigationEffect.GoBack
                        EnableSegWitEffect.GoToHome -> NavigationEffect.GoToHome
                        else -> null
                    }
                })
        )
    override val layoutId = R.layout.controller_enable_segwit

    override fun bindView(output: Consumer<EnableSegWitEvent>) = output.view {
        enable_button.onClick(EnableSegWitEvent.OnEnableClick)
        back_button.onClick(EnableSegWitEvent.OnBackClicked)
        continue_button.onClick(EnableSegWitEvent.OnContinueClicked)
        cancel_button.onClick(EnableSegWitEvent.OnCancelClicked)
        done_button.onClick(EnableSegWitEvent.OnDoneClicked)
    }

    override fun EnableSegWitModel.render() {
        ifChanged(EnableSegWitModel::state) {
            confirm_choice_layout.isVisible = state == EnableSegWitModel.State.CONFIRMATION
            enable_button.isVisible = state == EnableSegWitModel.State.ENABLE
            done_button.isVisible = state == EnableSegWitModel.State.DONE
            confirmation_layout.isVisible = state == EnableSegWitModel.State.DONE
        }
    }
}