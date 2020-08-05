/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 11/20/16.
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
package com.breadwallet.security;

import android.app.Activity;
import android.security.keystore.UserNotAuthenticatedException;
import androidx.test.rule.ActivityTestRule;

import androidx.test.runner.AndroidJUnit4;
import com.breadwallet.legacy.presenter.activities.settings.TestActivity;
import com.breadwallet.tools.security.BRKeyStore;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@Ignore("TokenUtils does not function")
public class KeyStoreTests {
    public static final String TAG = KeyStoreTests.class.getName();

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Test
    public void setGetPhrase() {
        //set get phrase
        byte[] phrase = "axis husband project any sea patch drip tip spirit tide bring belt".getBytes();
        try {
            BRKeyStore.putPhrase(phrase, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(BRKeyStore.PHRASE_ALIAS);

        byte[] freshGet = new byte[0];
        try {
            freshGet = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(freshGet, phrase);

        //set get Japaneese phrase
        byte[] japPhrase = "こせき　ぎじにってい　けっこん　せつぞく　うんどう　ふこう　にっすう　こせい　きさま　なまみ　たきび　はかい".getBytes();
        try {
            BRKeyStore.putPhrase(japPhrase, mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }
        assertFilesExist(BRKeyStore.PHRASE_ALIAS);
        byte[] freshJapGet = new byte[0];
        try {
            freshJapGet = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        Assert.assertArrayEquals(freshJapGet, japPhrase);

    }

    @Test
    public void setGetAll() {
        /*byte[] pubKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putMasterPublicKey(pubKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PUB_KEY_ALIAS);
        byte[] freshGet = BRKeyStore.getMasterPublicKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(pubKey, freshGet);

        pubKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putEthPublicKey(pubKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.ETH_PUBKEY_ALIAS);
        freshGet = BRKeyStore.getEthPublicKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, pubKey);

        byte[] authKey = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putAuthKey(authKey, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.AUTH_KEY_ALIAS);
        freshGet = BRKeyStore.getAuthKey(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, authKey);

        byte[] token = "26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes();
        BRKeyStore.putToken(token, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.TOKEN_ALIAS);
        freshGet = BRKeyStore.getToken(mActivityRule.getActivity());
        Assert.assertArrayEquals(freshGet, token);

        int time = 1479686841;
        BRKeyStore.putWalletCreationTime(time, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.WALLET_CREATION_TIME_ALIAS);
        long freshTime = BRKeyStore.getWalletCreationTime(mActivityRule.getActivity());
        Assert.assertEquals(time, freshTime);

        String passCode = "0124";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        String freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);
        passCode = "0000";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);
        passCode = "9999";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);
        passCode = "9876";
        BRKeyStore.putPinCode(passCode, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_CODE_ALIAS);
        freshPass = BRKeyStore.getPinCode(mActivityRule.getActivity());
        Assert.assertEquals(passCode, freshPass);

        int failCount = 2;
        BRKeyStore.putFailCount(failCount, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.FAIL_COUNT_ALIAS);
        int freshFailCount = BRKeyStore.getFailCount(mActivityRule.getActivity());
        Assert.assertEquals(failCount, freshFailCount);

        long failTime = 1479686841;
        BRKeyStore.putFailTimeStamp(failTime, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.FAIL_TIMESTAMP_ALIAS);
        long freshFailTime = BRKeyStore.getFailTimeStamp(mActivityRule.getActivity());
        Assert.assertEquals(failTime, freshFailTime);

        long passTime = 1479686841;
        BRKeyStore.putLastPinUsedTime(time, mActivityRule.getActivity());
        assertFilesExist(BRKeyStore.PASS_TIME_ALIAS);
        long freshPassTime = BRKeyStore.getLastPinUsedTime(mActivityRule.getActivity());
        Assert.assertEquals(passTime, freshPassTime);*/
    }

    @Test
    public void testClearKeyStore() {
        try {
            BRKeyStore.putPhrase("axis husband project any sea patch drip tip spirit tide bring belt".getBytes(), mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            Assert.fail();
        }

        //BRKeyStore.putMasterPublicKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        //BRKeyStore.putAuthKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        //BRKeyStore.putToken("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());
        BRKeyStore.putWalletCreationTime(1479686841);
        BRKeyStore.putPinCode("0123");
        BRKeyStore.putFailCount(3);
        BRKeyStore.putFailTimeStamp(1479686841);
        //BRKeyStore.putLastPinUsedTime(1479686841, mActivityRule.getActivity());
        //BRKeyStore.putEthPublicKey("26wZYDdvpmCrYZeUcxgqd1KquN4o6wXwLomBW5SjnwUqG".getBytes(), mActivityRule.getActivity());

        for (String a : BRKeyStore.ALIAS_OBJECT_MAP.keySet()) {
            assertFilesExist(a);
        }

        BRKeyStore.resetWalletKeyStore();

        for (String a : BRKeyStore.ALIAS_OBJECT_MAP.keySet()) {
            assertFilesDontExist(a);
        }


        byte[] phrase = "some".getBytes();
        try {
            phrase = BRKeyStore.getPhrase(mActivityRule.getActivity(), 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        Assert.assertNull(phrase);
        Assert.assertEquals(null, BRKeyStore.getMasterPublicKey());
        Assert.assertEquals(null, BRKeyStore.getEthPublicKey());
        Assert.assertEquals(null, BRKeyStore.getAuthKey());
        Assert.assertEquals(null, BRKeyStore.getToken());
        Assert.assertEquals(0, BRKeyStore.getWalletCreationTime());
        Assert.assertEquals("", BRKeyStore.getPinCode());
        Assert.assertEquals(0, BRKeyStore.getFailCount());
        Assert.assertEquals(0, BRKeyStore.getFailTimeStamp());

    }

    @Test
    public void testKeyStoreAuthTime() {
        Assert.assertEquals(BRKeyStore.AUTH_DURATION_SEC, 300);
    }

    @Test
    public void testKeyStoreAliasMap() {
        Assert.assertNotNull(BRKeyStore.ALIAS_OBJECT_MAP);
        Assert.assertEquals(BRKeyStore.ALIAS_OBJECT_MAP.size(), 13);
    }

    public void assertFilesExist(String alias) {
        Activity app = mActivityRule.getActivity();
        byte[] data = BRKeyStore.retrieveEncryptedData(app, alias);
        Assert.assertNotNull(data);
        Assert.assertNotEquals(data.length, 0);

        byte[] iv = BRKeyStore.retrieveEncryptedData(app, BRKeyStore.ALIAS_OBJECT_MAP.get(alias).getIvFileName());
        Assert.assertNotNull(iv);
        Assert.assertNotEquals(iv.length, 0);

    }

    public void assertFilesDontExist(String alias) {
        Activity app = mActivityRule.getActivity();
        byte[] data = BRKeyStore.retrieveEncryptedData(app, alias);
        Assert.assertNull(data);

        byte[] iv = BRKeyStore.retrieveEncryptedData(app, BRKeyStore.ALIAS_OBJECT_MAP.get(alias).getIvFileName());
        Assert.assertNull(iv);
    }

}
