package io.github.jwharm.javagi;

import rife.bld.BuildCommand;
import rife.bld.publish.*;

import java.nio.file.Path;
import java.util.stream.Stream;

/**
 * Base class that is run by the `bld` command.
 * All java-gi bindings Build classes are run from this class.
 */
public class JavaGIBuild extends ModularProject {

    private static final String URI = "https://github.com/gircore/gir-files.git";
    private final GitPullOperation gitPullOperation_;
    private Path girDirectory_;

    public void girDirectory(Path girDirectory) {
        girDirectory_ = girDirectory;
    }

    public Path girDirectory() {
        return girDirectory_;
    }

    public JavaGIBuild() {
        pkg = "io.github.jwharm.javagi";
        name = "javagi";
        version = version(0, 5).withQualifier("SNAPSHOT");
        showStacktrace = true;

        gitPullOperation_ = new GitPullOperation()
            .uri(URI)
            .directory(buildDirectory().toPath().resolve("gir-files"));
        girDirectory(gitPullOperation_.directory());

        publishOperation()
            .repository(repository("https://maven.pkg.github.com/jwharm/javagi"))
            .info(new PublishInfo()
                .groupId("io.github.jwharm.javagi")
                .artifactId("javagi")
                .description("Java bindings generated from GObject-Introspection")
                .url("https://jwharm.github.io/java-gi/")
                .developer(new PublishDeveloper()
                    .id("jwharm")
                    .name("Jan-Willem Harmannij")
                    .url("https://github.com/jwharm"))
                .license(new PublishLicense()
                    .name("GNU Lesser General Public License, version 3")
                    .url("https://www.gnu.org/licenses/lgpl-3.0.txt"))
                .scm(new PublishScm()
                    .connection("scm:git:https://github.com/jwharm/javagi.git")
                    .developerConnection("scm:git:git@github.com:jwharm/javagi.git")
                    .url("https://jwharm.github.io/java-gi/")))
            .artifacts(
                Stream.of(
                    modules().stream().map(m -> new PublishArtifact(m.jarOperation().destinationFile(), m.name(), "jar")),
                    modules().stream().map(m -> new PublishArtifact(m.jarJavadocOperation().destinationFile(), m.name(), "jar")),
                    modules().stream().map(m -> new PublishArtifact(m.jarSourcesOperation().destinationFile(), m.name(), "jar"))
                ).flatMap(i -> i).toList());
    }

    @BuildCommand(value="download-gir-files", summary="Pull gir files from remote repository")
    public void downloadGirFiles() throws Exception {
        gitPullOperation_.executeOnce();
    }

    @BuildCommand(summary="Build glib")
    public void glib() {
        modules(new GLibBuild(this));
    }

    @BuildCommand(summary="Build gtk")
    public void gtk() {
        modules(new GtkBuild(this));
    }

    @BuildCommand(summary="Build adwaita")
    public void adwaita() {
        modules(new AdwaitaBuild(this));
    }

    @BuildCommand(summary="Build gstreamer")
    public void gstreamer() {
        modules(new GStreamerBuild(this));
    }

    @BuildCommand(summary="Build all modules")
    public void all() {
        glib();
        gtk();
        adwaita();
        gstreamer();
    }

    public static void main(String[] args) {
        new JavaGIBuild().start(args);
    }
}
