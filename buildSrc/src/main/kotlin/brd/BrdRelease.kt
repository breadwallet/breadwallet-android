/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 10/21/20.
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

object BrdRelease {
    /** Major version. Usually affected by marketing. Maximum value: 99 */
    private const val marketing = 4
    /** Minor version. Usually affected by product. Maximum value: 99 */
    private const val product = 9
    /** Hot fix version. Usually affected by engineering. Maximum value: 9 */
    private const val engineering = 0
    /** Build version. Increase for each new build. Maximum value: 999 */
    private const val build = 5

    init {
        check(marketing in 0..99)
        check(product in 0..99)
        check(engineering in 0..9)
        check(build in 0..999)
    }

    // The version code must be monotonically increasing. It is used by Android to maintain upgrade/downgrade
    // relationship between builds with a max value of 2 100 000 000.
    const val versionCode = (marketing * 1000000) + (product * 10000) + (engineering * 1000) + build
    const val versionName = "$marketing.$product.$engineering"
    const val buildVersion = build
    const val internalVersionName = "$marketing.$product.$engineering.$build"

    const val ANDROID_TARGET_SDK = 29
    const val ANDROID_COMPILE_SDK = 29
    const val ANDROID_MINIMUM_SDK = 23
    const val ANDROID_BUILD_TOOLS = "29.0.3"
}
