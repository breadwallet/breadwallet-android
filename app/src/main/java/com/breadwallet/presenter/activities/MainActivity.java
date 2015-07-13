package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.fragments.MainFragmentDecoder;
import com.breadwallet.presenter.fragments.MainFragmentSettings;
import com.breadwallet.presenter.fragments.MainFragmentSettingsPressed;
import com.breadwallet.tools.adapter.MyPagerAdapter;
import com.breadwallet.tools.adapter.ParallaxViewPager;
import com.breadwallet.tools.animations.SpringAnimator;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    private static MainActivity app;

    private boolean doubleBackToExitPressedOnce;
    private boolean settingsON;
    private boolean settingsPressedOn;
    private boolean settingsAvailable = true;
    private boolean decoderFragmentOn;
    private MyPagerAdapter pagerAdapter;
    private ImageView pageIndicator;
    private Map<String, Integer> indicatorMap;
    private Button burgerButton;
    private MainFragmentSettings mainFragmentSettings;
    private ParallaxViewPager parallaxViewPager;
    private MainFragmentSettingsPressed mainFragmentSettingsPressed;
    private MainFragmentDecoder mainFragmentDecoder;

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

    }

    /**
     * Initializes all the views and components
     */
    private void initializeViews() {
        burgerButton = (Button) findViewById(R.id.mainbuttonburger);
        Log.d(TAG, "The burger button's id: " + burgerButton.getId());
        pageIndicator = (ImageView) findViewById(R.id.pagerindicator);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        indicatorMap = new HashMap<>();
        mainFragmentSettings = new MainFragmentSettings();
        mainFragmentSettingsPressed = new MainFragmentSettingsPressed();
        mainFragmentDecoder = new MainFragmentDecoder();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.pager));
        parallaxViewPager
                .setOverlapPercentage(0.99f)
                .setAdapter(pagerAdapter);
        indicatorMap.put("left", R.drawable.pageindicatorleft);
        indicatorMap.put("right", R.drawable.pageindicatorright);
    }

    /**
     * Animate the transition on burgerButton/MenuButton pressed
     */
    public void animateSettingsFragment() {
        if (settingsAvailable) {
            Log.d(TAG, "Inside the animateSettingsFragment!");
            settingsAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    settingsAvailable = true;
                }
            }, 300);
            if (!settingsON) {
                if (!settingsPressedOn) {
                    settingsON = true;
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.from_bottom);
                    fragmentTransaction.replace(R.id.mainlayout, mainFragmentSettings);
                    fragmentTransaction.commit();
                    pagerAdapter.showFragments(false);
                    pageIndicator.setVisibility(View.GONE);
                } else {
                    animateSettingsPressed();
                }

            } else {
                settingsON = false;
                FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                fragmentTransaction.setCustomAnimations(R.animator.to_bottom, R.animator.to_bottom);
                fragmentTransaction.remove(mainFragmentSettings);
                fragmentTransaction.commit();
                pagerAdapter.showFragments(true);
                pageIndicator.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Animates the fragment transition on button "Settings" pressed
     */
    public void animateSettingsPressed() {
        if (!settingsPressedOn) {
            settingsPressedOn = true;
            settingsON = false;
            //Disabled inspection: <Expected resource type anim>
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
            fragmentTransaction.replace(R.id.mainlayout, mainFragmentSettingsPressed);
            fragmentTransaction.commit();
        } else {
            settingsON = true;
            settingsPressedOn = false;
            FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
            fragmentTransaction.setCustomAnimations(R.animator.from_left, R.animator.to_right);
            fragmentTransaction.replace(R.id.mainlayout, mainFragmentSettings);
            fragmentTransaction.commit();
        }
    }

    public void animateDecoderFragment() {
        decoderFragmentOn = true;
        //Disabled inspection: <Expected resource type anim>
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.from_bottom, R.animator.to_top);
        fragmentTransaction.replace(R.id.mainlayout, mainFragmentDecoder);
        fragmentTransaction.commit();
        Log.e(TAG, "The mainFragmentDecoder: " + mainFragmentDecoder);
        Log.e(TAG, "The fragmentTransaction: " + fragmentTransaction);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {

            animateSettingsFragment();
            // return 'true' to prevent further propagation of the key event
            return true;
        }

        // let the system handle all other key events
        return super.onKeyDown(keyCode, event);
    }

    /**
     * Shows a custom toast using the given string as a paramater,
     *
     * @param message the message to be shown in the custom toast
     */
    public void showCustomToast(String message) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        TextView text = (TextView) layout.findViewById(R.id.toast_text);
        text.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, 100);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }

    @Override
    public void onBackPressed() {
        if (!settingsPressedOn) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                return;
            }
            if (settingsON) {
                animateSettingsFragment();
            }
            if (decoderFragmentOn) {
                decoderFragmentOn = false;
                getSupportFragmentManager().beginTransaction().
                        remove(mainFragmentDecoder).commit();
            } else {
                this.doubleBackToExitPressedOnce = true;
                showCustomToast("Press again to exit!");
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 1000);
            }

        } else {
            animateSettingsPressed();
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
     * @return the singleton instance of the MainActivity class
     */
    public static MainActivity getApp() {
        return app;
    }

    /**
     * @return the current instance of the class MainFragmentSettingsPressed
     */
    public MainFragmentSettingsPressed getMainFragmentSettingsPressed() {
        return mainFragmentSettingsPressed;
    }

}
