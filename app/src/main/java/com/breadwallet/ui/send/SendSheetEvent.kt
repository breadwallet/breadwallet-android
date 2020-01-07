package com.breadwallet.ui.send

import com.breadwallet.crypto.Transfer
import com.breadwallet.breadbox.TransferSpeed
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
        val networkFee: BigDecimal,
        val transferFeeBasis: TransferFeeBasis
    ) : SendSheetEvent()

    data class OnTransferSpeedChanged(
        val transferSpeed: TransferSpeed
    ) : SendSheetEvent()

    data class OnAddressValidated(
        val address: String,
        val isValid: Boolean,
        val clear: Boolean
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
}
