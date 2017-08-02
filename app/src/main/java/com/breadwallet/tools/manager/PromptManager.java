package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.UpdatePitActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.activities.settings.FingerprintActivity;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRPeerManager;

import static com.breadwallet.tools.manager.PromptManager.PromptItem.FINGER_PRINT;
import static com.breadwallet.tools.manager.PromptManager.PromptItem.PAPER_KEY;
import static com.breadwallet.tools.manager.PromptManager.PromptItem.RECOMMEND_RESCAN;
import static com.breadwallet.tools.manager.PromptManager.PromptItem.UPGRADE_PIN;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/18/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class PromptManager {

    private PromptManager() {
    }

    private static PromptManager instance;

    public static PromptManager getInstance() {
        if (instance == null) instance = new PromptManager();
        return instance;
    }

    public enum PromptItem {
        SYNCING,
        FINGER_PRINT,
        PAPER_KEY,
        UPGRADE_PIN,
        RECOMMEND_RESCAN
    }

    public class PromptInfo {
        public String title;
        public String description;
        public View.OnClickListener listener;

        private PromptInfo() {
        }

        public PromptInfo(String title, String description, View.OnClickListener listener) {
            assert (title != null);
            assert (description != null);
            assert (listener != null);
            this.title = title;
            this.description = description;
            this.listener = listener;
        }
    }

    public boolean shouldPrompt(Context app, PromptItem item) {
        assert (app != null);
        switch (item) {
            case FINGER_PRINT:
                return !BRSharedPrefs.getUseFingerprint(app);
            case PAPER_KEY:
                return !BRSharedPrefs.getPhraseWroteDown(app);
            case UPGRADE_PIN:
                return KeyStoreManager.getPinCode(app).length() != 6;
            case RECOMMEND_RESCAN:
                return false; //todo add code to this

        }
        return false;
    }

    public PromptItem nextPrompt(Context app) {
        if (shouldPrompt(app, RECOMMEND_RESCAN)) return RECOMMEND_RESCAN;
        if (shouldPrompt(app, UPGRADE_PIN)) return UPGRADE_PIN;
        if (shouldPrompt(app, PAPER_KEY)) return PAPER_KEY;
        if (shouldPrompt(app, FINGER_PRINT)) return FINGER_PRINT;
        return null;
    }

    public PromptInfo promptInfo(final Activity app, PromptItem item) {
        switch (item) {
            case FINGER_PRINT:
                return new PromptInfo(app.getString(R.string.Prompts_TouchId_title), app.getString(R.string.Prompts_TouchId_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(app, FingerprintActivity.class);
                        app.startActivity(intent);
                        app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    }
                }); //todo make it fingerprint, not touch id
            case PAPER_KEY:
                return new PromptInfo(app.getString(R.string.Prompts_PaperKey_title), app.getString(R.string.Prompts_PaperKey_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(app, WriteDownActivity.class);
                        app.startActivity(intent);
                        app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                    }
                });
            case UPGRADE_PIN:
                return new PromptInfo(app.getString(R.string.Prompts_UpgradePin_title), app.getString(R.string.Prompts_UpgradePin_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(app, UpdatePitActivity.class);
                        app.startActivity(intent);
                        app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    }
                });
            case RECOMMEND_RESCAN:
                return new PromptInfo(app.getString(R.string.Prompts_UpgradePin_title), app.getString(R.string.Prompts_UpgradePin_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                BRSharedPrefs.putStartHeight(app, 0);
                                BRPeerManager.getInstance().rescan();
//                                BRAnimator.startBreadActivity(app, false);

                            }
                        }).start();
                    }
                });

        }
        return null;
    }

}
