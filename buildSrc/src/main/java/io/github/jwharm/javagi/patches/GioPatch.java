package io.github.jwharm.javagi.patches;

import io.github.jwharm.javagi.generator.Patch;
import io.github.jwharm.javagi.model.Method;
import io.github.jwharm.javagi.model.RegisteredType;
import io.github.jwharm.javagi.model.Repository;
import io.github.jwharm.javagi.model.VirtualMethod;

import java.util.List;

public class GioPatch implements Patch {

    @Override
    public void patch(Repository repo) {
        // Override with different return type
        renameMethod(repo, "BufferedInputStream", "read_byte", "read_int");
        renameMethod(repo, "IOModule", "load", "load_module");

        // Override of static method
        removeVirtualMethod(repo, "SocketControlMessage", "get_type");

        // g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
        // It is meant to be implemented as a constructor (actually, a static factory method).
        // However, Java does not allow a (non-static) method to be implemented/overridden by a static method.
        // The current solution is to remove the method from the interface. It is still available in the implementing classes.
        removeMethod(repo, "AsyncInitable", "new_finish");

        // Add virtual methods as instance methods
        for (String type : List.of("FileInputStream", "FileOutputStream", "FileIOStream")) {
            addInstanceMethod(repo, type, "tell");
            addInstanceMethod(repo, type, "seek");
            addInstanceMethod(repo, type, "can_truncate");
            addInstanceMethod(repo, type, "can_seek");
        }

        // Let these classes implement the AutoCloseable interface, so they can be used in try-with-resources blocks.
        makeAutoCloseable(repo, "IOStream");
        makeAutoCloseable(repo, "InputStream");
        makeAutoCloseable(repo, "OutputStream");
    }
}
