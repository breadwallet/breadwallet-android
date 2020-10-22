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

private const val KOTLIN_VERSION = "1.4.10"
private const val COROUTINES_VERSION = "1.3.9"
private const val WALLETKIT_VERSION = "6.2.2"
private const val FIREBASE_MESSAGING_VERSION = "20.2.4"
private const val FIREBASE_ANALYTICS_VERSION = "17.5.0"
private const val FIREBASE_CONFIG_VERSION = "19.2.0"
private const val CRASHLYTICS_VERSION = "17.2.1"
private const val GUAVA_VERSION = "25.1-android"
private const val LIFECYCLE_EXT_VERSION = "2.2.0"
private const val WORK_MANAGER_VERSION = "2.4.0"
private const val SECURITY_VERSION = "1.0.0-rc03"
private const val LEGACY_V13 = "1.0.0"
private const val CORE_VERSION = "1.3.1"
private const val APPCOMPAT_VERSION = "1.2.0"
private const val CARDVIEW_VERSION = "1.0.0"
private const val CAMERAX_VERSION = "1.0.0-beta07"
private const val CAMERAX_VIEW_VERSION = "1.0.0-alpha14"
private const val RECYCLER_VERSION = "1.1.0"
private const val CONSTRAINT_LAYOUT_VERSION = "1.1.3"
private const val GRID_LAYOUT_VERSION = "1.0.0"
private const val FASTADAPTER_VERSION = "5.2.2"
private const val CONDUCTOR_VERSION = "3.0.0-rc2"
private const val KODEIN_VERSION = "6.5.1"
private const val MOBIUS_VERSION = "1.3.4"
private const val MOBIUS_COROUTINES_VERSION = "0.1.0"
private const val ESPRESSO_VERSION = "3.3.0-rc01"
private const val ANDROIDX_TEST_VERSION = "1.3.0-rc01"
private const val JUNIT_KTX_VERSION = "1.1.2-rc01"
private const val OKHTTP_VERSION = "4.5.0"
private const val PICASSO_VERSION = "2.71828"
private const val JETTY_VERSION = "9.2.19.v20160908" // DO NOT UPGRADE
private const val LEAKCANARY_VERSION = "2.2"
private const val ANR_WATCHDOG_VERSION = "1.4.0"
private const val ZXING_VERSION = "3.3.3"
private const val COMMONS_IO_VERSION = "2.6"
private const val JBSDIFF_VERSION = "1.0"
private const val SLF4J_VERSION = "1.7.26"
private const val JUNIT_VERSION = "4.12"
private const val MOCKITO_VERSION = "2.25.0"
private const val KASPRESSO_VERSION = "1.1.0"
private const val KAKAO_VERSION = "2.3.2"
private const val MATERIAL_VERSION = "1.2.0"
private const val REDACTED_VERSION = "0.0.3"
private const val DETEKT_VERSION = "1.0.1"
private const val COMMONS_COMPRESS_VERSION = "1.20"

object Libs {
    open class ArtifactGroup(
        val packageName: String = "",
        val versionNumber: String = ""
    )

    private fun ArtifactGroup.module(
        artifactName: String,
        versionNumber: String = this.versionNumber,
        packageName: String = this.packageName
    ): String = "$packageName:$artifactName:$versionNumber"

    object WalletKit : ArtifactGroup("com.breadwallet.core", WALLETKIT_VERSION) {
        val CoreAndroid = module("corecrypto-android")
    }

    object Androidx : ArtifactGroup() {

        val LifecycleExtensions = module(
            "lifecycle-extensions",
            LIFECYCLE_EXT_VERSION,
            "androidx.lifecycle"
        )

        val WorkManagerKtx = module(
            "work-runtime-ktx",
            WORK_MANAGER_VERSION,
            "androidx.work"
        )

        val WorkManagerTesting = module(
            "work-testing",
            WORK_MANAGER_VERSION,
            "androidx.work"
        )

        val CoreKtx = module(
            "core-ktx",
            CORE_VERSION,
            "androidx.core"
        )

        val AppCompat = module(
            "appcompat",
            APPCOMPAT_VERSION,
            "androidx.appcompat"
        )

        val CardView = module(
            "cardview",
            CARDVIEW_VERSION,
            "androidx.cardview"
        )

        val ConstraintLayout = module(
            "constraintlayout",
            CONSTRAINT_LAYOUT_VERSION,
            "androidx.constraintlayout"
        )

        val GridLayout = module(
            "gridlayout",
            GRID_LAYOUT_VERSION,
            "androidx.gridlayout"
        )

        val RecyclerView = module(
            "recyclerview",
            RECYCLER_VERSION,
            "androidx.recyclerview"
        )

        val Security = module(
            "security-crypto",
            SECURITY_VERSION,
            "androidx.security"
        )

        val LegacyV13 = module(
            "legacy-support-v13",
            LEGACY_V13,
            "androidx.legacy"
        )
    }

    object AndroidxTest : ArtifactGroup(
        "androidx.test",
        ANDROIDX_TEST_VERSION
    ) {
        val Runner = module("runner")
        val Rules = module("rules")
        val EspressoCore = module(
            "espresso-core",
            ESPRESSO_VERSION,
            "androidx.test.espresso"
        )
        val JunitKtx = module(
            "junit-ktx",
            JUNIT_KTX_VERSION,
            "androidx.test.ext"
        )
    }

    object AndroidxCamera : ArtifactGroup("androidx.camera", CAMERAX_VERSION) {
        val Core = module("camera-core")
        val Camera2 = module("camera-camera2")
        val Lifecycle = module("camera-lifecycle")
        val View = module("camera-view", CAMERAX_VIEW_VERSION)
    }

    object Firebase : ArtifactGroup("com.google.firebase") {
        val Messaging = module("firebase-messaging", FIREBASE_MESSAGING_VERSION)
        val Analytics = module("firebase-analytics", FIREBASE_ANALYTICS_VERSION)
        val ConfigKtx = module("firebase-config-ktx", FIREBASE_CONFIG_VERSION)
        val Crashlytics = module("firebase-crashlytics", CRASHLYTICS_VERSION)
    }

    object Material : ArtifactGroup("com.google.android.material", MATERIAL_VERSION) {
        val Core = module("material")
    }

    object Guava : ArtifactGroup("com.google.guava", GUAVA_VERSION) {
        val Core = module("guava")
    }

    object Zxing : ArtifactGroup("com.google.zxing", ZXING_VERSION) {
        val Core = module("core")
    }

    object FastAdapter : ArtifactGroup("com.mikepenz", FASTADAPTER_VERSION) {
        val Core = module("fastadapter")
        val DiffExtensions = module("fastadapter-extensions-diff")
        val DragExtensions = module("fastadapter-extensions-drag")
        val UtilExtensions = module("fastadapter-extensions-utils")
    }

    object Conductor : ArtifactGroup("com.bluelinelabs", CONDUCTOR_VERSION) {
        val Core = module("conductor")
        val Support = module("conductor-support")
    }

    object Kodein : ArtifactGroup("org.kodein.di", KODEIN_VERSION) {
        val CoreErasedJvm = module("kodein-di-erased-jvm")
        val FrameworkAndroidX = module("kodein-di-framework-android-x")
    }

    object Mobius : ArtifactGroup("com.spotify.mobius", MOBIUS_VERSION) {
        val Core = module("mobius-core")
        val Android = module("mobius-android")
        val Test = module("mobius-test")
        val Coroutines = module(
            "mobius-coroutines",
            MOBIUS_COROUTINES_VERSION,
            "drewcarlson.mobius"
        )
    }

    object OkHttp : ArtifactGroup("com.squareup.okhttp3", OKHTTP_VERSION) {
        val Core = module("okhttp")
        val LoggingInterceptor = module("logging-interceptor")
        val MockWebServer = module("mockwebserver")
    }

    object Picasso : ArtifactGroup("com.squareup.picasso", PICASSO_VERSION) {
        val Core = module("picasso")
    }

    object Coroutines : ArtifactGroup("org.jetbrains.kotlinx", COROUTINES_VERSION) {
        val Core = module("kotlinx-coroutines-core")
        val Android = module("kotlinx-coroutines-android")
        val Test = module("kotlinx-coroutines-test")
    }

    object Kotlin : ArtifactGroup("org.jetbrains.kotlin", KOTLIN_VERSION) {
        val StdLibJdk8 = module("kotlin-stdlib-jdk8")
        val Test = module("kotlin-test")
        val TestJunit = module("kotlin-test-junit")
    }

    object Jetty : ArtifactGroup("org.eclipse.jetty", JETTY_VERSION) {
        val Continuations = module("jetty-continuation")
        val Webapp = module("jetty-webapp")
        val WebSocket = module(
            "websocket-server",
            JETTY_VERSION,
            "$packageName.websocket"
        )
    }

    object LeakCanary : ArtifactGroup("com.squareup.leakcanary", LEAKCANARY_VERSION) {
        val Core = module("leakcanary-android")
    }

    object AnrWatchdog : ArtifactGroup("com.github.anrwatchdog", ANR_WATCHDOG_VERSION) {
        val Core = module("anrwatchdog")
    }

    object ApacheCommons : ArtifactGroup() {
        val IO = module("commons-io", COMMONS_IO_VERSION, "commons-io")
        val Compress = module("commons-compress", COMMONS_COMPRESS_VERSION, "org.apache.commons")
    }

    object Jbsdiff : ArtifactGroup("io.sigpipe", JBSDIFF_VERSION) {
        val Core = module("jbsdiff")
    }

    object Slf4j : ArtifactGroup("org.slf4j", SLF4J_VERSION) {
        val Api = module("slf4j-api")
    }

    object JUnit : ArtifactGroup("junit", JUNIT_VERSION) {
        val Core = module("junit")
    }

    object Mockito : ArtifactGroup("org.mockito", MOCKITO_VERSION) {
        val Android = module("mockito-android")
    }

    object Kaspresso : ArtifactGroup("com.kaspersky.android-components", KASPRESSO_VERSION) {
        val Core = module("kaspresso")
    }

    object Kakao : ArtifactGroup("com.agoda.kakao", KAKAO_VERSION) {
        val Core = module("kakao")
    }

    object Redacted : ArtifactGroup("io.sweers.redacted", REDACTED_VERSION) {
        val Annotation = module("redacted-compiler-plugin-annotation")
    }

    object Detekt : ArtifactGroup("io.gitlab.arturbosch.detekt", DETEKT_VERSION) {
        val Formatting = module("detekt-formatting")
    }
}