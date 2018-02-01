package com.breadwallet.tools.sqlite;

import android.content.Context;

import com.breadwallet.presenter.entities.BaseWallet;

/**
 * Created by byfieldj on 2/1/18.
 * <p>
 * This class acts as the router between transactions and their respective databases
 * It determines which type of transaction you are attempting to save, and opens the proper database for
 * that transaction
 */

public class TransactionStorageManager {


    private BaseWallet mWallet;
    private Context mContext;


    public TransactionStorageManager(Context context, BaseWallet wallet) {

        this.mContext = context;
        this.mWallet = wallet;

    }

    public void initTransactionStorage() {

        // TODO: Should probably change this to a switch statement in the future as we
        // add more currencies/wallets

        if (mWallet.getWalletType().equals(BaseWallet.WalletType.BTC_WALLET)) {

            // Open BTC/BCH DB and hand off "saving" operation to relevant class

            BtcBchTransactionDataStore.getInstance(mContext, mWallet).openDatabase();

        }
        else if(mWallet.getWalletType().equals(BaseWallet.WalletType.BCH_WALLET)){

            BtcBchTransactionDataStore.getInstance(mContext, mWallet).openDatabase();

        }


    }


}
