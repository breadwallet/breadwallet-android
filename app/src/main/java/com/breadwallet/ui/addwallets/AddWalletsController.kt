package com.breadwallet.ui.addwallets

import android.support.v7.widget.LinearLayoutManager
import com.breadwallet.R
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.textChanges
import kotlinx.android.synthetic.main.activity_add_wallets.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import org.kodein.di.direct
import org.kodein.di.erased.instance

@UseExperimental(ExperimentalCoroutinesApi::class, FlowPreview::class)
class AddWalletsController :
    BaseMobiusController<AddWalletsModel, AddWalletsEvent, AddWalletsEffect>() {

    override val layoutId: Int = R.layout.activity_add_wallets

    override val defaultModel = AddWalletsModel.createDefault()
    override val init = AddWalletsInit
    override val update = AddWalletsUpdate
    override val flowEffectHandler
        get() = AddWalletsEffectHandler.createEffectHandler(
            checkNotNull(applicationContext),
            direct.instance(),
            direct.instance(),
            direct.instance()
        )

    override fun bindView(modelFlow: Flow<AddWalletsModel>): Flow<AddWalletsEvent> {
        token_list.layoutManager = LinearLayoutManager(checkNotNull(activity))
        search_edit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                Utils.hideKeyboard(activity)
            }
        }

        return merge(
            search_edit.textChanges().map { AddWalletsEvent.OnSearchQueryChanged(it) },
            back_arrow.clicks().map { AddWalletsEvent.OnBackClicked },
            bindTokenList(modelFlow)
        ).onCompletion {
            Utils.hideKeyboard(activity)
        }
    }

    private fun bindTokenList(
        modelFlow: Flow<AddWalletsModel>
    ) = callbackFlow<AddWalletsEvent> {
        AddTokenListAdapter(
            context = checkNotNull(activity),
            tokensFlow = modelFlow
                .map { model -> model.tokens }
                .distinctUntilChanged(),
            sendChannel = channel
        ).also(token_list::setAdapter)

        awaitClose { token_list.adapter = null }
    }
}
