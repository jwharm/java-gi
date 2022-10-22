import io.github.jwharm.javagi.JavaGI
import io.github.jwharm.javagi.generator.PatchSet
import io.github.jwharm.javagi.model.Repository

plugins {
    `java-library`
}

buildscript {
    dependencies {
        classpath("io.github.jwharm.javagi:generator:1.0")
    }
}

java {
    // Temporarily needed until gradle 7.6 is out
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(19))
    }
    // Temporarily disabled since the generated docs were apparently invalid
//    withJavadocJar()
    withSourcesJar()
}

group = "io.github.jwharm.javagi"
version = "gtk4"

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")
}

val generatedPath = buildDir.resolve("generated/sources/javagi/java/main")

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
        fun source(name: String, pkg: String, patches: PatchSet?) = JavaGI.Source("$sourcePath/$name.gir", pkg, generatedPath.absolutePath, patches ?: PatchSet.EMPTY)
        JavaGI.generate(
            source("GLib-2.0", "org.gtk.glib", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // This method has parameters that jextract does not support
                    removeFunction(repo, "assertion_message_cmpnum");
                    // Incompletely defined
                    removeFunction(repo, "clear_error");
                }
            }),
            source("GObject-2.0", "org.gtk.gobject", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // These types require mapping va_list (varargs) types
                    removeType(repo, "VaClosureMarshal")
                    removeType(repo, "SignalCVaMarshaller")
                    removeFunction(repo, "signal_set_va_marshaller")
                    // Override with different return type
                    renameMethod(repo, "TypeModule", "use", "use_type_module")
                    // These functions have two Callback parameters, this isn't supported yet
                    removeFunction(repo, "signal_new_valist")
                    removeFunction(repo, "signal_newv")
                }
            }),
            source("Gio-2.0", "org.gtk.gio", object: PatchSet() {
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
            source("cairo-1.0", "org.cairographics", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Incompletely defined
                    removeFunction(repo, "image_surface_create");
                }
            }),
            source("HarfBuzz-0.0", "org.harfbuzz", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // This constant has type "language_t" which cannot be instantiated
                    removeConstant(repo, "LANGUAGE_INVALID");
                }
            }),
            source("Pango-1.0", "org.pango", null),
            source("GModule-2.0", "org.gtk.gmodule", null),
            source("GdkPixbuf-2.0", "org.gtk.gdkpixbuf", null),
            source("Gdk-4.0", "org.gtk.gdk", null),
            source("Graphene-1.0", "org.gtk.graphene", null),
            source("Gsk-4.0", "org.gtk.gsk", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // These types are defined in the GIR, but unavailable by default
                    removeType(repo, "BroadwayRenderer");
                    removeType(repo, "BroadwayRendererClass");
                }
            }),
            source("Gtk-4.0", "org.gtk.gtk", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "MenuButton", "get_direction", "get_arrow_direction");
                    renameMethod(repo, "PrintUnixDialog", "get_settings", "get_print_settings");
                }
            }),
            source("Adw-1", "org.gnome.adw", object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "ActionRow", "activate", "activate_row");
                    renameMethod(repo, "SplitButton", "get_direction", "get_arrow_direction");
                }
            })
        )
    }
}

tasks.compileJava.get().dependsOn(genSources.get())

// Temporarily needed until panama is out of preview
tasks.compileJava.get().options.compilerArgs.add("--enable-preview")
//(tasks.javadoc.get().options as CoreJavadocOptions).run {
//    addStringOption("source", "19")
//    addBooleanOption("-enable-preview", true)
//}
