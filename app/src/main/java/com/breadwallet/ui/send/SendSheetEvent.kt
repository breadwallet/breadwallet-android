package com.breadwallet.ui.send

import com.breadwallet.crypto.TransferFeeBasis
import io.hypno.switchboard.MobiusUpdateSpec
import java.math.BigDecimal

@MobiusUpdateSpec(
    baseModel = SendSheetModel::class,
    baseEffect = SendSheetEffect::class
)
sealed class SendSheetEvent {

    data class OnExchangeRateUpdated(
        val fiatPricePerUnit: BigDecimal
    ) : SendSheetEvent()

    data class OnBalanceUpdated(
        val balance: BigDecimal,
        val fiatBalance: BigDecimal
    ) : SendSheetEvent() {
        override fun toString(): String {
            return "OnBalanceUpdated(balance='***', fiatBalance='***')"
        }
    }

    data class OnNetworkFeeUpdated(
        val networkFee: BigDecimal,
        val transferFeeBasis: TransferFeeBasis
    ) : SendSheetEvent()

    data class OnTransferSpeedChanged(
        val transferSpeed: SendSheetModel.TransferSpeed
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

    object OnSendComplete : SendSheetEvent()
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
}
