package com.breadwallet.activities;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.adapter.MyPagerAdapter;
import com.breadwallet.listeners.MyOnPageChangeListener;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends FragmentActivity {
    public static final String TAG = "MainActivity";
    boolean doubleBackToExitPressedOnce = false;
    private MyPagerAdapter pagerAdapter;
    private ViewPager viewPager;
    private static MainActivity app;
    private ImageView pageIndicator;
    Map<String, Integer> indicatorMap;

    public MainActivity(){
        app = this;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ViewPager viewPager = (ViewPager) findViewById(R.id.pager);
        pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        viewPager.setAdapter(pagerAdapter);
        viewPager.setCurrentItem(0);
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());
        pageIndicator = (ImageView) findViewById(R.id.pagerindicator);
        indicatorMap = new HashMap<String, Integer>();
        indicatorMap.put("left", R.drawable.pageindicatorleft);
        indicatorMap.put("right", R.drawable.pageindicatorright);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return super.onOptionsItemSelected(item);
    }

    /* Shows a custom toast using: String param - the actual message,
     int param - margin y pixels from the bottom */
    public void showCustomToast(String message, int y) {
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        TextView text = (TextView) layout.findViewById(R.id.toast_text);
        text.setText(message);
        Toast toast = new Toast(getApplicationContext());
        toast.setGravity(Gravity.BOTTOM, 0, y);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        showCustomToast("Press again to exit!", 60);

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce = false;
            }
        }, 2000);
    }

    public static MainActivity getApp(){
        return app;
    }

    public void setPagerIndicator(int x){
        String item = (x == 0) ? "left" : "right";
        Log.d(TAG, "The item is: " + item);

        pageIndicator.setImageResource(indicatorMap.get(item));
    }
}
