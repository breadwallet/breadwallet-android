package com.breadwallet.legacy.presenter.activities.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.legacy.presenter.activities.util.BRActivity;
import com.breadwallet.legacy.wallet.WalletsMaster;
import com.breadwallet.legacy.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.TrustedNode;
import com.breadwallet.tools.util.Utils;

public class NodesActivity extends BRActivity {
    private static final String TAG = NodesActivity.class.getName();
    private Button switchButton;
    private TextView nodeStatus;
    private TextView trustNode;
    AlertDialog mDialog;
    private int mInterval = 3000;
    private Handler mHandler;
    private boolean updatingNode;
//    private TextView nodeLabel;

    //todo  run this on effecthandler
    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                //this function can change value of mInterval.
                updateButtonText();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nodes);

//        ImageButton faq = findViewById(R.id.faq_button);
//        faq.setVisibility(View.GONE);

        BRSharedPrefs.putCurrentWalletCurrencyCode(this, "BTC");

        nodeStatus = findViewById(R.id.node_status);
        trustNode = findViewById(R.id.node_text);

        switchButton = findViewById(R.id.button_switch);
        switchButton.setOnClickListener(v -> {
            if (!UiUtils.isClickAllowed()) return;
            final Activity app = NodesActivity.this;
            final WalletBitcoinManager wm = WalletBitcoinManager.getInstance(NodesActivity.this);

            if (BRSharedPrefs.getTrustNode(app, wm.getCurrencyCode()).isEmpty()) {
                createDialog();
            } else {
                if (!updatingNode) {
                    updatingNode = true;
                    BRSharedPrefs.putTrustNode(app, wm.getCurrencyCode(), "");
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            Thread.currentThread().setName("BG:" + TAG + ":updateFixedPeer");
                            WalletsMaster.getInstance().updateFixedPeer(app, wm);
                            updatingNode = false;
                            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                @Override
                                public void run() {
                                    updateButtonText();
                                }
                            });

                        }
                    });
                }

            }

        });

        updateButtonText();

    }

    private void updateButtonText() {
        WalletBitcoinManager wm = WalletBitcoinManager.getInstance(this);
        if (BRSharedPrefs.getTrustNode(this, wm.getCurrencyCode()).isEmpty()) {
            switchButton.setText(getString(R.string.NodeSelector_manualButton));
        } else {
            switchButton.setText(getString(R.string.NodeSelector_automaticButton));
        }
        nodeStatus.setText(wm.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Connected ?
                getString(R.string.NodeSelector_connected) : getString(R.string.NodeSelector_notConnected));
        if (trustNode != null)
            trustNode.setText(wm.getPeerManager().getCurrentPeerName());
    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(this);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(this, 32);
        int pad16 = Utils.getPixelsFromDps(this, 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.NodeSelector_enterTitle));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.NodeSelector_enterBody));

        final EditText input = new EditText(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(this, 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                (dialog, which) -> dialog.cancel());

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                (dialog, which) -> {

                });

        mDialog = alertDialog.show();

        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String str = input.getText().toString();
            final WalletBitcoinManager wm = WalletBitcoinManager.getInstance(NodesActivity.this);
            if (TrustedNode.isValid(str)) {
                mDialog.setMessage("");
                BRSharedPrefs.putTrustNode(NodesActivity.this, wm.getCurrencyCode(), str);
                if (!updatingNode) {
                    updatingNode = true;
                    customTitle.setText(getString(R.string.Webview_updating));
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            Thread.currentThread().setName("BG:" + TAG + ":updateFixedPeer");
                            WalletsMaster.getInstance().updateFixedPeer(NodesActivity.this, wm);
                            updatingNode = false;
                            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                @Override
                                public void run() {
                                    Thread.currentThread().setName("Ui:" + TAG + ":custom node title set");
                                    customTitle.setText(getString(R.string.RecoverWallet_done));
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            mDialog.dismiss();
                                            updateButtonText();
                                        }
                                    }, 500);

                                }
                            });
                        }
                    });
                }

            } else {
                customTitle.setText("Invalid Node");
                customTitle.setTextColor(NodesActivity.this.getColor(R.color.warning_color));
                new Handler().postDelayed(() -> {
                    customTitle.setText(getString(R.string.NodeSelector_enterTitle));
                    customTitle.setTextColor(NodesActivity.this.getColor(R.color.almost_black));
                }, 1000);
            }
            updateButtonText();
        });
        new Handler().postDelayed(() -> {
            input.requestFocus();
            final InputMethodManager keyboard = (InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            keyboard.showSoftInput(input, 0);
        }, 200);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler = new Handler();
        startRepeatingTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopRepeatingTask();
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
