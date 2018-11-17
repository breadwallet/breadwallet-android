/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/8/18.
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

package com.breadwallet.tools.animation;

import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.util.Log;

import com.breadwallet.presenter.activities.intro.OnBoardingActivity;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class OnBoardingAnimationManager {
    private static final String TAG = OnBoardingAnimationManager.class.getSimpleName();

    private static final int ANIMATION_FRAME_DURATION = 50;
    private static final String FRAMES_FOLDER = "frames";
    private static final String FRAME_NAME_PREFIX = "anim";
    private static List<AnimationDrawable> mAnimationDrawables = new ArrayList<>();

    /**
     * Loads and caches the animation Drawables
     * NODE: call disposeAnimationFrames() once done.
     *
     * @param context
     */
    public static void loadAnimationFrames(Context context) {
        Log.d(TAG, "loadAnimationFrames: Loading..");
        if (mAnimationDrawables.size() == 0) {
            for (int i = 0; i < OnBoardingActivity.FOURTH_SCENE; i++) {
                mAnimationDrawables.add(new AnimationDrawable());
            }
            try {
                String[] frames = context.getAssets().list(FRAMES_FOLDER);
                ArrayList<String> frameList = new ArrayList<>(Arrays.asList(frames));
                for (String frameName : frameList) {
                    try (InputStream inputstream = context.getAssets().open(FRAMES_FOLDER + "/" + frameName)) {
                        Drawable drawable = Drawable.createFromStream(inputstream, null);
                        int sceneNumber = Integer.valueOf(frameName.replace(FRAME_NAME_PREFIX, "").substring(0, 1));
                        mAnimationDrawables.get(sceneNumber - 1).addFrame(drawable, ANIMATION_FRAME_DURATION);
                    }
                }

            } catch (IOException e) {
                Log.e(TAG, "loadAnimationFrames: ", e);
            }
        }
    }

    public static synchronized AnimationDrawable getAnimationDrawable(int sceneNumber) {
        return mAnimationDrawables.get(sceneNumber - 1);
    }

    /**
     * Disposes the cached animation drawables from the memory.
     */
    public static synchronized void disposeAnimationFrames() {
        mAnimationDrawables.clear();
    }
}
