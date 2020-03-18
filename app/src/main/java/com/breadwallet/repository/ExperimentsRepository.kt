/**
 * BreadWallet
 *
 * Created by Pablo Budelli <pablo.budelli@breadwallet.com> on 8/14/19.
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
package com.breadwallet.repository

import android.content.Context
import com.breadwallet.model.Experiment
import com.breadwallet.model.Experiments
import com.platform.network.ExperimentsClientImpl

interface ExperimentsRepository {

    val experiments: Map<String, Experiment>

    /**
     * Refresh the set of experiments.
     */
    fun refreshExperiments(context: Context)

    /**
     * Check if a experiment is available or not.
     */
    fun isExperimentActive(experiment: Experiments): Boolean

}

/**
 * Implementation that stores the experiments in memory.
 */
object ExperimentsRepositoryImpl : ExperimentsRepository {

    private var experimentsCache = mapOf<String, Experiment>()
    override val experiments get() = experimentsCache

    override fun refreshExperiments(context: Context) {
        experimentsCache = ExperimentsClientImpl.getExperiments(context).map { it.name to it }.toMap()
    }

    override fun isExperimentActive(experiment: Experiments) = experiments[experiment.key]?.active ?: false
}
