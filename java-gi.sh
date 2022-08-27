#!/usr/bin/env bash

PACKAGE_NAME=org.gtk.interop.jextract

# Clean output directories
rm -rf generated
mkdir generated generated/bin generated/src generated/lib

PKG_LIBS=`pkg-config --libs gdk-pixbuf-2.0 gtk4-unix-print gtk4`
PKG_INCLUDES=`pkg-config --cflags-only-I gdk-pixbuf-2.0 gio-unix-2.0 gtk4-unix-print gtk4`

# Generate java source files and generate java classes
$JAVA_HOME/bin/jextract --source -d generated/src -t $PACKAGE_NAME $PKG_LIBS $PKG_INCLUDES gtk.h
$JAVA_HOME/bin/jextract -d generated/bin -t $PACKAGE_NAME $PKG_LIBS $PKG_INCLUDES gtk.h

# Create jar file and sources jar file
$JAVA_HOME/bin/jar cvf generated/lib/gtk-4.0.jar -C generated/bin .
$JAVA_HOME/bin/jar cvf generated/lib/gtk-4.0-src.zip -C generated/src .

# Cleanup java classes
rm -rf generated/bin
rm -rf generated/src
