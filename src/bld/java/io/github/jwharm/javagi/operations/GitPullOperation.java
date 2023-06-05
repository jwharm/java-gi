package io.github.jwharm.javagi.operations;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.StoredConfig;
import rife.bld.operations.AbstractOperation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GitPullOperation extends AbstractOperation<GitPullOperation> {

    String uri;
    Path directory;

    public GitPullOperation uri(String uri) {
        this.uri = uri;
        return this;
    }

    public String uri() {
        return uri;
    }

    public GitPullOperation directory(Path directory) {
        this.directory = directory;
        return this;
    }

    public Path directory() {
        return directory;
    }

    public void execute() throws Exception {
        if (Files.exists(directory)) {
            // Open existing repository
            Git git = new Git(new FileRepository(directory.resolve(".git").toString()));

            // Pull updates from origin
            git.pull()
                .setRemote("origin")
                .setRemoteBranchName("main")
                .call();
            git.close();

            if (! silent()) {
                System.out.println("Git pull completed successfully.");
            }
        } else {
            // Clone the repository
            Git.cloneRepository()
                .setURI(uri)
                .setBranchesToClone(List.of("refs/heads/main"))
                .setDirectory(directory.toFile())
                .call()
                .close();

            // Write config file that pull() will use
            Git git = Git.open(directory.toFile());
            StoredConfig config = git.getRepository().getConfig();
            config.setString("branch", "main", "merge", "refs/heads/main");
            config.setString("branch", "main", "remote", "origin");
            config.setString("remote", "origin", "fetch", "+refs/heads/main:refs/remotes/origin/main");
            config.setString("remote", "origin", "url", uri);
            config.save();
            git.close();

            if (! silent()) {
                System.out.println("Git clone completed successfully.");
            }
        }
    }
}
