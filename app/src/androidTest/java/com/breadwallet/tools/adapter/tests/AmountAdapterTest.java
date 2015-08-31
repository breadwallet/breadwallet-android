package com.breadwallet.tools.adapter.tests;

import android.support.test.InstrumentationRegistry;
import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.LargeTest;
import android.test.suitebuilder.annotation.MediumTest;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.AmountAdapter;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

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
public class AmountAdapterTest extends ActivityInstrumentationTestCase2<MainActivity> {
    private String testAddress = "mhBmRiqosSHR9YnPTKc3xXcvhEcKtjet2p";
    MainActivity activity;
    Button copyAddressFromClipboard;

    public AmountAdapterTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        setActivityInitialTouchMode(true);
        injectInstrumentation(InstrumentationRegistry.getInstrumentation()); // injects the Instrumentation for the Espresso
        activity = getActivity();
        copyAddressFromClipboard = (Button) activity.findViewById(R.id.main_button_pay_address_from_clipboard);
    }

    @MediumTest
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
        assertEquals(AmountAdapter.getRightValue(), "0");
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

    @MediumTest
    public void testDigits() {
        onView(withId(R.id.main_button_pay_address_from_clipboard)).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("0")).perform(click());
        onView(withText("2")).perform(click());
        assertEquals(AmountAdapter.getRightValue(), "2");
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());

    }

    @MediumTest
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
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());

    }

    @LargeTest
    public void testCurrenciesSwitch(){
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
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());
        onView(withId(R.id.keyboard_back_button)).perform(click());

    }
}
