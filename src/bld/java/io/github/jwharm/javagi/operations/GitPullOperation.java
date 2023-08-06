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
