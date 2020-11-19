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

import brd.eval
import brd.getExtra
import com.android.build.gradle.api.ApplicationVariant
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.extra
import javax.inject.Inject

open class AppetizeRemove @Inject constructor(
    private val buildVariant: ApplicationVariant
) : DefaultTask() {

    private val http = OkHttpClient()

    init {
        group = "Appetize Deployment"
        description = "Remove '${buildVariant.name}' from appetize."
    }

    @TaskAction
    fun delete() {
        val callbackUrl = System.getenv("APPETIZE_CALLBACK")
            ?: project.getExtra("appetizeCallback", "")
        check(callbackUrl.isNotBlank()) {
            "Appetize callback url must be set with APPETIZE_CALLBACK or appetizeCallback."
        }

        val url = callbackUrl.toHttpUrl()
            .newBuilder()
            .addQueryParameter("flavor", buildVariant.flavorName)
            .addQueryParameter("buildType", buildVariant.buildType.name)
            .apply {
                when {
                    project.extra.has("merge") -> {
                        addQueryParameter("type", "merge")
                        addQueryParameter("name", project.extra.get("merge") as String)
                    }
                    project.extra.has("tag") -> {
                        addQueryParameter("type", "tag")
                        addQueryParameter("name", project.extra.get("tag") as String)
                    }
                    else -> {
                        val branch = "git branch --show-current".eval()
                        val user = "git config user.name".eval()
                        addQueryParameter("type", "dev")
                        addQueryParameter("name", if (user.isNotBlank()) {
                            "dev-$user"
                        } else {
                            "dev-$branch"
                        })
                    }
                }
            }
            .build()
        val request = Request.Builder()
            .url(url)
            .delete()
            .build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                logger.error(response.body?.string())
                "Appetize callback failed (${response.code})"
            }
        }
    }
}