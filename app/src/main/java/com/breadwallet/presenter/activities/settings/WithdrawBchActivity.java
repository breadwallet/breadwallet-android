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
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.BRWalletManager;

import static com.breadwallet.tools.util.BRConstants.SCANNER_BCH_REQUEST;

public class WithdrawBchActivity extends BRActivity {
    private static final String TAG = WithdrawBchActivity.class.getName();
    private Button scan; //scan
    private Button paste; //paste
    private Button send; //button_send
    private EditText addressEdit; //address_edit
    private TextView txHash; //tx_hash
    private TextView txIdLabel;
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

//        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);
//
//        faq.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Activity app = WithdrawBchActivity.this;
//                Intent intent = new Intent(app, WebViewActivity.class);
//                intent.putExtra("url", URL_SUPPORT);
//                intent.putExtra("articleId", BRConstants.reScan);
//                app.startActivity(intent);
//                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
//            }
//        });
        txIdLabel = (TextView) findViewById(R.id.tx_label);
        paste = (Button) findViewById(R.id.paste);
        send = (Button) findViewById(R.id.button_send);
        scan = (Button) findViewById(R.id.scan);
        txHash = (TextView) findViewById(R.id.tx_hash);
        addressEdit = (EditText) findViewById(R.id.address_edit);


        txHash.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BRClipboardManager.putClipboard(WithdrawBchActivity.this, txHash.getText().toString().trim());
                BRToast.showCustomToast(WithdrawBchActivity.this, "Transaction Id copied", BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
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
                        builder.setMessage(getResources().getString(R.string.clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
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
                        builder.setMessage(getResources().getString(R.string.clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
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
                                            builder.setMessage(getResources().getString(R.string.address_already_in_your_wallet));
                                            builder.setNeutralButton(getResources().getString(R.string.ok),
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
                        builder.setMessage(getResources().getString(R.string.clipboard_invalid_data));
                        builder.setNeutralButton(getResources().getString(R.string.ok),
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
//                BRDialog.showCustomDialog(WithdrawBchActivity.this, "Sync with Blockchain?",
//                        "You will not be able to send money while syncing.", "Sync", "Cancel",
//                        new BRDialogView.BROnClickListener() {
//                            @Override
//                            public void onClick(BRDialogView brDialogView) {
//                                brDialogView.dismissWithAnimation();
//                                new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        BRSharedPrefs.putStartHeight(WithdrawBchActivity.this, 0);
//                                        BRPeerManager.getInstance().rescan();
//                                        BRAnimator.startBreadActivity(WithdrawBchActivity.this, false);
//
//                                    }
//                                }).start();
//                            }
//                        }, new BRDialogView.BROnClickListener() {
//                            @Override
//                            public void onClick(BRDialogView brDialogView) {
//                                brDialogView.dismissWithAnimation();
//                            }
//                        }, null, 0);
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
            Activity activity = BreadApp.getBreadContext();
            BRSharedPrefs.putBCHTxId(activity, txId);
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
        if (BRPeerManager.getCurrentBlockHeight() < 478559) {
            BRDialog.showCustomDialog(app, "Not synced", "Please wait for syncing to complete before using this feature.", "close", null,
                    new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {

                        }
                    }, null, null, 0);
        } else {
            address = theAddress;
            if (BRWalletManager.getBCashBalance(KeyStoreManager.getMasterPublicKey(app)) == 0) {
                BRDialog.showCustomDialog(app, "No balance", "You have 0 BCH", "close", null,
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {

                            }
                        }, null, null, 0);
            } else {
                AuthManager.getInstance().authPrompt(app, "Sending out BCH", theAddress, true, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        PostAuthenticationProcessor.getInstance().onSendBch(getApp(), false, address);
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
