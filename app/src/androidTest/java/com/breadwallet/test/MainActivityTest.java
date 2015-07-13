package com.breadwallet.test;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.activities.MainActivity;

/**
 * Created by Mihail on 7/7/15.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    MainActivity activity;

    public MainActivityTest() {
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        activity = getActivity();
    }

    @SmallTest
    public void testTextViewNotNull() {
        TextView textView = (TextView) activity.findViewById(R.id.textView);
        assertNotNull(textView);
    }


}
