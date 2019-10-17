/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/15/19.
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
package com.breadwallet.ui.receive

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.os.bundleOf

import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.util.CryptoUriParser2
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.view
import com.breadwallet.util.CurrencyCode
import com.spotify.mobius.Connectable
import com.spotify.mobius.First.first
import com.spotify.mobius.Init
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_receive.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class ReceiveController(
    args: Bundle
) : BaseMobiusController<ReceiveModel, ReceiveEvent, ReceiveEffect>(args) {

    constructor(
        currencyCode: CurrencyCode,
        isReceive: Boolean = true
    ) : this(
        bundleOf(
            EXTRA_CURRENCY_CODE to currencyCode,
            EXTRA_RECEIVE to isReceive
        )
    )

    companion object {
        private const val EXTRA_CURRENCY_CODE = "currency_code"
        private const val EXTRA_RECEIVE = "is_receive"
    }

    init {
        overridePushHandler(BottomSheetChangeHandler())
        overridePopHandler(BottomSheetChangeHandler())
    }

    private val currencyCode: String = arg(EXTRA_CURRENCY_CODE)
    private val cryptoUriParser by instance<CryptoUriParser2>()

    override val layoutId = R.layout.controller_receive

    override val defaultModel = ReceiveModel.createDefault(currencyCode)

    override val init = Init<ReceiveModel, ReceiveEffect> { model ->
        if (model.isDisplayingCopyMessage) {
            first(model, setOf(ReceiveEffect.ResetCopiedAfterDelay))
        } else {
            first(model)
        }
    }

    override val effectHandler: Connectable<ReceiveEffect, ReceiveEvent> =
        CompositeEffectHandler.from(
            Connectable { consumer ->
                ReceiveEffectHandler(
                    consumer,
                    currencyCode,
                    direct.instance(),
                    direct.instance(),
                    activity!!
                )
            },
            nestedConnectable({
                direct.instance<RouterNavigationEffectHandler>()
            }) { effect ->
                when (effect) {
                    ReceiveEffect.CloseSheet -> NavigationEffect.GoBack
                    is ReceiveEffect.GoToFaq -> NavigationEffect.GoToFaq(
                        BRConstants.FAQ_RECEIVE,
                        effect.currencyCode
                    )
                    else -> null
                }
            }
        )

    override val update = ReceiveUpdate

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        title.setText(R.string.UnlockScreen_myAddress)
        signal_layout.setOnTouchListener(SlideDetector(router, signal_layout))
        signal_layout.removeView(copied_layout)
        signal_layout.layoutTransition = UiUtils.getDefaultTransition()
        keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button)
        keyboard.setDeleteImage(R.drawable.ic_delete_black)
        keyboard.setBRKeyboardColor(R.color.white)
    }

    override fun bindView(output: Consumer<ReceiveEvent>) = output.view {
        share_button.onClick(ReceiveEvent.OnShareClicked)
        request_button.onClick(ReceiveEvent.OnRequestAmountClicked)

        address_text.onClick(ReceiveEvent.OnCopyAddressClicked)
        qr_image.onClick(ReceiveEvent.OnCopyAddressClicked)

        background_layout.onClick(ReceiveEvent.OnCloseClicked)
        close_button.onClick(ReceiveEvent.OnCloseClicked)

        faq_button.onClick(ReceiveEvent.OnFaqClicked)
    }

    override fun ReceiveModel.render() {
        ifChanged(ReceiveModel::sanitizedAddress, address_text::setText)

        ifChanged(ReceiveModel::receiveAddress) {
            val request = CryptoRequest.Builder().setAddress(receiveAddress).build()
            val uri = cryptoUriParser.createUrl(currencyCode, request)
            if (!QRUtils.generateQR(activity, uri.toString(), qr_image)) {
                error("failed to generate qr image for address")
            }
        }

        ifChanged(ReceiveModel::isRequestAmountSupported) {
            signal_layout.apply {
                if (isRequestAmountSupported) {
                    if (indexOfChild(request_button) == -1) {
                        addView(separator, indexOfChild(share_button))
                        addView(request_button, indexOfChild(request_button))
                    }
                } else {
                    removeView(separator)
                    removeView(request_button)
                }
            }
        }

        ifChanged(ReceiveModel::isDisplayingCopyMessage) {
            if (isDisplayingCopyMessage) {
                if (signal_layout.indexOfChild(copied_layout) == -1) {
                    signal_layout.addView(copied_layout, signal_layout.indexOfChild(share_button))
                }
            } else {
                signal_layout.removeView(copied_layout)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == QRUtils.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                eventConsumer.accept(ReceiveEvent.OnShareClicked)
            }
        }
    }
}
