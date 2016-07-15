package com.breadwallet.tools.manager;

import android.app.Instrumentation;
import android.graphics.Point;
import android.media.Image;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.v4.view.ViewPager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CustomPagerAdapter;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 7/14/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class BRTipsManager {
    public static final String TAG = BRTipsManager.class.getName();
    private static int count = 0;

    public static void showTipsTutorial(MainActivity app) {
        count = 0;
        if (app == null) app = MainActivity.app;
        if (app == null) return;
        final RelativeLayout tipsBlockPane = (RelativeLayout) app.findViewById(R.id.tips_block_pane);
        final TextView sendText = (TextView) app.findViewById(R.id.send_money_text);
        final ImageView qrcode = (ImageView) app.findViewById(R.id.main_image_qr_code);
        final ViewFlipper viewFlipper = (ViewFlipper) MainActivity.app.findViewById(R.id.middle_view_flipper);
        if (tipsBlockPane == null || qrcode == null ||
                sendText == null || viewFlipper == null) return;
        tipsBlockPane.setVisibility(View.VISIBLE);
        final MainActivity finalApp = app;
        viewFlipper.performClick();
        tipsBlockPane.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                switch (count) {
                    case 0:
                        viewFlipper.performClick();
                        count++;
                        break;
                    case 1:
                        sendText.performClick();
                        count++;
                        break;
                    case 2:
                        sendText.performClick();
                        tipsBlockPane.setVisibility(View.GONE);
                        swipeLeft(finalApp);
                        count++;
                        break;
                    case 3:
                        sendText.performClick();
                        qrcode.performClick();
                        count++;
                        break;
                    case 4:
                        qrcode.performClick();
                        count = 0;
                        tipsBlockPane.setVisibility(View.GONE);
                        break;

                }
            }
        });
    }

    private static void swipeLeft(MainActivity app) {


//        // Obtain MotionEvent object
//        if (app == null) return;
//
//        long downTime = SystemClock.uptimeMillis();
//        long eventTime = SystemClock.uptimeMillis() + 100;
//        float x = 0.5f;
//        float y = 0.0f;
//        int metaState = 0;
//        MotionEvent motionEvent = MotionEvent.obtain(
//                downTime,
//                eventTime,
//                MotionEvent.ACTION_MOVE,
//                x,
//                y,
//                metaState
//        );
//
//        app.findViewById(R.id.scan_clipboard_buttons_layout).dispatchTouchEvent(motionEvent);
    }
}
