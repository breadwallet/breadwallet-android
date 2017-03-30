package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentMenu;
import com.breadwallet.presenter.fragments.FragmentReceive;
import com.breadwallet.presenter.fragments.FragmentSend;
import com.breadwallet.tools.adapter.TransactionListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.CurrencyFetchManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;

import java.math.BigDecimal;
import java.util.Arrays;

import static com.breadwallet.tools.util.BRConstants.PLATFORM_ON;

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

public class BreadActivity extends AppCompatActivity implements BRWalletManager.OnBalanceChanged, BRPeerManager.OnTxStatusUpdate {
    private static final String TAG = BreadActivity.class.getName();

    private LinearLayout sendButton;
    private LinearLayout receiveButton;
    private LinearLayout menuButton;
    public static BreadActivity app;
    public static final Point screenParametersPoint = new Point();

    private TextView primaryPrice;
    private TextView secondaryPrice;
    private TextView emptyTip;
    private ProgressBar progressBar;
    private RecyclerView txList;
    private TransactionListAdapter adapter;

    public static boolean appInBackground = false;

    static {
        System.loadLibrary("core");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bread);
        BRWalletManager.getInstance().addBalanceChangedListener(this);
        BRPeerManager.getInstance().addStatusUpdateListener(this);
        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
        // Always cast your custom Toolbar here, and set it as the ActionBar.
        Toolbar tb = (Toolbar) findViewById(R.id.bread_bar);
        setSupportActionBar(tb);

        initializeViews();

        setListeners();

        progressBar.setProgress(80);
        new Thread(new Runnable() {
            @Override
            public void run() {
                setUpTxList();
            }
        }).start();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    private void setUpTxList() {
        final TransactionListItem[] arr = BRWalletManager.getInstance().getTransactions();
        Log.e(TAG, "setUpTxList: arr.size: " + Arrays.toString(arr));
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (arr == null) {
                    txList.setVisibility(View.GONE);
                    emptyTip.setVisibility(View.VISIBLE);
                } else {
                    txList.setVisibility(View.VISIBLE);
                    emptyTip.setVisibility(View.GONE);
                    adapter = new TransactionListAdapter(BreadActivity.this, Arrays.asList(arr));
                    txList.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                }
            }
        });

    }

    private void setStatusBarColor() {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(R.color.status_bar));
    }

    private void setUrlHandler(Intent intent) {
        Uri data = intent.getData();
        if (data == null) return;
        String scheme = data.getScheme();
        if (scheme != null && (scheme.startsWith("bitcoin") || scheme.startsWith("bitid"))) {
            String str = intent.getDataString();
            BitcoinUrlHandler.processRequest(this, str);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setUrlHandler(intent);

    }

    private void setListeners() {
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.showAnimation(v);
                showSendFragment(null);

            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.showAnimation(v);
                getFragmentManager().beginTransaction().add(android.R.id.content, new FragmentReceive(), FragmentReceive.class.getName())
                        .addToBackStack(FragmentReceive.class.getName()).commit();
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                SpringAnimator.showAnimation(v);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                transaction.add(android.R.id.content, new FragmentMenu(), FragmentMenu.class.getName());
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
        primaryPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                togglePriceTexts();
            }
        });
        secondaryPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                togglePriceTexts();
            }
        });

        txList.setLayoutManager(new LinearLayoutManager(this));
        txList.addOnItemTouchListener(new RecyclerItemClickListener(this,
                txList, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                Log.e(TAG, "onItemClick: " + position);
            }

            @Override
            public void onLongItemClick(View view, int position) {
            }
        }));
    }

    private void showSendFragment(String bitcoinUrl) {
        FragmentSend fragmentSend = new FragmentSend();
        getFragmentManager().beginTransaction()
                .add(android.R.id.content, fragmentSend, FragmentSend.class.getName())
                .addToBackStack(FragmentSend.class.getName()).commit();
        if (bitcoinUrl != null && !bitcoinUrl.isEmpty()) {
            fragmentSend.setUrl(bitcoinUrl);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        app = this;
    }

    @Override
    protected void onResume() {
        super.onResume();
        appInBackground = false;
        app = this;

        new Thread(new Runnable() {
            @Override
            public void run() {
                BRWalletManager.getInstance().setUpTheWallet(BreadActivity.this);
            }
        }).start();

        CurrencyFetchManager currencyManager = CurrencyFetchManager.getInstance(this);
        currencyManager.startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInBackground = true;
        CurrencyFetchManager.getInstance(this).stopTimerTask();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //sync the kv stores

        if (PLATFORM_ON) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    APIClient.getInstance(BreadActivity.this).syncKvStore();
                }
            }).start();

        }

    }

    private void initializeViews() {
        sendButton = (LinearLayout) findViewById(R.id.send_layout);
        receiveButton = (LinearLayout) findViewById(R.id.receive_layout);
        menuButton = (LinearLayout) findViewById(R.id.menu_layout);
        primaryPrice = (TextView) findViewById(R.id.primary_price);
        secondaryPrice = (TextView) findViewById(R.id.secondary_price);
        emptyTip = (TextView) findViewById(R.id.empty_tx_tip);
        progressBar = (ProgressBar) findViewById(R.id.load_wallet_progress);
        txList = (RecyclerView) findViewById(R.id.tx_list);
    }

    private void togglePriceTexts() {

    }

    //returns x-pos relative to root layout
    private float getRelativeX(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getX();
        else
            return myView.getX() + getRelativeX((View) myView.getParent());
    }

    //returns y-pos relative to root layout
    private float getRelativeY(View myView) {
        if (myView.getParent() == myView.getRootView())
            return myView.getY();
        else
            return myView.getY() + getRelativeY((View) myView.getParent());
    }

    //0 crypto is left, 1 crypto is right
    private int getSwapPosition() {
        if (primaryPrice == null || secondaryPrice == null) {
            return 0;
        }
        return getRelativeX(primaryPrice) < getRelativeX(secondaryPrice) ? 0 : 1;
    }

    @Override
    public void onBalanceChanged(final long balance) {
        Log.e(TAG, "onBalanceChanged: " + balance);
        String iso = SharedPreferencesManager.getIso(this);
        CurrencyEntity ent = CurrencyDataSource.getInstance(this).getCurrencyByIso(iso);
        if (ent == null) {
            Log.e(TAG, "onBalanceChanged: No currency with iso: " + iso);
            return;
        }
        final String bits = BRCurrency.getFormattedCurrencyString(this, "BTC", new BigDecimal(balance));

        float rateForIso = ent.rate;
        final String amount = BRCurrency.getExchangeForAmount(new BigDecimal(rateForIso), iso, new BigDecimal(balance), this);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                primaryPrice.setText(amount);
                secondaryPrice.setText(bits);
                SpringAnimator.showAnimation(primaryPrice);
                SpringAnimator.showAnimation(secondaryPrice);

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // 123 is the qrCode result
        if (requestCode == 123) {
            if (resultCode == Activity.RESULT_OK) {
                String result = data.getStringExtra("result");
                Log.e(TAG, "onActivityResult: result: " + result);
                FragmentSend fragmentSend = (FragmentSend) getFragmentManager().findFragmentByTag(FragmentSend.class.getName());
                if (fragmentSend != null && fragmentSend.isVisible()) {
                    fragmentSend.setUrl(result);
                } else {
                    showSendFragment(result);
                }
            }

        }
    }

    @Override
    public void onStatusUpdate() {
        setUpTxList();
    }
}