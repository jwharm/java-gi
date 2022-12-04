package io.github.jwharm.javagi.example;

import org.gtk.gio.ApplicationFlags;
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
                    // Using combined() would mutate these fields
                    new DialogFlags(DialogFlags.MODAL.getValue() | DialogFlags.DESTROY_WITH_PARENT.getValue()),
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
