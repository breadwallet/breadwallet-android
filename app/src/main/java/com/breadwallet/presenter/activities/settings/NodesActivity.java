package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.TrustedNode;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;


public class NodesActivity extends BRActivity {
    private static final String TAG = NodesActivity.class.getName();
    private Button switchButton;
    private TextView nodeStatus;
    private TextView trustNode;
    public static boolean appVisible = false;
    AlertDialog mDialog;
    private int mInterval = 3000;
    private Handler mHandler;
    private boolean updatingNode;
//    private TextView nodeLabel;

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
    private static NodesActivity app;


    public static NodesActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nodes);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);
        faq.setVisibility(View.GONE);

        nodeStatus = (TextView) findViewById(R.id.node_status);
        trustNode = (TextView) findViewById(R.id.node_text);

        switchButton = (Button) findViewById(R.id.button_switch);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;

                if (BRSharedPrefs.getTrustNode(NodesActivity.this).isEmpty()) {
                    createDialog();
                } else {
                    if (!updatingNode) {
                        updatingNode = true;
                        BRSharedPrefs.putTrustNode(NodesActivity.this, "");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                BRPeerManager.getInstance().updateFixedPeer(NodesActivity.this);
                                updatingNode = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        updateButtonText();
                                    }
                                });

                            }
                        }).start();
                    }

                }

            }
        });

        updateButtonText();

    }

    private void updateButtonText() {
        if (BRSharedPrefs.getTrustNode(this).isEmpty()) {
            switchButton.setText(getString(R.string.NodeSelector_manualButton));
        } else {
            switchButton.setText(getString(R.string.NodeSelector_automaticButton));
        }
        nodeStatus.setText(BRPeerManager.getInstance().isConnected() ? getString(R.string.NodeSelector_connected) : getString(R.string.NodeSelector_notConnected));
        if (trustNode != null)
            trustNode.setText(BRPeerManager.getInstance().getCurrentPeerName());
    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(app);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad32 = Utils.getPixelsFromDps(app, 32);
        int pad16 = Utils.getPixelsFromDps(app, 16);
        customTitle.setPadding(pad16, pad16, pad16, pad16);
        customTitle.setText(getString(R.string.NodeSelector_enterTitle));
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage(getString(R.string.NodeSelector_enterBody));

        final EditText input = new EditText(app);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(app, 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setNegativeButton(getString(R.string.Button_cancel),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton(getString(R.string.Button_ok),
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        mDialog = alertDialog.show();

        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String str = input.getText().toString();
                if (TrustedNode.isValid(str)) {
                    mDialog.setMessage("");
                    BRSharedPrefs.putTrustNode(app, str);
                    if (!updatingNode) {
                        updatingNode = true;
                        customTitle.setText(getString(R.string.Webview_updating));
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                BRPeerManager.getInstance().updateFixedPeer(app);
                                updatingNode = false;
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
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
                        }).start();
                    }

                } else {
                    customTitle.setText("invalid node");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            customTitle.setText(getString(R.string.NodeSelector_enterTitle));
                        }
                    }, 1000);
                }
                updateButtonText();
            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);
        mHandler = new Handler();
        startRepeatingTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
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
