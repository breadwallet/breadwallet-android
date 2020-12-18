/**
 * BreadWallet
 *
 * Created by Mihail Gutan on <mihail@breadwallet.com> 10/25/17.
 * Copyright (c) 2017 breadwallet LLC
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
package com.breadwallet.tools.threads.executor

import com.breadwallet.logger.logError
import com.breadwallet.tools.manager.BRReportsManager
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.asExecutor
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.RejectedExecutionHandler
import java.util.concurrent.ThreadPoolExecutor

/*
 * Singleton class for default executor supplier
 */
object BRExecutor : RejectedExecutionHandler {
    private val backgroundExecutor = Default.asExecutor()
    private val mainThreadExecutor = Main.asExecutor()
    /**
     * Returns the thread pool executor for light weight background task such as interacting with the Core or BRKeyStore.
     */
    fun forLightWeightBackgroundTasks(): Executor {
        return backgroundExecutor
    }

    /**
     * Returns the thread pool executor for main thread task.
     */
    fun forMainThreadTasks(): Executor {
        return mainThreadExecutor
    }

    override fun rejectedExecution(r: Runnable, executor: ThreadPoolExecutor) {
        logError("rejectedExecution: ")
        BRReportsManager.reportBug(RejectedExecutionException("rejectedExecution: core pool size: ${executor.corePoolSize}"))
    }

    @JvmStatic
    fun getInstance() = this
}