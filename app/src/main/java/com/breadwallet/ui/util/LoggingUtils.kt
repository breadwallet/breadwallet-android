/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 8/13/19.
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
package com.breadwallet.ui.util

import android.util.Log

/** Convenience method for retrieving the [Class.getSimpleName] of an object. */
@Suppress("NOTHING_TO_INLINE")
inline fun Any.tag(): String = this::class.java.simpleName

/**
 * Logs [message] with [Log.d], using the receivers [Class.getSimpleName]
 * as the tag, and prints any accompanying [data] items.
 *
 * If the first item in [data] is an exception, it will be logged using
 * the correct [Log.d] overload.
 */
fun Any.logDebug(message: String, vararg data: Any?) {
    if (data.isEmpty())
        Log.d(tag(), message)
    else {
        val hadException = when (val first = data.first()) {
            is Throwable -> {
                Log.d(tag(), message, first)
                true
            }
            else -> {
                Log.d(tag(), message)
                false
            }
        }

        data.drop(if (hadException) 1 else 0)
                .forEach { obj ->
                    Log.d(tag(), "\t${obj?.tag()}:")
                    Log.d(tag(), "\t\t$obj")
                }
    }
}

/**
 * Logs [message] with [Log.v], using the receivers [Class.getSimpleName]
 * as the tag, and prints any accompanying [data] items.
 *
 * If the first item in [data] is an exception, it will be logged using
 * the correct [Log.v] overload.
 */
fun Any.logVerbose(message: String, vararg data: Any?) {
    if (data.isEmpty())
        Log.v(tag(), message)
    else {
        val hadException = when (val first = data.first()) {
            is Throwable -> {
                Log.v(tag(), message, first)
                true
            }
            else -> {
                Log.v(tag(), message)
                false
            }
        }

        data.drop(if (hadException) 1 else 0)
                .forEach { obj ->
                    Log.v(tag(), "\t${obj?.tag()}:")
                    Log.v(tag(), "\t\t$obj")
                }
    }
}

/**
 * Logs [message] with [Log.i], using the receivers [Class.getSimpleName]
 * as the tag, and prints any accompanying [data] items.
 *
 * If the first item in [data] is an exception, it will be logged using
 * the correct [Log.i] overload.
 */
fun Any.logInfo(message: String, vararg data: Any?) {
    if (data.isEmpty())
        Log.i(tag(), message)
    else {
        val hadException = when (val first = data.first()) {
            is Throwable -> {
                Log.i(tag(), message, first)
                true
            }
            else -> {
                Log.i(tag(), message)
                false
            }
        }

        data.drop(if (hadException) 1 else 0)
                .forEach { obj ->
                    Log.i(tag(), "\t${obj?.tag()}:")
                    Log.i(tag(), "\t\t$obj")
                }
    }
}

/**
 * Logs [message] with [Log.e], using the receivers [Class.getSimpleName]
 * as the tag, and prints any accompanying [data] items.
 *
 * If the first item in [data] is an exception, it will be logged using
 * the correct [Log.e] overload.
 */
fun Any.logError(message: String, vararg data: Any?) {
    if (data.isEmpty())
        Log.e(tag(), message)
    else {
        val hadException = when (val first = data.first()) {
            is Throwable -> {
                Log.e(tag(), message, first)
                true
            }
            else -> {
                Log.e(tag(), message)
                false
            }
        }

        data.drop(if (hadException) 1 else 0)
                .forEach { obj ->
                    Log.e(tag(), "\t${obj?.tag()}:")
                    Log.e(tag(), "\t\t$obj")
                }
    }
}