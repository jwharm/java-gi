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

        var button = Button.newWithLabel("Hello world! 30");
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
                switch (ResponseType.of(responseId)) {
                    case OK -> {
                        window.close();
                    }
                    case CANCEL -> {
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
        };

        button.addTickCallback((widget, frameClock, userData) -> {
            if (state.tickStart == 0) state.tickStart = frameClock.getFrameTime();
            long sec = (frameClock.getFrameTime() - state.tickStart) / 1000000;
            if (sec > 30) {
                button.setLabel("Hello world!");
                System.out.println("Done counting");
                return GLib.SOURCE_REMOVE;
            }
            if (sec > state.second) {
                state.second = sec;
                button.setLabel("Hello world! " + (30 - sec));
                widget.queueDraw();
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
