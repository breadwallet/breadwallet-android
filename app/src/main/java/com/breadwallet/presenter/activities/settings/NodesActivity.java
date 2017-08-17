package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

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
    private Button switchButton;
    private TextView nodeStatus;
    private TextView nodeText;
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

        nodeStatus = (TextView) findViewById(R.id.node_status);
        nodeText = (TextView) findViewById(R.id.node_text);

        switchButton = (Button) findViewById(R.id.button_switch);
        switchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;

                if (!SharedPreferencesManager.getTrustNode((Activity) getContext()).isEmpty()) {
                    createDialog(2);
                } else {
                    createDialog(1);
                }

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
