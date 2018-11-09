package com.breadwallet.presenter.activities.settings;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;

import java.util.Locale;

public class AboutActivity extends BaseSettingsActivity {
    private static final String TAG = AboutActivity.class.getName();

    private static AboutActivity app;
    private BaseTextView mCopy;
    private BaseTextView mRewardsId;
    private static final int DEFAULT_VERSION_CODE = 0;
    private static final String DEFAULT_VERSION_NAME = "0";

    public static AboutActivity getApp() {
        return app;
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_about;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView infoText = findViewById(R.id.info_text);
        TextView policyText = findViewById(R.id.policy_text);


        PackageInfo packageInfo = null;
        try {
            packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int versionCode = packageInfo != null ? packageInfo.versionCode : DEFAULT_VERSION_CODE;
        String versionName = packageInfo != null ? packageInfo.versionName : DEFAULT_VERSION_NAME;

        infoText.setText(String.format(Locale.getDefault(), getString(R.string.About_footer), versionName, versionCode));

        ImageView redditLink = findViewById(R.id.reddit_share_button);
        ImageView telegramLink = findViewById(R.id.telegram_share_button);
        ImageView discordLink = findViewById(R.id.discord_share_button);
        ImageView siteLink = findViewById(R.id.site_share_button);
        ImageView crLink = findViewById(R.id.cr_share_button);

        redditLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_REDDIT));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        discordLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_DISCORD));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        telegramLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_TELEGRAM));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        siteLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_BLOG));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        crLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_BLOG));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        policyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_PRIVACY_POLICY));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        if (UiUtils.isLast(this)) {
            UiUtils.startBreadActivity(this, false);
        } else {
            super.onBackPressed();
        }
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
