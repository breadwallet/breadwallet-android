/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/10/20.
 * Copyright (c) 2020 breadwallet LLC
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
package com.breadwallet.ui.settings.logview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.FrameLayout.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.get
import androidx.core.view.setMargins
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_DRAGGING
import androidx.recyclerview.widget.RecyclerView.SCROLL_STATE_IDLE
import com.bluelinelabs.conductor.changehandler.FadeChangeHandler
import com.breadwallet.BuildConfig
import com.breadwallet.ext.throttleLatest
import com.breadwallet.ui.BaseController
import com.breadwallet.ui.flowbind.clicks
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import android.widget.LinearLayout.LayoutParams.MATCH_PARENT as LL_MATCH_PARENT
import android.widget.LinearLayout.LayoutParams.WRAP_CONTENT as LL_WRAP_CONTENT

private const val STREAM_LOGS = "logcat -v tag ${BuildConfig.APPLICATION_ID}"
private const val ALT_LINE_ALPHA = 25
private const val ERROR_LINE_ALPHA = 50
private const val BACKGROUND_ALPHA = 200
private const val LOG_FOLLOW_DEBOUNCE = 250L
private const val LOG_LINE_MARGIN = 8
private const val ERROR_TAG = "E"

class LogcatController(args: Bundle? = null) : BaseController(args) {

    init {
        overridePopHandler(FadeChangeHandler(false))
        overridePushHandler(FadeChangeHandler(false))
    }

    private val selectedLine = MutableStateFlow(-1)
    private val lockToBottom = MutableStateFlow(true)
    private var renderJob: Job? = null
    private var logLines = mutableListOf<String>()
    private val adapter = object : RecyclerView.Adapter<LogItemHolder>() {
        override fun getItemCount(): Int = logLines.size
        override fun onBindViewHolder(holder: LogItemHolder, position: Int) {
            holder.setText(logLines[position], isAlt = position % 2 == 0)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LogItemHolder {
            return LogItemHolder(parent.context)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedViewState: Bundle?): View {
        return LinearLayout(container.context).apply {
            isClickable = true
            orientation = LinearLayout.VERTICAL
            val recycler = RecyclerView(context).apply {
                background = ColorDrawable(Color.BLACK).apply {
                    alpha = BACKGROUND_ALPHA
                }
                adapter = this@LogcatController.adapter
                layoutManager = object : LinearLayoutManager(context) {
                    override fun onScrollStateChanged(state: Int) {
                        if (state == SCROLL_STATE_DRAGGING) {
                            selectedLine.value = -1
                            lockToBottom.value = false
                        }
                    }
                }
            }
            val actions = LinearLayout(context).apply {
                background = ColorDrawable(Color.BLACK)
                orientation = LinearLayout.HORIZONTAL
                weightSum = 4f

                val followButton = Button(context).apply {
                    lockToBottom
                        .onEach { text = "Follow ${if (it) "(on)" else "(off)"}" }
                        .flowOn(Main)
                        .launchIn(viewAttachScope)
                    clicks()
                        .map { lockToBottom.value = !lockToBottom.value }
                        .launchIn(viewAttachScope)
                }
                val previousErrorButton = Button(context).apply {
                    text = "↑ Error"
                    clicks()
                        .onEach { selectPreviousError() }
                        .flowOn(Main)
                        .launchIn(viewAttachScope)
                }
                val nextErrorButton = Button(context).apply {
                    text = "↓ Error"
                    clicks()
                        .onEach { selectNextError() }
                        .flowOn(Main)
                        .launchIn(viewAttachScope)
                }
                val buttonLp = LinearLayout.LayoutParams(LL_WRAP_CONTENT, LL_WRAP_CONTENT)
                    .apply { weight = 1f }
                addView(followButton, buttonLp)
                addView(previousErrorButton, buttonLp)
                addView(
                    nextErrorButton,
                    LinearLayout.LayoutParams(LL_WRAP_CONTENT, LL_WRAP_CONTENT)
                        .apply { weight = 1f }
                )
            }
            addView(recycler, LinearLayout.LayoutParams(LL_MATCH_PARENT, LL_MATCH_PARENT)
                .apply { weight = .8f })
            addView(actions, LinearLayout.LayoutParams(LL_MATCH_PARENT, LL_WRAP_CONTENT)
                .apply { weight = .2f })
        }
    }

    override fun onAttach(view: View) {
        super.onAttach(view)
        renderJob = renderLogs()
        selectedLine
            .drop(1)
            .onEach { line ->
                if (lockToBottom.value) {
                    lockToBottom.value = false
                }
                val recycler = ((view as ViewGroup)[0] as RecyclerView)
                if (line >= 0 && recycler.scrollState == SCROLL_STATE_IDLE) {
                    recycler.scrollToPosition(line)
                }
            }
            .flowOn(Main)
            .launchIn(viewAttachScope)
    }

    private fun renderLogs(logLevel: String = "V"): Job {
        return "$STREAM_LOGS:$logLevel"
            .execForLines()
            .onEach { line ->
                logLines.add(line)
                adapter.notifyItemInserted(logLines.lastIndex)
            }
            .throttleLatest(LOG_FOLLOW_DEBOUNCE)
            .filter { selectedLine.value == -1 && lockToBottom.value }
            .onEach {
                val recycler = ((view as ViewGroup)[0] as RecyclerView)
                if (recycler.scrollState == SCROLL_STATE_IDLE) {
                    recycler.scrollToPosition(logLines.lastIndex)
                }
            }
            .flowOn(Main)
            .launchIn(viewAttachScope)
    }

    @Suppress("BlockingMethodInNonBlockingContext")
    private fun String.execForLines() = flow {
        Runtime.getRuntime()
            .exec(this@execForLines)
            .inputStream
            .use { stream ->
                stream.bufferedReader().useLines { lines ->
                    lines.forEach { emit(it) }
                }
            }
    }.flowOn(Default)

    private fun selectPreviousError() {
        val currentSelected = selectedLine.value
        val selectedIndex = if (currentSelected == -1) {
            logLines.size
        } else {
            currentSelected
        }
        selectedLine.value = logLines
            .take(selectedIndex)
            .dropLastWhile { !it.startsWith(ERROR_TAG) }
            .lastIndex
    }

    private fun selectNextError() {
        val currentSelected = selectedLine.value
        selectedLine.value = if (currentSelected == -1) {
            logLines.indexOfFirst { it.startsWith(ERROR_TAG) }
        } else {
            val nextLines = logLines.drop(currentSelected + 1)
            val nextIndex = nextLines.indexOfFirst { it.startsWith(ERROR_TAG) }
            if (nextIndex == -1) {
                logLines.indexOfFirst { it.startsWith(ERROR_TAG) }
            } else {
                currentSelected + nextIndex + 1
            }
        }
    }

    class LogItemHolder(context: Context) : RecyclerView.ViewHolder(
        FrameLayout(context).apply {
            layoutParams = FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
            addView(
                TextView(context),
                FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    setMargins(LOG_LINE_MARGIN)
                }
            )
        }
    ) {
        private val altLine = ColorDrawable(Color.WHITE).apply {
            alpha = ALT_LINE_ALPHA
        }
        private val errorLine = ColorDrawable(Color.RED).apply {
            alpha = ERROR_LINE_ALPHA
        }

        fun setText(text: String, isAlt: Boolean) {
            val textView = ((itemView as ViewGroup)[0] as TextView)
            textView.text = text
            itemView.background = when {
                text.startsWith(ERROR_TAG) -> errorLine
                isAlt -> altLine
                else -> null
            }
        }
    }
}
