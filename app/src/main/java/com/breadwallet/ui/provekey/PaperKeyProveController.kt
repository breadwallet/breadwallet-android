/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 10/10/19.
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
package com.breadwallet.ui.provekey

import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.tools.animation.SpringAnimator
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.controllers.SignalController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.textChanges
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.provekey.PaperKeyProve.E
import com.breadwallet.ui.provekey.PaperKeyProve.F
import com.breadwallet.ui.provekey.PaperKeyProve.M
import com.breadwallet.util.normalize
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.android.synthetic.main.controller_paper_key_prove.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

private const val EXTRA_PHRASE = "phrase"
private const val EXTRA_ON_COMPLETE = "on-complete"

class PaperKeyProveController(args: Bundle) :
    BaseMobiusController<M, E, F>(args),
    SignalController.Listener {

    constructor(phrase: List<String>, onComplete: OnCompleteAction) : this(
        bundleOf(
            EXTRA_PHRASE to phrase,
            EXTRA_ON_COMPLETE to onComplete.name
        )
    )

    private val phrase: List<String> = arg(EXTRA_PHRASE)
    private val onComplete = OnCompleteAction.valueOf(arg(EXTRA_ON_COMPLETE))

    override val layoutId = R.layout.controller_paper_key_prove
    override val defaultModel = M.createDefault(phrase, onComplete)
    override val update = PaperKeyProveUpdate
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createPaperKeyProveHandler()

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return merge(
            submit_btn.clicks().map { E.OnSubmitClicked },
            first_word.textChanges().map { E.OnFirstWordChanged(it.normalize()) },
            second_word.textChanges().map { E.OnSecondWordChanged(it.normalize()) }
        )
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        Utils.hideKeyboard(activity)
    }

    override fun M.render() {
        ifChanged(M::firstWordState) {
            first_word.setTextColor(
                activity!!.getColor(
                    if (firstWordState == M.WordState.VALID) R.color.light_gray
                    else R.color.red_text
                )
            )
            check_mark_1.isVisible = firstWordState == M.WordState.VALID
        }
        ifChanged(M::secondWordSate) {
            second_word.setTextColor(
                activity!!.getColor(
                    if (secondWordSate == M.WordState.VALID) R.color.light_gray
                    else R.color.red_text
                )
            )
            check_mark_2.isVisible = secondWordSate == M.WordState.VALID
        }

        ifChanged(M::firstWordIndex) {
            first_word_label.text =
                activity!!.getString(R.string.ConfirmPaperPhrase_word, firstWordIndex + 1)
        }
        ifChanged(M::secondWordIndex) {
            second_word_label.text =
                activity!!.getString(R.string.ConfirmPaperPhrase_word, secondWordIndex + 1)
        }
    }

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            is F.ShakeWords -> {
                if (effect.first) {
                    SpringAnimator.failShakeAnimation(applicationContext, first_word)
                }
                if (effect.second) {
                    SpringAnimator.failShakeAnimation(applicationContext, second_word)
                }
            }
        }
    }

    override fun onSignalComplete() {
        eventConsumer.accept(E.OnBreadSignalShown)
    }
}
