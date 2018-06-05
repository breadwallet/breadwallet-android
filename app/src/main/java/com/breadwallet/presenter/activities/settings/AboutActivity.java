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
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;

import java.util.Locale;

public class AboutActivity extends BaseSettingsActivity {
    private static final String TAG = AboutActivity.class.getName();

    private static AboutActivity app;
    private BRText mCopy;
    private BRText mRewardsId;

    public static AboutActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_about;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView infoText = findViewById(R.id.info_text);
        TextView policyText = findViewById(R.id.policy_text);


        PackageInfo pInfo = null;
        try {
            pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        int verCode = pInfo != null ? pInfo.versionCode : 0;

        infoText.setText(String.format(Locale.getDefault(), getString(R.string.About_footer), verCode));

        ImageView redditShare = findViewById(R.id.reddit_share_button);
        ImageView twitterShare = findViewById(R.id.twitter_share_button);
        ImageView blogShare = findViewById(R.id.blog_share_button);
        mRewardsId = findViewById(R.id.brd_rewards_id);
        mCopy = findViewById(R.id.brd_copy);

        redditShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://reddit.com/r/breadwallet/"));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });

        twitterShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://twitter.com/breadapp"));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        blogShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://breadapp.com/blog/"));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        policyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://breadapp.com/privacy-policy"));
                startActivity(browserIntent);
                app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });

        mRewardsId.setText(BRSharedPrefs.getWalletRewardId(this));

        mCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRClipboardManager.putClipboard(AboutActivity.this, mRewardsId.getText().toString());
                Toast.makeText(AboutActivity.this, getString(R.string.Receive_copied), Toast.LENGTH_SHORT).show();
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
        if (ActivityUTILS.isLast(this)) {
            BRAnimator.startBreadActivity(this, false);
        } else {
            super.onBackPressed();
        }
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
