package com.breadwallet.ui.addwallets

import android.content.Context
import android.support.v7.widget.LinearLayoutManager
import android.view.View

import com.breadwallet.R
import com.breadwallet.mobius.CompositeEffectHandler
import com.breadwallet.mobius.nestedConnectable
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.navigation.NavigationEffect
import com.breadwallet.ui.navigation.RouterNavigationEffectHandler
import com.breadwallet.ui.view
import com.spotify.mobius.Connectable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_add_wallets.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class AddWalletsController :
    BaseMobiusController<AddWalletsModel, AddWalletsEvent, AddWalletsEffect>() {

    override val layoutId: Int = R.layout.activity_add_wallets

    override val defaultModel = AddWalletsModel.createDefault()
    override val init = AddWalletsInit
    override val update = AddWalletsUpdate
    override val effectHandler: Connectable<AddWalletsEffect, AddWalletsEvent> =
        CompositeEffectHandler.from(
            Connectable { output ->
                AddWalletsEffectHandler(
                    output,
                    direct.instance(),
                    direct.instance()
                ) { activity as Context }
            },
            nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
                when (effect) {
                    is AddWalletsEffect.GoBack -> NavigationEffect.GoBack
                    else -> null
                }
            })
        )

    private var mAdapter: AddTokenListAdapter? = null

    override fun bindView(output: Consumer<AddWalletsEvent>) = output.view {
        mAdapter = AddTokenListAdapter(
            activity!!,
            { output.accept(AddWalletsEvent.OnAddWalletClicked(it)) },
            { output.accept(AddWalletsEvent.OnRemoveWalletClicked(it)) }
        )

        token_list.layoutManager = LinearLayoutManager(activity!!)
        token_list.adapter = mAdapter

        search_edit.onTextChanged(AddWalletsEvent::OnSearchQueryChanged)
        search_edit.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                Utils.hideKeyboard(activity)
            }
        }

        back_arrow.onClick(AddWalletsEvent.OnBackClicked)

        onDispose { }
    }

    override fun onDetach(view: View) {
        super.onDetach(view)
        Utils.hideKeyboard(activity)
    }

    override fun AddWalletsModel.render() {
        val adapter = checkNotNull(mAdapter)

        ifChanged(AddWalletsModel::tokens) {
            adapter.tokens = tokens
        }
    }
}
