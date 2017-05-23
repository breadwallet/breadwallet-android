package com.breadwallet.presenter.activities.settings;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;

import java.util.Locale;

public class AboutActivity extends BRActivity {
    private static final String TAG = AboutActivity.class.getName();
    private TextView termsText;
    private TextView policyText;
    private TextView infoText;

    private ImageView redditShare;
    private ImageView twitterShare;
    private ImageView blogShare;
    private static AboutActivity app;

    public static AboutActivity getApp() {
        return app;
    }

    public static boolean appVisible = false;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        infoText = (TextView) findViewById(R.id.info_text);
        termsText = (TextView) findViewById(R.id.terms_text);
        policyText = (TextView) findViewById(R.id.policy_text);

        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int verCode = pInfo != null ? pInfo.versionCode : 0;

        infoText.setText(String.format(Locale.getDefault(), "%s %d", getString(R.string.About_footer), verCode));

        redditShare = (ImageView) findViewById(R.id.reddit_share_button);
        twitterShare = (ImageView) findViewById(R.id.twitter_share_button);
        blogShare = (ImageView) findViewById(R.id.blog_share_button);


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
