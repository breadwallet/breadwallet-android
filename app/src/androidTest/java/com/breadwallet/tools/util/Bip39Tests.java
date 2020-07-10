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
package com.breadwallet.tools.util;

import android.app.Activity;
import androidx.test.rule.ActivityTestRule;

import androidx.test.runner.AndroidJUnit4;
import com.breadwallet.legacy.presenter.activities.settings.TestActivity;
import com.breadwallet.tools.security.BRKeyStore;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

@RunWith(AndroidJUnit4.class)
public class Bip39Tests {
    private static final String EN_FIRST_WORD = "abandon";
    private static final String ES_FIRST_WORD = "ábaco";
    private static final String FR_FIRST_WORD= "abaisser";
    private static final String IT_FIRST_WORD = "abaco";
    private static final String JA_FIRST_WORD = "あいこくしん";
    private static final String KO_FIRST_WORD= "가격";
    private static final String ZH_HANS_FIRST_WORD = "的";
    private static final String ZH_HANT_FIRST_WORD = "的";

    @Rule
    public ActivityTestRule<TestActivity> mActivityRule = new ActivityTestRule<>(TestActivity.class);

    @Before
    public void setup() {
        BRKeyStore.provideContext(mActivityRule.getActivity());
        BRKeyStore.resetWalletKeyStore();
    }

    @Test
    public void testBip39() {
        Activity testActivity = mActivityRule.getActivity();

        List<String> enList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.EN.toString());
        List<String> esList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.ES.toString());
        List<String> frList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.FR.toString());
        List<String> itList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.IT.toString());
        List<String> jaList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.JA.toString());
        List<String> koList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.KO.toString());
        List<String> zhHansList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.ZH_HANS.toString());
        List<String> zhHantList = Bip39Reader.getBip39Words(testActivity, Bip39Reader.SupportedLanguage.ZH_HANT.toString());

        Assert.assertEquals(enList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(esList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(frList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(itList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(jaList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(koList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(zhHansList.size(), Bip39Reader.WORD_LIST_SIZE);
        Assert.assertEquals(zhHantList.size(), Bip39Reader.WORD_LIST_SIZE);

        Assert.assertTrue(enList.get(0).equalsIgnoreCase(EN_FIRST_WORD));
        Assert.assertTrue(esList.get(0).equalsIgnoreCase(ES_FIRST_WORD));
        Assert.assertTrue(frList.get(0).equalsIgnoreCase(FR_FIRST_WORD));
        Assert.assertTrue(itList.get(0).equalsIgnoreCase(IT_FIRST_WORD));
        Assert.assertTrue(jaList.get(0).equalsIgnoreCase(JA_FIRST_WORD));
        Assert.assertTrue(koList.get(0).equalsIgnoreCase(KO_FIRST_WORD));
        Assert.assertTrue(zhHansList.get(0).equalsIgnoreCase(ZH_HANS_FIRST_WORD));
        Assert.assertTrue(zhHantList.get(0).equalsIgnoreCase(ZH_HANT_FIRST_WORD));

        List<String> allWords = Bip39Reader.getAllBip39Words(testActivity);
        Assert.assertEquals(allWords.size(), Bip39Reader.WORD_LIST_SIZE * Bip39Reader.SupportedLanguage.values().length);
    }

}
