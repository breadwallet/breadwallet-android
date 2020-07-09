package com.breadwallet.ui.atm

import android.content.Context
import android.content.SharedPreferences
import android.util.Log

class AtmSharedPreferencesManager {
    companion object {
        private const val APP_SETTINGS = "APP_ATM_PREFERENCES"
        private const val WITHDRAWAL_REQUESTS = "WITHDRAWAL_REQUESTS"

        private fun getSharedPreferences(context: Context): SharedPreferences {
            return context.getSharedPreferences(APP_SETTINGS, Context.MODE_PRIVATE)
        }

        fun getWithdrawalRequests(context : Context) : MutableSet<String>? {
            val list =  getSharedPreferences(context).getStringSet(WITHDRAWAL_REQUESTS, HashSet<String>())
            Log.d("WithdrawalRequests", list.toString())
            return list
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

        fun clear(
            context: Context
        ) {
            getSharedPreferences(context).edit().clear().apply()
        }

        private fun updateStringSet(context:Context, list: MutableSet<String>?){
            val editor =
                getSharedPreferences(context).edit()
            editor.putStringSet(WITHDRAWAL_REQUESTS, list)
            editor.apply()
        }
    }
}