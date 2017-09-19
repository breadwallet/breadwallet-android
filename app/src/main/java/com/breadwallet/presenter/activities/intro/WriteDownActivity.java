package com.breadwallet.presenter.activities.intro;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.BRConstants;

public class WriteDownActivity extends BRActivity {
    private static final String TAG = WriteDownActivity.class.getName();
    private Button writeButton;
    private ImageButton close;
    public static boolean appVisible = false;
    private static WriteDownActivity app;

    public static WriteDownActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_down);

        writeButton = (Button) findViewById(R.id.button_write_down);
        close = (ImageButton) findViewById(R.id.close_button);
        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });
        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);
        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.paperKey);
            }
        });
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                AuthManager.getInstance().authPrompt(WriteDownActivity.this, null, getString(R.string.VerifyPin_continueBody), true, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        PostAuth.getInstance().onPhraseCheckAuth(WriteDownActivity.this, false);
                    }

                    @Override
                    public void onCancel() {

                    }
                });

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
        close();

    }

    private void close() {
        BRAnimator.startBreadActivity(this, false);
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
        if (!isDestroyed()) finish();
        //additional code
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
