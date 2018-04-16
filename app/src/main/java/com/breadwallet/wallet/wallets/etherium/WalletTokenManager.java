package com.breadwallet.wallet.wallets.etherium;

import android.content.Context;

import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseAddress;
import com.breadwallet.wallet.abstracts.BaseTransaction;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/13/18.
 * Copyright (c) 2018 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class WalletTokenManager implements BaseWalletManager {

    private WalletEthManager mWalletEthManager;
    private BREthereumWallet mWalletToken ;


    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    public WalletTokenManager(WalletEthManager walletEthManager, String contractAddress) {
        this.mWalletEthManager = walletEthManager;
       mWalletToken = walletEthManager.getNode().createWallet(BREthereumToken.tokenBRD);
    }

    @Override
    public int getForkId() {
        //no need for Tokens
        return -1;
    }

    @Override
    public boolean isAddressValid(String address) {
        return mWalletEthManager.isAddressValid(address);
    }

    @Override
    public byte[] signAndPublishTransaction(BaseTransaction tx, byte[] seed) {
        return mWalletEthManager.signAndPublishTransaction(tx, seed);
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        if (listener != null && !balanceListeners.contains(listener))
            balanceListeners.add(listener);
    }

    @Override
    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener list) {
        if (list != null && !txStatusUpdatedListeners.contains(list))
            txStatusUpdatedListeners.add(list);
    }

    @Override
    public void addSyncListeners(SyncListener list) {
        if (list != null && !syncListeners.contains(list))
            syncListeners.add(list);
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified list) {
        if (list != null && !txModifiedListeners.contains(list))
            txModifiedListeners.add(list);
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        return -1;
    }

    @Override
    public double getSyncProgress(long startHeight) {
        return mWalletEthManager.getSyncProgress(startHeight);
    }

    @Override
    public double getConnectStatus() {
        return mWalletEthManager.getConnectStatus();
    }

    @Override
    public void connect(Context app) {
        //no need for Tokens
    }

    @Override
    public void disconnect(Context app) {
        //no need for Tokens
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        //no need for tokens
        return false;
    }

    @Override
    public void rescan() {
        //no need for tokens
    }

    @Override
    public BaseTransaction[] getTxs() {
        return (BaseTransaction[]) mWalletToken.getTransactions();
    }

    @Override
    public BigDecimal getTxFee(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        return null;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return null;
    }

    @Override
    public BaseAddress getTxAddress(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        return null;
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return null;
    }

    @Override
    public BigDecimal getTransactionAmount(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return null;
    }

    @Override
    public void updateFee(Context app) {

    }

    @Override
    public void refreshAddress(Context app) {

    }

    @Override
    public void refreshCachedBalance(Context app) {

    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        return null;
    }

    @Override
    public boolean containsAddress(String address) {
        return false;
    }

    @Override
    public boolean addressIsUsed(String address) {
        return false;
    }

    @Override
    public BaseAddress createAddress(String address) {
        return null;
    }

    @Override
    public boolean generateWallet(Context app) {
        return false;
    }

    @Override
    public String getSymbol(Context app) {
        return null;
    }

    @Override
    public String getIso(Context app) {
        return null;
    }

    @Override
    public String getScheme(Context app) {
        return null;
    }

    @Override
    public String getName(Context app) {
        return null;
    }

    @Override
    public String getDenomination(Context app) {
        return null;
    }

    @Override
    public BaseAddress getReceiveAddress(Context app) {
        return null;
    }

    @Override
    public BaseTransaction createTransaction(BigDecimal amount, String address) {
        return null;
    }

    @Override
    public String decorateAddress(Context app, String addr) {
        return null;
    }

    @Override
    public String undecorateAddress(Context app, String addr) {
        return null;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return 0;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return null;
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return null;
    }

    @Override
    public void wipeData(Context app) {

    }

    @Override
    public void syncStarted() {

    }

    @Override
    public void syncStopped(String error) {

    }

    @Override
    public boolean networkIsReachable() {
        return false;
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {

    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return null;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return null;
    }

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return null;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        return null;
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        return null;
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        return null;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        return null;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        return null;
    }
}
