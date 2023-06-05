package io.github.jwharm.javagi.operations;

import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

public class GlibCompileResourcesOperation extends AbstractOperation<GlibCompileResourcesOperation> {

    File workDirectory;

    public GlibCompileResourcesOperation workDirectory(File directory) {
        this.workDirectory = directory;
        return this;
    }

    public File workDirectory() {
        return workDirectory;
    }

    private List<String> getCommand() throws FileNotFoundException {
        List<String> command = new ArrayList<>();
        command.add("glib-compile-resources");
        File[] files = workDirectory().listFiles((dir, name) -> name.endsWith(".gresource.xml"));
        if (files == null) {
            throw new FileNotFoundException("No .gresource.xml files found");
        }
        Arrays.stream(files).map(File::getAbsolutePath).forEach(command::add);
        return command;
    }

    @Override
    public void execute() throws Exception {
        int exitCode = new ProcessBuilder()
            .inheritIO()
            .directory(workDirectory())
            .command(getCommand())
            .start()
            .waitFor();

        if (exitCode != 0) {
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }
        if (! silent()) {
            System.out.println("GResource compilation completed successfully.");
        }
    }
}
