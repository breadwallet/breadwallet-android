package com.breadwallet.wallet.wallets;


import android.content.Context;

import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.wallet.abstracts.BalanceUpdateListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
//import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WalletManagerHelper {

    public static final int MAX_DECIMAL_PLACES = 8;
    //Show this number of decimal places in transaction info.
    public static final int MAX_DECIMAL_PLACES_FOR_UI = 5;

    private List<BalanceUpdateListener> mOnBalanceChangedListeners = new ArrayList<>();
    //    private List<OnTxStatusUpdatedListener> mOnTxStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> mSyncListeners = new ArrayList<>();
    private List<OnTxListModified> mOntxListModifiedListeners = new ArrayList<>();

    public void addBalanceChangedListener(BalanceUpdateListener listener) {
        if (listener != null && !mOnBalanceChangedListeners.contains(listener)) {
            mOnBalanceChangedListeners.add(listener);
        }
    }

    public void onBalanceChanged(Context context, String currencyCode, BigDecimal balance) {
        BRSharedPrefs.putCachedBalance(context, currencyCode, balance);
        for (BalanceUpdateListener listener : mOnBalanceChangedListeners) {
            if (listener != null) {
                listener.onBalanceChanged(balance);
            }
        }
    }

    public void addSyncListener(SyncListener listener) {
        if (listener != null && !mSyncListeners.contains(listener)) {
            mSyncListeners.add(listener);
        }
    }

    public void removeSyncListener(SyncListener listener) {
        if (listener != null && mSyncListeners.contains(listener)) {
            mSyncListeners.remove(listener);
        }
    }

    public void onSyncStarted() {
        for (SyncListener listener : mSyncListeners) {
            if (listener != null) {
                listener.syncStarted();
            }
        }
    }

    public void onSyncStopped(String error) {
        for (SyncListener listener : mSyncListeners) {
            if (listener != null) {
                listener.syncStopped(error);
            }
        }
    }

    public void addTxListModifiedListener(OnTxListModified listener) {
        if (listener != null && !mOntxListModifiedListeners.contains(listener)) {
            mOntxListModifiedListeners.add(listener);
        }
    }

    public void onTxListModified(String hash) {
        for (OnTxListModified listener : mOntxListModifiedListeners) {
            if (listener != null) {
                listener.txListModified(hash);
            }
        }
    }

}
