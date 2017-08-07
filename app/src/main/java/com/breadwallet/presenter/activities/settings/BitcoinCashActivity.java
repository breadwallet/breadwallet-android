package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.animation.BRAnimator;

public class BitcoinCashActivity extends BRActivity {
    private static final String TAG = BitcoinCashActivity.class.getName();
    private Button sendButton;
    private BRButton description;
    private BRButton paste;
    private BRButton scan;
    private BRText txLabel;
    private BRText txHash;
    public static boolean appVisible = false;
    private static BitcoinCashActivity app;

    public static BitcoinCashActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bitcoin_cash);

//        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

//        faq.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Activity app = BitcoinCashActivity.this;
//                Intent intent = new Intent(app, WebViewActivity.class);
//                intent.putExtra("url", URL_SUPPORT);
//                intent.putExtra("articleId", BRConstants.reScan);
//                app.startActivity(intent);
//                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
//            }
//        });
        txLabel = (BRText) findViewById(R.id.tx_label);
        txHash = (BRText) findViewById(R.id.tx_hash);
        description = (BRButton) findViewById(R.id.description);
        sendButton = (BRButton) findViewById(R.id.button_send);
        paste = (BRButton) findViewById(R.id.paste_button);
        scan = (BRButton) findViewById(R.id.scan);
        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
//                BreadDialog.showCustomDialog(BitcoinCashActivity.this, "Sync with Blockchain?",
//                        "You will not be able to send money while syncing.", "Sync", "Cancel",
//                        new BRDialogView.BROnClickListener() {
//                            @Override
//                            public void onClick(BRDialogView brDialogView) {
//                                brDialogView.dismissWithAnimation();
//                                new Thread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        BRSharedPrefs.putStartHeight(BitcoinCashActivity.this, 0);
//                                        BRPeerManager.getInstance().rescan();
//                                        BRAnimator.startBreadActivity(BitcoinCashActivity.this, false);
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
