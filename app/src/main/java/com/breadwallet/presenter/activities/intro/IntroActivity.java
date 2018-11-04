
package com.breadwallet.presenter.activities.intro;

import android.content.Intent;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.security.SmartValidator;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.platform.APIClient;


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

public class IntroActivity extends BRActivity {
    private static final String TAG = IntroActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);
        setOnClickListeners();
        updateBundles();

        Shader shader = new LinearGradient(
                90, 0, 100, 100,
                getColor(R.color.button_gradient_start_color),
                getColor(R.color.button_gradient_end_color),
                Shader.TileMode.CLAMP);

        TextView subtitle = findViewById(R.id.intro_subtitle);
        subtitle.getPaint().setShader(shader);

        ImageButton faq = findViewById(R.id.faq_button);
        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(IntroActivity.this).getCurrentWallet(IntroActivity.this);
                UiUtils.showSupportFragment(IntroActivity.this, BRConstants.FAQ_START_VIEW, wm);
            }
        });

        if (Utils.isEmulatorOrDebug(this)) {
            Utils.printPhoneSpecs(this);
        }

        PostAuth.getInstance().onCanaryCheck(IntroActivity.this, false);

        findViewById(R.id.splash_screen).setVisibility(View.GONE);
    }

    private void updateBundles() {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                final long startTime = System.currentTimeMillis();
                APIClient apiClient = APIClient.getInstance(IntroActivity.this);
                apiClient.updateBundle();
                long endTime = System.currentTimeMillis();
                Log.d(TAG, "updateBundle DONE in " + (endTime - startTime) + "ms");
            }
        });
    }

    private void setOnClickListeners() {
        findViewById(R.id.button_new_wallet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                Intent intent = new Intent(IntroActivity.this, InputPinActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
            }
        });

        findViewById(R.id.button_recover_wallet).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) {
                    return;
                }
                Intent intent = new Intent(IntroActivity.this, RecoverActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
    }
}
