package com.breadwallet.presenter.activities

import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper

import com.breadwallet.R
import com.breadwallet.tools.adapter.ManageWalletListAdapter
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback
import com.breadwallet.tools.listeners.OnStartDragListener
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.global.effect.NavigationEffect
import com.breadwallet.ui.global.effect.RouterNavigationEffectHandler
import com.breadwallet.ui.managewallets.ManageWalletsEffect
import com.breadwallet.ui.managewallets.ManageWalletsEffectHandler
import com.breadwallet.ui.managewallets.ManageWalletsEvent
import com.breadwallet.ui.managewallets.ManageWalletsInit
import com.breadwallet.ui.managewallets.ManageWalletsModel
import com.breadwallet.ui.managewallets.ManageWalletsUpdate
import com.breadwallet.ui.managewallets.Wallet
import com.breadwallet.ui.util.CompositeEffectHandler
import com.breadwallet.ui.util.logDebug
import com.breadwallet.ui.util.nestedConnectable
import com.spotify.mobius.Connectable
import com.spotify.mobius.disposables.Disposable
import com.spotify.mobius.functions.Consumer
import kotlinx.android.synthetic.main.activity_manage_wallets.*
import org.kodein.di.direct
import org.kodein.di.erased.instance

class ManageWalletsController :
    BaseMobiusController<ManageWalletsModel, ManageWalletsEvent, ManageWalletsEffect>(),
    OnStartDragListener {

    override val layoutId = R.layout.activity_manage_wallets

    override val defaultModel = ManageWalletsModel.createDefault()
    override val init = ManageWalletsInit
    override val update = ManageWalletsUpdate
    override val effectHandler: Connectable<ManageWalletsEffect, ManageWalletsEvent> =
        CompositeEffectHandler.from(
            Connectable { output ->
                ManageWalletsEffectHandler(
                    output,
                    direct.instance(),
                    direct.instance()
                )
            },
            nestedConnectable({ direct.instance<RouterNavigationEffectHandler>() }, { effect ->
                when (effect) {
                    is ManageWalletsEffect.GoToAddWallet -> NavigationEffect.GoToAddWallet
                    is ManageWalletsEffect.GoBack -> NavigationEffect.GoBack
                    else -> null
                }
            })
        )

    private var mAdapter: ManageWalletListAdapter? = null
    private var mItemTouchHelper: ItemTouchHelper? = null

    override fun bindView(output: Consumer<ManageWalletsEvent>): Disposable {
        mAdapter = ManageWalletListAdapter(
            activity!!,
            object : ManageWalletListAdapter.OnWalletShowOrHideListener {
                override fun onShow(wallet: Wallet) {
                    output.accept(ManageWalletsEvent.OnShowClicked(wallet.currencyId))
                }

                override fun onHide(wallet: Wallet) {
                    output.accept(ManageWalletsEvent.OnHideClicked(wallet.currencyId))
                }
            },
            { output.accept(ManageWalletsEvent.OnAddWalletClicked) },
            this,
            { wallets ->
                output.accept(
                    ManageWalletsEvent.OnWalletsReorder(
                        wallets.map { it.currencyId }
                    )
                )
            }
        )

        token_list.layoutManager = LinearLayoutManager(activity)
        token_list.adapter = mAdapter

        val callback = SimpleItemTouchHelperCallback(mAdapter)
        mItemTouchHelper = ItemTouchHelper(callback).apply { attachToRecyclerView(token_list) }
        back_button.setOnClickListener {
            output.accept(ManageWalletsEvent.OnBackClicked)
        }

        return Disposable {
        }
    }

    override fun ManageWalletsModel.render() {
        val adapter = checkNotNull(mAdapter)

        ifChanged(ManageWalletsModel::wallets) {
            adapter.wallets = wallets
        }
    }

    override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
        mItemTouchHelper!!.startDrag(viewHolder)
    }
}
