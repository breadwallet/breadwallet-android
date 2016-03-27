package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

public class IntroShowPhraseActivity extends Activity {
    private Button remindMeLater;
    private RelativeLayout writeDownLayout;
    private ImageView checkBox;
    private boolean checked = false;
    public static final String PHRASE_SAVED = "phraseSaved";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_show_phrase);

        if (savedInstanceState != null) {
            return;
        }

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
        writeDownLayout.setVisibility(View.GONE);
        String phrase = KeyStoreManager.getKeyStoreString(this);
        if (phrase != null && phrase.length() > 1) {
            thePhrase.setText(phrase);
        } else {
            throw new RuntimeException("Failed to retrieve the phrase!");
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
                SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(BRWalletManager.ASKED_TO_WRITE_PHRASE, true);
                editor.apply();
                startMainActivity();
            }
        });
    }

    @Override
    public void onBackPressed() {
    }

    private void startMainActivity() {
        Intent intent;
        intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        if (!IntroShowPhraseActivity.this.isDestroyed()) {
            finish();
        }
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setCheckBoxImage() {
        checkBox.setImageResource(!checked ? R.drawable.checkbox_checked : R.drawable.checkbox_empty);
        remindMeLater.setText(!checked ? getResources().getString(R.string.done) : getResources().getString(R.string.remind_me_later));
        checked = !checked;
        SharedPreferences prefs = getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PHRASE_SAVED, checked);
        editor.apply();
    }
}
