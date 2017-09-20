package com.breadwallet.tools.manager;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.breadwallet.tools.adapter.TransactionListAdapter;

import java.util.Calendar;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/19/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class SyncManager {

    private static final String TAG = SyncManager.class.getName();
    private static SyncManager instance;

    public static SyncManager getInstance() {
        if (instance == null) instance = new SyncManager();
        return instance;
    }

    private SyncManager() {
    }

    public void createAlarm(long time){
        AlarmManager alarmManager = (AlarmManager) getActivity().getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(getActivity(), MyReceiver.class);
        intent.setAction(MyReceiver.ACTION_ALARM_RECEIVER);//my custom string action name
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getActivity(), 1001, intent, PendingIntent.FLAG_CANCEL_CURRENT);//used unique ID as 1001
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), aroundInterval, pendingIntent);//first start will start asap
    }

    public void updateAlarms(){

    }

    public class SyncReceiver extends BroadcastReceiver {
        public  final String TAG = SyncReceiver.class.getSimpleName();
        public  final String ACTION_ALARM_RECEIVER = "ACTION_ALARM_RECEIVER";
        private Calendar c = Calendar.getInstance();
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null)
                if (ACTION_ALARM_RECEIVER.equals(intent.getAction())) {
                    Log.e(TAG, new Exception().getStackTrace()[0].getMethodName() + " " + c.getTime());
                    //do something here
                }
        }
    }

}
