package com.breadwallet.wallet.wallets.etherium;

import android.content.Context;
import android.util.Log;

import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumTransaction;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseTransaction;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoTransaction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    private static final String TAG = WalletTokenManager.class.getSimpleName();

    private WalletEthManager mWalletEthManager;
    private static Map<String, WalletTokenManager> mTokenWallets = new HashMap<>();
    private BREthereumWallet mWalletToken;

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();
    private WalletUiConfiguration uiConfig;

    private WalletTokenManager(WalletEthManager walletEthManager, BREthereumWallet tokenWallet) {
        this.mWalletEthManager = walletEthManager;
        this.mWalletToken = tokenWallet;
        uiConfig = new WalletUiConfiguration(tokenWallet.getToken().getColorLeft(), tokenWallet.getToken().getColorRight(), false);

    }

    private synchronized static WalletTokenManager getTokenWallet(WalletEthManager walletEthManager, String contractAddress) {
        if (mTokenWallets.containsKey(contractAddress.toLowerCase())) {
            return mTokenWallets.get(contractAddress.toLowerCase());
        } else {
            BREthereumToken tk = BREthereumToken.lookup(contractAddress);
            if (tk == null) {
                BRReportsManager.reportBug(new NullPointerException("Failed to lookup for token: " + contractAddress));
                return null;
            }
            BREthereumWallet w = walletEthManager.node.getWallet(tk);

            if (w != null) {
                if (w.getToken() == null) {
                    BRReportsManager.reportBug(new NullPointerException("getToken is null:" + contractAddress));
                    return null;
                }
                WalletTokenManager token = new WalletTokenManager(walletEthManager, w);
                mTokenWallets.put(w.getToken().getAddress().toLowerCase(), token);
                return token;
            } else {
                BRReportsManager.reportBug(new NullPointerException("Failed to find token by address: " + contractAddress), true);
            }

        }
        return null;
    }

    public static WalletTokenManager getBrdWallet(WalletEthManager walletEthManager) {
        BREthereumWallet brdWallet = walletEthManager.node.getWallet(BREthereumToken.tokenBRD);
        if (brdWallet.getToken() == null) {
            BRReportsManager.reportBug(new NullPointerException("getBrd failed"));
            return null;
        }
        if (mTokenWallets.containsKey(brdWallet.getToken().getAddress())) {
            return mTokenWallets.get(brdWallet.getToken().getAddress());
        } else {
            WalletTokenManager wt = new WalletTokenManager(walletEthManager, brdWallet);
            mTokenWallets.put(brdWallet.getToken().getAddress(), wt);
            return wt;
        }
    }

    public synchronized static WalletTokenManager getTokenWalletByIso(WalletEthManager walletEthManager, String iso) {
        for (BREthereumToken t : BREthereumToken.tokens) {
            if (t.getSymbol().toLowerCase().equalsIgnoreCase(iso.toLowerCase())) {
                return getTokenWallet(walletEthManager, t.getAddress());
            }
        }
        BRReportsManager.reportBug(new NullPointerException("Failed to getTokenWalletByIso: " + iso));
        return null;
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
        return new BigDecimal(tx.getEtherTx().getGasLimit())
                .multiply(new BigDecimal(tx.getEtherTx().getGasPrice(BREthereumAmount.Unit.ETHER_WEI)));
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        BigDecimal fee;
        if (amount == null) return null;
        if (amount.compareTo(new BigDecimal(0)) == 0) {
            fee = new BigDecimal(0);
        } else {
            fee = new BigDecimal(mWalletToken.transactionEstimatedFee(amount.toPlainString()));
        }
        return fee;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return null;
    }

    @Override
    public String getTxAddress(BaseTransaction tx) {
        return mWalletEthManager.getTxAddress(tx);
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        return mWalletEthManager.getMaxOutputAmount(app);
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return mWalletEthManager.getMinOutputAmount(app);
    }

    @Override
    public BigDecimal getTransactionAmount(BaseTransaction tx) {
        return mWalletEthManager.getTransactionAmount(tx);
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return mWalletEthManager.getMinOutputAmountPossible();
    }

    @Override
    public void updateFee(Context app) {
        //no need
    }

    @Override
    public void refreshAddress(Context app) {
        if (Utils.isNullOrEmpty(BRSharedPrefs.getReceiveAddress(app, getIso(app)))) {
            String address = getReceiveAddress(app);
            if (Utils.isNullOrEmpty(address)) {
                Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
                BRReportsManager.reportBug(new NullPointerException("empty address!"));
            }
            BRSharedPrefs.putReceiveAddress(app, address, getIso(app));
        }
    }

    @Override
    public void refreshCachedBalance(final Context app) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BigDecimal balance = new BigDecimal(mWalletToken.getBalance());
                BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
            }
        });
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        //todo implement
        return null;
    }

    @Override
    public boolean containsAddress(String address) {
        return mWalletEthManager.containsAddress(address);
    }

    @Override
    public boolean addressIsUsed(String address) {
        return mWalletEthManager.addressIsUsed(address);
    }

    @Override
    public boolean generateWallet(Context app) {
        return false;
    }

    @Override
    public String getSymbol(Context app) {
        return mWalletToken.getToken().getSymbol();
    }

    @Override
    public String getIso(Context app) {
        return mWalletToken.getToken().getSymbol();
    }

    @Override
    public String getScheme(Context app) {
        return null;
    }

    @Override
    public String getName(Context app) {
        return mWalletToken.getToken().getName();
    }

    @Override
    public String getDenomination(Context app) {
        return null;
    }

    @Override
    public String getReceiveAddress(Context app) {
        return mWalletToken.getToken().getAddress();
    }

    @Override
    public BaseTransaction createTransaction(BigDecimal amount, String address) {
        BREthereumTransaction tx = mWalletToken.createTransaction(address, amount.toPlainString(), BREthereumAmount.Unit.ETHER_WEI);
        return new CryptoTransaction(tx);
    }

    @Override
    public String decorateAddress(Context app, String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(Context app, String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return 5;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso(app));
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return mWalletEthManager.getTotalSent(app);
    }

    @Override
    public void wipeData(Context app) {
        //Not needed for Tokens
    }

    @Override
    public void syncStarted() {
        //Not needed for Tokens
    }

    @Override
    public void syncStopped(String error) {
        //Not needed for Tokens
    }

    @Override
    public boolean networkIsReachable() {
        return mWalletEthManager.networkIsReachable();
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
        for (OnBalanceChangedListener listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged(getIso(app), balance);
        }
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return mWalletEthManager.getMaxAmount(app);
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        //no settings for tokens, so empty
        return new WalletSettingsConfiguration();
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        return new BigDecimal(0);
    }
}
