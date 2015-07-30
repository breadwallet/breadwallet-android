package com.breadwallet.presenter.activities;

import android.test.ActivityInstrumentationTestCase2;
import android.test.suitebuilder.annotation.SmallTest;

/**
 * Created by Mihail on 7/30/15.
 */
public class ScanResultActivityTest extends ActivityInstrumentationTestCase2 {

    public ScanResultActivityTest(Class activityClass) {
        super(activityClass);
    }

    public void testOnCreate() throws Exception {

    }

    @SmallTest
    public void testIntent(){
        assertTrue(getActivity().getIntent().getExtras().getString("result").length() > 0);
    }

    public void testOnCreateOptionsMenu() throws Exception {

    }

    public void testOnOptionsItemSelected() throws Exception {

    }

    public void testOnBackPressed() throws Exception {

    }
}