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
import okhttp3.Credentials.basic
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import org.json.JSONObject
import java.net.URLEncoder
import javax.inject.Inject
import kotlin.text.Charsets.UTF_8

open class AppetizeUpload @Inject constructor(
    private val buildVariant: ApplicationVariant,
    private val appetizeToken: String
) : DefaultTask() {

    private val http = OkHttpClient()

    private val apkOutput = buildVariant.outputs.single()

    init {
        group = "Appetize Deployment"
        description = "Upload '${buildVariant.name}' to appetize."
        val noBuild = !apkOutput.outputFile.exists()
        val versionChanged = buildVariant.versionCode != apkOutput.versionCode
        if (noBuild || versionChanged) {
            logger.lifecycle("Exisiting APK not found for ${buildVariant.name}")
            dependsOn(buildVariant.assembleProvider)
        } else {
            logger.lifecycle("Uploading existing APK file from ${apkOutput.outputFile}")
        }
    }

    @TaskAction
    fun uploadApk() {
        check(appetizeToken.isNotBlank()) {
            "Appetize token must be provided with the APPETIZE_TOKEN env variable or -PappetizeToken flag."
        }
        val callbackUrl = System.getenv("APPETIZE_CALLBACK")
            ?: project.getExtra("appetizeCallback", "")
        check(callbackUrl.isNotBlank()) {
            "APPETIZE_CALLBACK or -PappetizeCallback=<value> must be provided."
        }

        val apkReqBody = apkOutput.outputFile.asRequestBody()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("platform", "android")
            .addFormDataPart("file", "file.apk", apkReqBody)
            .build()
        val request = Request.Builder()
            .addHeader("Authorization", basic(appetizeToken, ""))
            .url("https://api.appetize.io/v1/apps")
            .post(body)
            .build()
        http.newCall(request).execute().use { response ->
            val bodyString = response.body?.string()
            check(response.isSuccessful) {
                logger.error("Failed to upload build to Appetize. (response ${response.code})", bodyString)
                "Failed to upload build to Appetize. (response ${response.code})"
            }
            logger.info("Appetize upload successful", bodyString)
            val publicKey = JSONObject(bodyString).getString("publicKey")
            notifyCallbackUrl(callbackUrl, publicKey)
        }
    }

    private fun notifyCallbackUrl(callbackUrl: String, publicKey: String) {
        val jsonBody = JSONObject().apply {
            put("appetizeKey", publicKey)
            put("buildFlavor", buildVariant.flavorName)
            put("buildType", buildVariant.buildType.name)
        }
        if (System.getenv("CI") == "true") {
            jsonBody.put("author", System.getenv("GITLAB_USER_NAME"))
            val branchName = System.getenv("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME")
            val tagName = System.getenv("CI_COMMIT_TAG")
            if (branchName.isNotBlank()) {
                jsonBody.put("type", "merge")
                jsonBody.put("name", branchName)
            } else if (tagName.isNotBlank()) {
                jsonBody.put("type", "tag")
                jsonBody.put("name", tagName)
            }
            check(jsonBody.has("name")) {
                "Failed find version information for CI."
            }
        } else {
            val userName = "git config user.name".eval()
            val branchName = "git branch --show-current".eval()
            val name = when {
                userName.isNotBlank() -> userName
                else -> branchName
            }
            jsonBody.put("name", "dev-$name")
            jsonBody.put("author", userName)
            jsonBody.put("type", "dev")
        }

        val requestBody = jsonBody.toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(callbackUrl)
            .post(requestBody)
            .build()
        http.newCall(request).execute().use { response ->
            check(response.isSuccessful) {
                logger.error(response.body?.string())
                "Appetize callback failed (${response.code})"
            }
            val nativeReviewUrl = System.getenv("NATIVE_REVIEW_URL")
                ?: project.getExtra("nativeReviewUrl")
            val urlEncodedName = URLEncoder.encode(jsonBody.getString("name"), UTF_8.name())
            logger.lifecycle("Build available at $nativeReviewUrl?versionName=${urlEncodedName}")
        }
    }
}