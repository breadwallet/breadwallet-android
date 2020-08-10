package com.breadwallet.ui.send

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentManager
import com.bluelinelabs.conductor.Router

fun Router.fragmentManager(): FragmentManager? {
    activity?.let {
        return (it as AppCompatActivity).supportFragmentManager
    }
    return null
}