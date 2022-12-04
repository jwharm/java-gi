import io.github.jwharm.javagi.JavaGI
import io.github.jwharm.javagi.generator.PatchSet
import io.github.jwharm.javagi.model.Repository
import java.nio.file.Path

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
        val sourcePath = Path.of(if (project.hasProperty("girSources")) project.property("girSources").toString() else "/usr/share/gir-1.0")
        fun source(name: String, pkg: String, generate: Boolean, vararg natives: String, patches: PatchSet? = null) = JavaGI.Source(sourcePath.resolve("$name.gir"), pkg, generate, setOf(*natives), patches ?: PatchSet.EMPTY)
        JavaGI.generate(generatedPath.toPath(),
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
            source("cairo-1.0", "org.cairographics", true, "cairo", "cairo-gobject", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Incompletely defined
                    removeFunction(repo, "image_surface_create");
                }
            }),
            source("freetype2-2.0", "org.freetype", true),
            source("HarfBuzz-0.0", "org.harfbuzz", true, "harfbuzz", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // This constant has type "language_t" which cannot be instantiated
                    removeConstant(repo, "LANGUAGE_INVALID");
                }
            }),
            source("Pango-1.0", "org.pango", true, "pango-1.0"),
            source("PangoCairo-1.0", "org.pango.cairo", true, "pangocairo-1.0"),
            source("GdkPixbuf-2.0", "org.gtk.gdkpixbuf", true, "gdk_pixbuf-2.0"),
            source("Gdk-4.0", "org.gtk.gdk", true),
            source("Graphene-1.0", "org.gtk.graphene", true, "graphene-1.0"),
            source("Gsk-4.0", "org.gtk.gsk", true, patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // These types are defined in the GIR, but unavailable by default
                    removeType(repo, "BroadwayRenderer");
                    removeType(repo, "BroadwayRendererClass");
                }
            }),
            source("Gtk-4.0", "org.gtk.gtk", true, "gtk-4", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "MenuButton", "get_direction", "get_arrow_direction");
                    renameMethod(repo, "PrintUnixDialog", "get_settings", "get_print_settings");
                    renameMethod(repo, "PrintSettings", "get", "get_string");
                }
            }),
            source("Adw-1", "org.gnome.adw", true, "adwaita-1", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "ActionRow", "activate", "activate_row");
                    renameMethod(repo, "SplitButton", "get_direction", "get_arrow_direction");
                }
            })
        ).writeModuleInfo("""
            module org.gtk {
                requires org.jetbrains.annotations;
                requires org.glib;
                %s
            }
        """.trimIndent())
    }
}

tasks.compileJava.get().dependsOn(genSources)
