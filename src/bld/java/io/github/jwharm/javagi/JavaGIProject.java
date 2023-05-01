package io.github.jwharm.javagi;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.operations.JavadocOptions;

import java.io.File;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.compile;

public class JavaGIProject extends Project {

    private final GenerateSourcesOperation generateSourcesOperation_;

    public JavaGIProject(JavaGIBuild bld, String name) {
        javaRelease = 20;
        repositories = List.of(MAVEN_CENTRAL);
        buildMainDirectory = new File(bld.buildMainDirectory(), name);
        buildJavadocDirectory = new File(bld.buildJavadocDirectory(), name);
        generateSourcesOperation_ = new GenerateSourcesOperation();

        scope(compile)
            .include(dependency("org.jetbrains", "annotations", version(24,0,1)));

        generateSourcesOperation()
            .sourceDirectory(bld.girDirectory())
            .outputDirectory(bld.buildDirectory().toPath().resolve("generated").resolve(name));

        compileOperation()
            .mainSourceDirectories(generateSourcesOperation().outputDirectory().toFile())
            .compileOptions()
                .modulePath(libCompileDirectory(), buildDistDirectory())
                .enablePreview();

        javadocOperation()
            .sourceDirectories(generateSourcesOperation().outputDirectory().toFile())
            .javadocOptions(List.of("--module-path", libCompileDirectory() + ":" + buildDistDirectory()))
            .javadocOptions()
                .enablePreview()
                .docLint(JavadocOptions.DocLinkOption.NO_MISSING)
                .quiet();

        jarSourcesOperation()
            .sourceDirectories(generateSourcesOperation().outputDirectory().toFile());
    }

    public GenerateSourcesOperation generateSourcesOperation() {
        return generateSourcesOperation_;
    }

    @BuildCommand(value="generate-sources", summary="Generates Java sources from gir files")
    public void generateSources() throws Exception {
        generateSourcesOperation().executeOnce();
    }

    @Override
    public void compile() throws Exception {
        generateSources();
        super.compile();
    }
}
