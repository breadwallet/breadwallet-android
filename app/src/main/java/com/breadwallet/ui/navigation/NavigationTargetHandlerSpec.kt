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
package com.breadwallet.ui.navigation

interface NavigationTargetHandlerSpec {
    fun patch(effect: NavigationTarget): Unit = when (effect) {
        NavigationTarget.Back -> back()
        NavigationTarget.BrdRewards -> brdRewards()
        NavigationTarget.ReviewBrd -> reviewBrd()
        NavigationTarget.QRScanner -> qRScanner()
        NavigationTarget.BrdLogin -> brdLogin()
        NavigationTarget.Home -> home()
        NavigationTarget.Buy -> buy()
        NavigationTarget.Trade -> trade()
        NavigationTarget.AddWallet -> addWallet()
        NavigationTarget.DisabledScreen -> disabledScreen()
        NavigationTarget.NativeApiExplorer -> nativeApiExplorer()
        NavigationTarget.TransactionComplete -> transactionComplete()
        NavigationTarget.About -> about()
        NavigationTarget.DisplayCurrency -> displayCurrency()
        NavigationTarget.NotificationsSettings -> notificationsSettings()
        NavigationTarget.ShareDataSettings -> shareDataSettings()
        NavigationTarget.FingerprintSettings -> fingerprintSettings()
        NavigationTarget.WipeWallet -> wipeWallet()
        NavigationTarget.OnBoarding -> onBoarding()
        is NavigationTarget.ImportWallet -> importWallet(effect)
        NavigationTarget.BitcoinNodeSelector -> bitcoinNodeSelector()
        NavigationTarget.EnableSegWit -> enableSegWit()
        NavigationTarget.LegacyAddress -> legacyAddress()
        is NavigationTarget.SendSheet -> sendSheet(effect)
        is NavigationTarget.ReceiveSheet -> receiveSheet(effect)
        is NavigationTarget.ViewTransaction -> viewTransaction(effect)
        is NavigationTarget.DeepLink -> deepLink(effect)
        is NavigationTarget.GoToInAppMessage -> goToInAppMessage(effect)
        is NavigationTarget.Wallet -> wallet(effect)
        is NavigationTarget.SupportPage -> supportPage(effect)
        is NavigationTarget.SetPin -> setPin(effect)
        is NavigationTarget.AlertDialog -> alertDialog(effect)
        is NavigationTarget.Authentication -> authentication(effect)
        is NavigationTarget.WriteDownKey -> writeDownKey(effect)
        is NavigationTarget.PaperKey -> paperKey(effect)
        is NavigationTarget.PaperKeyProve -> paperKeyProve(effect)
        is NavigationTarget.Menu -> menu(effect)
        is NavigationTarget.SyncBlockchain -> syncBlockchain(effect)
        is NavigationTarget.FastSync -> fastSync(effect)
        is NavigationTarget.ATMMap -> aTMMap(effect)
        is NavigationTarget.Signal -> signal(effect)
        NavigationTarget.LogcatViewer -> logcatViewer()
        NavigationTarget.MetadataViewer -> metadataViewer()
        is NavigationTarget.Staking -> staking(effect)
        is NavigationTarget.CreateGift -> createGift(effect)
        is NavigationTarget.ShareGift -> shareGift(effect)
    }

    fun metadataViewer()

    fun logcatViewer()

    fun back(): Unit

    fun brdRewards(): Unit

    fun reviewBrd(): Unit

    fun qRScanner(): Unit

    fun brdLogin(): Unit

    fun home(): Unit

    fun buy(): Unit

    fun trade(): Unit

    fun addWallet(): Unit

    fun disabledScreen(): Unit

    fun nativeApiExplorer(): Unit

    fun transactionComplete(): Unit

    fun about(): Unit

    fun displayCurrency(): Unit

    fun notificationsSettings(): Unit

    fun shareDataSettings(): Unit

    fun fingerprintSettings(): Unit

    fun wipeWallet(): Unit

    fun onBoarding(): Unit

    fun importWallet(effect: NavigationTarget.ImportWallet): Unit

    fun bitcoinNodeSelector(): Unit

    fun enableSegWit(): Unit

    fun legacyAddress(): Unit

    fun sendSheet(effect: NavigationTarget.SendSheet): Unit

    fun receiveSheet(effect: NavigationTarget.ReceiveSheet): Unit

    fun viewTransaction(effect: NavigationTarget.ViewTransaction): Unit

    fun deepLink(effect: NavigationTarget.DeepLink): Unit

    fun goToInAppMessage(effect: NavigationTarget.GoToInAppMessage): Unit

    fun wallet(effect: NavigationTarget.Wallet): Unit

    fun supportPage(effect: NavigationTarget.SupportPage): Unit

    fun setPin(effect: NavigationTarget.SetPin): Unit

    fun alertDialog(effect: NavigationTarget.AlertDialog): Unit

    fun authentication(effect: NavigationTarget.Authentication): Unit

    fun writeDownKey(effect: NavigationTarget.WriteDownKey): Unit

    fun paperKey(effect: NavigationTarget.PaperKey): Unit

    fun paperKeyProve(effect: NavigationTarget.PaperKeyProve): Unit

    fun menu(effect: NavigationTarget.Menu): Unit

    fun syncBlockchain(effect: NavigationTarget.SyncBlockchain): Unit

    fun fastSync(effect: NavigationTarget.FastSync): Unit

    fun aTMMap(effect: NavigationTarget.ATMMap): Unit

    fun signal(effect: NavigationTarget.Signal): Unit

    fun staking(effect: NavigationTarget.Staking): Unit

    fun createGift(effect: NavigationTarget.CreateGift): Unit

    fun shareGift(effect: NavigationTarget.ShareGift): Unit
}
