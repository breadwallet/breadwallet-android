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

import android.os.Bundle
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.settings.segwit.EnableSegWit.E
import com.breadwallet.ui.settings.segwit.EnableSegWit.F
import com.breadwallet.ui.settings.segwit.EnableSegWit.M
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.android.synthetic.main.controller_enable_segwit.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance

class EnableSegWitController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args) {

    override val layoutId = R.layout.controller_enable_segwit

    override val defaultModel = M()
    override val update = EnableSegWitUpdate
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createSegWitHandler(
            checkNotNull(applicationContext),
            direct.instance()
        )

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return merge(
            enable_button.clicks().map { E.OnEnableClick },
            back_button.clicks().map { E.OnBackClicked },
            continue_button.clicks().map { E.OnContinueClicked },
            cancel_button.clicks().map { E.OnCancelClicked },
            done_button.clicks().map { E.OnDoneClicked }
        )
    }

    override fun M.render() {
        ifChanged(M::state) {
            confirm_choice_layout.isVisible = state == M.State.CONFIRMATION
            enable_button.isVisible = state == M.State.ENABLE
            done_button.isVisible = state == M.State.DONE
            confirmation_layout.isVisible = state == M.State.DONE
        }
    }
}
