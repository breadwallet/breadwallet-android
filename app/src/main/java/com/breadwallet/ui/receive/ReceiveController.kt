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

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import com.breadwallet.R
import com.breadwallet.databinding.ControllerReceiveBinding
import com.breadwallet.legacy.presenter.customviews.BRKeyboard
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.logger.logError
import com.breadwallet.tools.animation.SlideDetector
import com.breadwallet.tools.animation.UiUtils
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.changehandlers.BottomSheetChangeHandler
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.receive.ReceiveScreen.E
import com.breadwallet.ui.receive.ReceiveScreen.F
import com.breadwallet.ui.receive.ReceiveScreen.M
import com.breadwallet.ui.send.formatFiatForInputUi
import com.breadwallet.util.CryptoUriParser
import com.breadwallet.util.CurrencyCode
import com.spotify.mobius.First.first
import com.spotify.mobius.Init
import drewcarlson.mobius.flow.FlowTransformer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import org.kodein.di.direct
import org.kodein.di.erased.instance
import java.util.Currency
import java.util.Locale

private const val EXTRA_CURRENCY_CODE = "currency_code"
private const val EXTRA_RECEIVE = "is_receive"
private const val HIDE_COPY_MESSAGE_DELAY_MS: Long = 2 * 1000 // 2 Seconds

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

    override val defaultModel =
        M.createDefault(
            currencyCode = currencyCode,
            fiatCurrencyCode = BRSharedPrefs.getPreferredFiatIso()
        )
    override val init = Init<M, F> { model ->
        first(
            model, setOf(
                F.LoadWalletInfo(model.currencyCode),
                F.LoadExchangeRate(model.currencyCode)
            )
        )
    }
    override val update = ReceiveUpdate
    override val flowEffectHandler: FlowTransformer<F, E>
        get() = createReceiveHandler(
            checkNotNull(applicationContext),
            direct.instance()
        )

    private val binding by viewBinding(ControllerReceiveBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        with(binding) {
            title.setText(R.string.UnlockScreen_myAddress)
            signalLayout.removeView(copiedLayout)
            signalLayout.layoutTransition = UiUtils.getDefaultTransition()
            keyboard.setBRButtonBackgroundResId(R.drawable.keyboard_white_button)
            keyboard.setDeleteImage(R.drawable.ic_delete_black)
            keyboard.setBRKeyboardColor(R.color.white)
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        binding.signalLayout.setOnTouchListener(SlideDetector(router, binding.signalLayout))
    }

    override fun onDetach(view: View) {
        binding.signalLayout.setOnTouchListener(null)
        super.onDetach(view)
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            merge(
                faqButton.clicks().map { E.OnFaqClicked },
                shareButton.clicks().map { E.OnShareClicked },
                closeButton.clicks().map { E.OnCloseClicked },
                qrImage.clicks().map { E.OnCopyAddressClicked },
                textInputAmount.clicks().map { E.OnAmountClicked },
                backgroundLayout.clicks().map { E.OnCloseClicked },
                addressText.clicks().map { E.OnCopyAddressClicked },
                isoButton.clicks().map { E.OnToggleCurrencyClicked },
                keyboard.bindInput()
            )
        }
    }

    private fun BRKeyboard.bindInput() = callbackFlow<E> {
        setOnInsertListener { key ->
            offer(
                when {
                    key.isEmpty() -> E.OnAmountChange.Delete
                    key[0] == '.' -> E.OnAmountChange.AddDecimal
                    Character.isDigit(key[0]) -> E.OnAmountChange.AddDigit(key.toInt())
                    else -> return@setOnInsertListener
                }
            )
        }
        awaitClose { setOnInsertListener(null) }
    }

    @Suppress("LongMethod", "ComplexMethod")
    override fun M.render() {
        val res = checkNotNull(resources)

        with(binding) {
            ifChanged(M::sanitizedAddress, addressText::setText)
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
                isoButton.text = when {
                    isAmountCrypto -> currencyCode.toUpperCase(Locale.ROOT)
                    else -> "%s (%s)".format(
                        fiatCurrencyCode.toUpperCase(Locale.ROOT),
                        Currency.getInstance(fiatCurrencyCode).symbol
                    )
                }
            }

            ifChanged(M::currencyCode) {
                title.text = "%s %s".format(
                    res.getString(R.string.Receive_title),
                    currencyCode.toUpperCase(Locale.ROOT)
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
                    viewAttachScope.launch(Dispatchers.Main) {
                        cryptoUriParser.createUrl(currencyCode, request)?.let { uri ->
                            if (!QRUtils.generateQR(activity, uri.toString(), qrImage)) {
                                logError("failed to generate qr image for address")
                            }
                        }
                    }
                } else {
                    qrImage.setImageDrawable(null)
                }
            }

            ifChanged(M::isRequestAmountSupported) {
                amountLayout.isVisible = isRequestAmountSupported
            }

            ifChanged(M::isAmountEditVisible) {
                keyboardLayout.isVisible = isAmountEditVisible
            }
        }
    }

    override fun handleViewEffect(effect: ViewEffect) {
        when (effect) {
            is F.ShowCopiedMessage -> showCopiedMessage()
            is F.ShareRequest -> shareAddress(effect)
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

    private fun showCopiedMessage() {
        viewCreatedScope.launch(Dispatchers.Main) {
            with(binding) {
                if (signalLayout.indexOfChild(copiedLayout) == -1) {
                    signalLayout.addView(
                        copiedLayout,
                        signalLayout.indexOfChild(shareButton)
                    )
                }
                delay(HIDE_COPY_MESSAGE_DELAY_MS)
                signalLayout.removeView(copiedLayout)
            }
        }
    }

    private fun shareAddress(effect: F.ShareRequest) {
        val context = checkNotNull(applicationContext)
        val writePerm =
            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        if (writePerm == PackageManager.PERMISSION_GRANTED) {
            val cryptoRequest = CryptoRequest.Builder()
                .setAddress(effect.address)
                .setAmount(effect.amount)
                .build()
            viewAttachScope.launch(Dispatchers.Main) {
                cryptoUriParser.createUrl(currencyCode, cryptoRequest)
                    ?.let { cryptoUri ->
                        QRUtils.sendShareIntent(
                            context,
                            cryptoUri.toString(),
                            effect.address,
                            effect.walletName
                        )
                    }?.run(::startActivity)
            }
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                QRUtils.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID
            )
        }
    }
}
