//package com.breadwallet.presenter;
//
//import android.support.test.rule.ActivityTestRule;
//import android.support.test.runner.AndroidJUnit4;
//import android.util.Log;
//import android.widget.Button;
//import android.widget.TextView;
//
//import com.breadwallet.R;
//import com.breadwallet.presenter.activities.MainActivity;
//
//import org.junit.Before;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import static android.support.test.espresso.Espresso.onView;
//import static android.support.test.espresso.action.ViewActions.clearText;
//import static android.support.test.espresso.action.ViewActions.click;
//import static android.support.test.espresso.action.ViewActions.typeText;
//import static android.support.test.espresso.matcher.ViewMatchers.withId;
//import static android.support.test.espresso.matcher.ViewMatchers.withText;
//import static junit.framework.Assert.assertTrue;
//
///**
// * BreadWallet
// * <p/>
// * Created by Mihail on 8/28/15.
// * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
// * <p/>
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// * <p/>
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// * <p/>
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//@RunWith(AndroidJUnit4.class)
//public class ScanResultTest {
//    private static final String TAG = ScanResultTest.class.getName();
//    private String testAddress = "mhBmRiqosSHR9YnPTKc3xXcvhEcKtjet2p";
//    MainActivity activity;
//    Button copyAddressFromClipboard;
//
//    @Rule
//    public ActivityTestRule<MainActivity> mActivityRule = new ActivityTestRule<>(
//            MainActivity.class);
//
//    @Before
//    public void initStuff() {
//        Log.e(TAG, "initStuff: ");
//        activity = mActivityRule.getActivity();
//        onView(withId(R.id.address_edit_text)).perform(clearText());
//        copyAddressFromClipboard = (Button) activity.findViewById(R.id.main_button_pay_address_from_clipboard);
//        onView(withId(R.id.address_edit_text)).perform(typeText(testAddress));
//        onView(withId(R.id.main_button_pay_address_from_clipboard)).perform(click());
//    }
//
//    @Test
//    public void testSeparator() {
//        onView(withText("0")).perform(click());//0
//        TextView rightValue = (TextView) mActivityRule.getActivity().findViewById(R.id.right_textview);
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0"));
//        onView(withText(".")).perform(click());//0.
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0."));
//        onView(withText("0")).perform(click());//0.0
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.0"));
//        onView(withText("9")).perform(click());//0.09
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.09"));
//        onView(withText("7")).perform(click());//0.09
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.09"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.0
//        onView(withText("2")).perform(click());//0.02
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.02"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.0
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.0"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0."));
//        onView(withText("1")).perform(click());//0.1
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.1"));
//        onView(withText("2")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText(".")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText(".")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText("0")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText("0")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText("0")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText(".")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText(".")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText(".")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText("4")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withText("5")).perform(click());//0.12
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.12"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.1
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.1"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0."));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0"));
//        onView(withText(".")).perform(click());//0.
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0."));
//        onView(withText("8")).perform(click());//0.8
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.8"));
//        onView(withText("0")).perform(click());//0.80
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0.80"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.8
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0.
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0."));
//        onView(withId(R.id.keyboard_back_button)).perform(click());//0
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0"));
//    }
//
//    @Test
//    public void testDigits() {
//        TextView rightValue = (TextView) mActivityRule.getActivity().findViewById(R.id.right_textview);
//        onView(withText("1")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("1"));
//        onView(withText("2")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12"));
//        onView(withText("3")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("123"));
//        onView(withText("4")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("1234"));
//        onView(withText("5")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345"));
//        onView(withText("6")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("123456"));
//        onView(withText("7")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("1234567"));
//        onView(withText("8")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678"));
//        onView(withText("9")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("123456789"));
//        onView(withText("0")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("1234567890"));
//        onView(withText("5")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905"));
//        onView(withText("4")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("123456789054"));
//        onView(withText("2")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("1234567890542"));
//        onView(withText("8")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428"));//21,000,000,000,000 Max
//        onView(withText("3")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428"));//should be same, no change, max reached
//        onView(withText(".")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428."));
//        onView(withText("0")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.0"));
//        onView(withText("1")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.01"));
//        onView(withText("0")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.01"));
//        onView(withText(".")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.01"));
//        onView(withText(".")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.01"));
//        onView(withText(".")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.01"));
//        onView(withText(".")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("12345678905428.01"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0"));
//        onView(withText("0")).perform(click());
//        onView(withText("0")).perform(click());
//        onView(withText("0")).perform(click());
//        onView(withText("0")).perform(click());
//        onView(withText("0")).perform(click());
//        onView(withText("2")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("2"));
//
//    }
//
//    @Test
//    public void testBackPress() {
//        TextView rightValue = (TextView) mActivityRule.getActivity().findViewById(R.id.right_textview);
//        onView(withText("0")).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0"));
//        onView(withText("0")).perform(click());
//        onView(withText(".")).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0"));
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withText(".")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("0."));
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withText("1")).perform(click());
//        onView(withText(".")).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("1"));
//        onView(withText("1")).perform(click());
//        onView(withText(".")).perform(click());
//        onView(withText("0")).perform(click());
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase("11.0"));
//
//    }
//
//    @Test
//    public void testCurrenciesSwitch() {
//        TextView rightValue = (TextView) mActivityRule.getActivity().findViewById(R.id.right_textview);
//        TextView leftValue = (TextView) mActivityRule.getActivity().findViewById(R.id.left_textview);
//
//        onView(withText("9")).perform(click());
//        onView(withText("2")).perform(click());
//        onView(withText("2")).perform(click());
//        onView(withText(".")).perform(click());
//        onView(withText("0")).perform(click());
//        String leftVal = getCleanValue(leftValue.getText().toString());
//        onView(withId(R.id.left_textview)).perform(click());
//        assertTrue(getCleanValue(leftValue.getText().toString()).equalsIgnoreCase("922.0"));
//        assertTrue(getCleanValue(rightValue.getText().toString()).equalsIgnoreCase(leftVal));
//
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withId(R.id.keyboard_back_button)).perform(click());
//        onView(withText("1")).perform(click());
//
//    }
//
//    private String getCleanValue(String str) {
//        StringBuilder builder = new StringBuilder();
//        for (int i = 0; i < str.length(); i++) {
//            char c = str.charAt(i);
//            if (Character.isDigit(c) || c == '.')
//                builder.append(c);
//        }
//        return builder.toString();
//    }
//}