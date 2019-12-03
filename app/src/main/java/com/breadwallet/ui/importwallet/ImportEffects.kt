package com.breadwallet.ui.importwallet

import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.hashString
import com.breadwallet.breadbox.toBigDecimal
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.Wallet
import com.breadwallet.util.isBitcoin
import com.breadwallet.util.isBitcoinCash
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.transformLatest

private fun List<Wallet>.filterBtcLike(): List<Wallet> =
    filter { wallet ->
        wallet.currency.code.run { isBitcoin() || isBitcoinCash() }
    }

@UseExperimental(ExperimentalCoroutinesApi::class)
fun Flow<Import.F.ValidateKey>.handleValidateKey(
    breadBox: BreadBox
): Flow<Import.E.Key> = mapLatest { effect ->
    val keyBytes = effect.privateKey.toByteArray()
    val passwordBytes = effect.password?.toByteArray() ?: byteArrayOf()

    val btcWallets = breadBox.wallets()
        .first()
        .filterBtcLike()

    when {
        // Sweeping only supports BTC and BCH, ensure one is active.
        btcWallets.isEmpty() -> Import.E.Key.NoWallets
        passwordBytes.isNotEmpty() -> when {
            Key.createFromPrivateKeyString(keyBytes, passwordBytes).isPresent ->
                Import.E.Key.OnValid(true)
            else ->
                Import.E.Key.OnPasswordInvalid
        }
        passwordBytes.isEmpty() && Key.isProtectedPrivateKeyString(keyBytes) ->
            Import.E.Key.OnValid(true)
        Key.createFromPrivateKeyString(keyBytes).isPresent ->
            Import.E.Key.OnValid()
        else -> Import.E.Key.OnInvalid
    }
}

@UseExperimental(ExperimentalCoroutinesApi::class)
fun Flow<Import.F.EstimateImport>.handleEstimateImport(
    breadBox: BreadBox,
    walletImporter: WalletImporter
): Flow<Import.E> = transformLatest { effect ->
    val privateKey = effect.privateKey.toByteArray()
    val password = when (effect) {
        is Import.F.EstimateImport.KeyWithPassword ->
            effect.password.toByteArray()
        else -> null
    }

    walletImporter.setKey(privateKey, password)

    val btcWallets = breadBox.wallets().first().filterBtcLike()

    check(btcWallets.isNotEmpty()) {
        "Import requires an active BTC or BCH wallet."
    }

    val walletFound = btcWallets.asFlow()
        .map { wallet -> walletImporter.prepareSweeper(wallet) }
        .filterIsInstance<WalletImporter.PrepareResult.WalletFound>()
        .take(1)
        .singleOrNull()

    if (walletFound == null) {
        emit(Import.E.Estimate.NoBalance)
        return@transformLatest
    }

    val result = when (val result = walletImporter.estimateFee()) {
        is WalletImporter.FeeResult.Success ->
            Import.E.Estimate.Success(
                walletFound.balance,
                result.feeBasis.fee,
                walletFound.currencyCode
            )
        is WalletImporter.FeeResult.InsufficientFunds ->
            Import.E.Estimate.BalanceTooLow(walletFound.balance.toBigDecimal())
        is WalletImporter.FeeResult.Failed ->
            Import.E.Estimate.FeeError(walletFound.balance.toBigDecimal())
        else ->
            Import.E.Estimate.FeeError(walletFound.balance.toBigDecimal())
    }
    emit(result)
}

@UseExperimental(ExperimentalCoroutinesApi::class)
fun Flow<Import.F.SubmitImport>.handleSubmitTransfer(
    walletImporter: WalletImporter
): Flow<Import.E> = transformLatest { submitTransfer ->
    val privateKey = submitTransfer.privateKey.toByteArray()
    val password = submitTransfer.password?.toByteArray()
    if (!walletImporter.readyForImport(privateKey, password)) {
        emit(Import.E.RetryImport(submitTransfer.privateKey, submitTransfer.password))
        return@transformLatest
    }

    val transfer = walletImporter.import()
    val result = if (transfer != null) {
        Import.E.Transfer.OnSuccess(
            transferHash = transfer.hashString(),
            currencyCode = transfer.wallet.currency.code
        )
    } else {
        Import.E.Transfer.OnFailed
    }
    emit(result)
}
