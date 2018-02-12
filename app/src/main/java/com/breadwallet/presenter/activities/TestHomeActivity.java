package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.BaseWallet;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;

import java.util.ArrayList;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Dummy Home activity to simulate navigating to and from the new Currency screen
 */

public class TestHomeActivity extends Activity {

    RelativeLayout mBitcoinCard;
    RelativeLayout mBchCard;
    public static final String EXTRA_CURRENCY = "extra_currency";
    private static final String CURRENCY_BTC = "btc";
    private static final String CURRENCY_BCH = "bch";

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_home);


       ArrayList<BaseWallet> walletList = new ArrayList<>();

       BaseWallet bitcoinWallet = new BaseWallet();
       bitcoinWallet.setTradeValue("$16904.34 +2.75%");
       bitcoinWallet.setWalletName("Bitcoin");
       bitcoinWallet.setWalletBalanceUSD("$35,499.11");
       bitcoinWallet.setWalletBalanceCurrency("2.1 BTC");


       BaseWallet bchWallet = new BaseWallet();
       bchWallet.setTradeValue("$2665.41  +2.75%");
       bchWallet.setWalletName("BitcoinCash");
       bchWallet.setWalletBalanceUSD("$6,796.80");
       bchWallet.setWalletBalanceCurrency("2.55 BCH");

       walletList.add(bitcoinWallet);
       walletList.add(bchWallet);

       mWalletRecycler = findViewById(R.id.rv_wallet_list);
       mAdapter = new WalletListAdapter(this, walletList);


       mWalletRecycler.setLayoutManager(new LinearLayoutManager(this));
       mWalletRecycler.setAdapter(mAdapter);

       mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
           @Override
           public void onItemClick(View view, int position, float x, float y) {

               if(position == 0){
                   Intent newIntent = new Intent(TestHomeActivity.this, CurrencyActivity.class);
                   newIntent.putExtra(EXTRA_CURRENCY, "btc");
                   startActivity(newIntent);
               }

               else if(position == 1){
                   Intent newIntent = new Intent(TestHomeActivity.this, CurrencyActivity.class);
                   newIntent.putExtra(EXTRA_CURRENCY, "bch");
                   startActivity(newIntent);
               }
           }

           @Override
           public void onLongItemClick(View view, int position) {

           }
       }));
    }

    private void startCurrencyActivity(String currency) {

        Intent newIntent = new Intent(this, CurrencyActivity.class);
        newIntent.putExtra(EXTRA_CURRENCY, currency);
        startActivity(newIntent);
    }
}
