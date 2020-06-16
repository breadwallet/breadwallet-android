package com.breadwallet.ui.atm.utils

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlin.reflect.KProperty

class ViewModelDelegate<T : ViewModel>(private val clazz: Class<T>) {
    operator fun getValue(owner: LifecycleOwner, property: KProperty<*>): T {
        val provider = if (owner is Fragment) {
            ViewModelProvider(owner)
        } else {
            ViewModelProvider(owner as FragmentActivity)
        }
        return provider.get(clazz)
    }
}

class ActivityViewModelDelegate<T : ViewModel>(private val clazz: Class<T>) {
    operator fun getValue(owner: LifecycleOwner, property: KProperty<*>): T {
        val provider = if (owner is Fragment) {
            ViewModelProvider(owner.requireActivity())
        } else {
            ViewModelProvider(owner as FragmentActivity)
        }
        return provider.get(clazz)
    }
}
