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

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL

private const val DOWNLOAD_URL = "https://%s/assets/bundles/%s/download"
private const val RAW_PATH = "src/%s/res/raw"
private val bundles = listOf("brd-web-3", "brd-tokens")

open class DownloadBundles : DefaultTask() {

    @TaskAction
    fun run() {
        download("api.breadwallet.com", "main")
        download("stage2.breadwallet.com", "debug", "-staging")
        downloadTokens()
    }

    private fun downloadTokens() {
        val rawFolder = File(project.projectDir, RAW_PATH.format("main")).apply {
            if (!exists()) check(mkdirs()) {
                "Failed to create resource directory: $absolutePath"
            }
        }
        val currenciesMainnetFile = File(rawFolder, "tokens.json")
        val currenciesTestnetFile = File(rawFolder, "tokens_testnet.json")
        URL("https://api.breadwallet.com/currencies").saveTo(currenciesMainnetFile)
        currenciesTestnetFile.bufferedWriter().use { writer ->
            currenciesMainnetFile.bufferedReader().use { reader ->
                writer.append(
                    reader.readLine()
                        .replace("ethereum-mainnet:", "ethereum-ropsten:")
                        .replace("-mainnet:__native__", "-testnet:__native__")
                )
            }
        }
    }

    private fun download(host: String, sourceFolder: String, bundleSuffix: String = "") {
        val resFolder = File(project.projectDir, RAW_PATH.format(sourceFolder)).apply {
            if (!exists()) check(mkdirs()) {
                "Failed to create resource directory: $absolutePath"
            }
        }

        bundles.map { bundle ->
            val bundleName = "$bundle$bundleSuffix"
            val fileName = bundleName.replace("-", "_")
            val downloadUrl = DOWNLOAD_URL.format(host, bundleName)
            URL(downloadUrl).saveTo(File(resFolder, "$fileName.tar"))
        }
    }
}

/** Copy contents at a [URL] into a local [File]. */
fun URL.saveTo(file: File): Unit = openStream().use { input ->
    file.outputStream().use { input.copyTo(it) }
}
