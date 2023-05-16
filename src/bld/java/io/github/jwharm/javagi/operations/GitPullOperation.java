package io.github.jwharm.javagi.operations;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.StoredConfig;
import rife.bld.operations.AbstractOperation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class GitPullOperation extends AbstractOperation<GitPullOperation> {

    String uri_;
    Path directory_;

    public GitPullOperation uri(String uri) {
        uri_ = uri;
        return this;
    }

    public String uri() {
        return uri_;
    }

    public GitPullOperation directory(Path directory) {
        directory_ = directory;
        return this;
    }

    public Path directory() {
        return directory_;
    }

    public void execute() throws Exception {
        if (Files.exists(directory_)) {
            // Open existing repository
            Git git = new Git(new FileRepository(directory_.resolve(".git").toString()));

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
                .setURI(uri_)
                .setBranchesToClone(List.of("refs/heads/main"))
                .setDirectory(directory_.toFile())
                .call()
                .close();

            // Write config file that pull() will use
            Git git = Git.open(directory_.toFile());
            StoredConfig config = git.getRepository().getConfig();
            config.setString("branch", "main", "merge", "refs/heads/main");
            config.setString("branch", "main", "remote", "origin");
            config.setString("remote", "origin", "fetch", "+refs/heads/main:refs/remotes/origin/main");
            config.setString("remote", "origin", "url", uri_);
            config.save();
            git.close();

            if (! silent()) {
                System.out.println("Git clone completed successfully.");
            }
        }
    }
}
