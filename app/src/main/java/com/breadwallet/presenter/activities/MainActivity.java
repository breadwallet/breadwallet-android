package com.breadwallet.presenter.activities;

import android.content.ClipboardManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.fragments.MainFragmentDecoder;
import com.breadwallet.presenter.fragments.MainFragmentSettingsAll;
import com.breadwallet.presenter.fragments.allsettings.FragmentSettings;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentAbout;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentCurrency;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.others.CurrencyManager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    private static MainActivity app;
    public static final String PREFS_NAME = "MyPrefsFile";

    public static boolean decoderFragmentOn;
    private boolean onBackPressedAvailable = true;
    private boolean doubleBackToExitPressedOnce;
    private CustomPagerAdapter pagerAdapter;
    private ImageView pageIndicator;
    private Map<String, Integer> indicatorMap;
    private Button burgerButton;
    private MainFragmentSettingsAll mainFragmentSettingsAll;
    private ParallaxViewPager parallaxViewPager;
    private FragmentSettings fragmentSettings;
    private FragmentAbout fragmentAbout;
    private MainFragmentDecoder mainFragmentDecoder;
    private ClipboardManager myClipboard;
    private FragmentCurrency fragmentCurrency;

    /**
     * Public constructor used to assign the current instance to the app variable
     */
    public MainActivity() {
        app = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        SpringAnimator.showAnimation(burgerButton);
        myClipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        CurrencyManager.startTimer();
    }

    @Override
    protected void onStop() {
        super.onStop();
        CurrencyManager.stoptimertask();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        MainFragmentDecoder.getMainFragmentDecoder().stopCamera();
        Log.e(TAG, "Activity Destroyed!");
    }

    /**
     * Initializes all the views and components
     */
    private void initializeViews() {
        burgerButton = (Button) findViewById(R.id.mainbuttonburger);
        Log.d(TAG, "The burger button's id: " + burgerButton.getId());
        pageIndicator = (ImageView) findViewById(R.id.pagerindicator);
        pagerAdapter = new CustomPagerAdapter(getSupportFragmentManager());
        indicatorMap = new HashMap<>();
        fragmentSettings = new FragmentSettings();
        mainFragmentSettingsAll = new MainFragmentSettingsAll();
        mainFragmentDecoder = new MainFragmentDecoder();
        fragmentAbout = new FragmentAbout();
        fragmentCurrency = new FragmentCurrency();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.pager));
        parallaxViewPager
                .setOverlapPercentage(0.99f)
                .setAdapter(pagerAdapter);
        parallaxViewPager.setBackgroundResource(R.drawable.backgroundmain);
        indicatorMap.put("left", R.drawable.pageindicatorleft);
        indicatorMap.put("right", R.drawable.pageindicatorright);
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            FragmentAnimator.pressMenuButton(app);
            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        if (onBackPressedAvailable) {
            onBackPressedAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onBackPressedAvailable = true;
                }
            }, 300);
            if (FragmentAnimator.level == 0) {
                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed();
                    return;
                }
                if (decoderFragmentOn) {
                    decoderFragmentOn = false;
                    getSupportFragmentManager().beginTransaction().
                            remove(mainFragmentDecoder).commit();
                    CustomPagerAdapter.getAdapter().showFragments(true);
                } else {
                    this.doubleBackToExitPressedOnce = true;
                    ((BreadWalletApp) getApplicationContext()).showCustomToast(this,
                            "Press again to exit!", 100, Toast.LENGTH_SHORT);
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            doubleBackToExitPressedOnce = false;
                        }
                    }, 1000);
                }
            } else if (FragmentAnimator.level == 1) {
                FragmentAnimator.pressMenuButton(this);
            } else {
                FragmentAnimator.animateSlideToRight(this);
            }
        }
    }

    /**
     * Sets the little circle indicator to the selected page
     *
     * @patam x The page for the indicator to be shown
     */
    public void setPagerIndicator(int x) {

        String item = (x == 0) ? "left" : "right";
//        Log.d(TAG, "The item is: " + item);
        pageIndicator.setImageResource(indicatorMap.get(item));
    }

    /**
     * GETTERS AND SETTERS
     */

    public FragmentSettings getFragmentSettings() {
        return fragmentSettings;
    }

    public static MainActivity getApp() {
        return app;
    }

    public void setDecoderFragmentOn(boolean decoderFragmentOn) {
        this.decoderFragmentOn = decoderFragmentOn;
    }

    public void setMainFragmentDecoder(MainFragmentDecoder mainFragmentDecoder) {
        this.mainFragmentDecoder = mainFragmentDecoder;
    }

    public static void setApp(MainActivity app) {
        MainActivity.app = app;
    }

    public Button getBurgerButton() {
        return burgerButton;
    }

    public void setBurgerButton(Button burgerButton) {
        this.burgerButton = burgerButton;
    }

    public boolean isDecoderFragmentOn() {
        return decoderFragmentOn;
    }

    public boolean isDoubleBackToExitPressedOnce() {
        return doubleBackToExitPressedOnce;
    }

    public void setDoubleBackToExitPressedOnce(boolean doubleBackToExitPressedOnce) {
        this.doubleBackToExitPressedOnce = doubleBackToExitPressedOnce;
    }

    public Map<String, Integer> getIndicatorMap() {
        return indicatorMap;
    }

    public void setIndicatorMap(Map<String, Integer> indicatorMap) {
        this.indicatorMap = indicatorMap;
    }

    public MainFragmentDecoder getMainFragmentDecoder() {
        return mainFragmentDecoder;
    }

    public MainFragmentSettingsAll getMainFragmentSettingsAll() {
        return mainFragmentSettingsAll;
    }

    public void setMainFragmentSettingsAll(MainFragmentSettingsAll mainFragmentSettingsAll) {
        this.mainFragmentSettingsAll = mainFragmentSettingsAll;
    }

    public void setFragmentSettings(FragmentSettings fragmentSettings) {
        this.fragmentSettings = fragmentSettings;
    }

    public ImageView getPageIndicator() {
        return pageIndicator;
    }

    public void setPageIndicator(ImageView pageIndicator) {
        this.pageIndicator = pageIndicator;
    }

    public CustomPagerAdapter getPagerAdapter() {
        return pagerAdapter;
    }

    public void setPagerAdapter(CustomPagerAdapter pagerAdapter) {
        this.pagerAdapter = pagerAdapter;
    }

    public ClipboardManager getMyClipboard() {
        return myClipboard;
    }

    public void setMyClipboard(ClipboardManager myClipboard) {
        this.myClipboard = myClipboard;
    }

    public FragmentAbout getFragmentAbout() {
        return fragmentAbout;
    }

    public void setFragmentAbout(FragmentAbout fragmentAbout) {
        this.fragmentAbout = fragmentAbout;
    }

    public FragmentCurrency getFragmentCurrency() {
        return fragmentCurrency;
    }

    public void setFragmentCurrency(FragmentCurrency fragmentCurrency) {
        this.fragmentCurrency = fragmentCurrency;
    }

}
