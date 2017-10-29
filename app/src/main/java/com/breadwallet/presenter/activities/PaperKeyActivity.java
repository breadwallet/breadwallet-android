package com.breadwallet.presenter.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.fragments.FragmentPhraseWord;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.Utils;
import com.google.firebase.crash.FirebaseCrash;

import java.util.Locale;


public class PaperKeyActivity extends BRActivity {
    private static final String TAG = PaperKeyActivity.class.getName();
    private ViewPager wordViewPager;
    private Button nextButton;
    private Button previousButton;
    private LinearLayout buttonsLayout;
    private TextView itemIndexText;
    private SparseArray<String> wordMap;
    public static boolean appVisible = false;
    private static PaperKeyActivity app;
    private ImageButton close;

    public static PaperKeyActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_key);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        wordViewPager = (ViewPager) findViewById(R.id.phrase_words_pager);
        wordViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            public void onPageScrollStateChanged(int state) {

            }

            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            public void onPageSelected(int position) {
                if (position == 0)
                    setButtonEnabled(false);
                else
                    setButtonEnabled(true);

                updateItemIndexText();
            }
        });

        nextButton = (Button) findViewById(R.id.send_button);
        previousButton = (Button) findViewById(R.id.button_previous);
        close = (ImageButton) findViewById(R.id.close_button);
        itemIndexText = (TextView) findViewById(R.id.item_index_text);
        buttonsLayout = (LinearLayout) findViewById(R.id.buttons_layout);

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWordView(true);
            }
        });

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.startBreadActivity(PaperKeyActivity.this, false);
                if (!isDestroyed()) finish();
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWordView(false);

            }
        });
        String cleanPhrase = getIntent().getExtras() == null ? null : getIntent().getStringExtra("phrase");
        wordMap = new SparseArray<>();

        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new RuntimeException(TAG + ": cleanPhrase is null");
        }

        String wordArray[] = cleanPhrase.split(" ");

        if (wordArray.length == 12 && cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    "Paper Key error, please contact support at @LTCFoundation", getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
            FirebaseCrash.report(new IllegalArgumentException("Paper Key error, please contact support at loafwallet.com"));

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
                PostAuth.getInstance().onPhraseProveAuth(this, false);
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
        previousButton.setTextColor(getColor(b ? R.color.button_secondary_text : R.color.extra_light_gray));
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b ? 8 : 0, r.getDisplayMetrics());
        previousButton.setElevation(px);
        previousButton.setEnabled(b);
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

    private void updateItemIndexText() {
        String text = String.format(Locale.getDefault(), getString(R.string.WritePaperPhrase_step), wordViewPager.getCurrentItem() + 1, wordMap.size());
        itemIndexText.setText(text);
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
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

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}
