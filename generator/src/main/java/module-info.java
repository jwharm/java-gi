module io.github.jwharm.javagi.generator {
    requires java.compiler;
    requires java.xml;
    requires com.squareup.javapoet;
    requires org.jetbrains.annotations;
    exports io.github.jwharm.javagi;
    exports io.github.jwharm.javagi.configuration;
    exports io.github.jwharm.javagi.generators;
    exports io.github.jwharm.javagi.gir;
    exports io.github.jwharm.javagi.patches;
    exports io.github.jwharm.javagi.util;
}
