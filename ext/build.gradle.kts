tasks {
    val sourceDir = layout.projectDirectory.dir("gobject-introspection-tests")
    val buildDir = layout.buildDirectory.dir("testgir")

    val buildGir by registering(Exec::class) {
        group = "build"
        description = "Runs the meson build for gir-files"

        commandLine("sh", "-c", """
            meson setup "${buildDir.get().asFile.absolutePath}" "${sourceDir.asFile.absolutePath}" && \
            meson compile -C "${buildDir.get().asFile.absolutePath}"
        """.trimIndent())
        workingDir = sourceDir.asFile
        inputs.dir(sourceDir)
        outputs.dir(buildDir)
    }
}
