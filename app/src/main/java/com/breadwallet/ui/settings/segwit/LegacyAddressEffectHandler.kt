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

import android.app.Activity
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.toSanitizedString
import com.breadwallet.crypto.AddressScheme
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.legacy.presenter.entities.CryptoRequest
import com.breadwallet.legacy.wallet.wallets.bitcoin.BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE
import com.breadwallet.tools.manager.BRClipboardManager
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.util.CryptoUriParser2
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@UseExperimental(FlowPreview::class)
class LegacyAddressEffectHandler(
    private val output: Consumer<LegacyAddressEvent>,
    private val breadBox: BreadBox,
    private val cryptoUriParser: CryptoUriParser2,
    private val activity: Activity,
    private val showAddressCopiedAnimation: () -> Unit
) : Connection<LegacyAddressEffect>, CoroutineScope {

    override val coroutineContext = SupervisorJob() + Dispatchers.Default

    init {
        breadBox.wallet(BITCOIN_CURRENCY_CODE)
            .map { wallet ->
                wallet.getTargetForScheme(AddressScheme.BTC_LEGACY)
            }
            .distinctUntilChanged()
            .map { LegacyAddressEvent.OnAddressUpdated(it.toString(), it.toSanitizedString()) }
            .bindConsumerIn(output, this)

        breadBox.wallet(BITCOIN_CURRENCY_CODE)
            .map { it.currency.name }
            .distinctUntilChanged()
            .map { LegacyAddressEvent.OnWalletNameUpdated(it) }
            .bindConsumerIn(output, this)
    }

    override fun accept(effect: LegacyAddressEffect) {
        when (effect) {
            is LegacyAddressEffect.CopyAddressToClipboard -> {
                launch(Dispatchers.Main) {
                    BRClipboardManager.putClipboard(activity, effect.address)
                    showAddressCopiedAnimation()
                }
            }
            is LegacyAddressEffect.ShareAddress -> {
                launch(Dispatchers.Main) {
                    val cryptoRequest = CryptoRequest.Builder()
                        .setAddress(effect.address)
                        .build()
                    val cryptoUri = cryptoUriParser.createUrl(BITCOIN_CURRENCY_CODE, cryptoRequest)
                    QRUtils.sendShareIntent(
                        activity,
                        cryptoUri.toString(),
                        effect.address,
                        effect.walletName
                    )
                }
            }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }
}