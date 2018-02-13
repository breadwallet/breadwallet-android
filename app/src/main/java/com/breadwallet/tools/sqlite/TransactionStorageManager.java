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

    public static boolean putTransaction(Context app, BaseWalletManager wallet, BRTransactionEntity tx) {
        if (wallet == null || tx == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + wallet + "|" + tx);
            return false;

        }

        if (wallet.getIso(app).equalsIgnoreCase("btc") || wallet.getIso(app).equalsIgnoreCase("bch")) {
            BRTransactionEntity result = BtcBchTransactionDataStore.getInstance(app).putTransaction(app, wallet, tx);
            return result != null;
        }

        //other wallets

        return false;
    }

    public static List<BRTransactionEntity> getTransactions(Context app, BaseWalletManager wallet) {
        if (wallet == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + wallet);
            return null;

        }

        if (wallet.getIso(app).equalsIgnoreCase("btc") || wallet.getIso(app).equalsIgnoreCase("bch")) {
            return BtcBchTransactionDataStore.getInstance(app).getAllTransactions(app, wallet);
        }

        //other wallets

        return null;
    }
    public static boolean updateTransaction(Context app, BaseWalletManager wallet, BRTransactionEntity tx) {
        if (wallet == null || app == null) {
            Log.e(TAG, "putTransaction: failed: " + app + "|" + wallet);
            return false;

        }

        if (wallet.getIso(app).equalsIgnoreCase("btc") || wallet.getIso(app).equalsIgnoreCase("bch")) {
            return BtcBchTransactionDataStore.getInstance(app).updateTransaction(app, wallet, tx);

        }

        //other wallets

        return false;
    }


}
