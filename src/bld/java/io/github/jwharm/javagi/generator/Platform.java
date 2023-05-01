package io.github.jwharm.javagi.generator;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.model.Namespace;

public enum Platform {
    WINDOWS("windows"), MAC("macos"), LINUX("linux");

    public final String name;

    Platform(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
