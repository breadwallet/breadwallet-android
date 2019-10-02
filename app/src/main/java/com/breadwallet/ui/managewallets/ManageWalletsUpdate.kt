package com.breadwallet.ui.managewallets

import com.spotify.mobius.Update

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next

object ManageWalletsUpdate :
    Update<ManageWalletsModel, ManageWalletsEvent, ManageWalletsEffect>,
    ManageWalletsUpdateSpec {

    override fun update(model: ManageWalletsModel, event: ManageWalletsEvent) = patch(model, event)

    override fun onAddWalletClicked(model: ManageWalletsModel): Next<ManageWalletsModel, ManageWalletsEffect> {
        return dispatch(
            effects(
                ManageWalletsEffect.GoToAddWallet
            )
        )
    }

    override fun onBackClicked(model: ManageWalletsModel): Next<ManageWalletsModel, ManageWalletsEffect> {
        return dispatch(
            effects(
                ManageWalletsEffect.GoBack
            )
        )
    }

    override fun onHideClicked(
        model: ManageWalletsModel,
        event: ManageWalletsEvent.OnHideClicked
    ): Next<ManageWalletsModel, ManageWalletsEffect> {
        return dispatch(
            effects(
                ManageWalletsEffect.UpdateWallet(event.currencyId, false)
            )
        )
    }

    override fun onShowClicked(
        model: ManageWalletsModel,
        event: ManageWalletsEvent.OnShowClicked
    ): Next<ManageWalletsModel, ManageWalletsEffect> {
        return dispatch(
            effects(
                ManageWalletsEffect.UpdateWallet(event.currencyId, true)
            )
        )
    }

    override fun onWalletsUpdated(
        model: ManageWalletsModel,
        event: ManageWalletsEvent.OnWalletsUpdated
    ): Next<ManageWalletsModel, ManageWalletsEffect> {
        return next(
            model.copy(
                wallets = event.wallets
            )
        )
    }
}