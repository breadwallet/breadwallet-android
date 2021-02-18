buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        jcenter()
        maven {
            setUrl("https://dl.bintray.com/drewcarlson/redacted-plugin")
            content {
                includeGroup("dev.zacsweers.redacted")
            }
        }
    }

    val kotlin_version: String by project
    val firebase_distribution_version: String by project
    val firebase_crashlytics_gradle_version: String by project

    dependencies {
        classpath("dev.zacsweers.redacted:redacted-compiler-gradle-plugin:${brd.REDACTED_VERSION}")
        classpath("com.android.tools.build:gradle:4.0.2")
        classpath("com.google.gms:google-services:4.3.4") // google-services plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath("com.google.firebase:firebase-appdistribution-gradle:$firebase_distribution_version")
        classpath("com.google.firebase:firebase-crashlytics-gradle:$firebase_crashlytics_gradle_version")
    }
}

allprojects {
    repositories {
        exclusiveContent {
            forRepository {
                maven {
                    setUrl("https://dl.bintray.com/brd/walletkit-java")
                }
            }
            filter {
                includeGroup("com.breadwallet.core")
            }
        }
        mavenCentral()
        google()
        jcenter()
        maven {
            setUrl("https://dl.bintray.com/drewcarlson/redacted-plugin")
            content {
                includeGroup("dev.zacsweers.redacted")
            }
        }
    }
}
