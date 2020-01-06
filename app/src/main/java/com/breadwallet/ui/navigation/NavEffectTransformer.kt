package com.breadwallet.ui.navigation

import com.breadwallet.ext.throttleFirst
import com.spotify.mobius.Connectable
import com.spotify.mobius.flow.FlowTransformer
import com.spotify.mobius.flow.transform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val NAVIGATION_THROTTLE_MS = 300L

class NavEffectTransformer(
    private val navHandlerProvider: () -> RouterNavigationEffectHandler
) : FlowTransformer<NavEffectHolder, Nothing> {
    override fun invoke(effects: Flow<NavEffectHolder>): Flow<Nothing> =
        effects.map { effect -> effect.navigationEffect }
            .throttleFirst(NAVIGATION_THROTTLE_MS)
            .transform(Connectable { navHandlerProvider() })
}
