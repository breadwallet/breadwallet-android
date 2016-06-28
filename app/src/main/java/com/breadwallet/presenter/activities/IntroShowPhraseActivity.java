package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.SharedPreferencesManager;
import com.breadwallet.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 3/2/2016.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class IntroShowPhraseActivity extends Activity {
    private static final String TAG = IntroShowPhraseActivity.class.getName();
    private Button remindMeLater;
    private RelativeLayout writeDownLayout;
    private ImageView checkBox;
    private boolean checked = false;
    public static String phrase = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_show_phrase);

        TextView thePhrase = (TextView) findViewById(R.id.the_phrase_at_startup);
        remindMeLater = (Button) findViewById(R.id.remind_me_later_button);
        writeDownLayout = (RelativeLayout) findViewById(R.id.write_down_notice_layout);
        checkBox = (ImageView) findViewById(R.id.write_down_check_box);
        checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setCheckBoxImage();

            }
        });

        remindMeLater.setVisibility(View.VISIBLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE);

        writeDownLayout.setVisibility(View.GONE);

        if (phrase != null && phrase.length() > 1) {
            thePhrase.setText(phrase);
            if(phrase.charAt(0) > 0x3000)
                thePhrase.setText(phrase.replace(" ", "\u3000"));
            phrase = null;
        } else {
            phrase = null;
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!IntroShowPhraseActivity.this.isDestroyed())
                    writeDownLayout.setVisibility(View.VISIBLE);
            }
        }, 10000);
        remindMeLater.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                remindMeLater.setVisibility(View.GONE);

                startMainActivity();

                try {
                    finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        phrase = null;
    }

    @Override
    public void onBackPressed() {
    }

    private void startMainActivity() {
        Intent intent;
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        phrase = null;
        if (!IntroShowPhraseActivity.this.isDestroyed()) {
            finish();
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void setCheckBoxImage() {
        checkBox.setImageResource(!checked ? R.drawable.checkbox_checked : R.drawable.checkbox_empty);
        remindMeLater.setText(!checked ? getResources().getString(R.string.done) : getResources().getString(R.string.remind_me_later));
        checked = !checked;
        SharedPreferencesManager.putCheckBoxRecoveryPhraseFragment(this, checked);
    }
}
