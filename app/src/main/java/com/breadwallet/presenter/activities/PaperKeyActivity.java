package com.breadwallet.presenter.activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.SparseArray;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.fragments.FragmentPhraseWord;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.Utils;

import java.util.Locale;


public class PaperKeyActivity extends BRActivity {
    private static final String TAG = PaperKeyActivity.class.getName();

    private static final int NAVIGATION_BUTTONS_WEIGHT = 1;
    private static final float BUTTONS_LAYOUT_WEIGHT_SUM_DEFAULT = 2.0f;
    private static final float BUTTONS_LAYOUT_WEIGHT_SUM_SINGLE = 1.0f;

    private ViewPager mWordViewPager;
    private Button mNextButton;
    private Button mPreviousButton;
    private LinearLayout mButtonsLayout;
    private TextView mItemIndexTextView;
    private ImageButton mCloseImageButton;
    private SparseArray<String> mWordMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_paper_key);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);

        mWordViewPager = findViewById(R.id.phrase_words_pager);
        mWordViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                updateItemIndexText();
                if (position == 0) {
                    setButtonEnabled(false);
                    updateNavigationButtons(false);
                } else {
                    setButtonEnabled(true);
                    updateNavigationButtons(true);
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        mNextButton = findViewById(R.id.next_button);
        mPreviousButton = findViewById(R.id.previous_button);
        mCloseImageButton = findViewById(R.id.close_button);
        mItemIndexTextView = findViewById(R.id.item_index_text);
        mButtonsLayout = findViewById(R.id.buttons_layout);
        updateNavigationButtons(false);

        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWordView(true);
            }
        });

        mCloseImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                UiUtils.startBreadActivity(PaperKeyActivity.this, false);
                if (!isDestroyed()) finish();
            }
        });

        mPreviousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateWordView(false);

            }
        });

        String cleanPhrase = getIntent().getExtras() == null ? null : getIntent().getStringExtra("phrase");
        mWordMap = new SparseArray<>();

        if (Utils.isNullOrEmpty(cleanPhrase)) {
            throw new IllegalArgumentException(TAG + ": cleanPhrase is null");
        }

        String[] wordArray = cleanPhrase.split(" ");

        if (cleanPhrase.charAt(cleanPhrase.length() - 1) == '\0') {
            BRDialog.showCustomDialog(this, getString(R.string.JailbreakWarnings_title),
                    getString(R.string.Alert_keystore_generic_android), getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
            BRReportsManager.reportBug(new IllegalArgumentException("Paper Key error, please contact support at breadwallet.com: " + wordArray.length), true);
        } else {
            if (wordArray.length != 12) {
                BRReportsManager.reportBug(new IllegalArgumentException("Wrong number of paper keys: " + wordArray.length + ", lang: " + Locale.getDefault().getLanguage()), true);
            }
            WordPagerAdapter adapter = new WordPagerAdapter(getFragmentManager());
            adapter.setWords(wordArray);
            mWordViewPager.setAdapter(adapter);
            for (int i = 0; i < wordArray.length; i++) {
                mWordMap.append(i, wordArray[i]);
            }
            updateItemIndexText();
        }
    }

    /**
     * Show or hide the "Previous" button used to navigate the ViewPager.
     *
     * @param showPrevious
     */
    private void updateNavigationButtons(boolean showPrevious) {
        LinearLayout.LayoutParams nextButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        if (!showPrevious) {
            mButtonsLayout.setWeightSum(BUTTONS_LAYOUT_WEIGHT_SUM_SINGLE);

            nextButtonParams.weight = NAVIGATION_BUTTONS_WEIGHT;
            nextButtonParams.gravity = Gravity.CENTER_HORIZONTAL;
            nextButtonParams.setMargins((int) getResources().getDimension(R.dimen.margin), 0, (int) getResources().getDimension(R.dimen.margin), 0);
            mNextButton.setLayoutParams(nextButtonParams);
            mNextButton.setHeight((int) getResources().getDimension(R.dimen.large_button_height));

            mPreviousButton.setVisibility(View.GONE);
        } else {
            mButtonsLayout.setWeightSum(BUTTONS_LAYOUT_WEIGHT_SUM_DEFAULT);

            nextButtonParams.weight = NAVIGATION_BUTTONS_WEIGHT;
            nextButtonParams.setMargins(0, 0, (int) getResources().getDimension(R.dimen.margin), 0);
            mNextButton.setLayoutParams(nextButtonParams);
            mNextButton.setHeight((int) getResources().getDimension(R.dimen.large_button_height));

            LinearLayout.LayoutParams previousButtonParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
            previousButtonParams.weight = NAVIGATION_BUTTONS_WEIGHT;
            previousButtonParams.setMargins((int) getResources().getDimension(R.dimen.margin), 0, 0, 0);
            mPreviousButton.setLayoutParams(previousButtonParams);
            mPreviousButton.setVisibility(View.VISIBLE);
            mPreviousButton.setHeight((int) getResources().getDimension(R.dimen.large_button_height));
        }
    }

    private void updateWordView(boolean isNext) {
        int currentIndex = mWordViewPager.getCurrentItem();
        if (isNext) {
            setButtonEnabled(true);
            if (currentIndex >= 11) {
                PostAuth.getInstance().onPhraseProveAuth(this, false);
            } else {
                mWordViewPager.setCurrentItem(currentIndex + 1);
            }
        } else {
            if (currentIndex <= 1) {
                mWordViewPager.setCurrentItem(currentIndex - 1);
                setButtonEnabled(false);
            } else {
                mWordViewPager.setCurrentItem(currentIndex - 1);
            }
        }
    }

    private void setButtonEnabled(boolean b) {
        Resources r = getResources();
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, b ? 8 : 0, r.getDisplayMetrics());
        mPreviousButton.setElevation(px);
        mPreviousButton.setEnabled(b);
    }

    private void updateItemIndexText() {
        String text = String.format(Locale.getDefault(), getString(R.string.WritePaperPhrase_step), mWordViewPager.getCurrentItem() + 1, mWordMap.size());
        mItemIndexTextView.setText(text);
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
}
