package io.github.jwharm.javagi.generator;

public enum Platform {
    WINDOWS("windows"), MACOS("macos"), LINUX("linux");

    public final String name;

    Platform(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
