package io.github.jwharm.javagi;

import rife.bld.BuildCommand;
import java.nio.file.Path;

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
        version = version(0,5, 1);
        showStacktrace = true;

        gitPullOperation_ = new GitPullOperation()
            .uri(URI)
            .directory(buildDirectory().toPath().resolve("gir-files"));
        girDirectory(gitPullOperation_.directory());
    }

    @BuildCommand(value="download-gir-files", summary="Pull gir files from remote repository")
    public void downloadGirFiles() throws Exception {
        gitPullOperation_.executeOnce();
    }

    @BuildCommand(summary="Build glib")
    public void glib() throws Exception {
        downloadGirFiles();
        modules(new GLibBuild(this));
    }

    @BuildCommand(summary="Build gtk")
    public void gtk() throws Exception {
        downloadGirFiles();
        modules(new GtkBuild(this));
    }

    @BuildCommand(summary="Build adwaita")
    public void adwaita() throws Exception {
        downloadGirFiles();
        modules(new AdwaitaBuild(this));
    }

    @BuildCommand(summary="Build gstreamer")
    public void gstreamer() throws Exception {
        downloadGirFiles();
        modules(new GStreamerBuild(this));
    }

    @BuildCommand(summary="Build all modules")
    public void all() throws Exception {
        glib();
        gtk();
        adwaita();
        gstreamer();
    }

    public static void main(String[] args) {
        new JavaGIBuild().start(args);
    }
}
