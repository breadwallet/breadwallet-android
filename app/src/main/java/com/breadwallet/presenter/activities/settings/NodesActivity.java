package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.BRPeerManager;

import static com.platform.HTTPServer.URL_SUPPORT;

public class NodesActivity extends BRActivity {
    private static final String TAG = NodesActivity.class.getName();
    private Button scanButton;
    public static boolean appVisible = false;
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

        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BreadDialog.showCustomDialog(NodesActivity.this, "Sync with Blockchain?",
                        "You will not be able to send money while syncing.", "Sync", "Cancel",
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRSharedPrefs.putStartHeight(NodesActivity.this, 0);
                                        BRPeerManager.getInstance().rescan();
                                        BRAnimator.startBreadActivity(NodesActivity.this, false);

                                    }
                                }).start();
                            }
                        }, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, 0);
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
