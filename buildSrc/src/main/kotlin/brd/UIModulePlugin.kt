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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.project

class UIModulePlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val uiModule = UIModuleExtension(project)
        project.plugins.apply("dev.zacsweers.redacted")
        project.extensions.add(UIModuleExtension::class.java, "UIModule", uiModule)
        project.configureAndroidLibrary(uiModule.basePackage) {
            val uiCommonProject = project(":ui:ui-common")
            val uiNavigationProject = project(":ui:ui-navigation")
            if (project.path != uiCommonProject.dependencyProject.path &&
                project.path != uiNavigationProject.dependencyProject.path) {
                add("implementation", uiCommonProject)
            }
            add("implementation", project(":theme"))
            add("implementation", Libs.Material.Core)
            add("implementation", Libs.Androidx.CoreKtx)
            add("implementation", Libs.Mobius.Core)
            add("implementation", Libs.Mobius.Coroutines)
            add("implementation", Libs.Mobius.Android)
            add("implementation", Libs.Conductor.Core)
            add("implementation", Libs.Conductor.ViewPager)
            add("implementation", Libs.Kodein.CoreErasedJvm)
            add("implementation", Libs.Kodein.FrameworkAndroidX)
            add("compileOnly", Libs.Redacted.Annotation)
            add("testImplementation", Libs.JUnit.Core)
            add("testImplementation", Libs.Mobius.Test)
            if (uiModule.includeAppCore) {
                add("implementation", project(":app-core"))
            }
        }
    }
}

class UIModuleExtension(private val project: Project) {
    var basePackage: String = "com.breadwallet.ui"
    var includeAppCore: Boolean = true
}

val Project.UIModule get() = extensions.getByType(UIModuleExtension::class.java)
fun Project.UIModule(callback: UIModuleExtension.() -> Unit) = callback(UIModule)
