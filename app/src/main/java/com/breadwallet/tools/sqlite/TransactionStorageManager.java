package com.breadwallet.tools.sqlite;


import android.content.Context;
import android.util.Log;

import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.List;

/**
 * Created by byfieldj on 2/1/18.
 * <p>
 * This class acts as the router between transactions and their respective databases
 * It determines which type of transaction you are attempting to save, and opens the proper database for
 * that transaction
 */

public class TransactionStorageManager {
    private static final String TAG = TransactionStorageManager.class.getSimpleName();

    public static boolean putTransaction(Context app, String iso, BRTransactionEntity tx) {
        if (iso == null || tx == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + iso + "|" + tx);
            return false;

        }

        if (iso.equalsIgnoreCase("btc") || iso.equalsIgnoreCase("bch")) {
            BRTransactionEntity result = BtcBchTransactionDataStore.getInstance(app).putTransaction(app, iso, tx);
            return result != null;
        }

        //other wallets

        return false;
    }

    public static List<BRTransactionEntity> getTransactions(Context app, String iso) {
        if (iso == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + iso);
            return null;

        }

        if (iso.equalsIgnoreCase("btc") || iso.equalsIgnoreCase("bch")) {
            return BtcBchTransactionDataStore.getInstance(app).getAllTransactions(app, iso);
        }

        //other wallets

        return null;
    }

    public static boolean updateTransaction(Context app, String iso, BRTransactionEntity tx) {
        if (iso == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + iso);
            return false;

        }

        if (iso.equalsIgnoreCase("btc") || iso.equalsIgnoreCase("bch")) {
            return BtcBchTransactionDataStore.getInstance(app).updateTransaction(app, iso, tx);

        }

        //other wallets

        return false;
    }

    public static boolean removeTransaction(Context app, String iso, String hash) {
        if (iso == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + iso);
            return false;

        }

        if (iso.equalsIgnoreCase("btc") || iso.equalsIgnoreCase("bch")) {
            BtcBchTransactionDataStore.getInstance(app).deleteTxByHash(app, iso, hash);
            return true;
        }
        //other wallets

        return false;
    }


}
