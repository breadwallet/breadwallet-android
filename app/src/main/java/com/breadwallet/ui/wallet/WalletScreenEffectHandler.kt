package com.breadwallet.ui.wallet

import android.content.Context
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.sqlite.RatesDataSource
import com.breadwallet.tools.util.EventUtils
import com.breadwallet.wallet.WalletsMaster
import com.breadwallet.wallet.abstracts.BaseWalletManager
import com.platform.util.AppReviewPromptManager
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer


class WalletScreenEffectHandler(
        private val output: Consumer<WalletScreenEvent>
) : Connection<WalletScreenEffect> {

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.LoadCryptoPreferred -> {
                val isCryptoPreferred = BRSharedPrefs.isCryptoPreferred()
                output.accept(WalletScreenEvent.OnIsCryptoPreferredLoaded(isCryptoPreferred))
            }
            is WalletScreenEffect.UpdateCryptoPreferred -> {
                EventUtils.pushEvent(EventUtils.EVENT_AMOUNT_SWAP_CURRENCY) // TODO: Is this needed?
                BRSharedPrefs.setIsCryptoPreferred(b = value.cryptoPreferred)
            }
        }
    }

    override fun dispose() {
    }
}


class WalletReviewPromptHandler(
        private val output: Consumer<WalletScreenEvent>,
        private val context: Context,
        private val currencyCode: String
) : Connection<WalletScreenEffect> {

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.CheckReviewPrompt -> {
                val showPrompt = AppReviewPromptManager.showReview(context, currencyCode, value.transactions)
                if (showPrompt) {
                    output.accept(WalletScreenEvent.OnShowReviewPrompt)
                }
            }
            WalletScreenEffect.RecordReviewPrompt -> EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISPLAYED)
            WalletScreenEffect.RecordReviewPromptDismissed -> {
                EventUtils.pushEvent(EventUtils.EVENT_REVIEW_PROMPT_DISMISSED)
                AppReviewPromptManager.onReviewPromptDismissed(context)
            }
        }
    }

    override fun dispose() {
    }
}

class WalletRatesHandler(
        private val output : Consumer<WalletScreenEvent>,
        private val context: Context
) : Connection<WalletScreenEffect>,
        RatesDataSource.OnDataChanged {

    private val walletManager: BaseWalletManager =
            WalletsMaster.getInstance().getCurrentWallet(context)

    init {
        RatesDataSource.getInstance(context).addOnDataChangedListener(this)
    }

    override fun accept(value: WalletScreenEffect) {
        when (value) {
            is WalletScreenEffect.LoadFiatPricePerUnit -> loadFiatPerPriceUnit()
        }
    }

    override fun onChanged() {
        loadFiatPerPriceUnit()
    }

    private fun loadFiatPerPriceUnit()  {
        val fiatPricePerUnit = walletManager.getFiatExchangeRate(context)
        output.accept(WalletScreenEvent.OnFiatPricePerUpdated(fiatPricePerUnit.toFloat()))
    }

    override fun dispose() {
        RatesDataSource.getInstance(context).removeOnDataChangedListener(this)
    }
}