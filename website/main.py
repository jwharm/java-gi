def define_env(env):
    "Hook function"

    urls = { 'GLib': 'https://docs.gtk.org/glib/',
             'GObject': 'https://docs.gtk.org/gobject/',
             'GModule': 'https://docs.gtk.org/gmodule/',
             'GIO': 'https://docs.gtk.org/gio/',
             'Gsk': 'https://docs.gtk.org/gsk4/',
             'Gdk': 'https://docs.gtk.org/gdk4/',
             'Pango': 'https://docs.gtk.org/Pango/',
             'GdkPixbuf': 'https://docs.gtk.org/gdk-pixbuf/',
             'Gtk': 'https://docs.gtk.org/gtk4/'
           }

    @env.macro
    def doc(docstr):
        ns = get_ns(docstr)
        name = docstr.split(ns + '.')[1]
        url = urls[ns] + get_type(docstr) + '.' + name
        return f'[{name}]({url})'

    def get_ns(docstr):
        name = docstr.split('@')[1]
        return name.split('.')[0]

    def get_type(docstr):
        return docstr.split('@')[0]

    @env.macro
    def javadoc(docstr):
        url = 'https://jwharm.github.io/java-gi/javadoc/search.html'
        return f'[{docstr}]({url}?q={docstr}&r=1)'

