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

fun getChangelog(): String {
    val cmdGetCurrentTag = "git describe --tags --abbrev=-0"
    var currentTag = System.getenv("CI_COMMIT_TAG")
    var previousTag = cmdGetCurrentTag.eval()

    if (currentTag == null || currentTag == "") {
        currentTag = "HEAD"
    } else if (currentTag == previousTag) {
        val cmdGetPreviousTagRevision = "git rev-list --tags --skip=1 --max-count=1"
        val previousTagRevision = cmdGetPreviousTagRevision.eval()
        val cmdGetPreviousTag = "git describe --abbrev=0 --tags $previousTagRevision"
        previousTag = cmdGetPreviousTag.eval()
    }
    return "git log $previousTag..$currentTag --no-merges --pretty=format:%s".eval()
}