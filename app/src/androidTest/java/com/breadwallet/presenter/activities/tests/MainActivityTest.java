//package com.breadwallet.presenter.activities.tests;
//
//import android.support.test.InstrumentationRegistry;
//import android.test.ActivityInstrumentationTestCase2;
//import android.test.suitebuilder.annotation.MediumTest;
//import android.widget.Button;
//
//import com.breadwallet.R;
//import com.breadwallet.presenter.activities.MainActivity;
//import com.breadwallet.tools.adapter.CurrencyListAdapter;
//
//import java.util.Random;
//
//import static android.support.test.espresso.Espresso.onData;
//import static android.support.test.espresso.Espresso.onView;
//import static android.support.test.espresso.Espresso.pressBack;
//import static android.support.test.espresso.action.ViewActions.clearText;
//import static android.support.test.espresso.action.ViewActions.click;
//import static android.support.test.espresso.action.ViewActions.closeSoftKeyboard;
//import static android.support.test.espresso.action.ViewActions.swipeLeft;
//import static android.support.test.espresso.action.ViewActions.swipeRight;
//import static android.support.test.espresso.action.ViewActions.typeText;
//import static android.support.test.espresso.matcher.ViewMatchers.withId;
//import static android.support.test.espresso.matcher.ViewMatchers.withText;
//import static org.hamcrest.Matchers.startsWith;
//import static org.hamcrest.object.HasToString.hasToString;
//
///**
// * BreadWallet
// * <p/>
// * Created by Mihail on 7/7/15.
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
//
//public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {
//
//    MainActivity activity;
//    Button burgerButton;
//    Button scanQRCode;
//    Button copyAddressFromClipboard;
//
//    public MainActivityTest() {
//        super(MainActivity.class);
//    }
//
//    @Override
//    protected void setUp() throws Exception {
//        super.setUp();
//        setActivityInitialTouchMode(true);
//        injectInstrumentation(InstrumentationRegistry.getInstrumentation()); // injects the Instrumentation for the Espresso
//        activity = getActivity();
//        burgerButton = (Button) activity.findViewById(R.id.main_button_burger);
//        scanQRCode = (Button) activity.findViewById(R.id.main_button_scan_qr_code);
//        copyAddressFromClipboard = (Button) activity.findViewById(R.id.main_button_pay_address_from_clipboard);
//    }
//
//    public void testPreconditions() {
//        assertNotNull("activity is null", activity);
//        assertNotNull("burgerButton is null", burgerButton);
//        assertNotNull("scanQRCode is null", scanQRCode);
//        assertNotNull("copyAddressFromClipboard is null", copyAddressFromClipboard);
//    }
//
//    @MediumTest
//    public void testCurrencyList() {
//        onView(withId(R.id.main_button_burger)).perform(click());
//        onView(withId(R.id.settings)).perform(click());
//        onView(withId(R.id.local_currency)).perform(click());
//        if (!CurrencyListAdapter.currencyListAdapter.isEmpty()) {
//            Random r = new Random();
//
//            //click on random list item;
//            int rand;
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//            rand = r.nextInt(CurrencyListAdapter.currencyListAdapter.getCount());
//            onData(hasToString(startsWith("")))
//                    .inAdapterView(withId(R.id.currency_list_view)).atPosition(rand)
//                    .perform(click());
//        }
//        pressBack();
//        pressBack();
//        pressBack();
//
//    }
//
//    @MediumTest
//    public void testFragments() {
//        onView(withId(R.id.main_button_burger)).perform(click());
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        onView(withId(R.id.settings)).perform(click());
//        onView(withId(R.id.start_recovery_wallet)).perform(click());
//        pressBack();
//        onView(withId(R.id.recovery_phrase)).perform(click());
//        onView(withText("show")).perform(click());
//        pressBack();
//        onView(withId(R.id.about)).perform(click());
//        onView(withId(R.id.main_button_burger)).perform(click());
//        onView(withId(R.id.main_button_burger)).perform(click());
//        onView(withId(R.id.main_button_burger)).perform(click());
//        onView(withId(R.id.main_layout)).perform(swipeLeft());
//        onView(withId(R.id.theAddressLayout)).perform(click());
//        onView(withId(R.id.copy_address)).perform(click());
//        onView(withId(R.id.main_button_locker)).perform(click());
//        onView(withId(R.id.edit_password)).perform(typeText("1234"));
//        onView(withId(R.id.button_password_ok)).perform(click());
//        assertTrue(MainActivity.unlocked);
//        onView(withId(R.id.main_layout)).perform(swipeRight());
//        try {
//            Thread.sleep(500);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//    }
//
//
//    @MediumTest
//    public void testChangeText_sameActivity() {
//        onView(withId(R.id.address_edit_text))
//                .perform(clearText(), typeText("some testing text"), closeSoftKeyboard(), clearText());
//
//    }
//
//    @Override
//    protected void tearDown() throws Exception {
//        super.tearDown();
//
//    }
//}
