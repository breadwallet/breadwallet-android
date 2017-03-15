package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.fragments.FragmentPhraseWord;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.util.Locale;


public class IntroPhraseCheckActivity extends Activity {
    private static final String TAG = IntroPhraseCheckActivity.class.getName();
    private ViewPager wordViewPager;
    private Button nextButton;
    private Button previousButton;
    private LinearLayout buttonsLayout;
    private TextView itemIndexText;
    private SparseArray<String> wordMap;
//    private int currentIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_phrase_check);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
        setStatusBarColor(android.R.color.transparent);
        wordViewPager = (ViewPager) findViewById(R.id.phrase_words_pager);
        wordViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {
            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            public void onPageSelected(int position) {
                updateItemIndexText();
                // Check if this is the page you want.
            }
        });

        nextButton = (Button) findViewById(R.id.button_next);
        previousButton = (Button) findViewById(R.id.button_previous);
        itemIndexText = (TextView) findViewById(R.id.item_index_text);
        buttonsLayout = (LinearLayout) findViewById(R.id.buttons_layout);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWordView(true);
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWordView(false);

            }
        });
        String cleanPhrase = null;
        wordMap = new SparseArray<>();

        try {
            cleanPhrase = new String(KeyStoreManager.getKeyStorePhrase(this, 0));
            //todo DELETE THIS LOG
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new RuntimeException(TAG + ": cleanPhrase is null");
        }

        String wordArray[] = cleanPhrase.split(" ");

        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BreadDialog.showCustomDialog(this, getString(R.string.warning),
                    getString(R.string.phrase_error), getString(R.string.ok));
            FirebaseCrash.report(new IllegalArgumentException(getString(R.string.phrase_error)));

        } else {
            WordPagerAdapter adapter = new WordPagerAdapter(getFragmentManager());
            adapter.setWords(wordArray);
            wordViewPager.setAdapter(adapter);
            for (int i = 0; i < wordArray.length; i++) {
                wordMap.append(i, wordArray[i]);
            }
            updateItemIndexText();
        }

    }

    private void updateWordView(boolean isNext) {
        int currentIndex = wordViewPager.getCurrentItem();
        if (isNext) {
            setButtonEnabled(true);
            if (currentIndex >= 11) {
                Intent intent = new Intent(IntroPhraseCheckActivity.this, IntroPhraseProveActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            } else {
                wordViewPager.setCurrentItem(currentIndex + 1);
            }
        } else {
            if (currentIndex <= 1) {
                wordViewPager.setCurrentItem(currentIndex - 1);
                setButtonEnabled(false);
            } else {
                wordViewPager.setCurrentItem(currentIndex - 1);
            }
        }
    }

    private void setButtonEnabled(boolean b) {
        previousButton.setTextColor(getColor(b ? R.color.button_secondary_text : R.color.extra_light_grey));
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b ? 8 : 0, r.getDisplayMetrics());
        previousButton.setElevation(px);
        previousButton.setEnabled(b);
    }

    private void updateItemIndexText() {
        String text = String.format(Locale.getDefault(), "%d of %d", wordViewPager.getCurrentItem() + 1, wordMap.size());
        itemIndexText.setText(text);
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }

    @Override
    public void onBackPressed() {
        BRWalletManager.getInstance().startBreadActivity(this);

    }

    private class WordPagerAdapter extends FragmentPagerAdapter {

        private String[] words;

        public WordPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        public void setWords(String[] words) {
            this.words = words;
        }

        @Override
        public Fragment getItem(int pos) {
            return FragmentPhraseWord.newInstance(words[pos]);
        }

        @Override
        public int getCount() {
            return words == null ? 0 : words.length;
        }

    }
}
