repositories {
    jcenter()
}

plugins {
    `kotlin-dsl`
}

dependencies {
    // NOTE: Override gradle's embedded Kotlin version
    implementation(kotlin("stdlib-jdk8", "1.4.0"))
}

kotlinDslPluginOptions {
    experimentalWarning.set(false)
}

