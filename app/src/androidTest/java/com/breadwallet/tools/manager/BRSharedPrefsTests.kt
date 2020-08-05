/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 7/24/19.
 * Copyright (c) 2019 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.tools.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.test.platform.app.InstrumentationRegistry
import androidx.core.content.edit
import androidx.test.runner.AndroidJUnit4
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
        context = InstrumentationRegistry.getInstrumentation().context
        // TODO: For now we assert on real SharedPrefs content,
        //  in the future this should be a Unit test that
        //  mocks SharedPreferences.
        sharedPrefs = context.getSharedPreferences(BRSharedPrefs.PREFS_NAME, Context.MODE_PRIVATE)
        BRSharedPrefs.initialize(context)
    }

    @Test
    fun testReceiveAddressGet() {
        val storedAddress = "17EW5WDJ3NScoLP9YvyAXV3onKtkw39aWb"
        val iso = "BTC"

        sharedPrefs.edit {
            putString(
                BRSharedPrefs.RECEIVE_ADDRESS + iso.toUpperCase(),
                storedAddress
            )
        }

        val retrievedAddress = BRSharedPrefs.getReceiveAddress(iso = iso)

        assertEquals(storedAddress, retrievedAddress)
    }

    @Test
    fun testReceiveAddressSet() {
        val insertedAddress = "17EW5WDJ3NScoLP9YvyAXV3onKtkw39aWb"
        val iso = "BTC"
        BRSharedPrefs.putReceiveAddress(tmpAddr = insertedAddress, iso = iso)

        val storedAddress =
            sharedPrefs.getString(BRSharedPrefs.RECEIVE_ADDRESS + iso.toUpperCase(), "")

        assertEquals(insertedAddress, storedAddress)
    }
}
