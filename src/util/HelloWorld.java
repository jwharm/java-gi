package util;

//import jdk.incubator.foreign.MemorySegment;
//import org.gtk.gobject.GType;
//import org.gtk.gobject.Value;
//import org.gtk.gtk.*;
//import org.gtk.gio.ApplicationFlags;
//import org.gtk.interop.Interop;
//
//import static org.gtk.interop.jextract.gtk_h.*;

public class HelloWorld {

//    public void printSomething(Widget source) {
//        System.out.println("Event processed. Source = " + source.getClass().getSimpleName());
//    }
//
//    public void activate(org.gtk.gio.Application g_application) {
//        var window = new ApplicationWindow(Application.castFrom(g_application));
//        window.setTitle("Window");
//        window.setDefaultSize(300, 200);
//
//        var notebook = new Notebook();
//        notebook.setTabPos(PositionType.TOP);
//
//        var box = new Box(Orientation.VERTICAL, 0);
//        box.setHalign(Align.CENTER);
//        box.setValign(Align.CENTER);
//
//        var button = Button.newWithLabel("Exit");
//        button.onClicked(this::printSomething);
//        button.onClicked((btn) -> window.destroy());
//        box.append(button);
//        notebook.appendPage(box, new Label("Tab with button"));
//
//        var box2 = new Box(Orientation.VERTICAL, 0);
//        box2.setHalign(Align.CENTER);
//        box2.setValign(Align.CENTER);
//
//        var entry = new Entry();
//        entry.setPlaceholderText("Type something here");
//        entry.onActivate(this::printSomething);
//        box2.append(entry);
//        notebook.appendPage(box2, new Label("Tab with text entry"));
//
//        var box3 = new Box(Orientation.VERTICAL, 0);
//        box3.setHalign(Align.CENTER);
//        box3.setValign(Align.CENTER);
//
//        String[] strings = new String[] {"Item1", "Item2", "Item3"};
//        DropDown dd = new DropDown(strings);
//        box3.append(dd);
//        notebook.appendPage(box3, new Label("Tab with dropdown"));
//
//        notebook.onSwitchPage((source, page, pageNum) ->
//                System.out.println("Switched to page " + pageNum)
//        );
//
//        GType[] types = new GType[] {
//                new GType(G_TYPE_STRING()),
//                new GType(G_TYPE_STRING()),
//                new GType(G_TYPE_LONG())
//        };
//        ListStore store = ListStore.newv(3, types);
//        TreeIter treeIter = new TreeIter();
////        var iterSeg = MemorySegment.allocateNative(C_POINTER, Interop.getScope());
////        gtk_list_store_append(store.HANDLE(), iterSeg.address());
//        store.append(treeIter);
//
//        Value v1 = new Value();
//        v1.init(GType.STRING);
//        v1.setString("test1");
//        Value v2 = new Value();
//        v2.init(GType.STRING);
//        v2.setString("test2");
//        Value v3 = new Value();
//        v3.init(GType.LONG);
//        v3.setLong(10000L);
//
//        String s1 = v1.getString();
//        System.out.println(s1);
//
//        Value[] array1 = new Value[] {v1, v2, v3};
//
//        store.setValuesv(treeIter,
//                new int[] {0, 1, 2},
//                array1,
//                3);
//
//        var box4 = new Box(Orientation.VERTICAL, 0);
//        box4.setHalign(Align.CENTER);
//        box4.setValign(Align.CENTER);
//
//        TreeView treeView = new TreeView();
//        //treeView.setModel(store);
//        ScrolledWindow scrolledWindow = new ScrolledWindow();
//        scrolledWindow.setChild(treeView);
//        scrolledWindow.setPolicy(PolicyType.ALWAYS, PolicyType.ALWAYS);
//
//        box4.append(scrolledWindow);
//        notebook.appendPage(box4, new Label("Tab with list"));
//
//
//        window.setChild(notebook);
//        window.show();
//    }
//
//    public HelloWorld(String[] args) {
//        var app = new Application("org.gtk.example", ApplicationFlags.FLAGS_NONE);
//        app.onActivate(this::activate);
//        app.run(args.length, args);
//        app.unref();
//    }
//
//    public static void main(String[] args) {
//        new HelloWorld(args);
//    }
}
