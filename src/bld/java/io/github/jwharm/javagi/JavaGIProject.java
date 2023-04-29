package io.github.jwharm.javagi;

import io.github.jwharm.javagi.generator.Platform;
import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.operations.JavadocOptions;

import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.compile;

public class JavaGIProject extends Project {

    private final JavaGIOperation javaGIOperation_;

    public JavaGIProject(JavaGIBuild bld, String name) {
        javaRelease = 20;

        repositories = List.of(MAVEN_CENTRAL);
        scope(compile)
            .include(dependency("org.jetbrains", "annotations", version(24,0,1)));

        javaGIOperation_ = new JavaGIOperation()
            .sourceDirectory(bld.girDirectory())
            .outputDirectory(bld.buildDirectory().toPath().resolve("generated").resolve(name))
            .platform(Platform.LINUX);

        compileOperation()
            .mainSourceDirectories(javaGIOperation().outputDirectory().toFile())
            .compileOptions()
                .modulePath(libCompileDirectory())
                .enablePreview();

        javadocOperation()
            .sourceDirectories(javaGIOperation().outputDirectory().toFile())
            .javadocOptions(List.of("--module-path", libCompileDirectory().getAbsolutePath()))
            .javadocOptions()
                .enablePreview()
                .docLint(JavadocOptions.DocLinkOption.NO_MISSING) // ignore lint errors
                .quiet();

        // bld ignores the configured javadoc output directory, but not this one:
        buildJavadocDirectory = bld.buildDirectory().toPath().resolve("javadoc").resolve(name).toFile();

        jarSourcesOperation()
            .sourceDirectories(javaGIOperation().outputDirectory().toFile());
    }

    public JavaGIOperation javaGIOperation() {
        return javaGIOperation_;
    }

    @BuildCommand(value="generate-sources", summary="Generates Java sources from gir files")
    public void generateSources() throws Exception {
        javaGIOperation().executeOnce();
    }

    @Override
    public void compile() throws Exception {
        generateSources();
        super.compile();
    }
}
