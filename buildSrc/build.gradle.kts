repositories {
    mavenCentral()
    google()
    jcenter()
    gradlePluginPortal()
}

plugins {
    `kotlin-dsl`
}

val kotlinVersion = "1.4.0"

dependencies {
    // NOTE: Override gradle's embedded Kotlin version
    implementation(kotlin("stdlib-jdk8", kotlinVersion))
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.android.tools.build:gradle:4.0.2")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.gradle.kotlin:gradle-kotlin-dsl-plugins:1.4.2")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

