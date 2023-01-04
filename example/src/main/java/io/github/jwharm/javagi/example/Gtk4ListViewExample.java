package io.github.jwharm.javagi.example;

import org.gtk.gio.ApplicationFlags;
import org.gtk.gtk.*;

import java.util.*;

public class Gtk4ListViewExample {

    private final Application app;
    private final List<String> list;
    private final ListIndexModel listIndexModel;
    private final Random rnd = new Random();
    private SignalListItemFactory factory;
    private ScrolledWindow scroll;
    private ListView lv;

    public void activate() {
        var window = new ApplicationWindow(app);
        window.setTitle("Window");
        window.setDefaultSize(300, 500);

        var box = new Box(Orientation.VERTICAL, 0);

        factory = new SignalListItemFactory();
        factory.onSetup(object -> {
            ListItem listitem = (ListItem) object;
            listitem.setChild(new Label(""));
        });
        factory.onBind(object -> {
            ListItem listitem = (ListItem) object;
            Label label = (Label) listitem.getChild();
            ListIndexItem obj = (ListIndexItem) listitem.getItem();

            int index = obj.getIntValue();
            String text = list.get(index);
            label.setLabel(text);
        });

        scroll = new ScrolledWindow();
        lv = new ListView(new SingleSelection(listIndexModel), factory);
        scroll.setChild(lv);
        scroll.setVexpand(true);
        box.append(scroll);

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

    public Gtk4ListViewExample(String[] args) {
        app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);

        list = new ArrayList<>();
        int len = rnd.nextInt(900, 1000);
        for (int i = 0; i < len; i++) list.add(randomString());
        listIndexModel = new ListIndexModel(list.size());

        app.onActivate(this::activate);
        app.run(args.length, args);
    }
}
