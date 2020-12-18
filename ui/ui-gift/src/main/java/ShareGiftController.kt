/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/8/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.uigift

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.TextView
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.formatCryptoForUi
import com.breadwallet.logger.logError
import com.breadwallet.tools.qrcode.QRUtils
import com.breadwallet.tools.util.BRConstants.USD
import com.breadwallet.tools.util.btc
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.formatFiatForUi
import com.breadwallet.ui.uigift.ShareGift.E
import com.breadwallet.ui.uigift.ShareGift.F
import com.breadwallet.ui.uigift.ShareGift.M
import com.breadwallet.ui.uigift.databinding.ControllerShareGiftBinding
import drewcarlson.mobius.flow.FlowTransformer
import drewcarlson.mobius.flow.flowTransformer
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.invoke
import kotlinx.coroutines.launch
import org.kodein.di.erased.instance
import java.io.File
import java.io.IOException
import java.math.BigDecimal
import java.util.UUID

private const val TX_HASH = "ShareGiftController.TX_HASH"
private const val GIFT_URL = "ShareGiftController.GIFT_URL"
private const val GIFT_AMOUNT = "ShareGiftController.GIFT_AMOUNT"
private const val PRICE_PER_UNIT = "ShareGiftController.PRICE_PER_UNIT"
private const val RECIPIENT_NAME = "ShareGiftController.RECIPIENT_NAME"
private const val GIFT_AMOUNT_FIAT = "ShareGiftController.GIFT_AMOUNT_FIAT"
private const val AUTHORITY_BASE = "com.breadwallet"
private const val GIFT_IMAGE_FILE_SUFFIX = "-gift.png"
private const val COMPRESSION_QUALITY = 100

class ShareGiftController(
    args: Bundle
) : BaseMobiusController<M, E, F>(args) {

    constructor(
        txHash: String,
        giftUrl: String,
        recipientName: String,
        giftAmount: BigDecimal,
        pricePerUnit: BigDecimal,
        giftAmountFiat: BigDecimal
    ) : this(
        bundleOf(
            TX_HASH to txHash,
            GIFT_URL to giftUrl,
            GIFT_AMOUNT to giftAmount,
            PRICE_PER_UNIT to pricePerUnit,
            RECIPIENT_NAME to recipientName,
            GIFT_AMOUNT_FIAT to giftAmountFiat
        )
    )

    override val defaultModel: M = M(
        txHash = arg(TX_HASH),
        shareUrl = arg(GIFT_URL),
        giftAmount = arg(GIFT_AMOUNT),
        pricePerUnit = arg(PRICE_PER_UNIT),
        recipientName = arg(RECIPIENT_NAME),
        giftAmountFiat = arg(GIFT_AMOUNT_FIAT)
    )

    override val update = ShareGiftUpdate

    override val flowEffectHandler: FlowTransformer<F, E> =
        flowTransformer { it.transform { } }

    private val binding by viewBinding(ControllerShareGiftBinding::inflate)

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            with(modelFlow) {
                mapDistinct(M::shareUrl)
                    .onEach { QRUtils.generateQR(activity, it, imageView) }
                    .launchIn(uiBindScope)
                mapDistinct { "${it.recipientName}!" }.into(textView)
                mapDistinct { it.pricePerUnit.formatFiatForUi("USD") }
                    .into(labelPricePerUnit)
                mapDistinct { it.giftAmount.formatCryptoForUi(btc) }
                    .into(labelWalletCrypto)
                mapDistinct { it.giftAmountFiat.formatFiatForUi("USD") }
                    .shareIn(uiBindScope, SharingStarted.Lazily)
                    .run {
                        into(labelApproxFiat)
                        into(labelWalletFiat)
                    }
            }
            merge(
                button.clicks().map { E.OnSendClicked }
            )
        }
    }

    override fun handleViewEffect(effect: ViewEffect) {
        super.handleViewEffect(effect)
        if (effect is F.ExportGiftImage) {
            val context = checkNotNull(activity)
            viewAttachScope.launch(NonCancellable) {
                shareGift(context, effect)
            }
        }
    }

    private suspend fun shareGift(context: Context, effect: F.ExportGiftImage) {
        val qrCode = QRUtils.encodeAsBitmap(effect.giftUrl, 406)
        val bitmap = GiftCard.create(
            context,
            qrCode,
            effect.recipientName,
            effect.giftAmount.formatCryptoForUi(btc),
            effect.giftAmountFiat.formatFiatForUi(USD),
            effect.fiatPricePerUnit.formatFiatForUi(USD),
        )
        val file = File(context.externalCacheDir, "${UUID.randomUUID()}$GIFT_IMAGE_FILE_SUFFIX")
        check(file.save(bitmap)) {
            "Failed to save gift image image"
        }
        val breadBox by instance<BreadBox>()
        val authority = buildString {
            append(AUTHORITY_BASE)
            if (!breadBox.isMainnet) append(".testnet")
            if (BuildConfig.DEBUG) append(".debug")
        }
        val uri = FileProvider.getUriForFile(context, authority, file)
        context.shareImage(uri, effect.giftUrl)
    }

    private suspend fun File.save(bitmap: Bitmap): Boolean = IO {
        try {
            outputStream().use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, COMPRESSION_QUALITY, output)
            }
            true
        } catch (e: IOException) {
            logError("Failed to write bitmap to file", e)
            false
        }
    }

    private fun Context.shareImage(uri: Uri, text: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, text)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val title = getString(R.string.Receive_share)
        val chooserIntent = Intent.createChooser(shareIntent, title)
        chooserIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(chooserIntent)
    }

    inline fun <T, R> Flow<T>.mapDistinct(crossinline transform: (T) -> R): Flow<R> {
        return distinctUntilChanged().map { transform(it) }
    }

    fun Flow<String>.into(view: TextView) =
        onEach { view.text = it }.launchIn(uiBindScope)
}
