package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.app.util.UserMetricsUtil;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.activities.settings.FingerprintActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.customviews.PinLayout;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.regex.Pattern;

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

public final class PromptManager {
    private static final String TAG = PromptManager.class.getSimpleName();
    public static final String PROMPT_DISMISSED_FINGERPRINT = "fingerprint";
    private static final String PROMPT_TOUCH_ID = "touchIdPrompt";
    private static final String PROMPT_PAPER_KEY = "paperKeyPrompt";
    private static final String PROMPT_UPGRADE_PIN = "upgradePinPrompt";
    private static final String PROMPT_RECOMMEND_RESCAN = "recommendRescanPrompt";
    private static final String PROMPT_NO_PASSCODE = "noPasscodePrompt";
    private static final int HIDE_PROMPT_DELAY_MILLISECONDS = 3000;
    private static final String EMAIL_REGEX = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";

    private static PromptManager.PromptItem mCurrentPrompt;

    private PromptManager() {
    }

    private static PromptManager instance;

    public static PromptManager getInstance() {
        if (instance == null) instance = new PromptManager();
        return instance;
    }

    public enum PromptItem {
        EMAIL_COLLECTION,
        FINGER_PRINT,
        PAPER_KEY,
        UPGRADE_PIN,
        RECOMMEND_RESCAN,
        NO_PASSCODE
    }

    private static boolean shouldPrompt(Context context, PromptItem promptItem) {
        assert (context != null);
        switch (promptItem) {
            case EMAIL_COLLECTION:
                return !BRSharedPrefs.getEmailOptIn(context) && !BRSharedPrefs.getEmailOptInDismissed(context);
            case FINGER_PRINT:
                return !BRSharedPrefs.getUseFingerprint(context) && Utils.isFingerprintAvailable(context)
                        && !BRSharedPrefs.getPromptDismissed(context, PROMPT_DISMISSED_FINGERPRINT);
            case PAPER_KEY:
                return !BRSharedPrefs.getPhraseWroteDown(context);
            case UPGRADE_PIN:
                return BRKeyStore.getPinCode(context).length() != PinLayout.MAX_PIN_DIGITS;
            case RECOMMEND_RESCAN:
                BaseWalletManager wallet = WalletsMaster.getInstance().getCurrentWallet(context);
                return wallet != null && BRSharedPrefs.getScanRecommended(context, wallet.getCurrencyCode());
        }
        return false;
    }

    public static PromptItem nextPrompt(Context context) {
        if (shouldPrompt(context, PromptItem.RECOMMEND_RESCAN)) {
            mCurrentPrompt = PromptItem.RECOMMEND_RESCAN;
            return mCurrentPrompt;
        }
        if (shouldPrompt(context, PromptItem.UPGRADE_PIN)) {
            mCurrentPrompt = PromptItem.UPGRADE_PIN;
            return mCurrentPrompt;
        }
        if (shouldPrompt(context, PromptItem.PAPER_KEY)) {
            mCurrentPrompt = PromptItem.PAPER_KEY;
            return mCurrentPrompt;
        }
        if (shouldPrompt(context, PromptItem.FINGER_PRINT)) {
            mCurrentPrompt = PromptItem.FINGER_PRINT;
            return mCurrentPrompt;
        }
        if (shouldPrompt(context, PromptItem.EMAIL_COLLECTION)) {
            mCurrentPrompt = PromptItem.EMAIL_COLLECTION;
            return mCurrentPrompt;
        }
        return null;
    }

    public static View promptInfo(final Activity context, final PromptItem promptItem) {
        final View baseLayout = context.getLayoutInflater().inflate(R.layout.base_prompt, null);
        BaseTextView title = baseLayout.findViewById(R.id.prompt_title);
        BaseTextView description = baseLayout.findViewById(R.id.prompt_description);
        BRButton continueButton = baseLayout.findViewById(R.id.continue_button);
        BRButton dismissButton = baseLayout.findViewById(R.id.dismiss_button);
        dismissButton.setColor(context.getColor(R.color.settings_chevron_right));
        continueButton.setColor(context.getColor(R.color.button_add_wallet_text));
        dismissButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                hidePrompt(context, baseLayout);
            }
        });
        switch (promptItem) {
            case FINGER_PRINT:
                title.setText(context.getString(R.string.Prompts_TouchId_title_android));
                description.setText(context.getString(R.string.Prompts_TouchId_body_android));
                continueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, FingerprintActivity.class);
                        context.startActivity(intent);
                        context.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        sendPromptClickedEvent(promptItem);
                    }
                });
                break;
            case PAPER_KEY:
                title.setText(context.getString(R.string.Prompts_PaperKey_title));
                description.setText(context.getString(R.string.Prompts_PaperKey_body));
                continueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, WriteDownActivity.class);
                        intent.putExtra(WriteDownActivity.EXTRA_VIEW_REASON, WriteDownActivity.ViewReason.SETTINGS.getValue());
                        context.startActivity(intent);
                        context.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                        sendPromptClickedEvent(promptItem);
                    }
                });
                break;
            case UPGRADE_PIN:
                title.setText(context.getString(R.string.Prompts_UpgradePin_title));
                description.setText(context.getString(R.string.Prompts_UpgradePin_body));
                continueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(context, InputPinActivity.class);
                        intent.putExtra(InputPinActivity.EXTRA_PIN_MODE_UPDATE, true);
                        context.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        context.startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
                        sendPromptClickedEvent(promptItem);
                    }
                });
                break;
            case RECOMMEND_RESCAN:
                title.setText(context.getString(R.string.Prompts_RecommendRescan_title));
                description.setText(context.getString(R.string.Prompts_RecommendRescan_body));
                continueButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                BRSharedPrefs.putStartHeight(context, BRSharedPrefs.getCurrentWalletCurrencyCode(context), 0);
                                BaseWalletManager wallet = WalletsMaster.getInstance().getCurrentWallet(context);
                                wallet.rescan(context);
                                BRSharedPrefs.putScanRecommended(context, BRSharedPrefs.getCurrentWalletCurrencyCode(context), false);
                                sendPromptClickedEvent(promptItem);
                            }
                        });
                    }
                });
                break;
            case EMAIL_COLLECTION:
                final View customLayout = context.getLayoutInflater().inflate(R.layout.email_prompt, null);
                BaseTextView customTitle = customLayout.findViewById(R.id.prompt_title);
                BaseTextView customDescription = customLayout.findViewById(R.id.prompt_description);
                final BaseTextView customConfirmation = customLayout.findViewById(R.id.prompt_confirmation);
                final BRButton submitButton = customLayout.findViewById(R.id.submit_button);
                ImageView closeButton = customLayout.findViewById(R.id.close_button);
                final ImageView promptIcon = customLayout.findViewById(R.id.prompt_icon);
                final BREdit emailEditText = customLayout.findViewById(R.id.email_edit);
                submitButton.setColor(context.getColor(R.color.create_new_wallet_button_dark));
                customTitle.setText(context.getString(R.string.Prompts_Email_title));
                customDescription.setText(context.getString(R.string.Prompts_Email_body));
                closeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        hidePrompt(context, customLayout);
                        BRSharedPrefs.putEmailOptInDismissed(context, true);
                    }
                });
                submitButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        final String email = emailEditText.getText().toString().trim();
                        if (isEmailValid(email)) {
                            UserMetricsUtil.makeEmailOptInRequest(context, email);
                            promptIcon.setImageResource(R.drawable.ic_yay);
                            emailEditText.setVisibility(View.INVISIBLE);
                            submitButton.clearAnimation();
                            submitButton.setVisibility(View.INVISIBLE);
                            customConfirmation.setVisibility(View.VISIBLE);
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    if (!context.isDestroyed()) {
                                        hidePrompt(context, customLayout);
                                    }
                                }
                            }, HIDE_PROMPT_DELAY_MILLISECONDS);
                            BRSharedPrefs.putEmailOptIn(context, true);
                            sendPromptClickedEvent(promptItem);
                        } else {
                            SpringAnimator.failShakeAnimation(context, emailEditText);
                        }
                    }
                });
                return customLayout;
        }
        return baseLayout;
    }

    private static void sendPromptClickedEvent(PromptItem promptItem) {
        EventUtils.pushEvent(EventUtils.EVENT_PROMPT_PREFIX
                + PromptManager.getPromptName(promptItem) + EventUtils.EVENT_PROMPT_SUFFIX_TRIGGER);
    }

    private static boolean isEmailValid(String email) {
        return email != null && Pattern.compile(EMAIL_REGEX).matcher(email).matches();
    }

    private static void hidePrompt(Context context, View layout) {
        ViewGroup parentView = (ViewGroup) layout.getParent();
        if (parentView != null) {
            parentView.removeView(layout);
        }
        layout.setVisibility(View.GONE);
        if (mCurrentPrompt == PromptManager.PromptItem.FINGER_PRINT) {
            BRSharedPrefs.putPromptDismissed(context, PromptManager.PROMPT_DISMISSED_FINGERPRINT, true);
        }
        if (mCurrentPrompt != null) {
            EventUtils.pushEvent(EventUtils.EVENT_PROMPT_PREFIX
                    + getPromptName(mCurrentPrompt) + EventUtils.EVENT_PROMPT_SUFFIX_DISMISSED);
        }
        mCurrentPrompt = null;
    }

    /**
     * touchIdPrompt - Shown to the user to enable biometric authentication for purchases under a certain amount.
     * paperKeyPrompt - Shown to the user if they have not yet written down their paper key.
     * This is a persistent prompt and shows up until the user has gone through the paper key flow.
     * upgradePinPrompt - Shown to recommend to the user they should upgrade their PIN from 4 digits to 6. Only shown once. If the user dismisses do not show again.
     * recommendRescanPrompt - Shown when the user should rescan the blockchain
     * noPasscodePrompt - Shown when the user does not have a passcode set up for their device.
     * shareDataPrompt - Shown when asking the user if they wish to share anonymous data. Lowest priority prompt. Only show once and if they dismiss do not show again.
     */
    public static String getPromptName(PromptItem prompt) {
        if (prompt == null) return null;
        switch (prompt) {
            case FINGER_PRINT:
                return PROMPT_TOUCH_ID;
            case PAPER_KEY:
                return PROMPT_PAPER_KEY;
            case UPGRADE_PIN:
                return PROMPT_UPGRADE_PIN;
            case RECOMMEND_RESCAN:
                return PROMPT_RECOMMEND_RESCAN;
            case NO_PASSCODE:
                return PROMPT_NO_PASSCODE;
        }
        return null;
    }
}
