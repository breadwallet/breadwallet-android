/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/14/20.
 * Copyright (c) 2020 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.settings

import com.spotify.mobius.Next

interface SettingsScreenUpdateSpec {
    fun patch(model: SettingsScreen.M, event: SettingsScreen.E): Next<SettingsScreen.M, SettingsScreen.F> = when (event) {
        SettingsScreen.E.OnBackClicked -> onBackClicked(model)
        SettingsScreen.E.OnCloseClicked -> onCloseClicked(model)
        SettingsScreen.E.OnAuthenticated -> onAuthenticated(model)
        SettingsScreen.E.OnWalletsUpdated -> onWalletsUpdated(model)
        SettingsScreen.E.ShowHiddenOptions -> showHiddenOptions(model)
        SettingsScreen.E.OnCloseHiddenMenu -> onCloseHiddenMenu(model)
        is SettingsScreen.E.OnLinkScanned -> onLinkScanned(model, event)
        is SettingsScreen.E.OnOptionClicked -> onOptionClicked(model, event)
        is SettingsScreen.E.OnOptionsLoaded -> onOptionsLoaded(model, event)
        is SettingsScreen.E.ShowPhrase -> showPhrase(model, event)
        is SettingsScreen.E.SetApiServer -> setApiServer(model, event)
        is SettingsScreen.E.SetPlatformDebugUrl -> setPlatformDebugUrl(model, event)
        is SettingsScreen.E.SetPlatformBundle -> setPlatformBundle(model, event)
        is SettingsScreen.E.SetTokenBundle -> setTokenBundle(model, event)
        is SettingsScreen.E.OnATMMapClicked -> onATMMapClicked(model, event)
    }

    fun onBackClicked(model: SettingsScreen.M): Next<SettingsScreen.M, SettingsScreen.F>

    fun onCloseClicked(model: SettingsScreen.M): Next<SettingsScreen.M, SettingsScreen.F>

    fun onAuthenticated(model: SettingsScreen.M): Next<SettingsScreen.M, SettingsScreen.F>

    fun onWalletsUpdated(model: SettingsScreen.M): Next<SettingsScreen.M, SettingsScreen.F>

    fun showHiddenOptions(model: SettingsScreen.M): Next<SettingsScreen.M, SettingsScreen.F>

    fun onCloseHiddenMenu(model: SettingsScreen.M): Next<SettingsScreen.M, SettingsScreen.F>

    fun onLinkScanned(model: SettingsScreen.M, event: SettingsScreen.E.OnLinkScanned): Next<SettingsScreen.M, SettingsScreen.F>

    fun onOptionClicked(model: SettingsScreen.M, event: SettingsScreen.E.OnOptionClicked): Next<SettingsScreen.M, SettingsScreen.F>

    fun onOptionsLoaded(model: SettingsScreen.M, event: SettingsScreen.E.OnOptionsLoaded): Next<SettingsScreen.M, SettingsScreen.F>

    fun showPhrase(model: SettingsScreen.M, event: SettingsScreen.E.ShowPhrase): Next<SettingsScreen.M, SettingsScreen.F>

    fun setApiServer(model: SettingsScreen.M, event: SettingsScreen.E.SetApiServer): Next<SettingsScreen.M, SettingsScreen.F>

    fun setPlatformDebugUrl(model: SettingsScreen.M, event: SettingsScreen.E.SetPlatformDebugUrl): Next<SettingsScreen.M, SettingsScreen.F>

    fun setPlatformBundle(model: SettingsScreen.M, event: SettingsScreen.E.SetPlatformBundle): Next<SettingsScreen.M, SettingsScreen.F>

    fun setTokenBundle(model: SettingsScreen.M, event: SettingsScreen.E.SetTokenBundle): Next<SettingsScreen.M, SettingsScreen.F>

    fun onATMMapClicked(model: SettingsScreen.M, event: SettingsScreen.E.OnATMMapClicked): Next<SettingsScreen.M, SettingsScreen.F>
}