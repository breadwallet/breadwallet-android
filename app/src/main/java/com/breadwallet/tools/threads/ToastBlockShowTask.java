package com.breadwallet.tools.threads;

import android.app.Activity;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.wallet.BRPeerManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 3/24/16.
 * Copyright (c) 2016 Mihail Gutan <mihail@breadwallet.com>
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
public class ToastBlockShowTask extends Thread {
    public static final String TAG = ToastBlockShowTask.class.getName();
    private static Toast blocksToast;
    private TextView text;
    private View layout;
    private String currBlock;
    private String latestBlockKnown;
    private String formattedBlockInfo;
    private LayoutInflater inflater;
    private Activity activity;

    public ToastBlockShowTask(Activity activity) {
        this.activity = activity;
    }

    @Override
    public void run() {

        if (MainActivity.appInBackground || activity == null) {
            interrupt();
            return;
        }
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (blocksToast == null) {
                    blocksToast = new Toast(activity);
                    inflater = activity.getLayoutInflater();
                    layout = inflater.inflate(R.layout.toast,
                            (ViewGroup) activity.findViewById(R.id.toast_layout_root));
                    text = (TextView) layout.findViewById(R.id.toast_text);
                    currBlock = String.valueOf(BRPeerManager.getCurrentBlockHeight());
                    latestBlockKnown = String.valueOf(BRPeerManager.getEstimatedBlockHeight());
                    formattedBlockInfo = String.format("block #%s of %s", currBlock, latestBlockKnown);
                    text.setText(formattedBlockInfo);
                    blocksToast.setGravity(Gravity.TOP, 0, MainActivity.screenParametersPoint.y / 8);
                    blocksToast.setDuration(Toast.LENGTH_LONG);
                    blocksToast.setView(layout);
                    blocksToast.show();
                }
            }
        });

        while (blocksToast != null && text != null) {
            if (blocksToast.getView().isShown()) {
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        currBlock = String.valueOf(BRPeerManager.getCurrentBlockHeight());
                        latestBlockKnown = String.valueOf(BRPeerManager.getEstimatedBlockHeight());
                        formattedBlockInfo = String.format("block #%s of %s", currBlock, latestBlockKnown);
                        text.setText(formattedBlockInfo);
                    }
                });
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

}
