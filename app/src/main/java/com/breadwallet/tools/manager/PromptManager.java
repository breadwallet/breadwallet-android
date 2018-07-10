package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.activities.settings.FingerprintActivity;
import com.breadwallet.presenter.activities.settings.ShareDataActivity;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import static com.breadwallet.tools.manager.PromptManager.PromptItem.FINGER_PRINT;
import static com.breadwallet.tools.manager.PromptManager.PromptItem.PAPER_KEY;
import static com.breadwallet.tools.manager.PromptManager.PromptItem.RECOMMEND_RESCAN;
import static com.breadwallet.tools.manager.PromptManager.PromptItem.SHARE_DATA;
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
        FINGER_PRINT,
        PAPER_KEY,
        UPGRADE_PIN,
        RECOMMEND_RESCAN,
        NO_PASSCODE,
        SHARE_DATA
    }

    public class PromptInfo {
        public String title;
        public String description;
        public View.OnClickListener listener;

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
                return !BRSharedPrefs.getUseFingerprint(app) && Utils.isFingerprintAvailable(app) && !BRSharedPrefs.getPromptDismissed(app, "fingerprint");
            case PAPER_KEY:
                return !BRSharedPrefs.getPhraseWroteDown(app);
            case UPGRADE_PIN:
                return BRKeyStore.getPinCode(app).length() != 6;
            case RECOMMEND_RESCAN:
                BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
                return wallet != null && BRSharedPrefs.getScanRecommended(app, wallet.getIso());
            case SHARE_DATA:
                return !BRSharedPrefs.getShareData(app) && !BRSharedPrefs.getPromptDismissed(app, "shareData");

        }
        return false;
    }

    public PromptItem nextPrompt(Context app) {
        if (shouldPrompt(app, RECOMMEND_RESCAN)) return RECOMMEND_RESCAN;
        if (shouldPrompt(app, UPGRADE_PIN)) return UPGRADE_PIN;
        if (shouldPrompt(app, PAPER_KEY)) return PAPER_KEY;
        if (shouldPrompt(app, FINGER_PRINT)) return FINGER_PRINT;
        if (shouldPrompt(app, SHARE_DATA)) return SHARE_DATA;
        return null;
    }

    public PromptInfo promptInfo(final Activity context, PromptItem item) {
        switch (item) {
            case FINGER_PRINT:
                return new PromptInfo(context.getString(R.string.Prompts_TouchId_title_android), context.getString(R.string.Prompts_TouchId_body_android), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, FingerprintActivity.class);
                        context.startActivity(intent);
                        context.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    }
                });
            case PAPER_KEY:
                return new PromptInfo(context.getString(R.string.Prompts_PaperKey_title), context.getString(R.string.Prompts_PaperKey_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, WriteDownActivity.class);
                        intent.putExtra(WriteDownActivity.EXTRA_VIEW_REASON, WriteDownActivity.ViewReason.SETTINGS.getValue());
                        context.startActivity(intent);
                        context.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                    }
                });
            case UPGRADE_PIN:
                return new PromptInfo(context.getString(R.string.Prompts_UpgradePin_title), context.getString(R.string.Prompts_UpgradePin_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(context, InputPinActivity.class);
                        intent.putExtra(InputPinActivity.EXTRA_PIN_MODE_UPDATE, true);
                        context.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        context.startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);

                    }
                });
            case RECOMMEND_RESCAN:
                return new PromptInfo(context.getString(R.string.Prompts_RecommendRescan_title), context.getString(R.string.Prompts_RecommendRescan_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                BRSharedPrefs.putStartHeight(context, BRSharedPrefs.getCurrentWalletIso(context), 0);
                                BaseWalletManager wallet = WalletsMaster.getInstance(context).getCurrentWallet(context);
                                wallet.rescan(context);
                                BRSharedPrefs.putScanRecommended(context, BRSharedPrefs.getCurrentWalletIso(context), false);
                            }
                        });
                    }
                });
            case SHARE_DATA:
                return new PromptInfo(context.getString(R.string.Prompts_ShareData_title), context.getString(R.string.Prompts_ShareData_body), new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                Intent intent = new Intent(context, ShareDataActivity.class);
                                context.startActivity(intent);
                                context.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                            }
                        });
                    }
                });

        }
        return null;
    }

    /**
     * touchIdPrompt - Shown to the user to enable biometric authentication for purchases under a certain amount.
     * paperKeyPrompt - Shown to the user if they have not yet written down their paper key. This is a persistent prompt and shows up until the user has gone through the paper key flow.
     * upgradePinPrompt - Shown to recommend to the user they should upgrade their PIN from 4 digits to 6. Only shown once. If the user dismisses do not show again.
     * recommendRescanPrompt - Shown when the user should rescan the blockchain
     * noPasscodePrompt - Shown when the user does not have a passcode set up for their device.
     * shareDataPrompt - Shown when asking the user if they wish to share anonymous data. Lowest priority prompt. Only show once and if they dismiss do not show again.
     */
    public String getPromptName(PromptItem prompt) {
        if (prompt == null) return null;
        switch (prompt) {
            case FINGER_PRINT:
                return "touchIdPrompt";
            case PAPER_KEY:
                return "paperKeyPrompt";
            case UPGRADE_PIN:
                return "upgradePinPrompt";
            case RECOMMEND_RESCAN:
                return "recommendRescanPrompt";
            case NO_PASSCODE:
                return "noPasscodePrompt";
            case SHARE_DATA:
                return "shareDataPrompt";

        }
        return null;
    }

}
