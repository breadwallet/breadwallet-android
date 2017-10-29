package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;

import static com.breadwallet.tools.util.BRConstants.SCANNER_BCH_REQUEST;

public class WithdrawBchActivity extends BRActivity {
    private static final String TAG = WithdrawBchActivity.class.getName();
    private Button scan; //scan
    private Button paste; //paste
    private Button send; //button_send
    private EditText addressEdit; //address_edit
    private TextView txHash; //tx_hash
    private TextView txIdLabel;
    private TextView description;
    public static String address;

    public static boolean appVisible = false;
    private static WithdrawBchActivity app;

    public static WithdrawBchActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw_bch);

        description = (TextView) findViewById(R.id.description);
        txIdLabel = (TextView) findViewById(R.id.tx_label);
        paste = (Button) findViewById(R.id.paste);
        send = (Button) findViewById(R.id.button_send);
        scan = (Button) findViewById(R.id.scan);
        txHash = (TextView) findViewById(R.id.tx_hash);
        addressEdit = (EditText) findViewById(R.id.address_edit);

        String balance = BRCurrency.getFormattedCurrencyString(this, BRSharedPrefs.getPreferredBTC(this) ? "LTC" : BRSharedPrefs.getIso(this), new BigDecimal(BRWalletManager.getBCashBalance(BRKeyStore.getMasterPublicKey(app))));
        description.setText(String.format(getString(R.string.BCH_body), balance));


        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRClipboardManager.putClipboard(WithdrawBchActivity.this, txHash.getText().toString().trim());
                BRToast.showCustomToast(WithdrawBchActivity.this, getString(R.string.BCH_hashCopiedMessage), BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
            }
        });

        paste.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog[] alert = {null};
                final AlertDialog.Builder builder = new AlertDialog.Builder(WithdrawBchActivity.this);
                if (BRAnimator.isClickAllowed()) {

                    final String bitcoinUrl = BRClipboardManager.getClipboard(WithdrawBchActivity.this);
                    String ifAddress = null;
                    RequestObject obj = BitcoinUrlHandler.getRequestFromString(bitcoinUrl);
                    if (obj == null) {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.Send_invalidAddressOnPasteboard));
                        builder.setNeutralButton(getResources().getString(R.string.Button_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert[0] = builder.create();
                        alert[0].show();
                        BRClipboardManager.putClipboard(WithdrawBchActivity.this, "");
                        return;
                    }
                    ifAddress = obj.address;
                    if (ifAddress == null) {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.Send_invalidAddressOnPasteboard));
                        builder.setNeutralButton(getResources().getString(R.string.Button_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert[0] = builder.create();
                        alert[0].show();
                        BRClipboardManager.putClipboard(WithdrawBchActivity.this, "");
                        return;
                    }
//                    final String finalAddress = tempAddress;
                    BRWalletManager wm = BRWalletManager.getInstance();

                    if (wm.isValidBitcoinPrivateKey(ifAddress) || wm.isValidBitcoinBIP38Key(ifAddress)) {
                        BRWalletManager.getInstance().confirmSweep(WithdrawBchActivity.this, ifAddress);
                        return;
                    }

                    if (BRWalletManager.validateAddress(ifAddress.trim())) {
                        final BRWalletManager m = BRWalletManager.getInstance();
                        final String finalIfAddress = ifAddress;
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final boolean contained = m.addressContainedInWallet(finalIfAddress);
                                final boolean used = m.addressIsUsed(finalIfAddress);
                                if (used)
                                    throw new RuntimeException("address used for BCH? can't happen, we don't keep track");
                                WithdrawBchActivity.this.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (contained) {

                                            //builder.setTitle(getResources().getString(R.string.alert));
                                            builder.setMessage(getResources().getString(R.string.Send_UsedAddress_firstLine));
                                            builder.setNeutralButton(getResources().getString(R.string.Button_ok),
                                                    new DialogInterface.OnClickListener() {
                                                        public void onClick(DialogInterface dialog, int which) {
                                                            dialog.dismiss();
                                                        }
                                                    });
                                            alert[0] = builder.create();
                                            alert[0].show();
                                            BRClipboardManager.putClipboard(WithdrawBchActivity.this, "");

                                        } else {
                                            confirmSendingBCH(WithdrawBchActivity.this, bitcoinUrl);
                                        }
                                    }
                                });
                            }
                        }).start();

                    } else {
                        //builder.setTitle(getResources().getString(R.string.alert));
                        builder.setMessage(getResources().getString(R.string.Send_invalidAddressOnPasteboard));
                        builder.setNeutralButton(getResources().getString(R.string.Button_ok),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alert[0] = builder.create();
                        alert[0].show();
                        BRClipboardManager.putClipboard(WithdrawBchActivity.this, "");
                    }
                }
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.openScanner(WithdrawBchActivity.this, SCANNER_BCH_REQUEST);
            }
        });
        updateUi(this);

    }

    //called from native
    public static void setBCHTxId(String txId) {
        WithdrawBchActivity app = getApp();
        if (app != null) {
            BRSharedPrefs.putBCHTxId(app, txId);
            updateUi(app);
        } else {
            BRSharedPrefs.putBCHTxId(BreadApp.getInstance(), txId);
            Log.e(TAG, "updateUi: app is null");
        }
    }

    public static void updateUi(final Activity app) {
        if (app instanceof WithdrawBchActivity) {
            final WithdrawBchActivity wApp = (WithdrawBchActivity) app;
            wApp.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    String txIdString = BRSharedPrefs.getBCHTxId(app);
                    if (Utils.isNullOrEmpty(txIdString)) {
                        wApp.txIdLabel.setVisibility(View.GONE);
                        wApp.txHash.setVisibility(View.GONE);
                    } else {
                        wApp.txIdLabel.setVisibility(View.VISIBLE);
                        wApp.txHash.setVisibility(View.VISIBLE);
                        wApp.txHash.setText(txIdString);
                    }
                }
            });
        }
    }

    public static void confirmSendingBCH(final Activity app, final String theAddress) {
        if (BRPeerManager.getCurrentBlockHeight() < 478559) { //todo fix translations for this class
            BRDialog.showCustomDialog(app, "Not synced", "Please wait for syncing to complete before using this feature.", app.getString(R.string.AccessibilityLabels_close), null,
                    new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
        } else {
            address = theAddress;
            if (BRWalletManager.getBCashBalance(BRKeyStore.getMasterPublicKey(app)) == 0) {
                BRDialog.showCustomDialog(app, "No balance", "You have 0 BCH", app.getString(R.string.AccessibilityLabels_close), null,
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {

                            }
                        }, null, null, 0);
            } else {
                AuthManager.getInstance().authPrompt(app, "Sending out BCH", theAddress, true, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        PostAuth.getInstance().onSendBch(getApp(), false, address);
                    }

                    @Override
                    public void onCancel() {

                    }
                });

            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
