package util;

import org.gtk.gio.ListModel;
import org.gtk.gtk.*;
import org.gtk.gio.ApplicationFlags;

public class HelloWorld {

    public void printSomething(Widget source) {
        System.out.println("Event processed. Source = " + source.getClass().getSimpleName());
    }

    public void activate(org.gtk.gio.Application g_application) {
        var window = new ApplicationWindow(Application.castFrom(g_application));
        window.setTitle("Window");
        window.setDefaultSize(300, 200);

        var notebook = new Notebook();
        notebook.setTabPos(PositionType.TOP);

        var box = new Box(Orientation.VERTICAL, 0);
        box.setHalign(Align.CENTER);
        box.setValign(Align.CENTER);

        var button = Button.newWithLabel("Exit");
        button.onClicked(this::printSomething);
        button.onClicked((btn) -> window.destroy());
        box.append(button);
        notebook.appendPage(box, new Label("Tab with button"));

        var box2 = new Box(Orientation.VERTICAL, 0);
        box2.setHalign(Align.CENTER);
        box2.setValign(Align.CENTER);

        var entry = new Entry();
        entry.setPlaceholderText("Type something here");
        entry.onActivate(this::printSomething);
        box2.append(entry);
        notebook.appendPage(box2, new Label("Tab with text entry"));

        var box3 = new Box(Orientation.VERTICAL, 0);
        box3.setHalign(Align.CENTER);
        box3.setValign(Align.CENTER);

        String[] strings = new String[] {"Item1", "Item2", "Item3"};
        DropDown dd = new DropDown(strings);
        box3.append(dd);
        notebook.appendPage(box3, new Label("Tab with dropdown"));

        notebook.onSwitchPage((source, page, pageNum) ->
                System.out.println("Switched to page " + pageNum)
        );

        var box4 = new Box(Orientation.VERTICAL, 0);
        box4.setHalign(Align.CENTER);
        box4.setValign(Align.CENTER);

        SignalListItemFactory factory = new SignalListItemFactory();
        factory.onSetup(this::setup_listitem_cb);
        factory.onBind(this::bind_listitem_cb);

        String[] strings2 = new String[] {"ListItem1", "ListItem2", "ListItem3"};
        ListModel stringList = ListModel.castFrom(new StringList(strings2));
        SelectionModel model = SelectionModel.castFrom(new SingleSelection(stringList));
        ListView lv = new ListView(model, factory);
        lv.onActivate((source, position) -> System.out.println("Position " + position + " activated!"));
        box4.append(lv);
        notebook.appendPage(box4, new Label("Tab with list"));

        window.setChild(notebook);
        window.show();
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

    public HelloWorld(String[] args) {
        var app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);
        app.onActivate(this::activate);
        app.run(args.length, args);
        app.unref();
    }

    public static void main(String[] args) {
        new HelloWorld(args);
    }
}
