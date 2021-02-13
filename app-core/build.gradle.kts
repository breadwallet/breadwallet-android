import brd.BrdRelease
import brd.Libs

plugins {
    id("com.android.library")
    kotlin("android")
    id("kotlin-parcelize")
    id("dev.zacsweers.redacted")
}

redacted {
    replacementString.set("***")
}

project.tasks.register<brd.DownloadBundles>("downloadBundles")

android {
    compileSdkVersion(BrdRelease.ANDROID_COMPILE_SDK)
    buildToolsVersion(BrdRelease.ANDROID_BUILD_TOOLS)
    defaultConfig {
        minSdkVersion(BrdRelease.ANDROID_MINIMUM_SDK)
        buildConfigField("int", "VERSION_CODE", "${BrdRelease.versionCode}")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(project(":theme"))
    implementation(Libs.Kotlin.StdLibJdk8)
    implementation(Libs.Coroutines.Core)
    api(Libs.WalletKit.CoreAndroid)

    implementation(Libs.Androidx.LifecycleExtensions)
    implementation(Libs.Androidx.AppCompat)
    implementation(Libs.Androidx.CardView)
    implementation(Libs.Androidx.CoreKtx)
    api(Libs.Androidx.ConstraintLayout)
    implementation(Libs.Androidx.GridLayout)
    implementation(Libs.Zxing.Core)

    implementation(Libs.ApacheCommons.IO)
    implementation(Libs.ApacheCommons.Compress)
    compileOnly(Libs.Redacted.Annotation)

    implementation(Libs.Firebase.Crashlytics)

    // Kodein DI
    implementation(Libs.Kodein.CoreErasedJvm)
    implementation(Libs.Kodein.FrameworkAndroidX)

    implementation(Libs.Jbsdiff.Core)
}
