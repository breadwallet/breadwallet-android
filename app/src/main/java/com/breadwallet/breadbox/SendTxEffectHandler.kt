package com.breadwallet.breadbox

import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Transfer
import com.breadwallet.crypto.TransferFeeBasis
import com.breadwallet.logger.logError
import com.breadwallet.tools.security.KeyStore
import com.breadwallet.util.CurrencyCode
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import java.math.BigDecimal

object SendTx {
    data class Effect(
        val currencyCode: CurrencyCode,
        val address: String,
        val amount: BigDecimal,
        val transferFeeBasis: TransferFeeBasis
    ) {
        override fun toString(): String {
            return "SendTransaction(" +
                "currencyCode='$currencyCode', " +
                "address='***', " +
                "amount=$amount, " +
                "transferFeeBasis=$transferFeeBasis)"
        }
    }

    sealed class Result {
        data class Success(val transfer: Transfer) : Result()
        data class Error(val error: Throwable) : Result()
    }
}

/**
 * [SendTxEffectHandler] handles only [SendTx.Effect] and
 * produces a [SendTx.Result].
 *
 * This operation is launched in [retainedScope] which will
 * not be cancelled when [Connection.dispose] is called.
 *
 * [outputProducer] must return a valid [Consumer] that
 * can accept events even if the original loop has been
 * disposed.
 */
class SendTxEffectHandler(
    private val outputProducer: Consumer<SendTx.Result>,
    private val retainedScope: CoroutineScope,
    private val breadBox: BreadBox,
    private val keyStore: KeyStore
) : Connection<SendTx.Effect> {

    override fun accept(effect: SendTx.Effect) {
        breadBox.wallet(effect.currencyCode)
            .take(1)
            .flatMapLatest { wallet ->
                wallet.sendTransfer(
                    checkNotNull(keyStore.getPhrase()),
                    effect.address,
                    Amount.create(effect.amount.toDouble(), wallet.unit),
                    effect.transferFeeBasis
                )
            }
            .map { SendTx.Result.Success(it) as SendTx.Result }
            .catch { error ->
                logError("Failed to send transaction", error)
                emit(SendTx.Result.Error(error))
            }
            .onEach { outputProducer.accept(it) }
            .launchIn(retainedScope)
    }

    override fun dispose() = Unit
}
