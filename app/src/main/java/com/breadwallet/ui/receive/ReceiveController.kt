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
import androidx.core.view.isVisible
import cash.just.support.CashSupport
import cash.just.support.pages.GeneralSupportPage
import cash.just.ui.CashUI
import com.breadwallet.R
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.tools.util.BRConstants
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.receive.ReceiveScreen.E
import com.breadwallet.ui.receive.ReceiveScreen.F
import com.breadwallet.ui.receive.ReceiveScreen.M
import com.breadwallet.ui.send.formatFiatForInputUi
import com.breadwallet.ui.send.fragmentManager
import com.breadwallet.ui.view
import com.breadwallet.util.CryptoUriParser
import com.breadwallet.util.CurrencyCode
import com.spotify.mobius.Connectable
import com.spotify.mobius.First.first
import com.spotify.mobius.Init
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.controller_receive.*
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.util.Currency

private const val EXTRA_CURRENCY_CODE = "currency_code"
private const val EXTRA_RECEIVE = "is_receive"

class ReceiveController(args: Bundle) : BaseMobiusController<M, E, F>(args) {

    constructor(
        currencyCode: CurrencyCode,
        isReceive: Boolean = true
    ) : this(
        bundleOf(
            EXTRA_CURRENCY_CODE to currencyCode,
            EXTRA_RECEIVE to isReceive
        )
    )

    init {
        overridePushHandler(BottomSheetChangeHandler())
        overridePopHandler(BottomSheetChangeHandler())
    }

    private val currencyCode: String = arg(EXTRA_CURRENCY_CODE)
    private val cryptoUriParser by instance<CryptoUriParser>()

    override val layoutId = R.layout.controller_receive

    override val defaultModel =
        M.createDefault(
            currencyCode = currencyCode,
            fiatCurrencyCode = BRSharedPrefs.getPreferredFiatIso()
        )

    override val init = Init<M, F> { model ->
        if (model.isDisplayingCopyMessage) {
            first(model, setOf(F.ResetCopiedAfterDelay))
        } else {
            first(model)
        }
    }

    override val effectHandler: Connectable<F, E> =
        CompositeEffectHandler.from(
            Connectable { consumer ->
                ReceiveHandler(
                    consumer,
                    currencyCode,
                    defaultModel.fiatCurrencyCode,
                    direct.instance(),
                    direct.instance(),
                    this@ReceiveController
                )
            },
            nestedConnectable({
                direct.instance<RouterNavigationEffectHandler>()
            }) { effect ->
                when (effect) {
                    F.CloseSheet -> NavigationEffect.GoBack
                    is F.GoToFaq -> NavigationEffect.GoToFaq(
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

    override fun bindView(output: Consumer<E>) = output.view {
        faq_button.setOnClickListener {
            CashUI.showSupportPage(CashSupport.Builder().detail(GeneralSupportPage.RECEIVE), router.fragmentManager()!!)
        }
        share_button.onClick(E.OnShareClicked)
        close_button.onClick(E.OnCloseClicked)
        qr_image.onClick(E.OnCopyAddressClicked)
        textInputAmount.onClick(E.OnAmountClicked)
        background_layout.onClick(E.OnCloseClicked)
        address_text.onClick(E.OnCopyAddressClicked)
        iso_button.onClick(E.OnToggleCurrencyClicked)

        keyboard.setOnInsertListener { key ->
            output.accept(
                when {
                    key.isEmpty() -> E.OnAmountChange.Delete
                    key[0] == '.' -> E.OnAmountChange.AddDecimal
                    Character.isDigit(key[0]) -> E.OnAmountChange.AddDigit(key.toInt())
                    else -> return@setOnInsertListener
                }
            )
        }

        onDispose {
            signal_layout.setOnTouchListener(null)
            keyboard.setOnInsertListener(null)
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun M.render() {
        val res = checkNotNull(resources)

        ifChanged(M::sanitizedAddress, address_text::setText)
        ifChanged(
            M::rawAmount,
            M::fiatCurrencyCode
        ) {
            val formattedAmount = if (isAmountCrypto || rawAmount.isBlank()) {
                rawAmount
            } else {
                rawAmount.formatFiatForInputUi(fiatCurrencyCode)
            }
            textInputAmount.setText(formattedAmount)
        }

        ifChanged(
            M::isAmountCrypto,
            M::currencyCode,
            M::fiatCurrencyCode
        ) {
            iso_button.text = when {
                isAmountCrypto -> currencyCode.toUpperCase()
                else -> "%s (%s)".format(
                    fiatCurrencyCode.toUpperCase(),
                    Currency.getInstance(fiatCurrencyCode).symbol
                )
            }
        }

        ifChanged(M::currencyCode) {
            title.text = "%s %s".format(
                res.getString(R.string.Receive_title),
                currencyCode.toUpperCase()
            )
        }

        ifChanged(
            M::receiveAddress,
            M::amount
        ) {
            if (receiveAddress.isNotBlank()) {
                val request = CryptoRequest.Builder()
                    .setAddress(receiveAddress)
                    .setAmount(amount)
                    .build()
                val uri = cryptoUriParser.createUrl(currencyCode, request)
                if (!QRUtils.generateQR(activity, uri.toString(), qr_image)) {
                    error("failed to generate qr image for address")
                }
            } else {
                qr_image.setImageDrawable(null)
            }
        }

        ifChanged(M::isRequestAmountSupported) {
            amount_layout.isVisible = isRequestAmountSupported
        }

        ifChanged(M::isAmountEditVisible) {
            keyboard_layout.isVisible = isAmountEditVisible
        }

        ifChanged(M::isDisplayingCopyMessage) {
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
                eventConsumer.accept(E.OnShareClicked)
            }
        }
    }
}
