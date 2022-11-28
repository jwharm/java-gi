import io.github.jwharm.javagi.JavaGI
import io.github.jwharm.javagi.generator.PatchSet
import io.github.jwharm.javagi.model.Repository

plugins {
    id("java-gi.library-conventions")
}

val generatedPath = buildDir.resolve("generated/sources/javagi/java/main")

dependencies {
    implementation(project(":glib"))
}

sourceSets {
    main {
        java {
            srcDir(generatedPath)
        }
    }
}

val genSources by tasks.registering {
    doLast {
        val sourcePath = if (project.hasProperty("girSources")) project.property("girSources").toString() else "/usr/share/gir-1.0"
        fun source(name: String, pkg: String, generate: Boolean, vararg natives: String, patches: PatchSet? = null) = JavaGI.Source("$sourcePath/$name.gir", pkg, generate, setOf(*natives), generatedPath.toPath(), patches ?: PatchSet.EMPTY)
        JavaGI.generate(
            source("GLib-2.0", "org.gtk.glib", false, "glib-2.0", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // This method has parameters that jextract does not support
                    removeFunction(repo, "assertion_message_cmpnum");
                    // Incompletely defined
                    removeFunction(repo, "clear_error");
                }
            }),
            source("GObject-2.0", "org.gtk.gobject", false, "gobject-2.0", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // This is an alias for Callback type
                    removeType(repo, "VaClosureMarshal")
                    removeType(repo, "SignalCVaMarshaller")
                    removeFunction(repo, "signal_set_va_marshaller")
                    // Override with different return type
                    renameMethod(repo, "TypeModule", "use", "use_type_module")
                    // These functions have two Callback parameters, this isn't supported yet
                    removeFunction(repo, "signal_new_valist")
                    removeFunction(repo, "signal_newv")
                    removeFunction(repo, "signal_new")
                    removeFunction(repo, "signal_new_class_handler")
                }
            }),
            source("Gio-2.0", "org.gtk.gio", false, "gio-2.0", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "BufferedInputStream", "read_byte", "read_int");
                    // g_async_initable_new_finish is a method declaration in the interface AsyncInitable.
                    // It is meant to be implemented as a constructor (actually, a static factory method).
                    // However, Java does not allow a (non-static) method to be implemented/overridden by a static method.
                    // The current solution is to remove the method from the interface. It is still available in the implementing classes.
                    removeMethod(repo, "AsyncInitable", "new_finish");
                }
            }),
            source("GModule-2.0", "org.gtk.gmodule", false),
            source("Gst-1.0", "org.gstreamer.gst", true, "gstreamer-1.0"),
            source("GstBase-1.0", "org.gstreamer.base", true),
            source("GstCheck-1.0", "org.gstreamer.check", true),
            source("GstController-1.0", "org.gstreamer.controller", true),
            source("GstNet-1.0", "org.gstreamer.net", true)
        )
    }
}

tasks.compileJava.get().dependsOn(genSources.get())
