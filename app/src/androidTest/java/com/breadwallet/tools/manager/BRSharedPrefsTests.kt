package com.breadwallet.tools.manager

import android.content.Context
import android.content.SharedPreferences
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import androidx.core.content.edit
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class BRSharedPrefsTests {

    lateinit var context: Context
    lateinit var sharedPrefs: SharedPreferences

    @Before
    fun setup() {
        context = InstrumentationRegistry.getContext()
        // TODO: For now we assert on real SharedPrefs content,
        //  in the future this should be a Unit test that
        //  mocks SharedPreferences.
        sharedPrefs = context.getSharedPreferences(BRSharedPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        BRSharedPrefs.provideContext(context)
    }

    @Test
    fun testReceiveAddressGet() {
        val storedAddress = "17EW5WDJ3NScoLP9YvyAXV3onKtkw39aWb"
        val iso = "BTC"

        sharedPrefs.edit { putString(BRSharedPrefs.RECEIVE_ADDRESS + iso.toUpperCase(), storedAddress) }

        val retrievedAddress = BRSharedPrefs.getReceiveAddress(iso = iso)

        assertEquals(storedAddress, retrievedAddress)
    }

    @Test
    fun testReceiveAddressSet() {
        val insertedAddress = "17EW5WDJ3NScoLP9YvyAXV3onKtkw39aWb"
        val iso = "BTC"
        BRSharedPrefs.putReceiveAddress(tmpAddr = insertedAddress, iso = iso)

        val storedAddress = sharedPrefs.getString(BRSharedPrefs.RECEIVE_ADDRESS + iso.toUpperCase(), "")

        assertEquals(insertedAddress, storedAddress)
    }
}