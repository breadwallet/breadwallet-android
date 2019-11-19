package com.breadwallet.ui.txdetails

import android.content.Context
import com.breadwallet.logger.logError
import com.breadwallet.repository.RatesRepository
import com.breadwallet.util.CurrencyCode
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.math.BigDecimal

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class TxDetailsEffectHandler(
    private val output: Consumer<TxDetailsEvent>,
    private val context: Context
) : Connection<TxDetailsEffect>, CoroutineScope {

    companion object {
        private const val RATE_UPDATE_MS = 60_000L
    }

    override val coroutineContext =
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                logError("Error in coroutine", throwable)
            }

    override fun accept(effect: TxDetailsEffect) {
        when (effect) {
            is TxDetailsEffect.LoadFiatAmountNow ->
                loadFiatAmountNow(
                    effect.cryptoTransferredAmount,
                    effect.currencyCode,
                    effect.preferredFiatIso
                )
        }
    }

    private fun loadFiatAmountNow(
        cryptoAmount: BigDecimal,
        currencyCode: CurrencyCode,
        fiatIso: String
    ) {
        launch {
            while (isActive) {
                RatesRepository.getInstance(context).getFiatForCrypto(
                    cryptoAmount,
                    currencyCode,
                    fiatIso
                )?.run {
                    output.accept(TxDetailsEvent.OnFiatAmountNowUpdated(this))
                }

                delay(RATE_UPDATE_MS)
            }
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }
}
