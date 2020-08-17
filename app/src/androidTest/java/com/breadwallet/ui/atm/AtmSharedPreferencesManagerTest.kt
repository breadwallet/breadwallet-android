package com.breadwallet.ui.atm

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import cash.just.atm.AtmSharedPreferencesManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class AtmSharedPreferencesManagerTest {
    private lateinit var context: Context

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().context
    }

    @After
    fun cleanup() {
        AtmSharedPreferencesManager.clear(context)
    }

    @Test
    fun emptyTest() {
        val list = AtmSharedPreferencesManager.getWithdrawalRequests(context)
        assert(list != null && list.size == 0)
    }

    @Test
    fun setTest() {
        AtmSharedPreferencesManager.setWithdrawalRequest(context, "aaaa")

        val list = AtmSharedPreferencesManager.getWithdrawalRequests(context)
        list?.let {
            assert(it.size == 1)
            assert(it.contains("aaaa"))
        } ?:run {
            assert(false)
        }
    }

    @Test
    fun multiSetTest() {
        AtmSharedPreferencesManager.setWithdrawalRequest(context, "aaaa")
        AtmSharedPreferencesManager.setWithdrawalRequest(context, "bbbb")
        AtmSharedPreferencesManager.setWithdrawalRequest(context, "cccc")
        AtmSharedPreferencesManager.setWithdrawalRequest(context, "dddd")

        val list = AtmSharedPreferencesManager.getWithdrawalRequests(context)
        list?.let {
            assert(it.size == 4)
            assert(it.contains("aaaa"))
            assert(it.contains("bbbb"))
            assert(it.contains("cccc"))
            assert(it.contains("dddd"))
        } ?:run {
            assert(false)
        }

        AtmSharedPreferencesManager.deleteWithdrawalRequest(context, "aaaa")
        val newList = AtmSharedPreferencesManager.getWithdrawalRequests(context)
        newList?.let {
            assert(it.size == 3)
            assert(!it.contains("aaaa"))
            assert(it.contains("bbbb"))
            assert(it.contains("cccc"))
            assert(it.contains("dddd"))
        } ?:run {
            assert(false)
        }

        AtmSharedPreferencesManager.clear(context)
        val emptyList = AtmSharedPreferencesManager.getWithdrawalRequests(context)
        emptyList?.let {
            assert(it.size == 0)
        } ?:run {
            assert(false)
        }
    }
}