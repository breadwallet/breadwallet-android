fun String.execute() = Runtime.getRuntime().exec(this)
val Process.text get() = inputStream.reader().readText()

fun getChangelog(): String {
    val cmdGetCurrentTag = "git describe --tags --abbrev=-0"
    var currentTag = System.getenv("CI_COMMIT_TAG")
    var previousTag = cmdGetCurrentTag.execute().text.trim()

    if (currentTag == null || currentTag == "") {
        currentTag = "HEAD"
    } else if (currentTag == previousTag) {
        val cmdGetPreviousTagRevision = "git rev-list --tags --skip=1 --max-count=1"
        val previousTagRevision = cmdGetPreviousTagRevision.execute().text.trim()
        val cmdGetPreviousTag = "git describe --abbrev=0 --tags $previousTagRevision"
        previousTag = cmdGetPreviousTag.execute().text.trim()
    }
    return "git log $previousTag..$currentTag --no-merges --pretty=format:%s".execute().text
}