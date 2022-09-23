#!/usr/bin/env bash

PACKAGE_NAME=io.github.jwharm.javagi.interop.jextract

# Clean output directories
rm -rf generated
mkdir generated generated/bin generated/src generated/lib

PKG_LIBS=`pkg-config --libs gdk-pixbuf-2.0 gtk4-unix-print gtk4 libadwaita-1`
PKG_INCLUDES=`pkg-config --cflags-only-I gdk-pixbuf-2.0 gio-unix-2.0 gtk4-unix-print gtk4 libadwaita-1`

# Generate java classes
jextract --output generated/bin -t $PACKAGE_NAME $PKG_LIBS $PKG_INCLUDES gtk.h
# Generate java source files
# $JAVA_HOME/bin/jextract --output generated/src -t $PACKAGE_NAME $PKG_LIBS $PKG_INCLUDES gtk.h --source

# Create jar file
jar cvf generated/lib/gtk-4.0.jar -C generated/bin .
# Create sources zip file
# $JAVA_HOME/bin/jar cvf generated/lib/gtk-4.0-src.zip -C generated/src .

# Cleanup java classes
rm -rf generated/bin
rm -rf generated/src
