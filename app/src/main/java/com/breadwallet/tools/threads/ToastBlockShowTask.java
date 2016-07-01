package com.breadwallet.tools.threads;

import android.app.Activity;
import android.util.Log;
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
 * Created by Mihail Gutan on 3/24/16.
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
public class ToastBlockShowTask {
    public static final String TAG = ToastBlockShowTask.class.getName();
    private static ToastBlockShowTask toastBlockShowTask;
    private Activity activity;
    private ToastThread toastThread;

    private ToastBlockShowTask(Activity activity) {
        this.activity = activity;
    }

    public static ToastBlockShowTask getInstance(Activity activity) {
        if (toastBlockShowTask == null) {
            toastBlockShowTask = new ToastBlockShowTask(activity);
        }
        return toastBlockShowTask;
    }


    public void startOneToast() {
        if (toastThread != null) toastThread.interrupt();
        toastThread = new ToastThread();
        toastThread.start();
    }

    private class ToastThread extends Thread {
        private Toast blocksToast;
        private TextView text;
        private View layout;
        private String currBlock;
        private String latestBlockKnown;
        private String formattedBlockInfo;
        private LayoutInflater inflater;
        private int timePeriod = 0;
        private int interval = 500;

        @Override
        public void run() {
            if (MainActivity.appInBackground || activity == null) {
                interrupt();
                return;
            }
            if (blocksToast == null || !blocksToast.getView().isShown()) {
                currBlock = String.valueOf(BRPeerManager.getCurrentBlockHeight());
                latestBlockKnown = String.valueOf(BRPeerManager.getEstimatedBlockHeight());
                Runnable runnable = new Runnable() {
                    public void run() {
                        blocksToast = new Toast(activity);
                        inflater = activity.getLayoutInflater();
                        layout = inflater.inflate(R.layout.toast,
                                (ViewGroup) activity.findViewById(R.id.toast_layout_root));
                        text = (TextView) layout.findViewById(R.id.toast_text);
                        formattedBlockInfo = String.format(activity.getString(R.string.blocks), currBlock, latestBlockKnown);
                        text.setText(formattedBlockInfo);
                        blocksToast.setGravity(Gravity.TOP, 0, MainActivity.screenParametersPoint.y / 8);
                        blocksToast.setDuration(Toast.LENGTH_LONG);
                        blocksToast.setView(layout);
                        blocksToast.show();
                    }
                };
                activity.runOnUiThread(runnable);
            } else {
                return;
            }
            while ((timePeriod += interval) < 5000) {
                currBlock = String.valueOf(BRPeerManager.getCurrentBlockHeight());
                latestBlockKnown = String.valueOf(BRPeerManager.getEstimatedBlockHeight());
                Log.e(TAG, "in the while: " + getName());
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        formattedBlockInfo = String.format(activity.getString(R.string.blocks), currBlock, latestBlockKnown);
                        text.setText(formattedBlockInfo);
                    }
                });
                try {
                    Thread.sleep(interval);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
