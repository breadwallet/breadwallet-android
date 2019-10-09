package com.breadwallet.tools.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.app.BreadApp;
import com.breadwallet.legacy.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.legacy.presenter.activities.settings.AboutActivity;
import com.breadwallet.legacy.presenter.activities.settings.DisplayCurrencyActivity;
import com.breadwallet.legacy.presenter.activities.settings.FingerprintActivity;
import com.breadwallet.legacy.presenter.activities.settings.ImportActivity;
import com.breadwallet.legacy.presenter.activities.settings.NodesActivity;
import com.breadwallet.legacy.presenter.activities.settings.SegWitActivity;
import com.breadwallet.legacy.presenter.activities.settings.SettingsActivity;
import com.breadwallet.legacy.presenter.activities.settings.ShareDataActivity;
import com.breadwallet.legacy.presenter.activities.settings.SpendLimitActivity;
import com.breadwallet.legacy.presenter.activities.settings.SyncBlockchainActivity;
import com.breadwallet.legacy.presenter.activities.settings.UnlinkActivity;
import com.breadwallet.legacy.presenter.customviews.BRToast;
import com.breadwallet.legacy.presenter.entities.BRSettingsItem;
import com.breadwallet.legacy.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.legacy.presenter.settings.NotificationsSettingsActivity;
import com.breadwallet.legacy.pricealert.PriceAlertListActivity;
import com.breadwallet.legacy.pricealert.PriceAlertWorker;
import com.breadwallet.legacy.wallet.WalletsMaster;
import com.breadwallet.legacy.wallet.abstracts.BaseWalletManager;
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBchManager;
import com.breadwallet.model.Experiment;
import com.breadwallet.model.Experiments;
import com.breadwallet.repository.ExperimentsRepositoryImpl;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.ui.MainActivity;
import com.breadwallet.ui.navigation.NavigationEffectHandler;
import com.platform.APIClient;
import com.platform.HTTPServer;
import com.platform.RequestBuilderKt;
import com.platform.middlewares.plugins.LinkPlugin;
import com.platform.util.AppReviewPromptManager;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;

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
    private static final String TAG = SettingsUtil.class.getSimpleName();
    private static final String DEVELOPER_OPTIONS_TITLE = "Developer Options";
    private static final String SEND_LOGS = "Send Logs";
    private static final String API_SERVER = "API Server";
    private static final String API_SERVER_SET_FORMAT = "Api server %s set!";
    private static final String ONBOARDING_FLOW = "Onboarding flow";
    private static final String WEB_BUNDLE = "Web Platform Bundle";
    private static final String WEB_PLATFORM_DEBUG_URL = "Web Platform Debug URL";
    private static final String TOKEN_BUNDLE = "Token Bundle";
    private static final String BUNDLE_SET = "Bundle set: ";
    private static final String WEB_PLATFORM_DEBUG_URL_SET = "Web Platform Debug URL set: ";
    private static final String BUNDLE_PROMPT = "Please specify a Bundle";
    private static final String DEBUG_URL_PROMPT = "Please specify a Web Platform Debug URL";
    private static final String APPLY = "Apply";
    private static final String CANCEL = "Cancel";

    private SettingsUtil() {
    }

    public static List<BRSettingsItem> getMainSettings(final Activity activity) {
        List<BRSettingsItem> settingsItems = new ArrayList<>();
        final BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(activity);

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_scan), "", view -> {
            UiUtils.openScanner(activity);
        }, false, R.drawable.ic_camera));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_manageWallets), "", view -> {
            // TODO: Once converted to conductor, route to Manage Wallets controller
        }, false, R.drawable.ic_wallet));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.Settings_preferences), "", view -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_PREFERENCES);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }, false, R.drawable.ic_preferences));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_security), "", view -> {
            Intent intent = new Intent(activity, SettingsActivity.class);
            intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.MODE_SECURITY);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }, false, R.drawable.ic_security_settings));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.MenuButton_support), "", view -> {
            UiUtils.showSupportFragment((FragmentActivity) activity, null, walletManager);
        }, false, R.drawable.ic_support));

        settingsItems.add(new BRSettingsItem(activity.getString(R.string.Settings_review), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AppReviewPromptManager.INSTANCE.openGooglePlay(activity);
            }
        }, false, R.drawable.ic_review));
        settingsItems.add(new BRSettingsItem(activity.getString(R.string.Settings_rewards), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.openRewardsWebView(activity);
            }
        }, false, R.drawable.ic_reward));
        settingsItems.add(new BRSettingsItem(activity.getString(R.string.About_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(activity, AboutActivity.class);
                activity.startActivity(intent);
                activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.ic_about));
        Experiment mapExperiment = ExperimentsRepositoryImpl.INSTANCE.getExperiments().get(Experiments.ATM_MAP.getKey());
        if (mapExperiment != null && mapExperiment.getActive()) {
            settingsItems.add(new BRSettingsItem(activity.getString(R.string.Settings_atmMapMenuItemTitle), "", view -> {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                    String url = HTTPServer.getPlatformUrl(LinkPlugin.BROWSER_PATH);
                    Request request = RequestBuilderKt.buildSignedRequest(url, mapExperiment.getMeta().replace("\\/", "/"), "POST", LinkPlugin.BROWSER_PATH);
                    APIClient.getInstance(activity).sendRequest(request, false);
                });
            }, false, R.drawable.ic_atm_finder, activity.getString(R.string.Settings_atmMapMenuItemSubtitle)));
            activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }
        if (BuildConfig.DEBUG) {
            settingsItems.add(new BRSettingsItem(DEVELOPER_OPTIONS_TITLE, "", new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(activity, SettingsActivity.class);
                    intent.putExtra(SettingsActivity.EXTRA_MODE, SettingsActivity.DEVELOPER_OPTIONS);
                    activity.startActivity(intent);
                    activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, 0));
        }
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

        //TODO: Replace final WalletBitcoinManager walletBitcoinManager = WalletBitcoinManager.getInstance(activity);
        String bitcoinSettingsLabel = String.format("%s %s", "Bitcoin"/*todo: walletBitcoinManager.getName()*/, activity.getString(R.string.Settings_title));
        items.add(new BRSettingsItem(bitcoinSettingsLabel, null, view -> {
            BRSharedPrefs.putCurrentWalletCurrencyCode(activity, NavigationEffectHandler.BITCOIN_CURRENCY_CODE);
            startCurrencySettings(activity);
        }, false, 0));
        final WalletBchManager walletBchManager = WalletBchManager.getInstance(activity);
        String bchSettingsLabel = String.format("%s %s", walletBchManager.getName(), activity.getString(R.string.Settings_title));

        items.add(new BRSettingsItem(bchSettingsLabel, null, view -> {
            BRSharedPrefs.putCurrentWalletCurrencyCode(activity, walletBchManager.getCurrencyCode());
            startCurrencySettings(activity);
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.Prompts_ShareData_title), null, view -> {
            Intent intent = new Intent(activity, ShareDataActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.Settings_notifications), null, view -> {
            NotificationsSettingsActivity.Companion.start(activity);
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
                MainActivity.Companion.openPinUpdate(activity);
            }
        }, false, 0));
        items.add(new BRSettingsItem(activity.getString(R.string.SecurityCenter_paperKeyTitle_android), "", new View.OnClickListener() {
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

    public static List<BRSettingsItem> getDeveloperOptionsSettings(final Activity activity) {
        List<BRSettingsItem> items = new ArrayList<>();
        items.add(new BRSettingsItem(SEND_LOGS, "", view -> LogsUtils.shareLogs(activity), false, 0));
        items.add(new BRSettingsItem(API_SERVER, "", view -> showInputDialog(activity), false, 0));
        items.add(new BRSettingsItem(ONBOARDING_FLOW, "", view -> {
            // TODO: Not supported currently
            Toast.makeText(activity, "TODO: Not Implemented!", Toast.LENGTH_SHORT).show();
            //Intent intent = new Intent(activity, IntroActivity.class);
            //activity.startActivity(intent);
            //activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }, false, 0));

        String currentWebPlatformDebugURL = ServerBundlesHelper.getWebPlatformDebugURL(activity);
        items.add(new BRSettingsItem(WEB_PLATFORM_DEBUG_URL, currentWebPlatformDebugURL, view -> {
            showWebPlatformDebugURLTextDialog(activity, currentWebPlatformDebugURL);
        }, false, 0));

        if (Utils.isNullOrEmpty(currentWebPlatformDebugURL)) {
            String currentWebBundle = ServerBundlesHelper.getBundle(activity, ServerBundlesHelper.Type.WEB);
            items.add(new BRSettingsItem(WEB_BUNDLE, currentWebBundle, view -> {
                showBundleTextDialog(activity, ServerBundlesHelper.Type.WEB, currentWebBundle);
            }, false, 0));
        } else {
            items.add(new BRSettingsItem(WEB_BUNDLE, "(not used if debug URL specified)", view -> {
                // do nothing
            }, false, 0));
        }

        String currentTokenBundle = ServerBundlesHelper.getBundle(activity, ServerBundlesHelper.Type.TOKEN);
        items.add(new BRSettingsItem(TOKEN_BUNDLE, currentTokenBundle, view -> {
            showBundleTextDialog(activity, ServerBundlesHelper.Type.TOKEN, currentTokenBundle);
        }, false, 0));

        items.add(new BRSettingsItem(activity.getString(R.string.PriceAlertList_title), "", view -> {
            Intent intent = new Intent(activity, PriceAlertListActivity.class);
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        }, false, 0));
        items.add(new BRSettingsItem("Check Price Alerts", "", view -> {
            Toast.makeText(view.getContext(), "Checking your Price Alerts in the background.", Toast.LENGTH_LONG).show();
            WorkManager.getInstance().enqueue(OneTimeWorkRequest.from(PriceAlertWorker.class));
        }, false, 0));
        return items;
    }

    private static void showInputDialog(final Context context) {
        LayoutInflater layoutInflater = LayoutInflater.from(context);
        View promptView = layoutInflater.inflate(R.layout.input_api_server_dialog, null);
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setView(promptView);
        final EditText editText = promptView.findViewById(R.id.server_input);
        editText.setText(BreadApp.getHost());
        // setup a dialog window
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton(context.getString(R.string.Button_ok), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        String host = editText.getText().toString().trim();
                        if (Utils.isNullOrEmpty(host)) {
                            BreadApp.setDebugHost(null);
                        } else {
                            BreadApp.setDebugHost(host);
                        }
                        BRToast.showCustomToast(context, String.format(API_SERVER_SET_FORMAT, BreadApp.getHost().toUpperCase()), 0, Toast.LENGTH_LONG, 0);
                    }
                })
                .setNegativeButton(context.getString(R.string.Button_cancel),
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });
        // create an alert dialog
        AlertDialog alert = alertDialogBuilder.create();
        alert.show();
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

    /**
     * Displays an Alert Dialog with an input text field for entering a Bundle setting.
     *
     * @param activity      The activity context.
     * @param bundleType    The type of bundle (whether WEB or TOKEN).
     * @param defaultBundle The default bundle name that will initially appear in the input text field.
     */
    private static void showBundleTextDialog(final Activity activity, final ServerBundlesHelper.Type bundleType, final String defaultBundle) {
        final EditText bundleEditText = new EditText(activity);
        bundleEditText.setText(defaultBundle, TextView.BufferType.EDITABLE);
        AlertDialog bundleDialog = new AlertDialog.Builder(activity)
                .setMessage(BUNDLE_PROMPT)
                .setView(bundleEditText)
                .setPositiveButton(APPLY, (DialogInterface dialogInterface, int which) -> {
                    String bundleName = String.valueOf(bundleEditText.getText());
                    Toast.makeText(activity, BUNDLE_SET + bundleName, Toast.LENGTH_LONG).show();
                    ServerBundlesHelper.setDebugBundle(activity.getApplicationContext(), bundleType, bundleName);
                    activity.recreate();
                })
                .setNegativeButton(CANCEL, null)
                .create();
        bundleDialog.show();
    }

    /**
     * Displays an Alert Dialog with an input text field for entering a Platform Web URL.
     *
     * @param activity              The activity context.
     * @param defaultWebPlatformURL The default URL that will initially appear in the input text field.
     */
    private static void showWebPlatformDebugURLTextDialog(final Activity activity, String defaultWebPlatformURL) {
        final EditText urlEditText = new EditText(activity);
        urlEditText.setText(defaultWebPlatformURL, TextView.BufferType.EDITABLE);
        AlertDialog urlDialog = new AlertDialog.Builder(activity)
                .setMessage(DEBUG_URL_PROMPT)
                .setView(urlEditText)
                .setPositiveButton(APPLY, (DialogInterface dialogInterface, int which) -> {
                    String platformURL = String.valueOf(urlEditText.getText());
                    Toast.makeText(activity, WEB_PLATFORM_DEBUG_URL_SET + platformURL, Toast.LENGTH_LONG).show();
                    ServerBundlesHelper.setWebPlatformDebugURL(activity.getApplicationContext(), platformURL);
                    activity.recreate();
                })
                .setNegativeButton(CANCEL, null)
                .create();
        urlDialog.show();
    }

}
