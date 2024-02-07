package io.github.jwharm.javagi.generators;

import io.github.jwharm.javagi.configuration.ModuleInfo;
import io.github.jwharm.javagi.gir.Include;
import io.github.jwharm.javagi.gir.Namespace;

import java.util.Set;

public class ModuleInfoGenerator {

    private final Namespace ns;
    private final Set<String> packageNames;
    private final StringBuilder builder;

    public ModuleInfoGenerator(Namespace ns, Set<String> packageNames) {
        this.ns = ns;
        this.packageNames = packageNames;
        this.builder = new StringBuilder();
    }

    public String generate() {
        builder.append("""
                module %s {
                    requires org.jetbrains.annotations;
                """.formatted(ns.packageName()));

        ns.parent().includes().stream()
                .map(Include::name)
                .map(ModuleInfo::getPackageName)
                // A minimal set of FreeType bindings is included in the Cairo module
                .map(name -> name.replace("org.freedesktop.freetype", "org.freedesktop.cairo"))
                .forEach(this::requires);

        exports(ns.packageName());
        packageNames.forEach(this::exports);

        builder.append("}\n");
        return builder.toString();
    }

    private void requires(String module) {
        builder.append("    requires transitive ").append(module).append(";\n");
    }

    private void exports(String packageName) {
        builder.append("    exports ").append(packageName).append(";\n");
    }
}
