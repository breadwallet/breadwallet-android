import brd.BrdRelease
import brd.Libs

plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("android.extensions")
}

android {
    compileSdkVersion(BrdRelease.ANDROID_COMPILE_SDK)
    buildToolsVersion(BrdRelease.ANDROID_BUILD_TOOLS)
    defaultConfig {
        minSdkVersion(BrdRelease.ANDROID_MINIMUM_SDK)
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
    implementation(Libs.Kotlin.StdLibJdk8)

    implementation(Libs.Androidx.LifecycleExtensions)
    implementation(Libs.Androidx.AppCompat)

    implementation(Libs.ApacheCommons.IO)
    implementation(Libs.ApacheCommons.Compress)

    implementation(Libs.Firebase.Crashlytics)
}