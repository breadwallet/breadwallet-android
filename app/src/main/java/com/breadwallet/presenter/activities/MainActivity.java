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
import com.breadwallet.adapter.MyPagerAdapter;
import com.breadwallet.animations.SpringAnimator;
import com.breadwallet.presenter.fragments.MainFragmentSettings;
import com.breadwallet.presenter.fragments.MainFragmentSettingsPressed;
import com.breadwallet.tools.ParallaxViewPager;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    private static MainActivity app;

    boolean doubleBackToExitPressedOnce;

    boolean settingsON;
    boolean settingsPressedOn;
    private MyPagerAdapter pagerAdapter;
    private ImageView pageIndicator;
    private Map<String, Integer> indicatorMap;
    private Button burgerButton;
    private MainFragmentSettings mainFragmentSettings;
    private ParallaxViewPager parallaxViewPager;
    private boolean settingsAvailable = true;
    private MainFragmentSettingsPressed mainFragmentSettingsPressed;

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

    private void initializeViews() {
        burgerButton = (Button) findViewById(R.id.mainbuttonburger);
        Log.d(TAG, "The burger button's id: " + burgerButton.getId());
        pageIndicator = (ImageView) findViewById(R.id.pagerindicator);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        indicatorMap = new HashMap<>();
        mainFragmentSettings = new MainFragmentSettings();
        mainFragmentSettingsPressed = new MainFragmentSettingsPressed();
        parallaxViewPager = ((ParallaxViewPager) findViewById(R.id.pager));
        parallaxViewPager
                .setOverlapPercentage(0.99f)
                .setAdapter(pagerAdapter);
        indicatorMap.put("left", R.drawable.pageindicatorleft);
        indicatorMap.put("right", R.drawable.pageindicatorright);
    }

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

    public void animateSettingsPressed() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        if (!settingsPressedOn) {
            settingsPressedOn = true;
            settingsON = false;
            fragmentTransaction.setCustomAnimations(R.animator.from_right, R.animator.to_left);
            fragmentTransaction.replace(R.id.mainlayout, mainFragmentSettingsPressed);
            fragmentTransaction.commit();
        } else {
            settingsON = true;
            settingsPressedOn = false;
            fragmentTransaction.setCustomAnimations(R.animator.from_left, R.animator.to_right);
            fragmentTransaction.replace(R.id.mainlayout, mainFragmentSettings);
            fragmentTransaction.commit();
        }
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

    /* Shows a custom toast using: String param - the actual message,
     int param - margin y pixels from the bottom */
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

            this.doubleBackToExitPressedOnce = true;
            showCustomToast("Press again to exit!");
            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 1000);

            if (settingsON) {
                animateSettingsFragment();
            }
        } else {
            animateSettingsPressed();
        }
    }

    public void setPagerIndicator(int x) {
        String item = (x == 0) ? "left" : "right";
        Log.d(TAG, "The item is: " + item);
        pageIndicator.setImageResource(indicatorMap.get(item));
    }

    public static MainActivity getApp() {
        return app;
    }

    public MainFragmentSettingsPressed getMainFragmentSettingsPressed() {
        return mainFragmentSettingsPressed;
    }


}
