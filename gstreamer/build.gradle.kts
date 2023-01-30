import ext.*
import io.github.jwharm.javagi.generator.PatchSet.*

plugins {
    id("java-gi.library-conventions")
}

dependencies {
    api(project(":glib"))
}

setupGenSources {
    moduleInfo = """
        module org.gstreamer {
            requires static org.jetbrains.annotations;
            requires transitive org.gtk.glib;
            %s
        }
    """.trimIndent()

    source("GLib-2.0", "org.gtk.glib", false, "glib-2.0")
    source("GObject-2.0", "org.gtk.gobject", false, "gobject-2.0")
    source("Gio-2.0", "org.gtk.gio", false, "gio-2.0")
    source("GModule-2.0", "org.gtk.gmodule", false)

    source("Gst-1.0", "org.gstreamer.gst", true, "gstreamer-1.0") { repo ->
        // According to the gir file, the size parameter is an out parameter, but it isn't
        removeMethod(repo, "TypeFind", "peek")
    }
    source("GstBase-1.0", "org.gstreamer.base", true, "gstbase-1.0")
    source("GstAllocators-1.0", "org.gstreamer.allocators", true, "gstallocators-1.0")
    source("GstApp-1.0", "org.gstreamer.app", true, "gstapp-1.0") { repo ->
        // Override with different return type
        renameMethod(repo, "AppSrc", "set_caps", "set_capabilities")
    }
    source("GstAudio-1.0", "org.gstreamer.audio", true, "gstaudio-1.0")
    source("GstBadAudio-1.0", "org.gstreamer.badaudio", true, "gstbadaudio-1.0")
    source("GstCheck-1.0", "org.gstreamer.check", true, "gstcheck-1.0")
    source("GstController-1.0", "org.gstreamer.controller", true, "gstcontroller-1.0")
    source("GstGL-1.0", "org.gstreamer.gl", true, "gstgl-1.0")
    source("GstGLEGL-1.0", "org.gstreamer.gl.egl", true)
    source("GstGLWayland-1.0", "org.gstreamer.gl.wayland", true)
    source("GstGLX11-1.0", "org.gstreamer.gl.x11", true)
    source("GstInsertBin-1.0", "org.gstreamer.insertbin", true, "gstinsertbin-1.0")
    source("GstMpegts-1.0", "org.gstreamer.mpegts", true, "gstmpegts-1.0")
    source("GstNet-1.0", "org.gstreamer.net", true, "gstnet-1.0")
    source("GstPbutils-1.0", "org.gstreamer.pbutils", true, "gstpbutils-1.0")
    source("GstPlay-1.0", "org.gstreamer.play", true, "gstplay-1.0")
    source("GstPlayer-1.0", "org.gstreamer.player", true, "gstplayer-1.0")
    source("GstRtp-1.0", "org.gstreamer.rtp", true, "gstrtp-1.0")
    source("GstRtsp-1.0", "org.gstreamer.rtsp", true, "gstrtsp-1.0")
    source("GstSdp-1.0", "org.gstreamer.sdp", true, "gstsdp-1.0") { repo ->
        // Reference to array length points to the wrong parameter index
        removeMethod(repo, "MIKEYPayload", "key_data_set_interval")
    }
    source("GstTag-1.0", "org.gstreamer.tag", true, "gsttag-1.0")
    source("GstTranscoder-1.0", "org.gstreamer.transcoder", true, "gsttranscoder-1.0")
    source("GstVideo-1.0", "org.gstreamer.video", true, "gstvideo-1.0")
    source("Vulkan-1.0", "org.vulkan", true, "vulkan")
    source("GstVulkan-1.0", "org.gstreamer.vulkan", true, "gstvulkan-1.0") { repo ->
        // Override with different return type
        renameMethod(repo, "VulkanDescriptorCache", "acquire", "acquireDescriptorSet")
    }
    source("GstVulkanWayland-1.0", "org.gstreamer.vulkan.wayland", true)
    source("GstWebRTC-1.0", "org.gstreamer.webrtc", true, "gstwebrtc-1.0")
    source("GstCodecs-1.0", "org.gstreamer.codecs", true, "gstcodecs-1.0")
}

tasks.javadoc {
    dependsOn(project(":glib").tasks.javadoc)
    options.linksOffline("https://jwharm.github.io/java-gi/glib", project(":glib"))
}