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
import com.breadwallet.tools.animation.SpringAnimator;

import static com.breadwallet.R.color.dark_blue;
import static com.breadwallet.R.color.extra_light_grey;
import static com.breadwallet.R.color.light_gray;
import static com.breadwallet.R.id.view;

public class IntroRecoverWordsActivity extends Activity {
    private static final String TAG = IntroRecoverWordsActivity.class.getName();
    private Button leftButton;
    private Button rightButton;
    private Button nextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_recover_words);
        setStatusBarColor(android.R.color.transparent);

        leftButton = (Button) findViewById(R.id.left_button);
        rightButton = (Button) findViewById(R.id.right_button);
        nextButton = (Button) findViewById(R.id.next_button);

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

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: NEXT");
            }
        });

    }

    private void chooseWordsSize(boolean isLeft) {
        int activeColor = getColor(dark_blue);
        int nonActiveColor = getColor(extra_light_grey);
        GradientDrawable leftDrawable = (GradientDrawable) leftButton.getBackground().getCurrent();
        GradientDrawable rightDrawable = (GradientDrawable) rightButton.getBackground().getCurrent();

        int rad = 30;
        int stoke = 3;

        leftDrawable.setCornerRadii(new float[]{rad, rad, 0, 0, 0, 0, rad, rad});
        rightDrawable.setCornerRadii(new float[]{0, 0, rad, rad, rad, rad, 0, 0});

        if (isLeft) {
            leftDrawable.setStroke(stoke, activeColor, 0, 0);
            rightDrawable.setStroke(stoke, nonActiveColor, 0, 0);
            leftButton.setTextColor(activeColor);
            rightButton.setTextColor(nonActiveColor);
        } else {
            leftDrawable.setStroke(stoke, nonActiveColor, 0, 0);
            rightDrawable.setStroke(stoke, activeColor, 0, 0);
            leftButton.setTextColor(nonActiveColor);
            rightButton.setTextColor(activeColor);
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
