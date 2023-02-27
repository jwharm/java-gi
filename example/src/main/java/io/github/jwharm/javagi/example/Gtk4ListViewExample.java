package io.github.jwharm.javagi.example;

import io.github.jwharm.javagi.util.ListIndexModel;
import org.gnome.gio.ApplicationFlags;
import org.gnome.gtk.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Gtk4ListViewExample extends Application {

    private final List<String> list;
    private final ListIndexModel listIndexModel;
    private final Random rnd = new Random();

    public void activate() {
        var window = new ApplicationWindow(this);
        window.setTitle("Window");
        window.setDefaultSize(300, 500);

        var box = new Box(Orientation.VERTICAL, 0);

        SignalListItemFactory factory = new SignalListItemFactory();
        factory.onSetup(object -> {
            ListItem listitem = (ListItem) object;
            listitem.setChild(new Label(""));
        });
        factory.onBind(object -> {
            ListItem listitem = (ListItem) object;
            Label label = (Label) listitem.getChild();
            ListIndexModel.ListIndex item = (ListIndexModel.ListIndex) listitem.getItem();
            if (label == null || item == null)
                return;

            int index = item.getIndex();
            String text = list.get(index);
            label.setLabel(text);
        });

        ScrolledWindow scroll = new ScrolledWindow();
        ListView lv = new ListView(new SingleSelection(listIndexModel), factory);
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
        super("org.gnome.gtk.example", ApplicationFlags.FLAGS_NONE);

        list = new ArrayList<>();
        for (int i = 0, len = rnd.nextInt(400, 500); i < len; i++) list.add(randomString());
        listIndexModel = ListIndexModel.withSize(list.size());

        onActivate(this::activate);
        run(args);
    }
}
