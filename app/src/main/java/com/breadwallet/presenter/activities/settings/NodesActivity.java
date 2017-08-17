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
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TrustedNode;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;

import static com.platform.HTTPServer.URL_SUPPORT;

public class NodesActivity extends BRActivity {
    private static final String TAG = NodesActivity.class.getName();
    private Button switchButton;
    private TextView nodeStatus;
    private TextView trustNode;
    public static boolean appVisible = false;
    AlertDialog mDialog;
    private int mInterval = 3000;
    private Handler mHandler;
//    private TextView nodeLabel;

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateStatus(); //this function can change value of mInterval.
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

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Activity app = NodesActivity.this;
                Intent intent = new Intent(app, WebViewActivity.class);
                intent.putExtra("url", URL_SUPPORT);
                intent.putExtra("articleId", BRConstants.reScan);
                app.startActivity(intent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        updateButtonText();

        nodeStatus = (TextView) findViewById(R.id.node_status);
        trustNode = (TextView) findViewById(R.id.node_text);

        switchButton = (Button) findViewById(R.id.button_switch);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;

                if (!BRSharedPrefs.getTrustNode(NodesActivity.this).isEmpty()) {
                    createDialog();

                } else {
                    BRSharedPrefs.putTrustNode(NodesActivity.this, "");
                    BRPeerManager.getInstance().updateFixedPeer(NodesActivity.this);
                    updateButtonText();
                }

            }
        });

    }

    private void updateButtonText() {
        if (BRSharedPrefs.getTrustNode(this).isEmpty()) {
            switchButton.setText("Switch to Manual Mode");
        } else {
            switchButton.setText("Switch to Automatic Mode");
        }
    }

    private void createDialog() {

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(app);
        final TextView customTitle = new TextView(this);

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad = Utils.getPixelsFromDps(app, 32);
        customTitle.setPadding(pad, pad, pad, pad);
        customTitle.setText("Enter Node");
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);
        alertDialog.setMessage("Enter node ip address and port");

        final EditText input = new EditText(app);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(app, 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        alertDialog.setView(input);

        alertDialog.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton("OK",
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
                    BRPeerManager.getInstance().updateFixedPeer(app);
                    mDialog.dismiss();
                } else {
                    customTitle.setText("invalid node");
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            customTitle.setText("set a trusted node");
                        }
                    }, 1000);
                }
                updateButtonText();
            }
        });
    }

    private void updateStatus() {
        if (trustNode != null)
            trustNode.setText(BRPeerManager.getInstance().getCurrentPeerName());
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
