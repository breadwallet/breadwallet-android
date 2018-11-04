/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 10/16/18.
 * Copyright (c) 2018 breadwallet LLC
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

package com.breadwallet.presenter.activities.settings;

import android.content.Intent;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.app.util.UserMetricsUtil;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

/**
 * Represents the activity where the user is able to opt in segwit.
 */
public class SegWitActivity extends BaseSettingsActivity {
    private static final String TAG = SegWitActivity.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final BRButton enableButton = findViewById(R.id.enable_button);
        BRButton continueButton = findViewById(R.id.continue_button);
        BRButton cancelButton = findViewById(R.id.cancel_button);
        final ConstraintLayout confirmChoiceLayout = findViewById(R.id.confirm_choice_layout);
        final ConstraintLayout confirmationLayout = findViewById(R.id.confirmation_layout);
        enableButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableButton.setVisibility(View.GONE);
                confirmChoiceLayout.setVisibility(View.VISIBLE);
            }
        });
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableSegwit();
                confirmChoiceLayout.setVisibility(View.GONE);
                confirmationLayout.setVisibility(View.VISIBLE);
                enableButton.setVisibility(View.VISIBLE);
                enableButton.setText(getString(R.string.Button_BackToHomePage));
                enableButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(SegWitActivity.this, HomeActivity.class);
                        intent.setFlags(intent.getFlags() | Intent.FLAG_ACTIVITY_NO_HISTORY);
                        startActivity(intent);
                    }
                });
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    /**
     * Sets the segwit Shared Preferences flag to true and updates the bitcoin settings accordingly.
     */
    private void enableSegwit() {
        BRSharedPrefs.putIsSegwitEnabled(this, true);
        WalletBitcoinManager.getInstance(this).updateSettings(this);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                UserMetricsUtil.logSegwitEvent(SegWitActivity.this, UserMetricsUtil.ENABLE_SEG_WIT);
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_segwit;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }
}
