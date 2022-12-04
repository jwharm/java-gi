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
            source("GstBase-1.0", "org.gstreamer.base", true, "gstbase-1.0"),
            source("GstAllocators-1.0", "org.gstreamer.allocators", true, "gstallocators-1.0"),
            source("GstApp-1.0", "org.gstreamer.app", true, "gstapp-1.0", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "AppSrc", "set_caps", "set_capabilities")
                }
            }),
            source("GstAudio-1.0", "org.gstreamer.audio", true, "gstaudio-1.0"),
            source("GstBadAudio-1.0", "org.gstreamer.badaudio", true, "gstbadaudio-1.0"),
            source("GstCheck-1.0", "org.gstreamer.check", true, "gstcheck-1.0"),
            source("GstController-1.0", "org.gstreamer.controller", true, "gstcontroller-1.0"),
            source("GstGL-1.0", "org.gstreamer.gl", true, "gstgl-1.0"),
            source("GstGLEGL-1.0", "org.gstreamer.gl.egl", true),
            source("GstGLWayland-1.0", "org.gstreamer.gl.wayland", true),
            source("GstGLX11-1.0", "org.gstreamer.gl.x11", true),
            source("GstInsertBin-1.0", "org.gstreamer.insertbin", true, "gstinsertbin-1.0"),
            source("GstMpegts-1.0", "org.gstreamer.mpegts", true, "gstmpegts-1.0"),
            source("GstNet-1.0", "org.gstreamer.net", true, "gstnet-1.0"),
            source("GstPbutils-1.0", "org.gstreamer.pbutils", true, "gstpbutils-1.0"),
            source("GstPlay-1.0", "org.gstreamer.play", true, "gstplay-1.0"),
            source("GstPlayer-1.0", "org.gstreamer.player", true, "gstplayer-1.0"),
            source("GstRtp-1.0", "org.gstreamer.rtp", true, "gstrtp-1.0"),
            source("GstRtsp-1.0", "org.gstreamer.rtsp", true, "gstrtsp-1.0"),
            source("GstSdp-1.0", "org.gstreamer.sdp", true, "gstsdp-1.0"),
            source("GstTag-1.0", "org.gstreamer.tag", true, "gsttag-1.0"),
            source("GstTranscoder-1.0", "org.gstreamer.transcoder", true, "gsttranscoder-1.0"),
            source("GstVideo-1.0", "org.gstreamer.video", true, "gstvideo-1.0"),
            source("Vulkan-1.0", "org.vulkan", true, "vulkan"),
            source("GstVulkan-1.0", "org.gstreamer.vulkan", true, "gstvulkan-1.0", patches = object: PatchSet() {
                override fun patch(repo: Repository?) {
                    // Override with different return type
                    renameMethod(repo, "VulkanDescriptorCache", "acquire", "acquireDescriptorSet")
                }
            }),
            source("GstVulkanWayland-1.0", "org.gstreamer.vulkan.wayland", true),
            source("GstWebRTC-1.0", "org.gstreamer.webrtc", true, "gstwebrtc-1.0"),
            source("GstCodecs-1.0", "org.gstreamer.codecs", true, "gstcodecs-1.0")
        )
    }
}

tasks.compileJava.get().dependsOn(genSources)