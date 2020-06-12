package com.breadwallet.ui.atm

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.NonNull
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.SupportMapFragment

object AtmMapHelper {
    @SuppressWarnings("ReturnCount")
    fun getActivityFromContext(@NonNull context: Context): AppCompatActivity? {
        while (context is ContextWrapper) {
            if (context is AppCompatActivity) return context
            return context.baseContext as AppCompatActivity
        }
        return null //we failed miserably
    }

    fun addMapFragment(context: Context, containerViewId:Int, tag:String) : SupportMapFragment {
        val fragmentManager = getActivityFromContext(context)!!.supportFragmentManager
        var fragment = fragmentManager.findFragmentByTag(tag)
        if (fragment == null) fragment = SupportMapFragment()

        if (fragment.isAdded) {
            fragmentManager
                .beginTransaction()
                .remove(fragment)
                .commit()
            fragmentManager.executePendingTransactions()
        }

        fragmentManager
            .beginTransaction()
            .add(containerViewId, fragment, tag)
            .commit()

        return fragment as SupportMapFragment
    }
}