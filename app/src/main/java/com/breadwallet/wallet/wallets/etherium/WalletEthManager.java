package com.breadwallet.wallet.wallets.etherium;

import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.core.ethereum.BREthereumLightNode;
import com.breadwallet.core.ethereum.BREthereumNetwork;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.core.ethereum.BREthereumTransaction;
import com.breadwallet.core.ethereum.BREthereumWallet;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseAddress;
import com.breadwallet.wallet.abstracts.BaseTransaction;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.google.firebase.crash.FirebaseCrash;

import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/21/18.
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
public class WalletEthManager implements BaseWalletManager, BREthereumLightNode.ClientJSON_RPC {
    private static final String TAG = WalletEthManager.class.getSimpleName();

    private static String ISO = "ETH";
    public static final String ETH_SCHEME = null;

    private static final String mName = "Ethereum";

    private List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private List<OnTxStatusUpdatedListener> txStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> syncListeners = new ArrayList<>();
    private List<OnTxListModified> txModifiedListeners = new ArrayList<>();

    private static WalletEthManager instance;
    private WalletUiConfiguration uiConfig;
    private final BigDecimal MAX_ETH = new BigDecimal("90000000000000000000000000"); // 90m ETH * 18 (WEI)
    private final BigDecimal WEI_ETH = new BigDecimal("1000000000000000000"); //1ETH = 1000000000000000000 WEI
    private BREthereumWallet mWallet;
    private BREthereumLightNode node; //must be here for JNI stuff, no touchy

    private WalletEthManager(final Context app, BRCoreMasterPubKey masterPubKey, BREthereumNetwork network) {
        uiConfig = new WalletUiConfiguration("#5e70a3", true, true, false, false, false, false);

        String testPaperKey = null;
        try {
            testPaperKey = new String(BRKeyStore.getPhrase(app, 0));
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        //todo change the hardcoded priv key to master pub key when done

        BREthereumLightNode node = new BREthereumLightNode.JSON_RPC(this, network, testPaperKey);

        mWallet = node.getWallet();
        mWallet.setDefaultUnit(BREthereumAmount.Unit.ETHER_WEI);

        BREthereumWallet walletToken = node.createWallet(BREthereumToken.tokenBRD);
        walletToken.setDefaultUnit(BREthereumAmount.Unit.TOKEN_DECIMAL);

    }

    public synchronized static WalletEthManager getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
//
            instance = new WalletEthManager(app, pubKey, BuildConfig.BITCOIN_TESTNET ? BREthereumNetwork.testnet : BREthereumNetwork.mainnet);

        }
        return instance;
    }

    @Override
    public int getForkId() {
        //No need for ETH
        return -1;
    }

    @Override
    public byte[] signAndPublishTransaction(BaseTransaction tx, byte[] phrase) {
        mWallet.sign((BREthereumTransaction) tx, new String(phrase));
        mWallet.submit((BREthereumTransaction) tx);
        return null;
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
        //todo implement
        return 0;
    }

    @Override
    public double getSyncProgress(long startHeight) {
        //Not needed for ETH, return fully synced always
        return 1.0;
    }

    @Override
    public double getConnectStatus() {
        //Not needed for ETH, return Connected always
        return 2;
    }

    @Override
    public void connect() {
        //Not needed for ETH
    }

    @Override
    public void disconnect() {
        //Not needed for ETH
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        //Not needed for ETH
        return false;
    }

    @Override
    public void rescan() {
        //Not needed for ETH
    }

    @Override
    public BaseTransaction[] getTxs() {
        return (BaseTransaction[]) mWallet.getTransactions();
    }

    @Override
    public BigDecimal getTxFee(BaseTransaction tx) {
        return null;
    }

    @Override
    public BigDecimal getFeeForTxAmount(BigDecimal amount) {
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
    public BigDecimal getMaxOutputAmount() {
        return null;
    }

    @Override
    public BigDecimal getMinOutputAmount() {
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

        if (app == null) {
            app = BreadApp.getBreadContext();

            if (app == null) {
                Log.d(TAG, "updateFee: FAILED, app is null");
                return;
            }
        }

        String jsonString = BRApiManager.urlGET(app, "https://" + BreadApp.HOST + "/fee-per-kb?currency=" + getIso(app));

        if (jsonString == null || jsonString.isEmpty()) {
            Log.e(TAG, "updateFeePerKb: failed to update fee, response string: " + jsonString);
            return;
        }

        BigDecimal fee;
        BigDecimal economyFee;
        try {
            JSONObject obj = new JSONObject(jsonString);
            fee = new BigDecimal(obj.getString("fee_per_kb"));
            economyFee = new BigDecimal(obj.getString("fee_per_kb_economy"));
            Log.d(TAG, "updateFee: " + getIso(app) + ":" + fee + "|" + economyFee);

            if (fee.compareTo(new BigDecimal(0)) > 0) {
                BRSharedPrefs.putFeeRate(app, getIso(app), fee);
                BRSharedPrefs.putFeeTime(app, getIso(app), System.currentTimeMillis()); //store the time of the last successful fee fetch
            } else {
                FirebaseCrash.report(new NullPointerException("Fee is weird:" + fee));
                Log.d(TAG, "Error: Fee is unexpected value");

            }
            if (economyFee.compareTo(new BigDecimal(0)) > 0) {
                BRSharedPrefs.putEconomyFeeRate(app, getIso(app), economyFee);
            } else {
                FirebaseCrash.report(new NullPointerException("Economy fee is weird:" + economyFee));
                Log.d(TAG, "Error: Economy fee is unexpected value");
            }
        } catch (JSONException e) {
            Log.e(TAG, "updateFeePerKb: FAILED: " + jsonString, e);
            BRReportsManager.reportBug(e);
            BRReportsManager.reportBug(new IllegalArgumentException("JSON ERR: " + jsonString));
        }

    }

    @Override
    public void refreshAddress(Context app) {
        BaseAddress address = getReceiveAddress(app);
        if (Utils.isNullOrEmpty(address.stringify())) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address.stringify(), getIso(app));
    }

    @Override
    public void refreshCachedBalance(Context app) {
        BigDecimal balance = new BigDecimal(mWallet.getBalance());
        BRSharedPrefs.putCachedBalance(app, ISO, balance);
    }

    @Override
    public List<TxUiHolder> getTxUiHolders() {
        BREthereumTransaction txs[] = mWallet.getTransactions();
        if (txs == null || txs.length <= 0) return null;
        List<TxUiHolder> uiTxs = new ArrayList<>();
        for (int i = txs.length - 1; i >= 0; i--) { //revere order
            BREthereumTransaction tx = txs[i];
            uiTxs.add(new TxUiHolder(tx, tx.getBlockTimestamp(), (int) tx.getBlockNumber(), null,
                    null, null,
                    null, new BigDecimal(tx.getGasUsed()).multiply(new BigDecimal(tx.getGasPrice(BREthereumAmount.Unit.ETHER_WEI))),
                    new BigDecimal(tx.getGasPrice(BREthereumAmount.Unit.ETHER_WEI)), new BigDecimal(tx.getGasLimit()),
                    tx.getTargetAddress(), tx.getSourceAddress(), null, 0,
                    new BigDecimal(tx.getAmount()), true));
        }

        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        //Not needed for ETH
        return false;
    }

    @Override
    public boolean addressIsUsed(String address) {
        //Not needed for ETH
        return false;
    }

    @Override
    public BaseAddress createAddress(String address) {
        return new ETHAddress(address);
    }

    @Override
    public boolean generateWallet(Context app) {
        //Not needed for ETH
        return false;
    }

    @Override
    public boolean connectWallet(Context app) {
        //Not needed for ETH
        return false;
    }

    @Override
    public String getSymbol(Context app) {
        return BRConstants.symbolEther;
    }

    @Override
    public String getIso(Context app) {
        return ISO;
    }

    @Override
    public String getScheme(Context app) {
        return ETH_SCHEME;
    }

    @Override
    public String getName(Context app) {
        return mName;
    }

    @Override
    public String getDenomination(Context app) {
        throw new RuntimeException("not implemented");
    }

    @Override
    public BaseAddress getReceiveAddress(Context app) {
        return new ETHAddress(mWallet.getAccount().getPrimaryAddress());
    }

    @Override
    public BaseTransaction createTransaction(BigDecimal amount, String address) {
        return null;
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
        return 8;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, getIso(app));
    }

    @Override
    public BigDecimal getTotalSent(Context app) {
        return new BigDecimal(0);
    }

    @Override
    public void wipeData(Context app) {
        Log.e(TAG, "wipeData: ");
    }

    @Override
    public void syncStarted() {
        //Not needed for ETH
    }

    @Override
    public void syncStopped(String error) {
        //Not needed for ETH
    }

    @Override
    public boolean networkIsReachable() {
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
        refreshAddress(app);
        for (OnBalanceChangedListener listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged(getIso(app), balance);
        }

    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return MAX_ETH;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {
        BigDecimal fiatData = getFiatForEth(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) return null;
        return fiatData; //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if (app == null) return null;
        return getFiatForSmallestCrypto(app, getCachedBalance(app), null);
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        if (ent != null) {
            //passed in a custom CurrencyEntity
            //get crypto amount
            BigDecimal cryptoAmount = amount.divide(WEI_ETH, 8, BRConstants.ROUNDING_MODE);
            //multiply by fiat rate
            return cryptoAmount.multiply(new BigDecimal(ent.rate));
        }
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(WEI_ETH, 8, BRConstants.ROUNDING_MODE);

        BigDecimal fiatData = getFiatForEth(app, cryptoAmount, iso);
        if (fiatData == null) return null;
        return fiatData;
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal fiatAmount) {
        if (fiatAmount == null || fiatAmount.doubleValue() == 0) return fiatAmount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        return getEthForFiat(app, fiatAmount, iso);

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.doubleValue() == 0) return amount;
        return amount.divide(WEI_ETH, 8, ROUNDING_MODE);
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.doubleValue() == 0) return amount;
        return amount.multiply(WEI_ETH);
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount == null || amount.doubleValue() == 0) return amount;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        BigDecimal ethAmount = getEthForFiat(app, amount, iso);
        if (ethAmount == null) return null;
        return ethAmount.multiply(WEI_ETH);
    }

    //pass in a eth amount and return the specified amount in fiat
    //ETH rates are in BTC (thus this math)
    private BigDecimal getFiatForEth(Context app, BigDecimal ethAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), "BTC");
        if (btcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return ethAmount.multiply(new BigDecimal(ethBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    //pass in a fiat amount and return the specified amount in ETH
    //ETH rates are in BTC (thus this math)
    private BigDecimal getEthForFiat(Context app, BigDecimal fiatAmount, String code) {
        //fiat rate for btc
        CurrencyEntity btcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ether
        CurrencyEntity ethBtcRate = CurrencyDataSource.getInstance(app).getCurrencyByCode(app, getIso(app), "BTC");
        if (btcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No USD rates for BTC");
            return null;
        }
        if (ethBtcRate == null) {
            Log.e(TAG, "getUsdFromBtc: No BTC rates for ETH");
            return null;
        }

        return fiatAmount.divide(new BigDecimal(ethBtcRate.rate).multiply(new BigDecimal(btcRate.rate)), 8, BRConstants.ROUNDING_MODE);
    }


    /**
     * The JSON RPC callbacks
     * Implement JSON RPC methods synchronously
     */

    @Override
    public void assignNode(BREthereumLightNode node) {
        this.node = node;
    }

    @Override
    public void getBalance(int wid, String address, int rid) {

    }

    @Override
    public void getGasPrice(int wid, int rid) {

    }

    @Override
    public void getGasEstimate(int wid, int tid, String to, String amount, String data, int rid) {

    }

    @Override
    public void submitTransaction(int wid, int tid, String rawTransaction, int rid) {

    }

    @Override
    public void getTransactions(String address, int rid) {

    }
}
