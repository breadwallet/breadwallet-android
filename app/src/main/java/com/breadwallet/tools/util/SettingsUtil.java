package com.breadwallet.tools.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.ManageWalletsActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.activities.settings.AboutActivity;
import com.breadwallet.presenter.activities.settings.DisplayCurrencyActivity;
import com.breadwallet.presenter.activities.settings.FingerprintActivity;
import com.breadwallet.presenter.activities.settings.ImportActivity;
import com.breadwallet.presenter.activities.settings.NodesActivity;
import com.breadwallet.presenter.activities.settings.SegWitActivity;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.settings.ShareDataActivity;
import com.breadwallet.presenter.activities.settings.SpendLimitActivity;
import com.breadwallet.presenter.activities.settings.SyncBlockchainActivity;
import com.breadwallet.presenter.activities.settings.UnlinkActivity;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/27/18.
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
public final class SettingsUtil {

    private static final String MARKET_URI = "market://details?id=com.breadwallet";
    private static final String GOOGLE_PLAY_URI = "https://play.google.com/store/apps/details?id=com.breadwallet";
    private static final String APP_STORE_PACKAGE = "com.android.vending";

    private SettingsUtil() {
    }

    public static List<BRSettingsItem> getMainSettings(final Activity activity) {
        List<BRSettingsItem> settingsItems = new ArrayList<>();
        final BaseWalletManager walletManager = WalletsMaster.getInstance(activity).getCurrentWallet(activity);

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_scan), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.openScanner(activity, BRConstants.SCANNER_REQUEST);
            }
        }, false, R.drawable.ic_camera));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_manageWallets), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, ManageWalletsActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.ic_wallet));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.Settings_preferences), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_PREFERENCES);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.ic_preferences));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_security), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, SettingsActivity.class);
                intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SECURITY);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.ic_security_settings));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_support), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.showSupportFragment((FragmentActivity) activity, null, walletManager);
            }
        }, false, R.drawable.ic_support));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.About_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, AboutActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.ic_about));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.Settings_review), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI));
                    appStoreIntent.setPackage(APP_STORE_PACKAGE);

                    activity.startActivity(appStoreIntent);
                } catch (android.content.ActivityNotFoundException exception) {
                    activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(GOOGLE_PLAY_URI)));
                }
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.ic_review));
        return settingsItems;
    }

    public static List<BRSettingsItem> getPreferencesSettings(final Activity activity) {
        List<BRSettingsItem> items = new ArrayList<>();

        String currentFiatCode = BRSharedPrefs.getPreferredFiatIso(activity);
        items.add(new BRSettingsItem(activity.getString(R.string.Settings_currency), currentFiatCode, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, DisplayCurrencyActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        }, false, 0));
        final WalletBitcoinManager walletBitcoinManager = WalletBitcoinManager.getInstance(activity);
        String bitcoinSettingsLabel = String.format("%s %s", walletBitcoinManager.getName(), activity.getString(R.string.Settings_title));
        items.add(new BRSettingsItem(bitcoinSettingsLabel, currentFiatCode, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putCurrentWalletIso(activity, walletBitcoinManager.getIso());
                startCurrencySettings(activity);
            }
        }, false, 0));
        final WalletBchManager walletBchManager = WalletBchManager.getInstance(activity);
        String bchSettingsLabel = String.format("%s %s", walletBchManager.getName(), activity.getString(R.string.Settings_title));

        items.add(new BRSettingsItem(bchSettingsLabel, currentFiatCode, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRSharedPrefs.putCurrentWalletIso(activity, walletBchManager.getIso());
                startCurrencySettings(activity);
            }
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.Prompts_ShareData_title), currentFiatCode, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, ShareDataActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));
        return items;
    }

    private static void startCurrencySettings(Activity activity) {
        Intent intent = new Intent(activity, SettingsActivity.class);
        intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_CURRENCY_SETTINGS);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    public static List<BRSettingsItem> getSecuritySettings(final Activity activity) {
        List<BRSettingsItem> items = new ArrayList<>();
        items.add(new BRSettingsItem(activity.getString(R.string.TouchIdSettings_switchLabel_android), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, FingerprintActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

            }
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.UpdatePin_updateTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, InputPinActivity.class);
                intent.putExtra(InputPinActivity.EXTRA_PIN_MODE_UPDATE, true);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                activity.startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.SecurityCenter_paperKeyTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, WriteDownActivity.class);
                intent.putExtra(WriteDownActivity.EXTRA_VIEW_REASON, WriteDownActivity.ViewReason.SETTINGS.getValue());
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
            }
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.Settings_wipe), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, UnlinkActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));
        return items;
    }

    public static List<BRSettingsItem> getBitcoinSettings(final Context context) {
        List<BRSettingsItem> items = new ArrayList<>();
        if (AuthManager.isFingerPrintAvailableAndSetup(context)) {
            items.add(new BRSettingsItem(context.getString(R.string.Settings_touchIdLimit_android), "", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    final Activity currentActivity = (Activity) view.getContext();
                    AuthManager.getInstance().authPrompt(currentActivity, null,
                            currentActivity.getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                                @Override
                                public void onComplete() {
                                    Intent intent = new Intent(currentActivity, SpendLimitActivity.class);
                                    currentActivity.startActivity(intent);
                                    currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                                }

                                @Override
                                public void onCancel() {

                                }
                            });

                }
            }, false, 0));
        }
        items.add(new BRSettingsItem(context.getString(R.string.Settings_importTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!UiUtils.isClickAllowed()) return;
                Activity currentActivity = (Activity) view.getContext();
                Intent intent = new Intent(currentActivity, ImportActivity.class);
                currentActivity.startActivity(intent);
                currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));

        items.add(new BRSettingsItem(context.getString(R.string.ReScan_header), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!UiUtils.isClickAllowed()) return;
                Activity currentActivity = (Activity) view.getContext();
                Intent intent = new Intent(currentActivity, SyncBlockchainActivity.class);
                currentActivity.startActivity(intent);
                currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));

        //add that for all currencies
        items.add(new BRSettingsItem(context.getString(R.string.NodeSelector_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Activity currentActivity = (Activity) view.getContext();
                Intent intent = new Intent(currentActivity, NodesActivity.class);
                currentActivity.startActivity(intent);
                currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));
        if (!BRSharedPrefs.getIsSegwitEnabled(context)) {
            items.add(new BRSettingsItem(context.getString(R.string.Settings_EnableSegwit), "", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Activity currentActivity = (Activity) view.getContext();
                    Intent intent = new Intent(currentActivity, SegWitActivity.class);
                    currentActivity.startActivity(intent);
                    currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, 0));
        } else {
            items.add(new BRSettingsItem(context.getString(R.string.Settings_ViewLegacyAddress), "", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    UiUtils.showLegacyAddressFragment((FragmentActivity) view.getContext());
                }
            }, false, 0));
        }

        return items;
    }

    public static List<BRSettingsItem> getBitcoinCashSettings(final Context context) {
        List<BRSettingsItem> items = new ArrayList<>();
        items.add(new BRSettingsItem(context.getString(R.string.Settings_importTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                Activity currentActivity = (Activity) v.getContext();
                Intent intent = new Intent(currentActivity, ImportActivity.class);
                currentActivity.startActivity(intent);
                currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));

        items.add(new BRSettingsItem(context.getString(R.string.ReScan_header), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                Activity currentActivity = (Activity) v.getContext();
                Intent intent = new Intent(currentActivity, SyncBlockchainActivity.class);
                currentActivity.startActivity(intent);
                currentActivity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, 0));

        return items;
    }

}
