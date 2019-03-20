package com.breadwallet.view.dialog;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.util.BRConstants;

/**
 * This is a transparent activity used to host dialog fragments. {@link DialogActivity#startDialogActivity} should
 * be used to show a dialog of the caller's choosing.
 */
// TODO: Move BRDialog code into here and clean up.
public class DialogActivity extends AppCompatActivity {
    private static final String TAG = DialogActivity.class.getName();

    private static final String DIALOG_TYPE_EXTRA = "com.breadwallet.view.dialog.DialogActivity";
    private static final String BRD_SUPPORT_EMAIL = "support@brd.com";
    private static final String PACKAGE_PREFIX = "package:";

    public enum DialogType {
        DEFAULT,
        ENABLE_DEVICE_PASSWORD,
        KEY_STORE_INVALID_WIPE,
        KEY_STORE_INVALID_UNINSTALL;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog);

        Intent intent = getIntent();
        if (intent != null) {
            String dialogTypeName = intent.getStringExtra(DIALOG_TYPE_EXTRA);
            if (dialogTypeName != null) {
                switch (DialogType.valueOf(dialogTypeName)) {
                    case ENABLE_DEVICE_PASSWORD:
                        showEnableDevicePasswordDialog();
                        break;
                    case KEY_STORE_INVALID_WIPE:
                        showKeyStoreInvalidDialogAndWipe();
                        break;
                    case KEY_STORE_INVALID_UNINSTALL:
                        showKeyStoreInvalidDialogAndUninstall();
                        break;
                    case DEFAULT:
                        // Fall through
                    default:
                        throw new IllegalArgumentException("Invalid dialog type");
                }
            }
        }
    }

    /**
     * Show a dialog of the specified type.
     *
     * @param context The context in which we are operating.
     * @param dialogType The type of dialog that should be shown. See {@link DialogType}.
     */
    public static void startDialogActivity(Context context, DialogType dialogType) {
        Intent intent = new Intent(context, DialogActivity.class);
        intent.putExtra(DIALOG_TYPE_EXTRA, dialogType.name());

        if (!(context instanceof Activity)) {
            // If the activity is being started from the application context, this flag is needed.
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        context.startActivity(intent);
    }

    //region Specific dialog methods
    private void showEnableDevicePasswordDialog() {
        BRDialogView dialog = showDialog(
                getString(R.string.JailbreakWarnings_title),
                getString(R.string.Prompts_NoScreenLock_body_android),
                getString(R.string.Button_securitySettings_android),
                getString(R.string.AccessibilityLabels_close),
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        // Open password settings so the user can enable a password easily.
                        Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            Log.e(TAG, "showEnableDevicePasswordDialog: Security Settings button failed.");
                        }

                        finish();
                    }
                },
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        // Close the app.
                        finishAffinity();
                    }
                },
                null
        );

        dialog.setCancelable(false);
    }

    private void showKeyStoreInvalidDialogAndWipe() {
        showKeyStoreInvalidDialog(
                getString(R.string.Alert_keystore_invalidated_wipe_android),
                getString(R.string.Button_wipe_android),
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        Log.d(TAG, "showKeyStoreInvalidDialogAndWipe: Clearing app data.");
                        BreadApp.clearApplicationUserData();
                    }
                });
    }

    private void showKeyStoreInvalidDialogAndUninstall() {
        showKeyStoreInvalidDialog(
                getString(R.string.Alert_keystore_invalidated_uninstall_android),
                getString(R.string.Button_uninstall_android),
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        Log.d(TAG, "showKeyStoreInvalidDialogAndUninstall: Uninstalling");
                        Intent intent = new Intent(Intent.ACTION_DELETE);
                        intent.setData(Uri.parse(PACKAGE_PREFIX + BuildConfig.APPLICATION_ID));

                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            Log.e(TAG, "showKeyStoreInvalidDialogAndUninstall: Uninstall button failed.");
                        }
                    }
                });
    }

    private void showKeyStoreInvalidDialog(String message, String positiveButton, BRDialogView.BROnClickListener positiveListener) {
        BRDialogView dialog = showDialog(
                getString(R.string.Alert_keystore_title_android),
                message,
                positiveButton,
                getString(R.string.Button_contactSupport_android),
                positiveListener,
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        intent.setType(BRConstants.CONTENT_TYPE_TEXT);
                        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{BRD_SUPPORT_EMAIL});
                        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.Alert_keystore_title_android));
                        if (intent.resolveActivity(getPackageManager()) != null) {
                            startActivity(intent);
                        } else {
                            Log.e(TAG, "showKeyStoreInvalidDialog: Customer support button failed.");
                        }
                    }
                },
                null
        );

        dialog.setCancelable(false);
    }

    //endRegion

    //region Generic dialog methods TODO: Move generic BRDialog methods here.
    private BRDialogView showDialog(@NonNull String title, @NonNull String message, @NonNull String positiveButton,
                                    String negativeButton, BRDialogView.BROnClickListener positiveListener,
                                    BRDialogView.BROnClickListener negativeListener,
                                    DialogInterface.OnDismissListener dismissListener) {
        BRDialogView dialog = new BRDialogView();
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setPosButton(positiveButton);
        dialog.setNegButton(negativeButton);
        dialog.setPosListener(positiveListener);
        dialog.setNegListener(negativeListener);
        dialog.setDismissListener(dismissListener);
        getFragmentManager().beginTransaction().add(dialog, dialog.getTag()).commit();
        return dialog;
    }
    //endRegion
}
