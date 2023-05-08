package io.github.jwharm.javagi;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.operations.JavadocOptions;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishInfo;
import rife.bld.publish.PublishLicense;
import rife.tools.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Repository.MAVEN_LOCAL;
import static rife.bld.dependencies.Scope.compile;

public class JavaGIProject extends Project {

    public static final Pattern SOURCES_JAR_FILE_PATTERN = Pattern.compile("^.*-sources\\.jar$");

    private final GenerateSourcesOperation generateSourcesOperation_;

    public JavaGIProject(JavaGIBuild bld, String name) {
        this.name = name;
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
                .modulePath(getModulePath())
                .enablePreview();

        javadocOperation()
            .sourceDirectories(generateSourcesOperation().outputDirectory().toFile())
            .javadocOptions(List.of("--module-path", getModulePathString()))
            .javadocOptions()
                .enablePreview()
                .docLint(JavadocOptions.DocLinkOption.NO_MISSING)
                .quiet();

        jarSourcesOperation()
            .sourceDirectories(generateSourcesOperation().outputDirectory().toFile());

        publishOperation()
            .repository(MAVEN_LOCAL)
            .info(new PublishInfo()
                .groupId("io.github.jwharm.javagi")
                .artifactId(name)
                .description("Java bindings for " + name + ", generated from GObject-Introspection")
                .url("https://jwharm.github.io/java-gi/")
                .developer(new PublishDeveloper()
                    .id("jwharm")
                    .name("Jan-Willem Harmannij")
                    .url("https://github.com/jwharm"))
                .license(new PublishLicense()
                    .name("GNU Lesser General Public License, version 3")
                    .url("https://www.gnu.org/licenses/lgpl-3.0.txt")));
    }

    public GenerateSourcesOperation generateSourcesOperation() {
        return generateSourcesOperation_;
    }

    /* Create the module-path: The lib/compile directory is in the module path, and the
     * generated jar files, excluding sources-jar files.
     */
    private List<File> getModulePath() {
        List<File> modules = new ArrayList<>();
        modules.add(libCompileDirectory());
        modules.addAll(FileUtils.getFileList(buildDistDirectory(), FileUtils.JAR_FILE_PATTERN, SOURCES_JAR_FILE_PATTERN)
            .stream().map(file -> new File(buildDistDirectory(), file)).toList());
        return modules;
    }

    private String getModulePathString() {
        return FileUtils.joinPaths(getModulePath().stream().map(File::getAbsolutePath).toList());
    }

    @BuildCommand(value="generate-sources", summary="Generates Java sources from gir files")
    public void generateSources() throws Exception {
        generateSourcesOperation().executeOnce();
    }

    @Override
    public void compile() throws Exception {
        generateSources();
        compileOperation().compileOptions().modulePath(getModulePath());
        super.compile();
    }

    /**
     * Overrides {@link Project#javadoc()} to avoid recompiling the project
     */
    @Override
    public void javadoc() throws Exception {
        javadocOperation().javadocOptions(List.of("--module-path", getModulePathString()));
        javadocOperation().executeOnce(() -> javadocOperation().fromProject(this));
    }
}
