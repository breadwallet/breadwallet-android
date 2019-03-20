package com.breadwallet.wallet.wallets.ela;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWalletManager;
import com.breadwallet.core.ethereum.BREthereumAmount;
import com.breadwallet.presenter.activities.ExploreWebActivity;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.TxManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.CryptoAddress;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.WalletManagerHelper;
import com.breadwallet.wallet.wallets.ela.data.ElaTransactionEntity;
import com.elastos.jni.Utility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
 * Copyright (c) 2016 breadwallet LLC
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
public class WalletElaManager extends BRCoreWalletManager implements BaseWalletManager {

    private static final String TAG = WalletElaManager.class.getSimpleName();

    public static final String ONE_ELA_IN_SALA = "100000000"; // 1 ela in sala, 100 millions
    public static final String MAX_ELA = "10000000000"; //Max amount in ela
    public static final String ELA_SYMBOL = "ELA";
    private static final String SCHEME = "elastos";
    private static final String NAME = "Elastos";
    private static final String ELA_ADDRESS_PREFIX = "E";


    public static final BigDecimal ONE_ELA_TO_SALA = new BigDecimal(ONE_ELA_IN_SALA);

    private static WalletElaManager mInstance;

    private final BigDecimal MAX_SALA = new BigDecimal(MAX_ELA);

    private final BigDecimal MIN_SALA = new BigDecimal("0");

    private WalletSettingsConfiguration mSettingsConfig;
    private WalletUiConfiguration mUiConfig;

    private WalletManagerHelper mWalletManagerHelper;

    protected String mPrivateKey;

    private static Context mContext;

    public static WalletElaManager getInstance(Context context) {

        if (mInstance == null) {
            mInstance = new WalletElaManager(context, null, null, 0);
        }

        return mInstance;
    }

    private WalletElaManager(Context context, BRCoreMasterPubKey masterPubKey,
                             BRCoreChainParams chainParams,
                             double earliestPeerTime) {
        super(masterPubKey, chainParams, 0);
        mContext = context;
        mUiConfig = new WalletUiConfiguration("#003d79", null,
                true, WalletManagerHelper.MAX_DECIMAL_PLACES);

        mSettingsConfig = new WalletSettingsConfiguration(context, getIso(), SettingsUtil.getElastosSettings(mContext), new ArrayList<BigDecimal>(0));

        mWalletManagerHelper = new WalletManagerHelper();
    }


    @Override
    public int getForkId() {
        Log.i(TAG, "getForkId");
        return -1;
    }

    @Override
    public BREthereumAmount.Unit getUnit() {
        Log.i(TAG, "getUnit");
        throw new RuntimeException("stub");
    }

    public String getPrivateKey() {
        if (mPrivateKey == null) {
            try {
                byte[] phrase = BRKeyStore.getPhrase(mContext, 0);
                mPrivateKey = Utility.getInstance(mContext).getSinglePrivateKey(new String(phrase));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return mPrivateKey;
    }

    public String getPublicKey(){
        String pk = getPrivateKey();
        if(StringUtil.isNullOrEmpty(pk)) return null;
        return Utility.getInstance(mContext).getPublicKeyFromPrivateKey(pk);
    }

    private String mAddress;
    @Override
    public String getAddress() {
        if (StringUtil.isNullOrEmpty(mAddress)) {
            String publickey = getPublicKey();
            if(publickey != null) {
                mAddress = Utility.getInstance(mContext).getAddress(publickey);
            }
        }

        // TODO: Test of we can remove the caching in memory and always call core directly.
        return mAddress;
    }

    @Override
    public boolean isAddressValid(String address) {
        Log.i(TAG, "isAddressValid");
        return !Utils.isNullOrEmpty(address) && (address.startsWith(ELA_ADDRESS_PREFIX)
                || address.startsWith("8"));
    }

    @Override
    public byte[] signAndPublishTransaction(CryptoTransaction tx, byte[] seed) {
        Log.i(TAG, "signAndPublishTransaction");
        if(tx == null) return new byte[1];
        BRElaTransaction raw = tx.getElaTx();
        if(raw == null) return new byte[1];
        String mRwTxid = ElaDataSource.getInstance(mContext).sendElaRawTx(raw.getTx());

        if(StringUtil.isNullOrEmpty(mRwTxid)) return new byte[1];
        TxManager.getInstance().updateTxList(mContext);
        if(!StringUtil.isNullOrEmpty(WalletActivity.mCallbackUrl)) {
            if(WalletActivity.mCallbackUrl.contains("?")){
                UiUtils.startWebviewActivity(mContext, WalletActivity.mCallbackUrl+"&txid="+mRwTxid);
            } else {
                UiUtils.startWebviewActivity(mContext, WalletActivity.mCallbackUrl+"?txid="+mRwTxid);
            }
        }
        WalletActivity.mCallbackUrl = null;
        return mRwTxid.getBytes();
    }

    public void updateTxHistory() {
        String address = getAddress();
        if(StringUtil.isNullOrEmpty(address)) return;
        ElaDataSource.getInstance(mContext).getHistory(address);
        TxManager.getInstance().updateTxList(mContext);
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener list) {
        mWalletManagerHelper.addBalanceChangedListener(list);
    }

    @Override
    public void onBalanceChanged(BigDecimal balance) {
        Log.i(TAG, "onBalanceChanged");
        mWalletManagerHelper.onBalanceChanged(balance);
    }

    @Override
    public void addSyncListener(SyncListener listener) {
        Log.i(TAG, "addSyncListener");
    }

    @Override
    public void removeSyncListener(SyncListener listener) {
        Log.i(TAG, "removeSyncListener");
    }

    @Override
    public void addTxListModifiedListener(OnTxListModified list) {
        Log.i(TAG, "addTxListModifiedListener");
        mWalletManagerHelper.addTxListModifiedListener(list);
    }

    @Override
    public void watchTransactionForHash(CryptoTransaction tx, OnHashUpdated listener) {
        Log.i(TAG, "watchTransactionForHash");
    }

    @Override
    public long getRelayCount(byte[] txHash) {
        Log.i(TAG, "getRelayCount");
        return 0;
    }

    private static double time = 0;

    @Override
    public double getSyncProgress(long startHeight) {
        Log.i(TAG, "getSyncProgress");
//        time += 0.1;
//        if(time >= 1.0) time = 1.0;
        return /*time*/1.0;
    }

    @Override
    public double getConnectStatus() {
        Log.i(TAG, "getConnectStatus");
        return 2;
    }

    @Override
    public void connect(Context app) {
        Log.i(TAG, "connect");
    }

    @Override
    public void disconnect(Context app) {
        Log.i(TAG, "disconnect");
    }

    @Override
    public boolean useFixedNode(String node, int port) {
        Log.i(TAG, "useFixedNode");
        return false;
    }

    @Override
    public void rescan(Context app) {
        Log.i(TAG, "rescan");
    }

    @Override
    public CryptoTransaction[] getTxs(Context app) {
        Log.i(TAG, "getTxs");
        return new CryptoTransaction[0];
    }

    private static final BigDecimal ELA_FEE = new BigDecimal(4860).divide(ONE_ELA_TO_SALA, 8, BRConstants.ROUNDING_MODE);

    @Override
    public BigDecimal getTxFee(CryptoTransaction tx) {
        Log.i(TAG, "getTxFee");
        return ELA_FEE;
    }

    @Override
    public BigDecimal getEstimatedFee(BigDecimal amount, String address) {
        return ELA_FEE;
    }

    @Override
    public BigDecimal getFeeForTransactionSize(BigDecimal size) {
        return ELA_FEE;
    }

    @Override
    public String getTxAddress(CryptoTransaction tx) {
        return getAddress();
    }

    @Override
    public BigDecimal getMaxOutputAmount(Context app) {
        BigDecimal balance = getCachedBalance(app);
        if (balance.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        if (ELA_FEE.compareTo(balance) > 0) return BigDecimal.ZERO;
        return balance.subtract(ELA_FEE);
    }

    @Override
    public BigDecimal getMinOutputAmount(Context app) {
        return MIN_SALA;
    }

    @Override
    public BigDecimal getTransactionAmount(CryptoTransaction tx) {
        Log.i(TAG, "getTransactionAmount");
        return new BigDecimal(getWallet().getTransactionAmount(tx.getCoreTx()));
    }

    @Override
    public BigDecimal getMinOutputAmountPossible() {
        return new BigDecimal(BRCoreTransaction.getMinOutputAmount());
    }

    @Override
    public void updateFee(Context app) {

    }

    @Override
    public void refreshAddress(Context app) {

    }

    @Override
    public void refreshCachedBalance(final Context app) {
        Log.i(TAG, "refreshCachedBalance");
        try {
            String address = getAddress();
            Log.i("balance_test", "address:"+address);
            if(address == null) return;
            String balance = ElaDataSource.getInstance(mContext).getElaBalance(address);
            if(balance == null) return;
            final BigDecimal tmp = new BigDecimal((balance == null || balance.equals("")) ? "0" : balance);
            BRSharedPrefs.putCachedBalance(app, getIso(), tmp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<TxUiHolder> getTxUiHolders(Context app) {
        List<ElaTransactionEntity> transactionEntities = ElaDataSource.getInstance(mContext).getAllTransactions();
        List<TxUiHolder> uiTxs = new ArrayList<>();
        try {
            for (ElaTransactionEntity entity : transactionEntities) {
                BigDecimal fee = new BigDecimal(entity.fee).divide(ONE_ELA_TO_SALA, 8, BRConstants.ROUNDING_MODE);
                BigDecimal amount = new BigDecimal(entity.amount).divide(ONE_ELA_TO_SALA, 8, BRConstants.ROUNDING_MODE);
                TxUiHolder txUiHolder = new TxUiHolder(null,
                        entity.isReceived,
                        entity.timeStamp,
                        entity.blockHeight,
                        entity.hash,
                        entity.txReversed,
                        fee,
                        entity.toAddress,
                        entity.fromAddress,
                        new BigDecimal(String.valueOf(entity.balanceAfterTx))
                        , entity.txSize,
                        amount
                        , entity.isValid);
                txUiHolder.memo = entity.memo;
                uiTxs.add(txUiHolder);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return uiTxs;
    }

    @Override
    public boolean containsAddress(String address) {
        String addr = getAddress();
        return addr.equals(address);
    }

    @Override
    public boolean addressIsUsed(String address) {
        return true;
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, one key for all wallets so far
        return true;
    }

    @Override
    public String getSymbol(Context app) {
        return "ELA";
    }

    @Override
    public String getIso() {
        return "ELA";//图标的选取，数量，价格都是根据这个iso来获取的
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String getDenominator() {
        return String.valueOf(ONE_ELA_IN_SALA);
    }

    @Override
    public CryptoAddress getReceiveAddress(Context app) {
        return new CryptoAddress(getAddress(), null);
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address) {
        return null;
    }

    @Override
    public CryptoTransaction createTransaction(BigDecimal amount, String address, String meno) {
        Log.i(TAG, "createTransaction");
        BRElaTransaction brElaTransaction = ElaDataSource.getInstance(mContext).createElaTx(getAddress(), address, amount.multiply(ONE_ELA_TO_SALA).longValue(), meno);
        return new CryptoTransaction(brElaTransaction);
    }

    @Override
    public String decorateAddress(String addr) {
        return addr;
    }

    @Override
    public String undecorateAddress(String addr) {
        return addr;
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        return WalletManagerHelper.MAX_DECIMAL_PLACES;
    }

    @Override
    public BigDecimal getCachedBalance(Context app) {
        Log.i(TAG, "getCachedBalance");
        if(app == null) return new BigDecimal(0);
        return BRSharedPrefs.getCachedBalance(app, getIso());
    }

    //TODO wait
    @Override
    public BigDecimal getTotalSent(Context app) {
        Log.i(TAG, "getTotalSent");
        return BigDecimal.ZERO;
    }

    @Override
    public void wipeData(Context app) {
        BRSharedPrefs.putCachedBalance(app, getIso(),  new BigDecimal(0));
        ElaDataSource.getInstance(app).deleteAllTransactions();
        mPrivateKey = null;
        mAddress = null;
        mInstance = null;
    }

    @Override
    public void syncStarted() {
        Log.i(TAG, "syncStarted");
        mWalletManagerHelper.onSyncStarted();
    }

    @Override
    public void syncStopped(String error) {
        Log.i(TAG, "syncStopped");
        mWalletManagerHelper.onSyncStopped(error);
    }

    @Override
    public boolean networkIsReachable() {
        Log.i(TAG, "networkIsReachable");
        Context app = BreadApp.getBreadContext();
        return InternetManager.getInstance().isConnected(app);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(), balance);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        return MAX_SALA;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return mUiConfig;
    }

    @Override
    public WalletSettingsConfiguration getSettingsConfiguration() {
        return mSettingsConfig;
    }

    private BigDecimal getFiatForEla(Context app, BigDecimal elaAmount, String code) {//总资产
        if (elaAmount == null || elaAmount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        //fiat rate for btc
        CurrencyEntity btcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, "BTC", code);
        //Btc rate for ela
        CurrencyEntity elaBtcRate = RatesDataSource.getInstance(app).getCurrencyByCode(app, getIso(), "BTC");
        if (btcRate == null) {
            return null;
        }
        if (elaBtcRate == null) {
            return null;
        }

        return elaAmount.multiply(new BigDecimal(elaBtcRate.rate)).multiply(new BigDecimal(btcRate.rate));
    }

    @Override
    public BigDecimal getFiatExchangeRate(Context app) {//行情价格
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if (fiatData == null) new BigDecimal(0);
        return fiatData; //dollars
    }

    @Override
    public BigDecimal getFiatBalance(Context app) {
        if(app == null) return new BigDecimal(0);
        BigDecimal amount = BRSharedPrefs.getCachedBalance(app, getIso());
        return getFiatForEla(app, amount, BRSharedPrefs.getPreferredFiatIso(app));
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount, CurrencyEntity ent) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if(fiatData==null || fiatData.doubleValue()==0) return new BigDecimal(0);
        return amount.multiply(fiatData);
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        if(amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        return amount.multiply(fiatData);
    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        return amount;
    }

    @Override
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        return amount;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal fiatData = getFiatForEla(app, new BigDecimal(1), BRSharedPrefs.getPreferredFiatIso(app));
        if(fiatData == null) return new BigDecimal(0);
        if(fiatData.compareTo(BigDecimal.ZERO) == 0) return new BigDecimal(0);
        BigDecimal tmp = amount.divide(fiatData, 8, BRConstants.ROUNDING_MODE);
        return tmp;
    }
}
