import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

val pkgVersion = "pkg-config --modversion gstreamer-1.0".runCommand(project, "1.0")
version = "$pkgVersion-$version"

setupGenSources {
    moduleInfo = """
        module org.freedesktop.gstreamer {
            requires static org.jetbrains.annotations;
            requires transitive org.gnome.glib;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gnome.glib", false, "glib-2.0")
    source("GObject-2.0", "org.gnome.gobject", false, "gobject-2.0")
    source("Gio-2.0", "org.gnome.gio", false, "gio-2.0")
    source("GModule-2.0", "org.gnome.gmodule", false)

    source("Gst-1.0", "org.freedesktop.gstreamer.gst", true, "gstreamer-1.0") { repo ->
        // According to the gir file, the size parameter is an out parameter, but it isn't
        removeMethod(repo, "TypeFind", "peek")
    }
    source("GstBase-1.0", "org.freedesktop.gstreamer.base", true, "gstbase-1.0")
    source("GstAllocators-1.0", "org.freedesktop.gstreamer.allocators", true, "gstallocators-1.0")
    source("GstApp-1.0", "org.freedesktop.gstreamer.app", true, "gstapp-1.0") { repo ->
        // Override with different return type
        setReturnType(repo, "AppSrc", "set_caps", "gboolean", "gboolean", "true", "always %TRUE")
        setReturnType(repo, "AppSink", "set_caps", "gboolean", "gboolean", "true", "always %TRUE")
    }
    source("GstAudio-1.0", "org.freedesktop.gstreamer.audio", true, "gstaudio-1.0") { repo ->
        // Override with different return type
        setReturnType(repo, "AudioSink", "stop", "gboolean", "gboolean", "true", "always %TRUE")
        
        // This is a Fraction property, but I don't know how to put a Fraction object in a GValue
        removeProperty(repo, "AudioAggregator", "output-buffer-duration-fraction")
    }
    source("GstBadAudio-1.0", "org.freedesktop.gstreamer.badaudio", true, "gstbadaudio-1.0")
    source("GstCheck-1.0", "org.freedesktop.gstreamer.check", true, "gstcheck-1.0")
    source("GstController-1.0", "org.freedesktop.gstreamer.controller", true, "gstcontroller-1.0")
    source("GstGL-1.0", "org.freedesktop.gstreamer.gl", true, "gstgl-1.0")
    source("GstGLEGL-1.0", "org.freedesktop.gstreamer.gl.egl", true)
    source("GstGLWayland-1.0", "org.freedesktop.gstreamer.gl.wayland", true)
    source("GstGLX11-1.0", "org.freedesktop.gstreamer.gl.x11", true)
    source("GstInsertBin-1.0", "org.freedesktop.gstreamer.insertbin", true, "gstinsertbin-1.0")
    source("GstMpegts-1.0", "org.freedesktop.gstreamer.mpegts", true, "gstmpegts-1.0")
    source("GstNet-1.0", "org.freedesktop.gstreamer.net", true, "gstnet-1.0")
    source("GstPbutils-1.0", "org.freedesktop.gstreamer.pbutils", true, "gstpbutils-1.0")
    source("GstPlay-1.0", "org.freedesktop.gstreamer.play", true, "gstplay-1.0")
    source("GstPlayer-1.0", "org.freedesktop.gstreamer.player", true, "gstplayer-1.0")
    source("GstRtp-1.0", "org.freedesktop.gstreamer.rtp", true, "gstrtp-1.0")
    source("GstRtsp-1.0", "org.freedesktop.gstreamer.rtsp", true, "gstrtsp-1.0")
    source("GstSdp-1.0", "org.freedesktop.gstreamer.sdp", true, "gstsdp-1.0") { repo ->
        // Reference to array length points to the wrong parameter index
        removeMethod(repo, "MIKEYPayload", "key_data_set_interval")
    }
    source("GstTag-1.0", "org.freedesktop.gstreamer.tag", true, "gsttag-1.0")
    source("GstTranscoder-1.0", "org.freedesktop.gstreamer.transcoder", true, "gsttranscoder-1.0")
    source("GstVideo-1.0", "org.freedesktop.gstreamer.video", true, "gstvideo-1.0")
    source("Vulkan-1.0", "org.vulkan", true, "vulkan")
    source("GstVulkan-1.0", "org.freedesktop.gstreamer.vulkan", true, "gstvulkan-1.0") { repo ->
        // Override with different return type
        renameMethod(repo, "VulkanDescriptorCache", "acquire", "acquireDescriptorSet")
    }
    source("GstVulkanWayland-1.0", "org.freedesktop.gstreamer.vulkan.wayland", true)
    source("GstWebRTC-1.0", "org.freedesktop.gstreamer.webrtc", true, "gstwebrtc-1.0")
    source("GstCodecs-1.0", "org.freedesktop.gstreamer.codecs", true, "gstcodecs-1.0")
}

tasks.javadoc {
    linksOffline("https://jwharm.github.io/java-gi/glib", project(":glib"))
}