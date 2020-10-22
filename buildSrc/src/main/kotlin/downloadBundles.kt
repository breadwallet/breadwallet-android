import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.net.URL

private const val DOWNLOAD_URL = "https://%s/assets/bundles/%s/download"
private const val RES_PATH = "src/%s/res/raw"
private val bundles = listOf("brd-web-3", "brd-tokens")

open class DownloadBundles : DefaultTask() {

    @TaskAction
    fun run() {
        download("api.breadwallet.com", "main")
        download("stage2.breadwallet.com", "debug", "-staging")
    }

    private fun download(host: String, sourceFolder: String, bundleSuffix: String = "") {
        val resFolder = File(project.projectDir, RES_PATH.format(sourceFolder)).apply {
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
