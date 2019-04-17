/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 10/13/18.
 * Copyright (c) 2018 breadwallet LLC
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

package com.breadwallet.protocols.messageexchange;

import android.arch.lifecycle.Lifecycle;
import android.content.Context;

import com.breadwallet.app.ApplicationLifecycleObserver;


/**
 * Application lifecycle observer that starts and stops inbox polling.
 */
public class InboxPollingAppLifecycleObserver implements
        ApplicationLifecycleObserver.ApplicationLifecycleListener {

    private Context mContext;

    public InboxPollingAppLifecycleObserver(Context context) {
        mContext = context;
    }

    @Override
    public void onLifeCycle(Lifecycle.Event event) {
        switch (event) {
            case ON_START:
                InboxPollingHandler.getInstance().startPolling(mContext);
                break;
            case ON_STOP:
                InboxPollingHandler.getInstance().enqueueCleanUp();
                break;
        }
    }
}
