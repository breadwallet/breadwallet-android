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
package com.breadwallet.ui.writedownkey

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import com.bluelinelabs.conductor.RouterTransaction
import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.auth.AuthenticationController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationEffectHandler
import com.breadwallet.ui.navigation.OnCompleteAction
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_write_down.*
import kotlinx.android.synthetic.main.email_prompt.close_button
import org.kodein.di.direct
import org.kodein.di.erased.instance

class WriteDownKeyController(args: Bundle? = null) :
    BaseMobiusController<WriteDownKeyModel, WriteDownKeyEvent, WriteDownKeyEffect>(args),
    AuthenticationController.Listener {

    companion object {
        private const val EXTRA_ON_COMPLETE = "on-complete"
    }

    constructor(
        doneAction: OnCompleteAction
    ) : this(
        bundleOf(
            EXTRA_ON_COMPLETE to doneAction.name
        )
    )

    init {
        registerForActivityResult(BRConstants.SHOW_PHRASE_REQUEST_CODE)
    }

    private val onComplete = OnCompleteAction.valueOf(arg(EXTRA_ON_COMPLETE))

    override val layoutId: Int = R.layout.activity_write_down
    override val defaultModel = WriteDownKeyModel.createDefault(onComplete)
    override val update = WriteDownKeyUpdate
    override val effectHandler = CompositeEffectHandler.from<WriteDownKeyEffect, WriteDownKeyEvent>(
        Connectable { output ->
            WriteDownKeyEffectHandler(eventConsumer, controllerScope, direct.instance())
        },
        nestedConnectable({ direct.instance<NavigationEffectHandler>() }, { effect ->
            when (effect) {
                WriteDownKeyEffect.GoToBuy -> NavigationEffect.GoToBuy
                else -> null
            }
        }),
        nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
            when (effect) {
                WriteDownKeyEffect.GoToFaq -> NavigationEffect.GoToFaq(BRConstants.FAQ_PAPER_KEY)
                WriteDownKeyEffect.GoToHome -> NavigationEffect.GoToHome
                WriteDownKeyEffect.GoBack -> NavigationEffect.GoBack
                is WriteDownKeyEffect.GoToPaperKey -> NavigationEffect.GoToPaperKey(
                    effect.phrase,
                    effect.onComplete
                )
                else -> null
            }
        })
    )

    override fun WriteDownKeyModel.render() = Unit

    override fun bindView(output: Consumer<WriteDownKeyEvent>): Disposable {
        close_button.setOnClickListener { output.accept(WriteDownKeyEvent.OnCloseClicked) }
        faq_button.setOnClickListener { output.accept(WriteDownKeyEvent.OnFaqClicked) }

        button_write_down.setOnClickListener {
            val isAuthOnTop = router.backstack.first().controller() is AuthenticationController
            if (!isAuthOnTop) {
                val controller = AuthenticationController(
                    title = resources!!.getString(R.string.VerifyPin_title),
                    message = resources!!.getString(R.string.VerifyPin_authorize)
                )
                controller.targetController = this@WriteDownKeyController
                router.pushController(RouterTransaction.with(controller))
            }
        }
        return Disposable {}
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BRConstants.SHOW_PHRASE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            eventConsumer.accept(WriteDownKeyEvent.OnUserAuthenticated)
        }
    }

    override fun onAuthenticationSuccess() {
        eventConsumer.accept(WriteDownKeyEvent.OnUserAuthenticated)
    }
}
