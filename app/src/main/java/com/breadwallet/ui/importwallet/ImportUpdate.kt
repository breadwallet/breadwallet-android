package com.breadwallet.ui.importwallet

import com.breadwallet.breadbox.toBigDecimal
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next
import com.spotify.mobius.Next.noChange
import com.spotify.mobius.Update

val ImportUpdate = Update<Import.M, Import.E, Import.F> { model, event ->
    when (event) {
        is Import.E.OnScanClicked -> dispatch(setOf(Import.F.Nav.GoToScan))
        Import.E.OnFaqClicked -> dispatch(setOf(Import.F.Nav.GoToFaq))
        Import.E.OnCloseClicked -> dispatch(setOf(Import.F.Nav.GoBack))
        Import.E.Key.NoWallets -> dispatch(setOf(Import.F.Nav.GoBack))
        is Import.E.Key.OnValid -> when {
            event.isPasswordProtected -> when {
                !model.keyPassword.isNullOrEmpty() -> next(
                    model.copy(
                        isKeyValid = true,
                        keyRequiresPassword = true
                    ),
                    setOf<Import.F>(
                        Import.F.EstimateImport.KeyWithPassword(
                            privateKey = checkNotNull(model.privateKey),
                            password = model.keyPassword
                        )
                    )
                )
                else -> next(
                    model.copy(
                        isKeyValid = true,
                        keyRequiresPassword = true
                    ),
                    setOf<Import.F>(Import.F.ShowPasswordInput)
                )
            }
            else -> next(
                model.copy(
                    isKeyValid = true,
                    keyRequiresPassword = false
                ),
                setOf<Import.F>(
                    Import.F.EstimateImport.Key(
                        privateKey = checkNotNull(model.privateKey)
                    )
                )
            )
        }
        Import.E.Key.OnInvalid -> next(
            model.reset(),
            setOf(Import.F.ShowKeyInvalid)
        )
        Import.E.Key.OnPasswordInvalid -> next(
            model.reset(),
            setOf(Import.F.ShowPasswordInvalid)
        )
        is Import.E.OnKeyScanned -> next(
            model.copy(
                privateKey = event.privateKey,
                keyRequiresPassword = event.isPasswordProtected,
                isKeyValid = true,
                loadingState = if (event.isPasswordProtected) {
                    Import.M.LoadingState.VALIDATING
                } else {
                    Import.M.LoadingState.ESTIMATING
                }
            ),
            setOf(
                if (event.isPasswordProtected) {
                    Import.F.ShowPasswordInput
                } else {
                    Import.F.EstimateImport.Key(event.privateKey)
                }
            )
        )
        is Import.E.RetryImport -> {
            val newModel = model.copy(
                privateKey = event.privateKey,
                keyPassword = event.password,
                keyRequiresPassword = event.password != null,
                isKeyValid = true,
                loadingState = Import.M.LoadingState.ESTIMATING
            )
            val estimateEffect = when {
                newModel.keyRequiresPassword ->
                    Import.F.EstimateImport.KeyWithPassword(
                        privateKey = event.privateKey,
                        password = checkNotNull(event.password)
                    )
                else ->
                    Import.F.EstimateImport.Key(
                        privateKey = event.privateKey
                    )
            }
            next(model, setOf<Import.F>(estimateEffect))
        }
        is Import.E.Estimate.Success -> {
            val balance = event.balance.toBigDecimal()
            val fee = event.feeAmount.toBigDecimal()
            next(
                model.copy(
                    currencyCode = event.currencyCode
                ), setOf(
                    Import.F.ShowConfirmImport(
                        receiveAmount = (balance - fee).toPlainString(),
                        feeAmount = fee.toPlainString()
                    )
                )
            )
        }
        is Import.E.Estimate.FeeError ->
            next(model.reset(), setOf(Import.F.ShowImportFailed))
        is Import.E.Estimate.BalanceTooLow ->
            next(model.reset(), setOf(Import.F.ShowBalanceTooLow))
        Import.E.Estimate.NoBalance ->
            next(model.reset(), setOf(Import.F.ShowNoBalance))
        is Import.E.Transfer.OnSuccess ->
            next(model.reset(), setOf(Import.F.ShowImportSuccess))
        Import.E.Transfer.OnFailed ->
            next(model.reset(), setOf(Import.F.ShowImportFailed))
        Import.E.OnImportCancel -> next(model.reset())
        Import.E.OnImportConfirm ->
            next(
                model.copy(
                    loadingState = Import.M.LoadingState.SUBMITTING
                ),
                setOf(
                    Import.F.SubmitImport(
                        privateKey = checkNotNull(model.privateKey),
                        password = model.keyPassword,
                        currencyCode = checkNotNull(model.currencyCode)
                    )
                )
            )
        is Import.E.OnPasswordEntered -> when {
            model.privateKey != null && model.keyRequiresPassword ->
                next(
                    model.copy(
                        keyPassword = event.password,
                        loadingState = Import.M.LoadingState.VALIDATING
                    ),
                    setOf<Import.F>(
                        Import.F.ValidateKey(
                            privateKey = model.privateKey,
                            password = event.password
                        )
                    )
                )
            else -> noChange()
        }
    }
}
