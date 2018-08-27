/**
 * BreadWallet
 * <p/>
 * Created by Shivangi Gandhi on <shivangi@brd.com> 6/7/18.
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
package com.breadwallet.app;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.util.Log;

import com.breadwallet.app.util.UserMetricsUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Use the {@link ApplicationLifecycleObserver} to listen for application lifecycle events.
 */
public class ApplicationLifecycleObserver implements LifecycleObserver {
    private static final String TAG = ApplicationLifecycleObserver.class.getSimpleName();

    private static List<ApplicationLifecycleListener> mListeners = new ArrayList<>();

    /**
     * Called when the application is first created.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onCreate() {
        Log.d(TAG, "onCreate");
        onLifeCycleEvent(Lifecycle.Event.ON_CREATE);
    }

    /**
     * Called when the application is brought into the foreground.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        Log.d(TAG, "onStart");
        onLifeCycleEvent(Lifecycle.Event.ON_START);

        UserMetricsUtil.sendUserMetricsRequest(); //TODO: move out to more appropriate class who listens for these events.
    }

    /**
     * Called when the application is put into the background.
     */
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Log.d(TAG, "onStop");
        onLifeCycleEvent(Lifecycle.Event.ON_STOP);
    }

    /**
     * Passes a lifecycle event to all registered listeners.
     *
     * @param event The lifecycle event that has just occurred.
     */
    private void onLifeCycleEvent(Lifecycle.Event event) {
        for (int i = 0; i < mListeners.size(); i++) {
            ApplicationLifecycleListener listener = mListeners.get(i);
            if (listener != null) {
                listener.onLifeCycle(event);
            }
        }
    }

    /**
     * Registers an application lifecycle listener to receive lifecycle events.
     *
     * @param listener The listener to register.
     */
    public static void addApplicationLifecycleListener(ApplicationLifecycleListener listener) {
        if (listener != null && !mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    /**
     * Unregisters an application lifecycle listener from receiving lifecycle events.
     *
     * @param listener The listener to unregister.
     */
    public static void removeApplicationLifecycleListener(ApplicationLifecycleListener listener) {
        if (listener != null && mListeners != null && mListeners.contains(listener)) {
            mListeners.remove(listener);
        }
    }

    /**
     * The interface to implement to register for lifecycle events.
     */
    public interface ApplicationLifecycleListener {
        void onLifeCycle(Lifecycle.Event event);
    }
}
