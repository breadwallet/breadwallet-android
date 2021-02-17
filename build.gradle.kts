buildscript {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        jcenter()
        maven(url = "https://dl.bintray.com/drewcarlson/redacted-plugin") {
            content {
                includeGroup("dev.zacsweers.redacted")
            }
        }
    }

    dependencies {
        classpath(brd.Libs.Redacted.Plugin)
        classpath(brd.Libs.Android.GradlePlugin)
        classpath(brd.Libs.Google.ServicesPlugin)
        classpath(brd.Libs.Kotlin.GradlePlugin)
        classpath(brd.Libs.Firebase.DistributionPlugin)
        classpath(brd.Libs.Firebase.CrashlyticsPlugin)
    }
}

allprojects {
    repositories {
        exclusiveContent {
            forRepository {
                maven(url = "https://dl.bintray.com/brd/walletkit-java")
            }
            filter {
                includeGroup("com.breadwallet.core")
            }
        }
        mavenCentral()
        google()
        jcenter()
        maven(url = "https://dl.bintray.com/drewcarlson/redacted-plugin") {
            content {
                includeGroup("dev.zacsweers.redacted")
            }
        }
    }
}
