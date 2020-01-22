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
import android.os.Handler
import android.text.format.DateUtils
import android.view.View
import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.wallets.bitcoin.BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.settings.segwit.LegacyAddress.E
import com.breadwallet.ui.settings.segwit.LegacyAddress.F
import com.breadwallet.ui.settings.segwit.LegacyAddress.M
import com.breadwallet.ui.view
import com.breadwallet.util.CryptoUriParser
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_legacy_address.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class LegacyAddressController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args) {

    private val cryptoUriParser by instance<CryptoUriParser>()
    private lateinit var copiedLayout: View
    private val copyHandler = Handler()

    init {
        overridePushHandler(BottomSheetChangeHandler())
        overridePopHandler(BottomSheetChangeHandler())
    }

    override val layoutId = R.layout.controller_legacy_address
    override val defaultModel = M()
    override val update = LegacyAddressUpdate
    override val init = LegacyAddressInit
    override val effectHandler =
        CompositeEffectHandler.from<F, E>(
            Connectable { output ->
                LegacyAddressHandler(
                    output,
                    direct.instance(),
                    direct.instance(),
                    this@LegacyAddressController,
                    ::showAddressCopied
                )
            },
            nestedConnectable(
                { direct.instance<RouterNavigationEffectHandler>() },
                { effect ->
                    when (effect) {
                        F.GoBack -> NavigationEffect.GoBack
                        else -> null
                    }
                }
            )
        )

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        copiedLayout = copied_layout
        signal_layout.removeView(copiedLayout)
    }

    override fun bindView(output: Consumer<E>) = output.view {
        signal_layout.setOnTouchListener(SlideDetector(router, signal_layout))
        background_layout.setOnClickListener { router.popCurrentController() }
        close_button.onClick(E.OnCloseClicked)
        share_button.onClick(E.OnShareClicked)
        address_text.onClick(E.OnAddressClicked)
        onDispose {
            copyHandler.removeCallbacksAndMessages(null)
            signal_layout.setOnTouchListener(null)
            background_layout.setOnClickListener(null)
        }
    }

    override fun M.render() {
        ifChanged(M::sanitizedAddress, address_text::setText)
        ifChanged(M::receiveAddress) {
            val request = CryptoRequest.Builder()
                .setAddress(receiveAddress)
                .build()
            val uri = cryptoUriParser.createUrl(BITCOIN_CURRENCY_CODE, request)
            if (!QRUtils.generateQR(activity, uri.toString(), qr_image)) {
                error("failed to generate qr image for address")
            }
        }
    }

    private fun showAddressCopied() {
        if (signal_layout.indexOfChild(copiedLayout) == -1) {
            signal_layout.addView(copiedLayout, signal_layout.indexOfChild(share_button))
            copyHandler.postDelayed(
                { signal_layout.removeView(copiedLayout) },
                DateUtils.SECOND_IN_MILLIS * 2
            )
        } else {
            copyHandler.removeCallbacksAndMessages(null)
            signal_layout.removeView(copiedLayout)
        }
    }
}
