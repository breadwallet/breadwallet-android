/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 6/2/2019.
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
package com.breadwallet.ext

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.Transformations
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.fragment.app.FragmentActivity
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Returns a lazily acquired ViewModel using [factory]
 * as an optional factory for [T].
 */
inline fun <reified T : ViewModel> FragmentActivity.viewModel(
    noinline factory: (() -> T)? = null
): Lazy<T> = lazy {
    if (factory != null) {
        val vmFactory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T2 : ViewModel?> create(modelClass: Class<T2>): T2 =
                factory() as T2
        }
        ViewModelProvider(this, vmFactory).get(T::class.java)
    } else ViewModelProvider(this).get(T::class.java)
}

/**
 * Factory function for [MutableLiveData] that takes an
 * optional [default] parameter.
 */
fun <T> mutableLiveData(default: T? = null) =
    MutableLiveData<T>().apply { default?.let(::postValue) }

/**
 * Turn the values emitted by this [LiveData] from [I] to [O]
 * using the [transformer] function.
 */
inline fun <I, O> LiveData<I>.map(crossinline transformer: (I) -> O): LiveData<O> =
    Transformations.map(this) { transformer(it) }

/**
 * Turn the values emitted by this [LiveData] from [I] to
 * a new [LiveData] that emits [O] using [transformer].
 */
inline fun <I, O> LiveData<I>.switchMap(crossinline transformer: (I) -> LiveData<O>): LiveData<O> =
    Transformations.switchMap(this) { transformer(it) }

/**
 * Combine the latest values of two [LiveData]s using
 * [combineFun] when either source emits a value.
 */
fun <I1, I2, O> LiveData<I1>.combineLatest(
    second: LiveData<I2>,
    combineFun: (I1, I2) -> O
): LiveData<O> {
    val first = this
    return MediatorLiveData<O>().apply {
        var firstEmitted = false
        var firstValue: I1? = null

        var secondEmitted = false
        var secondValue: I2? = null
        addSource(first) { value ->
            firstEmitted = true
            firstValue = value
            if (firstEmitted && secondEmitted) {
                postValue(combineFun(firstValue!!, secondValue!!))
            }
        }
        addSource(second) { value ->
            secondEmitted = true
            secondValue = value
            if (firstEmitted && secondEmitted) {
                postValue(combineFun(firstValue!!, secondValue!!))
            }
        }
    }
}

/**
 * Returns a live data that will only emit values when
 * the next value is not the same as the previous.
 */
fun <T> LiveData<T>.distinctUntilChanged(): LiveData<T> =
    MediatorLiveData<T>().also { newLiveData ->
        var oldVal: T? = null
        newLiveData.addSource(this) { newVal ->
            if (oldVal != newVal) {
                oldVal = newVal
                newLiveData.postValue(newVal)
            }
        }
    }

/**
 * Lazily call [binding] and clear it's reference with [Lifecycle.Event.ON_DESTROY]
 */
fun <T> FragmentActivity.bindCreated(
    binding: () -> T
): ReadOnlyProperty<FragmentActivity, T> =
    object : ReadOnlyProperty<FragmentActivity, T>, LifecycleObserver {
        init {
            this@bindCreated.lifecycle.addObserver(this)
        }

        private var cache: T? = null
        override fun getValue(thisRef: FragmentActivity, property: KProperty<*>): T =
            cache ?: binding().also { cache = it }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        fun clear() {
            cache = null
        }
    }

/**
 * Lazily call [binding] and clear it's reference in [Lifecycle.Event.ON_PAUSE]
 */
fun <T> FragmentActivity.bindResumed(
    binding: () -> T
): ReadOnlyProperty<FragmentActivity, T> =
    object : ReadOnlyProperty<FragmentActivity, T>, LifecycleObserver {
        init {
            this@bindResumed.lifecycle.addObserver(this)
        }

        private var cache: T? = null
        override fun getValue(thisRef: FragmentActivity, property: KProperty<*>): T =
            cache ?: binding().also { cache = it }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        fun clear() {
            cache = null
        }
    }
