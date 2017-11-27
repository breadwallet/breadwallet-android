package com.breadwallet.security;

import android.app.Activity;
import android.content.Context;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.BRExecutor;
import com.jniwrappers.BRKey;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;

import static com.breadwallet.tools.security.BRKeyStore.aliasObjectMap;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/20/16.
 * Copyright (c) 2016 breadwallet LLC
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
@RunWith(AndroidJUnit4.class)
public class NewKeyStoreTests {
    public static final String TAG = NewKeyStoreTests.class.getName();

    @Rule
    public ActivityTestRule<BreadActivity> mActivityRule = new ActivityTestRule<>(BreadActivity.class);

    @Test
    public void testBase64() {
//        Activity app = mActivityRule.getActivity();
//        String temp = "here is some data to encrypt! @#$%^&*";
//        byte[] phrase = temp.getBytes();
//        BRKeyStore.storeEncryptedData(app, phrase, "phrase");
//        byte[] retrievedPhrase = BRKeyStore.retrieveEncryptedData(app, "phrase");
//        Assert.assertNotNull(retrievedPhrase);
//        Assert.assertArrayEquals("Oh no", phrase, retrievedPhrase);
//        String newTemp = new String(retrievedPhrase);
//        Assert.assertEquals(temp, newTemp);

    }


}
