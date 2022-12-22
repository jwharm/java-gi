package io.github.jwharm.javagi.example;

import io.github.jwharm.javagi.Out;
import org.gstreamer.gst.*;
import org.gtk.glib.Error;
import org.gtk.glib.GLib;
import org.gtk.glib.MainLoop;
import org.gtk.glib.Source;

import java.lang.foreign.MemoryAddress;
import java.util.Objects;
import java.util.stream.Stream;

public class GStreamerExample {

    MainLoop loop;
    Pipeline pipeline;
    Element source, demuxer, decoder, conv, sink;
    Bus bus;
    int busWatchId;

    private boolean busCall(Bus bus, Message msg) {

        if (msg.getType().equals(MessageType.EOS)) {
            GLib.print("End of stream\n");
            loop.quit();
        }

        else if (msg.getType().equals(MessageType.ERROR)) {
            Out<Error> error = new Out<>();
            Out<String> debug = new Out<>();
            msg.parseError(error, debug);

            GLib.printerr("Error: %s\n", error.get().getMessage());

            loop.quit();
        }

        return true;
    }

    private void onPadAdded(Pad pad) {

        // We can now link this pad with the vorbis-decoder sink pad
        GLib.print("Dynamic pad created, linking demuxer/decoder\n");

        Pad sinkpad = decoder.getStaticPad("sink");

        if (sinkpad != null) {
            pad.link(sinkpad);
        } else {
            GLib.printerr("Sink pad not set!\n");
        }
    }

    public GStreamerExample(String[] args) {

        // Initialisation
        Gst.init(new Out<>(args.length), new Out<>(args));

        loop = new MainLoop(null, false);

        // Check input arguments
        if (args.length != 1) {
            GLib.printerr("Usage: %s <Ogg/Vorbis filename>\n", getClass().getName());
            return;
        }

        // Create gstreamer elements
        pipeline = new Pipeline("audio-player");
        source = ElementFactory.make("filesrc", "file-source");
        demuxer = ElementFactory.make("oggdemux", "ogg-demuxer");
        decoder = ElementFactory.make("vorbisdec", "vorbis-decoder");
        conv = ElementFactory.make("audioconvert", "converter");
        sink = ElementFactory.make("autoaudiosink", "audio-output");

        if (Stream.of(source, demuxer, decoder, conv, sink).anyMatch(Objects::isNull)) {
            GLib.printerr("One element could not be created. Exiting.\n");
            return;
        }

        // Set up the pipeline

        // We set the input filename to the source element
        source.set("location", args[0], null);

        // We add a message handler
        bus = pipeline.getBus();
        busWatchId = bus.addWatch(this::busCall);

        // We add all elements into the pipeline
        // file-source | ogg-demuxer | vorbis-decoder | converter | alsa-output
        pipeline.addMany(source, demuxer, decoder, conv, sink, null);

        // We link the elements together
        // file-source -> ogg-demuxer ~> vorbis-decoder -> converter -> alsa-output
        source.link(demuxer);
        decoder.linkMany(conv, sink, null);
        demuxer.onPadAdded(this::onPadAdded);

        // Set the pipeline
        GLib.print("Now playing: %s\n", args[0]);
        pipeline.setState(State.PLAYING);

        // Iterate
        GLib.print("Running...\n");
        loop.run();

        // Out of the main loop, clean up nicely
        GLib.print("Returned, stopping playback\n");
        pipeline.setState(State.NULL);

        GLib.print("Deleting pipeline\n");
        Source.remove(busWatchId);
    }
}
