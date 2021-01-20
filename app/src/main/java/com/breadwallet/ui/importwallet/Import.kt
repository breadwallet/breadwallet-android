/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 12/3/19.
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
package com.breadwallet.ui.importwallet

import com.breadwallet.R
import com.breadwallet.crypto.Amount
import com.breadwallet.tools.util.BRConstants.FAQ_IMPORT_WALLET
import com.breadwallet.ui.ViewEffect
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.NavigationTarget
import com.breadwallet.util.CurrencyCode
import dev.zacsweers.redacted.annotations.Redacted
import java.math.BigDecimal

object Import {

    const val CONFIRM_IMPORT_DIALOG = "confirm_import"
    const val IMPORT_SUCCESS_DIALOG = "import_success"

    data class M(
        @Redacted val privateKey: String? = null,
        @Redacted val keyPassword: String? = null,
        val keyRequiresPassword: Boolean = false,
        val isKeyValid: Boolean = false,
        val loadingState: LoadingState = LoadingState.IDLE,
        val currencyCode: CurrencyCode? = null,
        val reclaimGiftHash: String? = null,
        val scanned: Boolean = false,
        val gift: Boolean = false
    ) {
        enum class LoadingState {
            IDLE, VALIDATING, ESTIMATING, SUBMITTING
        }

        val isLoading: Boolean =
            loadingState != LoadingState.IDLE

        fun reset(): M = copy(
            privateKey = null,
            keyPassword = null,
            keyRequiresPassword = false,
            isKeyValid = false,
            loadingState = LoadingState.IDLE
        )

        companion object {
            fun createDefault(
                privateKey: String? = null,
                isPasswordProtected: Boolean = false,
                reclaimGiftHash: String? = null,
                scanned: Boolean = false,
                gift: Boolean = false
            ): M = M(
                privateKey = privateKey,
                keyRequiresPassword = isPasswordProtected,
                reclaimGiftHash = reclaimGiftHash,
                scanned = scanned,
                gift = gift,
                loadingState = if (privateKey == null) {
                    LoadingState.IDLE
                } else {
                    LoadingState.VALIDATING
                }
            )
        }
    }

    sealed class E {

        object OnFaqClicked : E()
        object OnScanClicked : E()
        object OnCloseClicked : E()

        object OnImportConfirm : E()
        object OnImportCancel : E()

        data class OnPasswordEntered(
            @Redacted val password: String
        ) : E()

        data class RetryImport(
            @Redacted val privateKey: String,
            @Redacted val password: String?
        ) : E()

        data class OnKeyScanned(
            @Redacted val privateKey: String,
            val isPasswordProtected: Boolean
        ) : E()

        sealed class Key : E() {
            object NoWallets : Key()
            object OnInvalid : Key()
            object OnPasswordInvalid : Key()
            data class OnValid(
                val isPasswordProtected: Boolean = false
            ) : Key()
        }

        sealed class Estimate : E() {
            data class Success(
                val balance: Amount,
                val feeAmount: Amount,
                val currencyCode: CurrencyCode
            ) : Estimate()

            data class FeeError(
                val balance: BigDecimal
            ) : Estimate()

            object NoBalance : Estimate()
            data class BalanceTooLow(
                val balance: BigDecimal
            ) : Estimate()
        }

        sealed class Transfer : E() {
            data class OnSuccess(
                @Redacted val transferHash: String,
                val currencyCode: CurrencyCode
            ) : Transfer()

            object OnFailed : Transfer()
        }
    }

    sealed class F {

        object ShowPasswordInput : F(), ViewEffect
        object ShowKeyInvalid : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                messageResId = R.string.Import_Error_notValid,
                positiveButtonResId = R.string.Button_ok
            )
        }
        object ShowPasswordInvalid : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                messageResId = R.string.Import_wrongPassword,
                positiveButtonResId = R.string.Button_ok
            )
        }
        object ShowBalanceTooLow : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.Import_title,
                messageResId = R.string.Import_Error_highFees,
                positiveButtonResId = R.string.Button_ok
            )
        }
        object ShowNoBalance : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.Import_title,
                messageResId = R.string.Import_Error_empty,
                positiveButtonResId = R.string.Button_ok
            )
        }
        object ShowImportFailed : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.Import_title,
                messageResId = R.string.Import_Error_signing,
                positiveButtonResId = R.string.Button_ok
            )
        }
        object ShowImportSuccess : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.Import_success,
                messageResId = R.string.Import_SuccessBody,
                positiveButtonResId = R.string.Button_ok,
                dialogId = IMPORT_SUCCESS_DIALOG
            )
        }

        data class ShowConfirmImport(
            val receiveAmount: String,
            val feeAmount: String
        ) : F(), NavigationEffect {
            override val navigationTarget = NavigationTarget.AlertDialog(
                titleResId = R.string.Import_title,
                messageResId = R.string.Import_confirm,
                messageArgs = listOf(receiveAmount, feeAmount),
                positiveButtonResId = R.string.Import_importButton,
                negativeButtonResId = R.string.Button_cancel,
                dialogId = CONFIRM_IMPORT_DIALOG
            )
        }

        data class ValidateKey(
            @Redacted val privateKey: String,
            @Redacted val password: String?
        ) : F()

        data class SubmitImport(
            @Redacted val privateKey: String,
            @Redacted val password: String?,
            val currencyCode: CurrencyCode,
            val reclaimGiftHash: String?,
        ) : F()

        data class TrackEvent(
            val eventString: String,
        ) : F()

        sealed class Nav(
            override val navigationTarget: NavigationTarget
        ) : F(), NavigationEffect {
            object GoBack : Nav(NavigationTarget.Back)
            object GoToFaq : Nav(NavigationTarget.SupportPage(FAQ_IMPORT_WALLET))
            object GoToScan : Nav(NavigationTarget.QRScanner)
        }

        sealed class EstimateImport : F() {
            abstract val privateKey: String

            data class Key(
                @Redacted override val privateKey: String
            ) : EstimateImport()

            data class KeyWithPassword(
                @Redacted override val privateKey: String,
                @Redacted val password: String
            ) : EstimateImport()
        }
    }
}
