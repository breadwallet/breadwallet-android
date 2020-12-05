import brd.BrdRelease

plugins {
    id("com.android.library")
    kotlin("android")
}

android {
    compileSdkVersion(BrdRelease.ANDROID_COMPILE_SDK)
    buildToolsVersion(BrdRelease.ANDROID_BUILD_TOOLS)
    defaultConfig {
        minSdkVersion(BrdRelease.ANDROID_MINIMUM_SDK)
    }
}

dependencies {
    implementation(brd.Libs.Material.Core)
}