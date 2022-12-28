package io.github.jwharm.javagi.example;

import org.gtk.gio.ApplicationFlags;
import org.gtk.glib.GLib;
import org.gtk.gtk.*;

import java.util.*;

public class Gtk4Example {

    private static final Queue<Runnable> SCHEDULED = new ArrayDeque<>();

    public static void schedule(Runnable task) {
        SCHEDULED.add(Objects.requireNonNull(task));
    }


    private final Application app;
    private final List<String> list;
    private final ListIndex listIndex;
    private final Random rnd = new Random();

    public void activate() {
        var window = new ApplicationWindow(app);
        window.setTitle("Window");
        window.setDefaultSize(300, 200);

        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        var button = Button.newWithLabel("Hello world! 30");
        button.onClicked(() -> {
            list.clear();
            int len = rnd.nextInt(100, 1000);
            for (int i = 0; i < len; i++) list.add(randomString());
            listIndex.setSize(list.size());
        });

        box.append(button);

        box.append(new ListView(listIndex.inSelectionModel(), new CListEntryFactory(list)));

        window.setChild(box);
        window.show();
    }

    private String randomString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = rnd.nextInt(5, 10); i < len; i++) {
            sb.append((char) rnd.nextInt('a', 'z' + 1));
        }
        return sb.toString();
    }

    private static class CListEntryFactory extends SignalListItemFactory {
        public CListEntryFactory(List<String> entries) {
            onSetup(object -> {
                ListItem.castFrom(object).setChild(new Label(""));
            });
            onBind(object -> {
                ListItem item = ListItem.castFrom(object);
                String text = entries.get(ListIndex.toIndex(item));
                Label.castFrom(Objects.requireNonNull(item.getChild())).setLabel(text);
            });
        }
    }

    public Gtk4Example(String[] args) {
        app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);

        GLib.idleAdd(() -> {
            Runnable r;
            while ((r = SCHEDULED.poll()) != null) {
                try {
                    r.run();
                } catch (Throwable t) {
                    System.err.println("Could not run scheduled task");
                    t.printStackTrace();
                }
            }
            return true;
        });

        list = new ArrayList<>();
        listIndex = new ListIndex();

        app.onActivate(this::activate);
        app.run(args.length, args);
    }
}
