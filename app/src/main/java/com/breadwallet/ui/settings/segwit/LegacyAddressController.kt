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
import android.text.format.DateUtils
import android.view.View
import androidx.core.view.contains
import com.breadwallet.R
import com.breadwallet.databinding.ControllerLegacyAddressBinding
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.logger.logError
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.tools.util.btc
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.settings.segwit.LegacyAddress.E
import com.breadwallet.ui.settings.segwit.LegacyAddress.F
import com.breadwallet.ui.settings.segwit.LegacyAddress.M
import com.breadwallet.util.CryptoUriParser
import com.spotify.mobius.Connectable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.erased.instance

class LegacyAddressController(
    args: Bundle? = null
) : BaseMobiusController<M, E, F>(args) {

    private val cryptoUriParser by instance<CryptoUriParser>()
    private lateinit var copiedLayout: View

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
            }
        )

    private val binding by viewBinding(ControllerLegacyAddressBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        copiedLayout = binding.copiedLayout
        binding.signalLayout.removeView(copiedLayout)
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        binding.signalLayout.setOnTouchListener(SlideDetector(router, binding.signalLayout))
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        binding.signalLayout.setOnTouchListener(null)
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            merge(
                closeButton.clicks().map { E.OnCloseClicked },
                backgroundLayout.clicks().map { E.OnCloseClicked },
                shareButton.clicks().map { E.OnShareClicked },
                addressText.clicks().map { E.OnAddressClicked },
                qrImage.clicks().map { E.OnAddressClicked }
            )
        }
    }

    override fun M.render() {
        with(binding) {
            ifChanged(M::sanitizedAddress, addressText::setText)
            ifChanged(M::receiveAddress) {
                val request = CryptoRequest.Builder()
                    .setAddress(receiveAddress)
                    .build()
                viewAttachScope.launch(Dispatchers.Main) {
                    cryptoUriParser.createUrl(btc, request)?.let { uri ->
                        if (!QRUtils.generateQR(activity, uri.toString(), qrImage)) {
                            logError("failed to generate qr image for address")
                            router.popCurrentController()
                        }
                    }
                }
            }
        }
    }

    private fun showAddressCopied() {
        with(binding) {
            if (signalLayout.contains(copiedLayout)) return

            signalLayout.addView(copiedLayout, signalLayout.indexOfChild(shareButton))
            viewAttachScope.launch(Dispatchers.Main) {
                delay(DateUtils.SECOND_IN_MILLIS * 2)
                if (signalLayout.contains(copiedLayout)) {
                    signalLayout.removeView(copiedLayout)
                }
            }
        }
    }
}
