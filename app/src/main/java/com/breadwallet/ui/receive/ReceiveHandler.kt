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
import androidx.core.content.ContextCompat.checkSelfPermission
import com.bluelinelabs.conductor.Controller
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.AddressScheme
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.repository.RatesRepository
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.ui.receive.ReceiveScreen.E
import com.breadwallet.ui.receive.ReceiveScreen.F
import com.breadwallet.util.CryptoUriParser
import com.breadwallet.util.CurrencyCode
import com.breadwallet.util.errorHandler
import com.breadwallet.util.isBitcoin
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

private const val RATE_UPDATE_MS = 60_000L
private const val HIDE_COPY_MESSAGE_DELAY_MS: Long = 2 * 1000 // 2 Seconds

class ReceiveHandler(
    private val output: Consumer<E>,
    private val currencyCode: CurrencyCode,
    private val fiatCode: String,
    private val breadBox: BreadBox,
    private val cryptoUriParser: CryptoUriParser,
    private val ratesRepository: RatesRepository,
    private val controller: Controller
) : Connection<F>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default + errorHandler()

    private val resetCopiedChannel = Channel<Unit>(BUFFERED)

    init {
        breadBox.wallet(currencyCode)
            .map { wallet ->
                if (currencyCode.isBitcoin()) {
                    wallet.getTargetForScheme(
                        when (BRSharedPrefs.getIsSegwitEnabled()) {
                            true -> AddressScheme.BTC_SEGWIT
                            false -> AddressScheme.BTC_LEGACY
                        }
                    )
                } else {
                    wallet.target
                }
            }
            .distinctUntilChanged()
            .map { E.OnReceiveAddressUpdated(it.toString(), it.toSanitizedString()) }
            .bindConsumerIn(output, this)

        breadBox.wallet(currencyCode)
            .map { it.currency.name }
            .distinctUntilChanged()
            .map { E.OnWalletNameUpdated(it) }
            .bindConsumerIn(output, this)

        resetCopiedChannel
            .consumeAsFlow()
            .mapLatest {
                delay(HIDE_COPY_MESSAGE_DELAY_MS)
                E.OnHideCopyMessage
            }
            .bindConsumerIn(output, this)

        launch {
            while (isActive) {
                val fiatRate = ratesRepository.getFiatForCrypto(BigDecimal.ONE, currencyCode, fiatCode)

                output.accept(E.OnExchangeRateUpdated(fiatRate ?: BigDecimal.ZERO))

                // TODO: Display out of date, invalid (0) rate, etc.
                delay(RATE_UPDATE_MS)
            }
        }
    }

    override fun accept(effect: F) {
        when (effect) {
            F.ResetCopiedAfterDelay -> resetCopiedChannel.offer(Unit)
            is F.CopyAddressToClipboard ->
                copyAddressToClipboard(effect)
            is F.ShareRequest -> {
                launch(Dispatchers.Main) {
                    val context = checkNotNull(controller.applicationContext)
                    val writePerm = checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    if (writePerm == PackageManager.PERMISSION_GRANTED) {
                        val cryptoRequest = CryptoRequest.Builder()
                            .setAddress(effect.address)
                            .setAmount(effect.amount)
                            .build()
                        val cryptoUri = cryptoUriParser.createUrl(currencyCode, cryptoRequest)
                        QRUtils.sendShareIntent(
                            context,
                            cryptoUri.toString(),
                            effect.address,
                            effect.walletName
                        )?.run(controller::startActivity)
                    } else {
                        controller.requestPermissions(
                            arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                            QRUtils.WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST_ID
                        )
                    }
                }
            }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun copyAddressToClipboard(effect: F.CopyAddressToClipboard) {
        launch(Dispatchers.Main) {
            val context = requireNotNull(controller.applicationContext)
            BRClipboardManager.putClipboard(context, effect.address)
        }
        EventUtils.pushEvent(EventUtils.EVENT_RECEIVE_COPIED_ADDRESS)
    }
}
