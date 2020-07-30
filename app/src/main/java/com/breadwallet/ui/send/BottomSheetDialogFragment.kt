package com.breadwallet.ui.send

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

fun BottomSheetDialogFragment.showIn(activity: Activity?, tag:String = "UNKNOWN_TAG") {
    activity?.let {
        val fragmentManager = (it as AppCompatActivity).supportFragmentManager
        show(fragmentManager, tag)
    }
}