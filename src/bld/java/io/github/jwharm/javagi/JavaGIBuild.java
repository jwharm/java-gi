/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package io.github.jwharm.javagi;

import io.github.jwharm.javagi.modules.*;
import io.github.jwharm.javagi.operations.GitPullOperation;
import io.github.jwharm.javagi.operations.GlibCompileResourcesOperation;
import rife.bld.BuildCommand;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Scope.test;

/**
 * Base class that is run by the `bld` command.
 * All java-gi bindings Build classes are run from this class.
 */
public class JavaGIBuild extends ModularProject {

    private final GitPullOperation gitPullOperation;
    private static final String URI = "https://github.com/gircore/gir-files.git";
    private Path girDirectory;

    private final GlibCompileResourcesOperation glibCompileResourcesOperation;

    public void girDirectory(Path girDirectory) {
        this.girDirectory = girDirectory;
    }

    public Path girDirectory() {
        return girDirectory;
    }

    public JavaGIBuild() {
        pkg = "io.github.jwharm.javagi";
        name = "javagi";
        version = version(0,6, 2);
        javaRelease = 20;
        showStacktrace = true;
        repositories = List.of(MAVEN_CENTRAL);

        scope(test)
            .include(dependency("org.junit.jupiter", "junit-jupiter", version(5,9,3)))
            .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1,9,3)));

        compileOperation()
            .compileTestClasspath(getModuleClasspath())
            .compileOptions()
                .enablePreview();

        testOperation()
            .classpath(getModuleClasspath())
            .javaOptions()
                .enablePreview()
                .enableNativeAccess(List.of("ALL-UNNAMED"));

        gitPullOperation = new GitPullOperation()
            .uri(URI)
            .directory(buildDirectory().toPath().resolve("gir-files"));
        girDirectory(gitPullOperation.directory());

        glibCompileResourcesOperation = new GlibCompileResourcesOperation()
            .workDirectory(srcTestResourcesDirectory());
    }

    @BuildCommand(value="download-gir-files", summary="Pulls gir files from remote repository")
    public void downloadGirFiles() throws Exception {
        gitPullOperation.executeOnce();
    }

    @BuildCommand(value="compile-test-resources", summary="Compiles gresource.xml test files")
    public void compileTestResources() throws Exception {
        glibCompileResourcesOperation.executeOnce();
    }

    @Override
    public void download() throws Exception {
        downloadGirFiles();
        super.download();
    }

    @Override
    public void updates() throws Exception {
        downloadGirFiles();
        super.updates();
    }

    @Override
    public void test() throws Exception {
        compileTestResources();
        super.test();
    }

    @BuildCommand(summary="Builds glib")
    public void glib() {
        modules(new GLibBuild(this));
    }

    @BuildCommand(summary="Builds gtk")
    public void gtk() {
        modules(new GtkBuild(this));
    }

    @BuildCommand(summary="Builds adwaita")
    public void adwaita() {
        modules(new AdwaitaBuild(this));
    }

    @BuildCommand(summary="Builds gstreamer")
    public void gstreamer() {
        modules(new GStreamerBuild(this));
    }

    @BuildCommand(summary="Builds gtksourceview")
    public void gtksourceview() {
        modules(new GtkSourceViewBuild(this));
    }

    @BuildCommand(summary="Builds webkitgtk")
    public void webkitgtk() {
        modules(new WebKitGtkBuild(this));
    }

    @BuildCommand(summary="Builds all modules")
    public void all() {
        glib();
        gtk();
        adwaita();
        gstreamer();
        gtksourceview();
        webkitgtk();
    }

    private List<String> getModuleClasspath() {
        File[] modules = buildMainDirectory().listFiles(File::isDirectory);
        return modules == null ? new ArrayList<>() : Arrays.stream(modules)
            .map(File::getAbsolutePath)
            .toList();
    }

    public static void main(String[] args) {
        new JavaGIBuild().start(args);
    }
}
