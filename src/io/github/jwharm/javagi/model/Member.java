package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;

public class Member extends GirElement {

    String name;
    int value;

    public Member(GirElement parent, String name, String value) {
        super(parent);
        if (name != null) {
            this.name = Conversions.prefixDigits(name);
        }
        try {
            this.value = Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            System.err.println("Error in <member name=\"" + name + "\": Not an integer: " + value);
        }
    }
}
