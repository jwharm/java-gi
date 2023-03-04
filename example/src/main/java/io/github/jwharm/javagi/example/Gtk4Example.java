package io.github.jwharm.javagi.example;

import org.gnome.gio.ApplicationFlags;
import org.gnome.gtk.*;

public class Gtk4Example extends Application {

    public static void main(String[] args) {
        new Gtk4Example(args);
    }

    public Gtk4Example(String[] args) {
        super("io.github.jwharm.javagi.Example", ApplicationFlags.DEFAULT_FLAGS);
        onActivate(this::activate);
        run(args);
    }

    public void activate() {
        var window = new ApplicationWindow(this);
        window.setTitle("GTK from Java");
        window.setDefaultSize(300, 200);

        var box = Box.builder()
                .setOrientation(Orientation.VERTICAL)
                .setHalign(Align.CENTER)
                .setValign(Align.CENTER)
                .build();

        var button = Button.newWithLabel("Hello world!");
        button.onClicked(window::close);

        box.append(button);
        window.setChild(box);
        window.show();
    }
}
