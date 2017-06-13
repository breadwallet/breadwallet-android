package com.breadwallet.presenter.activities.intro;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ReEnterPinActivity;
import com.breadwallet.presenter.activities.SetPitActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
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
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;

                PostAuthenticationProcessor.getInstance().onPhraseCheckAuth(WriteDownActivity.this, false);
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
        Intent intent = new Intent(WriteDownActivity.this, SetPitActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
        finish();

    }

    private void close() {
        BRAnimator.startBreadActivity(this, false);
        if (!isDestroyed()) finish();
        //additional code
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

}
