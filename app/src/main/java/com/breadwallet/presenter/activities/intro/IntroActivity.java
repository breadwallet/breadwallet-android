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
package com.breadwallet.presenter.activities.intro;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.widget.ImageButton;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

/**
 * Activity shown when there is no wallet, here the user can pick between creating new wallet or recovering one with
 * the paper key.
 */
public class IntroActivity extends BRActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_intro);
        setOnClickListeners();

        if (BuildConfig.DEBUG) {
            Utils.printPhoneSpecs(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventUtils.pushEvent(EventUtils.EVENT_LANDING_PAGE_APPEARED);
    }

    private void setOnClickListeners() {
        BRButton buttonNewWallet = findViewById(R.id.button_new_wallet);
        BRButton buttonRecoverWallet = findViewById(R.id.button_recover_wallet);
        ImageButton faq = findViewById(R.id.faq_button);
        buttonNewWallet.setOnClickListener(v -> {
            if (!UiUtils.isClickAllowed()) {
                return;
            }
            EventUtils.pushEvent(EventUtils.EVENT_LANDING_PAGE_GET_STARTED);
            Intent intent = new Intent(IntroActivity.this, OnBoardingActivity.class);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            startActivity(intent);
        });

        buttonRecoverWallet.setOnClickListener(v -> {
            if (!UiUtils.isClickAllowed()) {
                return;
            }
            EventUtils.pushEvent(EventUtils.EVENT_LANDING_PAGE_RESTORE_WALLET);
            Intent intent = new Intent(IntroActivity.this, RecoverActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        });
        faq.setOnClickListener(v -> {
            if (!UiUtils.isClickAllowed()) return;
            BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(IntroActivity.this);
            UiUtils.showSupportFragment(IntroActivity.this, BRConstants.FAQ_START_VIEW, wm);
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        return checkOverlayAndDispatchTouchEvent(event);
    }
}
