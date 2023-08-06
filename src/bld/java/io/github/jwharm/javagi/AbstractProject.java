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

import io.github.jwharm.javagi.operations.GenerateSourcesOperation;
import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.dependencies.Repository;
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
import static rife.bld.dependencies.Scope.test;

/**
 * Base class for Java-GI module build files that contains shared build settings.
 */
public abstract class AbstractProject extends Project {

    public static final Repository JITPACK = new Repository("https://jitpack.io");

    private final GenerateSourcesOperation generateSourcesOperation_;

    public AbstractProject(JavaGIBuild bld, String name) {
        this.name = name;
        javaRelease = bld.javaRelease();
        generateSourcesOperation_ = new GenerateSourcesOperation();

        srcDirectory = new File(bld.workDirectory(), name);
        buildMainDirectory = new File(bld.buildMainDirectory(), name);
        buildJavadocDirectory = new File(bld.buildJavadocDirectory(), name);

        repositories = List.of(MAVEN_CENTRAL, JITPACK);
        scope(compile)
            .include(dependency("org.jetbrains", "annotations", version(24,0,1)));
        downloadSources = true;

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
            .javadocOptions()
                .modulePath(getModulePath())
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
                .description("Java bindings for " + name + ", generated with Java-GI")
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
        Pattern sourcesJarPattern = Pattern.compile("^.*-sources\\.jar$");
        List<File> modules = new ArrayList<>();
        modules.add(libCompileDirectory());
        modules.addAll(FileUtils.getFileList(buildDistDirectory(), FileUtils.JAR_FILE_PATTERN, sourcesJarPattern)
            .stream().map(file -> new File(buildDistDirectory(), file)).toList());
        return modules;
    }

    @BuildCommand(value="generate-sources", summary="Generates Java sources from gir files")
    public void generateSources() throws Exception {
        generateSourcesOperation().executeOnce();
    }

    /**
     * Overrides {@link Project#compile()} to run {@link #generateSources()}
     * and set the module path
     */
    @Override
    public void compile() throws Exception {
        generateSources();
        compileOperation().compileOptions().modulePath(getModulePath());
        super.compile();
    }

    /**
     * Overrides {@link Project#javadoc()} to set the module path
     */
    @Override
    public void javadoc() throws Exception {
        javadocOperation().javadocOptions().modulePath(getModulePath());
        super.javadoc();
    }
}
