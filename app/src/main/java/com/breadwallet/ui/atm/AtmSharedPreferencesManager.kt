package com.breadwallet.ui.atm

import android.content.Context
import android.content.SharedPreferences

object AtmSharedPreferencesManager {
    private const val APP_SETTINGS = "APP_ATM_PREFERENCES"

    // properties
    private const val WITHDRAWAL_REQUESTS = "WITHDRAWAL_REQUESTS"
    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(
            APP_SETTINGS,
            Context.MODE_PRIVATE
        )
    }

    private fun getWithdrawalRequests(context : Context) : MutableSet<String>? {
       return getSharedPreferences(context).getStringSet(WITHDRAWAL_REQUESTS, HashSet<String>())
    }

    fun setWithdrawalRequest(context: Context, value: String) {
        val list = getWithdrawalRequests(context)
        list?.add(value)
        updateStringSet(context, list)
    }

    fun deleteWithdrawalRequest(
        context: Context,
        value: String
    ) {
        val list = getWithdrawalRequests(context)
        list?.remove(value)
        updateStringSet(context, list)
    }

    private fun updateStringSet(context:Context, list: MutableSet<String>?){
        val editor =
            getSharedPreferences(context).edit()
        editor.putStringSet(WITHDRAWAL_REQUESTS, list)
        editor.apply()
    }
}