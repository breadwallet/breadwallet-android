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
package brd

import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File
import java.util.Locale

internal fun Project.configureAndroidLibrary(
    basePackage: String,
    dependencies: DependencyHandler.() -> Unit
) {
    plugins.apply("com.android.library")
    plugins.apply("kotlin-android")

    extensions.getByType<LibraryExtension>().apply {
        compileSdkVersion(BrdRelease.ANDROID_COMPILE_SDK)
        buildToolsVersion(BrdRelease.ANDROID_BUILD_TOOLS)
        defaultConfig {
            minSdkVersion(BrdRelease.ANDROID_MINIMUM_SDK)
            targetSdkVersion(BrdRelease.ANDROID_TARGET_SDK)
        }
        buildFeatures.viewBinding = true

        tasks.withType<KotlinCompile> {
            kotlinOptions {
                jvmTarget = "1.8"
                freeCompilerArgs += listOf(
                    "-Xopt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-Xopt-in=kotlinx.coroutines.FlowPreview",
                    "-Xopt-in=kotlin.time.ExperimentalTime",
                    "-Xopt-in=kotlin.RequiresOptIn"
                )
            }
        }

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_1_8
            targetCompatibility = JavaVersion.VERSION_1_8
        }

        val packageName = name.replace("-", "").toLowerCase(Locale.ROOT)
        val manifestDir = File(buildDir, "androidManifest")
        val manifestFile = File(manifestDir, "AndroidManifest.xml")
        sourceSets["main"].manifest.srcFile(manifestFile)

        if (!manifestFile.exists()) {
            manifestDir.mkdirs()
            manifestFile.createManifest(basePackage, packageName)
        }
    }
    this.dependencies.apply(dependencies)
}

private fun File.createManifest(
    basePackage: String,
    packageName: String
) = writeText(
    """
        <manifest package="$basePackage.$packageName"/>
    """.trimIndent()
)