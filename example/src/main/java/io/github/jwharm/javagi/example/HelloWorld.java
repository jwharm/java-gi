package io.github.jwharm.javagi.example;

public class HelloWorld {

    public static void main(String[] args) {

        // GTK4 simple example
        new Gtk4Example(args);

        // GTK4 ListView example
        new Gtk4ListViewExample(args);

        // GStreamer example
        // To run example, provide a valid path to an Ogg Vorbis file:
        new GStreamerExample(new String[]{"Example.ogg"});
    }
}
