package com.breadwallet.ui.addwallets

import com.spotify.mobius.Update

import com.spotify.mobius.Effects.effects
import com.spotify.mobius.Next
import com.spotify.mobius.Next.dispatch
import com.spotify.mobius.Next.next

object AddWalletsUpdate :
    Update<AddWalletsModel, AddWalletsEvent, AddWalletsEffect>,
    AddWalletsUpdateSpec {

    override fun update(model: AddWalletsModel, event: AddWalletsEvent) = patch(model, event)

    override fun onBackClicked(model: AddWalletsModel): Next<AddWalletsModel, AddWalletsEffect> {
        return dispatch(
            effects(
                AddWalletsEffect.GoBack
            )
        )
    }

    override fun onSearchQueryChanged(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnSearchQueryChanged
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return next(
            model.copy(searchQuery = event.query),
            effects(
                AddWalletsEffect.SearchTokens(event.query)
            )
        )
    }

    override fun onTokensChanged(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnTokensChanged
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return next(
            model.copy(
                tokens = event.tokens.toMutableList().apply { sortBy { it.currencyCode } }
            )
        )
    }

    override fun onAddWalletClicked(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnAddWalletClicked
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return dispatch(
            effects(
                AddWalletsEffect.AddWallet(event.token)
            )
        )
    }

    override fun onRemoveWalletClicked(
        model: AddWalletsModel,
        event: AddWalletsEvent.OnRemoveWalletClicked
    ): Next<AddWalletsModel, AddWalletsEffect> {
        return dispatch(
            effects(
                AddWalletsEffect.RemoveWallet(event.token)
            )
        )
    }
}
