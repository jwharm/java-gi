package test;

import io.github.jwharm.javagi.interop.ProxyFactory;
import org.gtk.gtk.*;
import org.gtk.gio.ApplicationFlags;

public class TestNotebook {

    public void printSomething(Widget source) {
        System.out.println("Event processed. Source = " + source.getClass().getSimpleName());
    }

    public void activate(org.gtk.gio.Application g_application) {
        var window = new ApplicationWindow(Application.castFrom(g_application));
        window.setTitle("Window");
        window.setDefaultSize(300, 200);

        var notebook = new Notebook();
        notebook.setTabPos(PositionType.TOP);
        notebook.onSwitchPage((source, page, pageNum) ->
                System.out.println("Switched to page " + pageNum)
        );

        notebook.appendPage(boxWithButton(window), new Label("Tab with button"));
        notebook.appendPage(boxWithTextEntry(), new Label("Tab with text entry"));
        //notebook.appendPage(boxWithDropdown(), new Label("Tab with dropdown"));
        //notebook.appendPage(boxWithList(), new Label("Tab with list"));
        //notebook.appendPage(boxWithCombobox(), new Label("Tab with combobox"));

        window.setChild(notebook);
        window.show();
    }

    private Box boxWithCombobox() {
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        ComboBox cb = ComboBox.newWithEntry();

        box.append(cb);
        return box;
    }

    private Box boxWithList() {
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        SignalListItemFactory factory = new SignalListItemFactory();
        factory.onSetup(this::setup_listitem_cb);
        factory.onBind(this::bind_listitem_cb);

        String[] strings2 = new String[] {"ListItem1", "ListItem2", "ListItem3"};
        SelectionModel model = new SingleSelection(new StringList(strings2));
        ListView lv = new ListView(model, factory);
        lv.onActivate((source, position) -> System.out.println("Position " + position + " activated!"));
        box.append(lv);
        return box;
    }

    private Box boxWithDropdown() {
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        String[] strings = new String[] {"Item1", "Item2", "Item3"};
        DropDown dd = new DropDown(strings);
        box.append(dd);
        return box;
    }

    private Box boxWithTextEntry() {
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        var entry = new Entry();
        entry.setPlaceholderText("Type something here");
        entry.onActivate(this::printSomething);
        box.append(entry);
        return box;
    }

    private Box boxWithButton(ApplicationWindow window) {
        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        var button = Button.newWithLabel("Exit");
        button.onClicked(this::printSomething);
        button.onClicked((btn) -> window.destroy());
        box.append(button);
        return box;
    }

    private void setup_listitem_cb(SignalListItemFactory signalListItemFactory, ListItem listItem) {
        Widget label = new Label("");
        listItem.setChild(label);
    }

    private void bind_listitem_cb(SignalListItemFactory signalListItemFactory, ListItem listItem) {
        Widget label = listItem.getChild();
        // Object item = listItem.getItem();
        Label.castFrom(label).setLabel("myobject getstring");
    }

    public TestNotebook(String[] args) {
        var app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(this::activate);
        app.run(args.length, args);
        app.unref();
    }

    public static void main(String[] args) throws Exception {
        TestNotebook tn = new TestNotebook(args);
        tn = null;

        byte[][] for_nothing = new byte[10][];

        for (int k = 0; k < 10 ; k ++)
            for_nothing[k] = new byte[10_000_000];
        System.gc();
    }
}
