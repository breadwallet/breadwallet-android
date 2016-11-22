package com.breadwallet.tools.manager;

import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ViewFlipper;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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
    private static boolean tryToGoToMainScreen = true;

    public static void showTipsTutorial(MainActivity app) {
        tryToGoToMainScreen = true;
        count = 0;
        if (app == null) app = MainActivity.app;
        if (app == null || SharedPreferencesManager.getTipsShown(app)) return;
        final RelativeLayout tipsBlockPane = (RelativeLayout) app.findViewById(R.id.tips_block_pane);
        final LinearLayout sendText = (LinearLayout) app.findViewById(R.id.send_money_text_layout);
        final ImageView qrcode = (ImageView) app.findViewById(R.id.main_image_qr_code);
        final ViewFlipper viewFlipper = (ViewFlipper) app.findViewById(R.id.middle_view_flipper);
        if (tipsBlockPane == null || qrcode == null ||
                sendText == null || viewFlipper == null) return;
        tipsBlockPane.setVisibility(View.VISIBLE);
        final MainActivity finalApp = app;
        while (BRAnimator.level != 0 && tryToGoToMainScreen) BRAnimator.pressMenuButton(app);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tryToGoToMainScreen = false;
            }
        }, 1000);
        app.parallaxViewPager.setCurrentItem(1);
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                finalApp.middleBubble1.setVisibility(View.VISIBLE);
                SpringAnimator.showBubbleAnimation(finalApp.middleBubble1);
                tipsBlockPane.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        switch (count) {
                            case 0:
                                finalApp.middleBubble1.setVisibility(View.GONE);
                                finalApp.middleBubble2.setVisibility(View.VISIBLE);
                                SpringAnimator.showBubbleAnimation(finalApp.middleBubble2);
                                count++;
                                break;
                            case 1:
                                qrcode.performClick();
                                count++;
                                break;
                            case 2:
                                qrcode.performClick();
                                count++;
                                break;
                            case 3:
                                finalApp.parallaxViewPager.setCurrentItem(0);
                                sendText.performClick();
                                count++;
                                break;
                            case 4:
                                sendText.performClick();
                                count++;
                                break;
                            case 5:
                                finalApp.parallaxViewPager.setCurrentItem(1);
                                count = 0;
                                SharedPreferencesManager.putTipsShown(finalApp, true);
                                tipsBlockPane.setVisibility(View.GONE);
                                break;
                        }
                    }
                });
            }
        }, 500);

    }

    public static void setSendBubblesPosition(int send1, int send2) {

        MainActivity app = MainActivity.app;
        if (app != null && app.qrBubble1 != null && app.qrBubble2 != null) {
            app.sendBubble1.setY(send1);
            app.sendBubble2.setY(send2);
        }
    }

    public static void setQrBubblesPosition(int qr1, int qr2) {
        MainActivity app = MainActivity.app;
        if (app != null && app.qrBubble1 != null && app.qrBubble2 != null) {
            app.qrBubble1.setY(qr1);
            app.qrBubble2.setY(qr2);
        }

    }

}
