package com.breadwallet.security;

import android.app.Activity;
import android.security.keystore.UserNotAuthenticatedException;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.breadwallet.legacy.presenter.activities.settings.TestActivity;
import com.breadwallet.tools.security.BRKeyStore;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;


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
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setup() {
        BRKeyStore.provideContext(mActivityRule.getActivity());
        BRKeyStore.resetWalletKeyStore();
    }

    @Test
    public void testBase64() {
        Activity app = mActivityRule.getActivity();
        String temp = "here is some data to encrypt! @#$%^&*";
        byte[] phrase = temp.getBytes();
        BRKeyStore.storeEncryptedData(app, phrase, "BASE64_TEST");
        byte[] retrievedPhrase = BRKeyStore.retrieveEncryptedData(app, "BASE64_TEST");
        Assert.assertNotNull(retrievedPhrase);
        Assert.assertArrayEquals(phrase, retrievedPhrase);
        String newTemp = new String(retrievedPhrase);
        Assert.assertEquals(temp, newTemp);
    }

    @Test
    @Ignore("Does not work as intended.")
    public void setNewGetNew() {
        //set get phrase
        byte[] phrase = "axis husband project any sea patch drip tip spirit tide bring belt".getBytes();
        try {
            boolean b = BRKeyStore.putPhrase(phrase, mActivityRule.getActivity(), 0);
            Assert.assertEquals(true, b);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            byte[] getPhrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
            Assert.assertNotNull(getPhrase);
            Assert.assertEquals(new String(getPhrase), new String(phrase));
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    /*@Test
    public void setOldSetNew() {
        byte[] phrase = "axis husband project any sea patch drip tip spirit tide bring belt".getBytes();
        BRKeyStore.AliasObject obj = ALIAS_OBJECT_MAP.get(PHRASE_ALIAS);
        try {
            boolean b = BRKeyStore._setOldData(mActivityRule.getActivity(), phrase, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, true);
            Assert.assertEquals(true, b);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            byte[] getPhrase = BRKeyStore._getOldData(mActivityRule.getActivity(), obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
            Assert.assertNotNull(getPhrase);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

        try {
            boolean b = BRKeyStore.putPhrase(phrase, mActivityRule.getActivity(), 0);
            Assert.assertEquals(true, b);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

        try {
            byte[] getPhrase = BRKeyStore._getOldData(mActivityRule.getActivity(), obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
            Assert.assertNull(getPhrase);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

        try {
            byte[] getPhrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
            Assert.assertNotNull(getPhrase);
            Assert.assertEquals(new String(getPhrase), new String(phrase));
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }*/

    /*@Test
    public void setOldGetNew() {

        //set get phrase
        byte[] phrase = "axis husband project any sea patch drip tip spirit tide bring belt".getBytes();
        BRKeyStore.AliasObject obj = ALIAS_OBJECT_MAP.get(PHRASE_ALIAS);
        try {
            boolean s = BRKeyStore._setOldData(mActivityRule.getActivity(), phrase, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, true);
            Assert.assertEquals(true, s);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            byte[] getPhrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
            Assert.assertNotNull(getPhrase);
            Assert.assertEquals(new String(getPhrase), new String(phrase));
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            byte[] getPhrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
            Assert.assertNotNull(getPhrase);
            Assert.assertEquals(new String(getPhrase), new String(phrase));
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            byte[] getPhrase = BRKeyStore._getOldData(mActivityRule.getActivity(), obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
            Assert.assertNull(getPhrase);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        try {
            byte[] getPhrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
            Assert.assertNotNull(getPhrase);
            Assert.assertEquals(new String(getPhrase), new String(phrase));
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

    }

    @Test
    public void allMultiThreading() {
        for (int i = 0; i < 10; i++) {
            setup();
            setNewGetNew();
            setup();
            setOldGetNew();
            setup();
            setOldSetNew();
            setup();

        }

    }*/

}
