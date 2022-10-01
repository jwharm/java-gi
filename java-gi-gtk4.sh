#!/usr/bin/env bash

/opt/jdk-19/bin/java --enable-preview -classpath generated/lib/gtk4-jextract.jar:out io.github.jwharm.javagi.JavaGI input.xml ../java-gtk4/src/main/java/

