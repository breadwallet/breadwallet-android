package com.breadwallet.ui.settings.currency

import androidx.recyclerview.widget.LinearLayoutManager
import com.breadwallet.R
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.clicks
import com.spotify.mobius.flow.FlowTransformer
import kotlinx.android.synthetic.main.controller_display_currency.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class DisplayCurrencyController :
    BaseMobiusController<DisplayCurrency.M, DisplayCurrency.E, DisplayCurrency.F>() {

    override val layoutId = R.layout.controller_display_currency

    override val defaultModel = DisplayCurrency.M.createDefault()
    override val init = DisplayCurrencyInit
    override val update = CurrencyUpdate
    override val flowEffectHandler: FlowTransformer<DisplayCurrency.F, DisplayCurrency.E>?
        get() = DisplayCurrencyEffects.createEffectHandler(direct.instance())

    override fun bindView(modelFlow: Flow<DisplayCurrency.M>): Flow<DisplayCurrency.E> {
        currency_list.layoutManager = LinearLayoutManager(checkNotNull(activity))
        return merge(
            back_button.clicks().map { DisplayCurrency.E.OnBackClicked },
            faq_button.clicks().map { DisplayCurrency.E.OnFaqClicked },
            bindCurrencyList(modelFlow)
        )
    }

    private fun bindCurrencyList(
        modelFlow: Flow<DisplayCurrency.M>
    ) = callbackFlow<DisplayCurrency.E> {
        currency_list.adapter = FiatCurrencyAdapter(
            modelFlow
                .map { model -> model.currencies }
                .distinctUntilChanged(),
            modelFlow
                .map { model -> model.selectedCurrency }
                .distinctUntilChanged(),
            sendChannel = channel
        )
        awaitClose { currency_list.adapter = null }
    }
}
