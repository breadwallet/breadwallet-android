package com.breadwallet.presenter.activities;

import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.FragmentBreadSignal;
import com.breadwallet.presenter.fragments.FragmentReceive;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;

import java.util.Observable;
import java.util.Observer;

import static com.breadwallet.tools.animation.BRAnimator.showBreadMenu;
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

public class BreadActivity extends AppCompatActivity implements Observer {
    private static final String TAG = BreadActivity.class.getName();

    private LinearLayout sendButton;
    private LinearLayout receiveButton;
    private LinearLayout menuButton;
    public static BreadActivity app;
    public static final Point screenParametersPoint = new Point();

    private TextView primaryPrice;
    private TextView secondaryPrice;

    public static boolean appInBackground = false;

    static {
        System.loadLibrary("core");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_bread);
        app = this;
        getWindowManager().getDefaultDisplay().getSize(screenParametersPoint);
        // Always cast your custom Toolbar here, and set it as the ActionBar.
        Toolbar tb = (Toolbar) findViewById(R.id.bread_bar);
        setSupportActionBar(tb);

        initializeViews();
        setListeners();

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
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
            RequestHandler.processRequest(this, str);
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
                SpringAnimator.showAnimation(v);
            }
        });

        receiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                BreadActivity.this.getFragmentManager().beginTransaction().add(android.R.id.content, new FragmentReceive(), FragmentReceive.class.getName())
                        .setCustomAnimations(R.animator.to_bottom,R.animator.to_bottom, R.animator.to_bottom,  R.animator.to_bottom)
                        .addToBackStack(FragmentReceive.class.getName()).commit();
            }
        });

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SpringAnimator.showAnimation(v);
                showBreadMenu(BreadActivity.this);
            }
        });
        primaryPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePriceTexts();
            }
        });
        secondaryPrice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                togglePriceTexts();
            }
        });
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

        CurrencyManager currencyManager = CurrencyManager.getInstance(this);
        currencyManager.startTimer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appInBackground = true;
        CurrencyManager.getInstance(this).stopTimerTask();
    }

    @Override
    protected void onStop() {
        super.onStop();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //sync the kv stores
        if (PLATFORM_ON)
            APIClient.getInstance(this).syncKvStore();

    }

    //
//    /**
//     * Initializes all the views and components
//     */
//
    private void initializeViews() {
        sendButton = (LinearLayout) findViewById(R.id.send_layout);
        receiveButton = (LinearLayout) findViewById(R.id.receive_layout);
        menuButton = (LinearLayout) findViewById(R.id.menu_layout);
        primaryPrice = (TextView) findViewById(R.id.primary_price);
        secondaryPrice = (TextView) findViewById(R.id.secondary_price);
    }

    private void togglePriceTexts() {

//        String tmp = leftIso;
//        leftIso = rightIso;
//        rightIso = tmp;

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
    public void update(Observable observable, Object data) {

    }

}