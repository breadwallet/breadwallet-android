package com.breadwallet.wallet.wallets;

import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.abstracts.OnTxListModified;
//import com.breadwallet.wallet.abstracts.OnTxStatusUpdatedListener;
import com.breadwallet.wallet.abstracts.SyncListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class WalletManagerHelper {

    private List<OnBalanceChangedListener> mOnBalanceChangedListeners = new ArrayList<>();
//    private List<OnTxStatusUpdatedListener> mOnTxStatusUpdatedListeners = new ArrayList<>();
    private List<SyncListener> mSyncListeners = new ArrayList<>();
    private List<OnTxListModified> mOntxListModifiedListeners = new ArrayList<>();

    public void addBalanceChangedListener(OnBalanceChangedListener listener) {
        if (listener != null && !mOnBalanceChangedListeners.contains(listener)) {
            mOnBalanceChangedListeners.add(listener);
        }
    }

    public void onBalanceChanged(String uid, BigDecimal balance) {
        for (OnBalanceChangedListener listener : mOnBalanceChangedListeners) {
            if (listener != null) {
                listener.onBalanceChanged(uid, balance);
            }
        }
    }

//    public void addTxStatusUpdatedListener(OnTxStatusUpdatedListener listener) {
//        if (listener != null && !mOnTxStatusUpdatedListeners.contains(listener))
//            mOnTxStatusUpdatedListeners.add(listener);
//    }

    public void addSyncListeners(SyncListener listener) {
        if (listener != null && !mSyncListeners.contains(listener)) {
            mSyncListeners.add(listener);
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
