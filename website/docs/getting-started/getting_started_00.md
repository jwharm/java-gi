This guide is based off the "[Getting Started](https://docs.gtk.org/gtk4/getting_started.html)" guide on [docs.gtk.org](https://docs.gtk.org/). The examples have been ported to Java, and the build instructions will help you setup a Java project and build it with Gradle.

GTK is a [widget toolkit](http://en.wikipedia.org/wiki/Widget_toolkit). Each user interface created by GTK consists of widgets. This is implemented in C using {{ javadoc('GObject') }}, an object-oriented framework for C. Widgets are organized in a hierarchy. The window widget is the main container. The user interface is then built by adding buttons, drop-down menus, input fields, and other widgets to the window. If you are creating complex user interfaces it is recommended to use GtkBuilder and its GTK-specific markup description language, instead of assembling the interface manually.

GTK is event-driven. The toolkit listens for events such as a click on a button, and passes the event to your application.

This chapter contains some tutorial information to get you started with GTK programming. It assumes that you have GTK, its dependencies, a Java compiler and the Gradle build tool installed and ready to use. If you need to build GTK itself first, refer to the [Compiling the GTK libraries](https://docs.gtk.org/gtk4/building.html/building.html) section in this
reference. If you don't know how to install Java or Gradle (we use Gradle in this tutorial), just install a Java IDE and follow its instructions, or use a command-line toolkit manager such as [SDKMAN!](https://sdkman.io/).

[Next](getting_started_01.md){ .md-button }
