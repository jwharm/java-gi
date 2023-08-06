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

import rife.bld.BuildCommand;
import rife.bld.Project;

import java.util.ArrayList;
import java.util.List;

/**
 * This project builds a list of other projects sequentially.
 */
public class ModularProject extends Project {

    private final List<Project> modules_ = new ArrayList<>();

    /**
     * Retrieves the modules to build.
     * @return the modules
     */
    public List<Project> modules() {
        return modules_;
    }

    /**
     * Provides the projects to build.
     * @param projects the projects
     * @return this Project instance
     */
    public ModularProject modules(Project... projects) {
        return modules(List.of(projects));
    }

    /**
     * Provides the projects to build.
     * @param projects the projects
     * @return this Project instance
     */
    public ModularProject modules(List<Project> projects) {
        this.modules_.addAll(projects);
        return this;
    }

    @BuildCommand(value="generate-sources", summary="Generates Java sources from gir files")
    public void generateSources() throws Exception {
        for (var project : modules_) {
            if (project instanceof AbstractProject p) {
                p.generateSources();
            }
        }
    }


    @Override
    public void clean() throws Exception {
        super.clean();
        for (var project : modules_) {
            project.clean();
        }
    }

    @Override
    public void compile() throws Exception {
        for (var project : modules_) {
            project.compile();
        }
        super.compile();
    }

    @Override
    public void dependencyTree() throws Exception {
        if (modules_.isEmpty()) {
            super.dependencyTree();
        } else {
            for (var project : modules_) {
                System.out.println(project.name());
                System.out.println("=".repeat(project.name().length()));
                project.dependencyTree();
            }
        }
    }

    @Override
    public void download() throws Exception {
        super.download();
        for (var project : modules_) {
            project.download();
        }
    }

    @Override
    public void jar() throws Exception {
        for (var project : modules_) {
            project.jar();
        }
    }

    @Override
    public void jarJavadoc() throws Exception {
        for (var project : modules_) {
            project.jarJavadoc();
        }
    }

    @Override
    public void jarSources() throws Exception {
        for (var project : modules_) {
            project.jarSources();
        }
    }

    @Override
    public void javadoc() throws Exception {
        for (var project : modules_) {
            project.javadoc();
        }
    }

    @Override
    public void publish() throws Exception {
        for (var project : modules_) {
            project.publish();
        }
    }

    @Override
    public void purge() throws Exception {
        super.purge();
        for (var project : modules_) {
            project.purge();
        }
    }

    @Override
    public void updates() throws Exception {
        super.updates();
        for (var project : modules_) {
            project.updates();
        }
    }
}
