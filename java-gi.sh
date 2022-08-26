# Clean output directories
rm -rf generated
mkdir generated generated/bin generated/lib

PACKAGE_NAME=org.gtk.interop.jextract

PKG_LIBS=`pkg-config --libs gdk-pixbuf-2.0 gtk4-unix-print gtk4`
PKG_INCLUDES=`pkg-config --cflags-only-I gdk-pixbuf-2.0 gio-unix-2.0 gtk4-unix-print gtk4`

# Generate java source files (optional)
# mkdir generated/src
# $JAVA_HOME/bin/jextract --source -d generated/src -t $PACKAGE_NAME $PKG_LIBS $PKG_INCLUDES gtk.h

# Generate java classes
$JAVA_HOME/bin/jextract -d generated/bin -t $PACKAGE_NAME $PKG_LIBS $PKG_INCLUDES gtk.h

# Create jar file
$JAVA_HOME/bin/jar cvf generated/lib/gtk-4.0.jar -C generated/bin .

# Cleanup java classes
rm -rf generated/bin/*
