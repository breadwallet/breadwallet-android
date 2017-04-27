package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.wallet.BRPeerManager;

public class SyncBlockchainActivity extends AppCompatActivity {
    private static final String TAG = SyncBlockchainActivity.class.getName();
    private Button scanButton;
    public static boolean appVisible = false;
    private static SyncBlockchainActivity app;

    public static SyncBlockchainActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync_blockchain);
        ActivityUTILS.setStatusBarColor(this, R.color.status_bar);
        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!BRAnimator.isClickAllowed()) return;
                BreadDialog.showCustomDialog(SyncBlockchainActivity.this, "Sync with Blockchain?",
                        "You will not be able to send money while syncing.", "Sync", "Cancel",
                        new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRPeerManager.getInstance().rescan();
                                        SharedPreferencesManager.putStartHeight(SyncBlockchainActivity.this, BRPeerManager.getCurrentBlockHeight());
                                        BRAnimator.startBreadActivity(SyncBlockchainActivity.this, false);

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
