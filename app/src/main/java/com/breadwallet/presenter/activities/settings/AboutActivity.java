package com.breadwallet.presenter.activities.settings;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.FileProvider;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.FileHelper;
import com.platform.APIClient;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class AboutActivity extends BaseSettingsActivity {
    private static final int VERSION_CLICK_COUNT_FOR_BACKDOOR = 5;
    private static final String DEFAULT_LOGS_EMAIL = "android@brd.com";
    private static final String LOGCAT_COMMAND = String.format("logcat -d %s:V", BuildConfig.APPLICATION_ID); // Filters out our apps events at log level = verbose
    private static final String NO_EMAIL_APP_ERROR_MESSAGE = "No email app found.";
    private static final String FAILED_ERROR_MESSAGE = "Failed to get logs.";
    private static final String LOGS_EMAIL_SUBJECT = "BRD Android App Feedback [ID:%s]"; // Placeholder is for a unique id. 
    private static final String DEFAULT_LOG_ATTACHMENT_BODY = "No logs.";
    private static final String LOGS_FILE_NAME = "Logs.txt";
    private static final String MIME_TYPE = "text/plain";

    private BaseTextView mCopy;
    private BaseTextView mRewardsId;
    private int mVersionClickedCount;

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

        infoText.setText(String.format(Locale.getDefault(), getString(R.string.About_footer), BuildConfig.VERSION_NAME, BuildConfig.BUILD_VERSION));
        infoText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mVersionClickedCount++;
                if (mVersionClickedCount >= VERSION_CLICK_COUNT_FOR_BACKDOOR) {
                    mVersionClickedCount = 0;
                    shareLogs();
                }
            }
        });

        ImageView redditShare = findViewById(R.id.reddit_share_button);
        ImageView twitterShare = findViewById(R.id.twitter_share_button);
        ImageView blogShare = findViewById(R.id.blog_share_button);
        mRewardsId = findViewById(R.id.brd_rewards_id);
        mCopy = findViewById(R.id.brd_copy);

        redditShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_REDDIT));
                startActivity(browserIntent);
                AboutActivity.this.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });

        twitterShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_TWITTER));
                startActivity(browserIntent);
                AboutActivity.this.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        blogShare.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_BLOG));
                startActivity(browserIntent);
                AboutActivity.this.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        policyText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(BRConstants.URL_PRIVACY_POLICY));
                startActivity(browserIntent);
                AboutActivity.this.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
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
        mVersionClickedCount = 0;
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

    private void shareLogs() {
        File file = FileHelper.saveToExternalStorage(this, LOGS_FILE_NAME, getLogs());
        Uri uri = FileProvider.getUriForFile(this, BuildConfig.APPLICATION_ID, file);
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType(MIME_TYPE);
        emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String[]{DEFAULT_LOGS_EMAIL});
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, String.format(LOGS_EMAIL_SUBJECT, BRSharedPrefs.getDeviceId(this)));
        emailIntent.putExtra(Intent.EXTRA_TEXT, getDeviceInfo());

        try {
            startActivity(Intent.createChooser(emailIntent, getString(R.string.Receive_share)));
        } catch (ActivityNotFoundException e) {
            BRToast.showCustomToast(this, NO_EMAIL_APP_ERROR_MESSAGE,
                    BRSharedPrefs.getScreenHeight(this) / 2, Toast.LENGTH_LONG, 0);
        }
    }

    private String getLogs() {
        try {
            Process process = Runtime.getRuntime().exec(LOGCAT_COMMAND);
            return IOUtils.toString(process.getInputStream());
        } catch (IOException ex) {
            BRToast.showCustomToast(this, FAILED_ERROR_MESSAGE,
                    BRSharedPrefs.getScreenHeight(this) / 2, Toast.LENGTH_LONG, 0);
        }
        return DEFAULT_LOG_ATTACHMENT_BODY;
    }

    private String getDeviceInfo() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Feedback\n");
        stringBuilder.append("------------\n");
        stringBuilder.append("[Please add your feedback.]\n\n");
        stringBuilder.append("Device Info\n");
        stringBuilder.append("------------\n");
        stringBuilder.append("Wallet id: " + BRSharedPrefs.getWalletRewardId(this));
        stringBuilder.append("\nDevice id: " + BRSharedPrefs.getDeviceId(this));
        stringBuilder.append("\nDebuggable: " + BuildConfig.DEBUG);
        stringBuilder.append("\nApplication id: " + BuildConfig.APPLICATION_ID);
        stringBuilder.append("\nBuild Type: " + BuildConfig.BUILD_TYPE);
        stringBuilder.append("\nBuild Flavor: " + BuildConfig.FLAVOR);
        stringBuilder.append("\nApp Version: " + (BuildConfig.VERSION_NAME + " " + BuildConfig.BUILD_VERSION));
        for (String bundleName : APIClient.BUNDLE_NAMES) {
            stringBuilder.append(String.format("\n Bundle %s - Version: %s", bundleName, BRSharedPrefs.getBundleHash(this, bundleName)));
        }

        stringBuilder.append("\nNetwork: " + (BuildConfig.BITCOIN_TESTNET ? "Testnet" : "Mainnet"));
        stringBuilder.append("\nOS Version: " + Build.VERSION.RELEASE);
        stringBuilder.append("\nDevice Type: " + (Build.MANUFACTURER + " " + Build.MODEL + "\n"));

        return stringBuilder.toString();
    }

}
