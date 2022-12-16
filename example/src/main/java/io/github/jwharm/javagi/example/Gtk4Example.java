package io.github.jwharm.javagi.example;

import org.gtk.gio.ApplicationFlags;
import org.gtk.glib.GLib;
import org.gtk.gtk.*;

public class Gtk4Example {

    public void activate(org.gtk.gio.Application g_application) {
        var window = new ApplicationWindow(Application.castFrom(g_application));
        window.setTitle("Window");
        window.setDefaultSize(300, 200);

        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        var button = Button.newWithLabel("Hello world!");
        button.onClicked(btn -> {
            MessageDialog dialog = new MessageDialog(
                    window,
                    DialogFlags.MODAL.or(DialogFlags.DESTROY_WITH_PARENT),
                    MessageType.INFO,
                    ButtonsType.OK_CANCEL,
                    null
            );
            dialog.setTitle("Hello!");
            dialog.setMarkup("This is some **content**");
            dialog.onResponse(($, responseId) -> {
                // Would be ResponseType, but that can't be used in a switch
                switch (responseId) {
                    case -5 -> { // OK
                        window.close();
                    }
                    case -6 -> { // Cancel
                        System.out.println("Cancel");
                    }
                }
                dialog.close();
            });
            dialog.show();
        });

        var state = new Object() {
            long tickStart;
            long second;
            int callbackId;
        };

        state.callbackId = button.addTickCallback((widget, frameClock, userData) -> {
            if (state.tickStart == 0) state.tickStart = frameClock.getFrameTime();
            else {
                long time = frameClock.getFrameTime();
                long sec = (time - state.tickStart) / 1000000;
                if (sec > 60) {
                    button.removeTickCallback(state.callbackId);
                    button.setLabel("Hello world!");
                } else if (sec > state.second) {
                    state.second = sec;
                    button.setLabel("Hello world! " + (60 - sec));
                    widget.queueDraw();
                }
            }
            return GLib.SOURCE_CONTINUE;
        }, null, data -> {});

        box.append(button);
        window.setChild(box);
        window.show();
    }

    public Gtk4Example(String[] args) {
        var app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(this::activate);
        app.run(args.length, args);
    }
}
