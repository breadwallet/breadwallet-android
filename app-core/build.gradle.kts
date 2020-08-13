plugins {
    id("com.android.library")
    kotlin("android")
}


android {
    compileSdkVersion(29)
    buildToolsVersion("29.0.3")
    defaultConfig {
        minSdkVersion(23)
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val kotlin_version: String by project
val ax_lifecycle_ext: String by project
val ax_appcompat: String by project

dependencies {
    implementation("androidx.lifecycle:lifecycle-extensions:$ax_lifecycle_ext")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlin_version")
    implementation("androidx.appcompat:appcompat:$ax_appcompat")
}