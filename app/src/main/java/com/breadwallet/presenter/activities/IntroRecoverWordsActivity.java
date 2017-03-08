package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.graphics.drawable.shapes.Shape;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRSoftKeyboard;

import static com.breadwallet.R.color.dark_blue;
import static com.breadwallet.R.color.light_gray;
import static com.breadwallet.R.id.view;

public class IntroRecoverWordsActivity extends Activity {
    private static final String TAG = IntroRecoverWordsActivity.class.getName();
    private Button leftButton;
    private Button rightButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_recover_words);
        setStatusBarColor(android.R.color.transparent);

        leftButton = (Button) findViewById(R.id.left_button);
        rightButton = (Button) findViewById(R.id.right_button);

        chooseWordsSize(true);

        leftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseWordsSize(true);
            }
        });

        rightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseWordsSize(false);
            }
        });

    }

    private void chooseWordsSize(boolean isLeft) {
        GradientDrawable leftDrawable = (GradientDrawable) leftButton.getBackground().getCurrent();
        GradientDrawable rightDrawable = (GradientDrawable) rightButton.getBackground().getCurrent();
        int rad = 30;

        leftDrawable.setCornerRadii(new float[]{rad, rad, 0, 0, 0, 0, rad, rad});
        rightDrawable.setCornerRadii(new float[]{0, 0, rad, rad, rad, rad, 0, 0});

        if (isLeft) {
            leftDrawable.setStroke(2, getColor(dark_blue), 0, 0);
            rightDrawable.setStroke(2, getColor(light_gray), 0, 0);
            leftButton.setTextColor(getColor(dark_blue));
            rightButton.setTextColor(getColor(light_gray));
        } else {
            leftDrawable.setStroke(2, getColor(light_gray), 0, 0);
            rightDrawable.setStroke(2, getColor(dark_blue), 0, 0);
            leftButton.setTextColor(getColor(light_gray));
            rightButton.setTextColor(getColor(dark_blue));
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }

}
