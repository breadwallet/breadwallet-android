/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/30/20.
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
package brd.appetize

import brd.getExtra
import com.android.build.gradle.AppExtension
import com.android.build.gradle.api.ApplicationVariant
import com.android.build.gradle.internal.plugins.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.register
import java.util.Locale

class AppetizePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        check(project.pluginManager.hasPlugin("com.android.application")) {
            "AppetizePlugin must be applied after 'com.android.application'."
        }
        val appetizeToken = System.getenv("APPETIZE_TOKEN")
            ?: project.getExtra("appetizeToken", "")
        project.afterEvaluate {
            val app = project.plugins
                .getPlugin(AppPlugin::class)
                .extension as AppExtension
            val uploadTasks = app.applicationVariants.map { variant ->
                project.tasks.register(
                    variant.uploadTaskName,
                    AppetizeUpload::class,
                    variant,
                    appetizeToken
                )
            }
            val removeTasks = app.applicationVariants.map { variant ->
                project.tasks.register(
                    variant.removeTaskName,
                    AppetizeRemove::class,
                    variant
                )
            }
            with(project.tasks) {
                register("appetizeUpload").configure {
                    group = "Appetize Deployment"
                    description = "Upload all variants to appetize."
                    setDependsOn(uploadTasks)
                }
                register("appetizeRemove").configure {
                    group = "Appetize Deployment"
                    description = "Remove all associated variants from appetize."
                    setDependsOn(removeTasks)
                }
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    private val ApplicationVariant.uploadTaskName: String
        get() = buildString {
            append("appetizeUpload")
            append(flavorName.capitalize(Locale.ROOT))
            append(buildType.name.capitalize(Locale.ROOT))
        }

    @OptIn(ExperimentalStdlibApi::class)
    private val ApplicationVariant.removeTaskName: String
        get() = buildString {
            append("appetizeRemove")
            append(flavorName.capitalize(Locale.ROOT))
            append(buildType.name.capitalize(Locale.ROOT))
        }
}