subprojects {
    plugins.apply(brd.UIModulePlugin::class)
    configure<dev.zacsweers.redacted.gradle.RedactedPluginExtension> {
        replacementString.set("***")
    }
}
