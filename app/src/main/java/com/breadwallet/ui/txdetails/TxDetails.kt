/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/21/20.
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
package com.breadwallet.ui.txdetails

import com.breadwallet.crypto.Transfer
import com.breadwallet.platform.entities.GiftMetaData
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.models.TransactionState
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.ui.send.TransferField
import com.breadwallet.util.CurrencyCode
import com.breadwallet.platform.entities.TxMetaData
import dev.zacsweers.redacted.annotations.Redacted
import java.math.BigDecimal
import java.util.Date

object TxDetails {
    data class M(
        val currencyCode: CurrencyCode,
        @Redacted val transactionHash: String,
        val isCryptoPreferred: Boolean,
        val preferredFiatIso: String,
        val showDetails: Boolean = false,
        val isEth: Boolean = false,
        val isErc20: Boolean = false,
        val cryptoTransferredAmount: BigDecimal = BigDecimal.ZERO,
        val fee: BigDecimal = BigDecimal.ZERO,
        val feeCurrency: CurrencyCode = "",
        val isReceived: Boolean = false,
        val fiatAmountNow: BigDecimal = BigDecimal.ZERO,
        val gasPrice: BigDecimal = BigDecimal.ZERO,
        val gasLimit: BigDecimal = BigDecimal.ZERO,
        val blockNumber: Int = 0,
        @Redacted val toOrFromAddress: String = "",
        @Redacted val memo: String = "",
        val memoLoaded: Boolean = false,
        val exchangeRate: BigDecimal = BigDecimal.ZERO,
        val exchangeCurrencyCode: String = "",
        val confirmationDate: Date? = null,
        @Redacted val confirmedInBlockNumber: String = "",
        val transactionState: TransactionState? = null,
        val isCompleted: Boolean = false,
        val feeToken: String = "",
        val confirmations: Int = 0,
        val transferFields: List<TransferField> = emptyList(),
        val gift: GiftMetaData? = null
    ) {
        companion object {
            /** Create a [TxDetails.M] using only the required values. */
            fun createDefault(
                currencyCode: CurrencyCode,
                transactionHash: String,
                preferredFiatIso: String,
                isCryptoPreferred: Boolean
            ) = M(
                currencyCode = currencyCode,
                transactionHash = transactionHash,
                preferredFiatIso = preferredFiatIso,
                isCryptoPreferred = isCryptoPreferred
            )
        }

        val destinationTag: TransferField? =
            transferFields.find { it.key == TransferField.DESTINATION_TAG }

        val hederaMemo: TransferField? =
            transferFields.find { it.key == TransferField.HEDERA_MEMO }

        val transactionTotal: BigDecimal
            get() = cryptoTransferredAmount + fee

        val fiatAmountWhenSent: BigDecimal
            get() = cryptoTransferredAmount.multiply(exchangeRate)

        val isFeeForToken: Boolean
            get() = feeToken.isNotBlank()
    }

    sealed class E {
        data class OnTransactionUpdated(
            val transaction: Transfer,
            val gasPrice: BigDecimal,
            val gasLimit: BigDecimal
        ) : E()

        data class OnFiatAmountNowUpdated(val fiatAmountNow: BigDecimal) : E()
        data class OnMetaDataUpdated(val metaData: TxMetaData) : E()
        data class OnMemoChanged(@Redacted val memo: String) : E()
        object OnTransactionHashClicked : E()
        object OnAddressClicked : E()
        object OnClosedClicked : E()
        object OnShowHideDetailsClicked : E()
        object OnGiftResendClicked : E()
        object OnGiftReclaimClicked : E()
    }

    sealed class F {

        data class LoadTransaction(
            val currencyCode: CurrencyCode,
            val transactionHash: String
        ) : F()

        data class LoadTransactionMetaData(
            val currencyCode: String,
            @Redacted val transactionHash: String
        ) : F()

        data class LoadFiatAmountNow(
            val cryptoTransferredAmount: BigDecimal,
            val currencyCode: String,
            val preferredFiatIso: String
        ) : F()

        data class UpdateMemo(
            val currencyCode: String,
            @Redacted val transactionHash: String,
            @Redacted val memo: String
        ) : F()

        data class CopyToClipboard(
            @Redacted val text: String
        ) : F(), ViewEffect

        object Close : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.Back
        }

        data class ImportGift(
            @Redacted val privateKey: String,
            val transferHash: String
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ImportWallet(
                privateKey,
                reclaimingGift = transferHash
            )
        }

        data class ShareGift(
            @Redacted val giftUrl: String,
            @Redacted val txHash: String,
            @Redacted val recipientName: String,
            val giftAmount: BigDecimal,
            val giftAmountFiat: BigDecimal,
            val pricePerUnit: BigDecimal
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.ShareGift(giftUrl, txHash, recipientName, giftAmount, giftAmountFiat, pricePerUnit)
        }
    }
}
