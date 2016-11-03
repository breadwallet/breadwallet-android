package com.breadwallet.tools.adapter.tests;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.util.Log;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.tools.manager.BRClipboardManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.R.attr.tag;
import static android.R.attr.type;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.action.ViewActions.typeText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 8/28/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class AmountAdapterTest {
    private static final String TAG = AmountAdapterTest.class.getName();
    private String testAddress = "mhBmRiqosSHR9YnPTKc3xXcvhEcKtjet2p";
    MainActivity activity;
    Button copyAddressFromClipboard;

    @Rule
    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void initStuff() {
        Log.e(TAG, "initStuff: ");
        activity = mActivityRule.getActivity();
        copyAddressFromClipboard = (Button) activity.findViewById(R.id.main_button_pay_address_from_clipboard);
        onView(withId(R.id.address_edit_text)).perform(typeText(testAddress));
        
    }

    @Test
    public void testSeparator() {
        onView(withId(R.id.main_button_pay_address_from_clipboard)).perform(click());
        onView(withText("0")).perform(click());//0
        onView(withText(".")).perform(click());//0.
        onView(withText("0")).perform(click());//0.0
        onView(withText("9")).perform(click());//0.09
        onView(withId(R.id.keyboard_back_button)).perform(click());//0.0
        onView(withText("0")).perform(click());//0.00
        onView(withId(R.id.keyboard_back_button)).perform(click());//0.0
        onView(withId(R.id.keyboard_back_button)).perform(click());//0.
        onView(withText("1")).perform(click());//0.1
        onView(withText("2")).perform(click());//0.12
        onView(withText(".")).perform(click());//0.12
        onView(withText(".")).perform(click());//0.12
        onView(withText("0")).perform(click());//0.12
        onView(withText("0")).perform(click());//0.12
        onView(withText("0")).perform(click());//0.12
        onView(withText(".")).perform(click());//0.12
        onView(withText(".")).perform(click());//0.12
        onView(withText(".")).perform(click());//0.12
        onView(withText("4")).perform(click());//0.12
        onView(withText("5")).perform(click());//0.12
        onView(withId(R.id.keyboard_back_button)).perform(click());//0.1
        onView(withId(R.id.keyboard_back_button)).perform(click());//0.
        onView(withId(R.id.keyboard_back_button)).perform(click());//0
        onView(withText(".")).perform(click());//0.
        onView(withText("8")).perform(click());//0.8
        onView(withText("0")).perform(click());//0.80
        onView(withId(R.id.amount_before_arrow)).perform(click());//0.8
        onView(withId(R.id.keyboard_back_button)).perform(click());//0.
        onView(withId(R.id.keyboard_back_button)).perform(click());//0
        try {
            System.out.println("WAITING 4 SECONDS");
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertEquals("0", AmountAdapter.getRightValue());
        onView(withText("1")).perform(click());
        onView(withText("2")).perform(click());
        onView(withText("3")).perform(click());
        onView(withText("4")).perform(click());
        onView(withText("5")).perform(click());
        onView(withText("6")).perform(click());
        onView(withText("7")).perform(click());
        onView(withText("8")).perform(click());
        onView(withText("9")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("5")).perform(click());
        onView(withText("4")).perform(click());
        onView(withText("2")).perform(click());
        onView(withText("8")).perform(click());
        onView(withText(".")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("1")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText(".")).perform(click());
        onView(withText(".")).perform(click());
        onView(withText(".")).perform(click());
        onView(withText(".")).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());

    }

    @Test
    public void testDigits() {
        onView(withId(R.id.main_button_pay_address_from_clipboard)).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("2")).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "2");
        onView(withId(R.id.keyboard_back_button)).perform(longClick());

    }

    @Test
    public void testBackPress() {
        onView(withId(R.id.main_button_pay_address_from_clipboard)).perform(click());
        onView(withText("0")).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "0");
        onView(withText("0")).perform(click());
        onView(withText(".")).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "0");
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withText(".")).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "0.");
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withText("1")).perform(click());
        onView(withText(".")).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "1");
        onView(withText("1")).perform(click());
        onView(withText(".")).perform(click());
        onView(withText("0")).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "11.0");// check some failing
        onView(withId(R.id.keyboard_back_button)).perform(longClick());

    }

    @Test
    public void testCurrenciesSwitch() {
        onView(withId(R.id.main_button_pay_address_from_clipboard)).perform(click());
        onView(withText("1")).perform(click());
        onView(withText(".")).perform(click());
        onView(withText("0")).perform(click());
        onView(withId(R.id.amount_before_arrow)).perform(click());//0.8
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withText(".")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("1")).perform(click());
        onView(withText(".")).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(longClick());

    }
}