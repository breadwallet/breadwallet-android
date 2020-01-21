/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
package com.breadwallet.ui.send

import com.breadwallet.crypto.Transfer
import com.breadwallet.breadbox.TransferSpeed
import com.breadwallet.crypto.PaymentProtocolRequest
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.util.CurrencyCode
import io.hypno.switchboard.MobiusUpdateSpec
import java.math.BigDecimal

@MobiusUpdateSpec(
    baseModel = SendSheetModel::class,
    baseEffect = SendSheetEffect::class
)
sealed class SendSheetEvent {

    data class OnRequestScanned(
        val currencyCode: CurrencyCode,
        val amount: BigDecimal?,
        val targetAddress: String?
    ) : SendSheetEvent()

    data class OnExchangeRateUpdated(
        val fiatPricePerUnit: BigDecimal,
        val fiatPricePerFeeUnit: BigDecimal,
        val feeCurrencyCode: CurrencyCode
    ) : SendSheetEvent()

    data class OnBalanceUpdated(
        val balance: BigDecimal,
        val fiatBalance: BigDecimal
    ) : SendSheetEvent() {
        override fun toString(): String {
            return "OnBalanceUpdated(balance='***', fiatBalance='***')"
        }
    }

    object OnNetworkFeeError : SendSheetEvent()

    data class OnNetworkFeeUpdated(
        val targetAddress: String,
        val amount: BigDecimal,
        val networkFee: BigDecimal,
        val transferFeeBasis: TransferFeeBasis
    ) : SendSheetEvent()

    data class OnTransferSpeedChanged(
        val transferSpeed: TransferSpeed
    ) : SendSheetEvent()

    data class OnAddressValidated(
        val address: String,
        val isValid: Boolean
    ) : SendSheetEvent() {
        override fun toString() = "OnAddressValidated(isValid=$isValid)"
    }

    sealed class OnAmountChange : SendSheetEvent() {
        object AddDecimal : OnAmountChange()
        object Delete : OnAmountChange()
        object Clear : OnAmountChange()

        data class AddDigit(
            val digit: Int
        ) : OnAmountChange() {
            override fun toString() = "AddDigit(digit=***)"
        }
    }

    data class OnTargetAddressChanged(
        val toAddress: String
    ) : SendSheetEvent() {
        override fun toString() = "${this::class.java.name}(toAddress='***')"
    }

    data class OnMemoChanged(val memo: String) : SendSheetEvent() {
        override fun toString() = "${this::class.java.name}(memo='***')"
    }

    sealed class ConfirmTx : SendSheetEvent() {
        object OnConfirmClicked : ConfirmTx()
        object OnCancelClicked : ConfirmTx()
    }

    sealed class OnAddressPasted : SendSheetEvent() {

        data class ValidAddress(
            val address: String
        ) : OnAddressPasted() {
            override fun toString() = "${this::class.java.name}()"
        }

        object NoAddress : OnAddressPasted()
        object InvalidAddress : OnAddressPasted()
    }

    object GoToEthWallet : SendSheetEvent()

    data class OnSendComplete(val transfer: Transfer) : SendSheetEvent()
    object OnSendFailed : SendSheetEvent()

    object OnSendClicked : SendSheetEvent()
    object OnAuthSuccess : SendSheetEvent()
    object OnAuthCancelled : SendSheetEvent()

    object OnScanClicked : SendSheetEvent()
    object OnFaqClicked : SendSheetEvent()
    object OnCloseClicked : SendSheetEvent()
    object OnPasteClicked : SendSheetEvent()
    object OnAmountEditClicked : SendSheetEvent()
    object OnAmountEditDismissed : SendSheetEvent()

    object OnToggleCurrencyClicked : SendSheetEvent()

    data class OnAuthenticationSettingsUpdated(internal val isFingerprintEnable: Boolean) :
        SendSheetEvent()

    sealed class PaymentProtocol : SendSheetEvent() {
        data class OnPaymentLoaded(
            val paymentRequest: PaymentProtocolRequest,
            val cryptoAmount: BigDecimal
        ) : PaymentProtocol()

        data class OnLoadFailed(val message: String) : PaymentProtocol()
        object OnPostCompleted : PaymentProtocol()
        object OnPostFailed : PaymentProtocol()
    }
}
