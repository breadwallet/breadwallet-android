package com.breadwallet.wallet.wallets.ethereum;

import android.content.Context;

import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;
import com.breadwallet.wallet.wallets.WalletManagerHelper;

import java.math.BigDecimal;

public abstract class BaseEthereumWalletManager implements BaseWalletManager {

    private WalletManagerHelper mWalletManagerHelper;

    public BaseEthereumWalletManager() {
        mWalletManagerHelper = new WalletManagerHelper();
    }

    protected WalletManagerHelper getWalletManagerHelper() {
        return mWalletManagerHelper;
    }

    //TODO Not used by ETH, ERC20
    @Override
    public int getForkId() {
        return -1;
    }

    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        mWalletManagerHelper.addBalanceChangedListener(listener);
    }

    // TODO Not Used by ERC20
    protected void onBalanceChanged(String uid, BigDecimal balance) {
        mWalletManagerHelper.onBalanceChanged(uid, balance);
    }

//    @Override
//    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener listener) {
//        mWalletManagerHelper.addTxStatusUpdatedListener(listener);
//    }

    // TODO not used by ETH, ERC20
    @Override
    public void addSyncListeners(SyncListener listener) {
//        mWalletManagerHelper.addSyncListeners(listener);
    }

    // TODO not used by ETH, ERC20
    @Override
    public void addTxListModifiedListener(OnTxListModified listener) {
//        mWalletManagerHelper.addTxListModifiedListener(listener);
    }

    @Override
    public void setCachedBalance(Context app, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(app, getIso(app), balance);
        onBalanceChanged(getIso(app), balance);
    }

}
