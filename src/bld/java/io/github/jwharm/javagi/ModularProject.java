package io.github.jwharm.javagi;

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
