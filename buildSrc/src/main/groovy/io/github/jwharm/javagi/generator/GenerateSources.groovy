package io.github.jwharm.javagi.generator

import io.github.jwharm.javagi.model.Repository
import io.github.jwharm.javagi.model.Module
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*

import java.nio.file.Path

/**
 * GenerateSources is a Gradle task that will parse a GIR file (and all included GIR files)
 * and generate Java source files for the types defined in the GIR file.
 */
abstract class GenerateSources extends DefaultTask {

    @InputDirectory
    abstract DirectoryProperty getInputDirectory()

    @OutputDirectory
    abstract DirectoryProperty getOutputDirectory()

    @Input
    abstract Property<String> getGirFile()

    @Input @Optional
    abstract Property<String> getUrlPrefix()

    @Input @Optional
    abstract Property<Patch> getPatch()

    @TaskAction
    void execute() {
        Conversions.packageNames = PackageNames.map

        Module windows = parse(Platform.WINDOWS, inputDirectory.get(), girFile.get(),
                urlPrefix.getOrElse(null), patch.getOrElse(null))
        Module linux = parse(Platform.LINUX, inputDirectory.get(), girFile.get(),
                urlPrefix.getOrElse(null), patch.getOrElse(null))
        Module macos = parse(Platform.MACOS, inputDirectory.get(), girFile.get(),
                urlPrefix.getOrElse(null), patch.getOrElse(null))

        Module module = new Merge().merge(windows, linux, macos)

        for (Repository repository : module.repositories.values()) {
            if (repository.generate) {
                Path basePath = outputDirectory.get().file(repository.namespace.pathName).asFile.toPath()
                repository.generate(basePath)
            }
        }
    }

    private static Module parse(Platform platform, Directory sourceDirectory, String girFile, String urlPrefix, Patch patch) {
        Module module = new Module(platform)
        Directory girPath = sourceDirectory.dir(platform.name().toLowerCase())
        if (! girPath.asFile.exists()) {
            System.out.println("Not found: " + girPath)
            return null
        }
        GirParser parser = new GirParser(girPath.asFile.toPath(), module)

        // Parse the GI files into Repository objects
        try {
            // Parse the file
            Repository r = parser.parse(girFile)

            // Check if this one has already been parsed
            if (module.repositories.containsKey(r.namespace.name)) {
                r = module.repositories.get(r.namespace.name)
            } else {
                // Add the repository to the module
                module.repositories.put(r.namespace.name, r)
            }

            r.urlPrefix = urlPrefix

            // Flag unsupported va_list methods so they will not be generated
            module.flagVaListFunctions()

            // Apply patch
            if (patch != null) {
                patch.patch(r)
            }

        } catch (IOException ignored) {
            // Gir file not found for this platform: This will generate code with UnsupportedPlatformExceptions
        }

        // Link the type references to the GIR type definition across the GI repositories
        module.link()

        return module
    }
}

