module org.javagi.generator {
    requires java.compiler;
    requires java.logging;
    requires java.xml;
    requires com.squareup.javapoet;
    requires info.picocli;
    requires org.jspecify;
    exports org.javagi;
    exports org.javagi.configuration;
    exports org.javagi.generators;
    exports org.javagi.gir;
    exports org.javagi.patches;
    exports org.javagi.util;
    opens org.javagi to info.picocli;
}
