/**
 * BreadWallet
 *
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
 * Copyright (c) 2016 breadwallet LLC
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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import java.lang.Exception
import kotlin.properties.Delegates

object BRClipboardManager {
    private var context by Delegates.notNull<Context>()
    private val clipboard by lazy {
        context.getSystemService(ClipboardManager::class.java)
    }

    fun provideContext(context: Context) {
        this.context = context
    }

    fun putClipboard(text: String?) {
        try {
            val clip = ClipData.newPlainText("message", text)
            clipboard.setPrimaryClip(clip)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getClipboard(): String {
        // Gets a content resolver instance
        val cr = context.contentResolver

        // Gets the clipboard data from the clipboard
        val clip = clipboard.primaryClip
        if (clip != null) {

            // Gets the first item from the clipboard data
            val item = clip.getItemAt(0)
            return coerceToText(item).toString()
        }
        return ""
    }

    private fun coerceToText(item: ClipData.Item): CharSequence {
        // If this Item has an explicit textual value, simply return that.
        val text = item.text
        return text ?: "no text"
    }
}