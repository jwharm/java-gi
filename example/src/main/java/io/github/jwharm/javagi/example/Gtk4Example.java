package io.github.jwharm.javagi.example;

import org.gnome.gio.ApplicationFlags;
import org.gnome.glib.GLib;
import org.gnome.gtk.*;

import java.util.concurrent.atomic.AtomicInteger;

public class Gtk4Example {

    private final Application app;

    public void activate() {
        var window = new ApplicationWindow(app);
        window.setTitle("Window");
        window.setDefaultSize(300, 200);

        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        AtomicInteger sec = new AtomicInteger(29);

        var button = Button.newWithLabel("Hello world! 30");
        button.onClicked(() -> {
            sec.set(0); // stop the timer
            MessageDialog dialog = new MessageDialog(
                    window,
                    DialogFlags.MODAL.or(DialogFlags.DESTROY_WITH_PARENT),
                    MessageType.INFO,
                    ButtonsType.OK_CANCEL,
                    null
            );
            dialog.setTitle("Hello!");
            dialog.setMarkup("This is some <b>content</b>");
            dialog.onResponse(responseId -> {
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

        GLib.timeoutAddSeconds(1, () -> {
            button.setLabel("Hello world! " + (sec.getAndDecrement()));
            if (sec.get() == 0) {
                button.emitClicked();
            }
            return sec.get() >= 0;
        });

        box.append(button);
        window.setChild(box);

        window.show();

        window.onCloseRequest(() -> false);
        System.out.println(window.emitCloseRequest());
    }

    public Gtk4Example(String[] args) {
        app = new Application("org.gnome.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(this::activate);
        app.run(args);
    }
}
