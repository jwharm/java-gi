package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.generator.Conversions;
import io.github.jwharm.javagi.generator.Numbers;

public class Member extends GirElement {

    public final String cIdentifier;
    final int value;
    final boolean usable;

    public Member(GirElement parent, String name, String cIdentifier, String value) {
        super(parent);
        this.cIdentifier = cIdentifier;
        if (name != null) {
            this.name = Conversions.prefixDigits(name);
        }
        int v;
        boolean u = true;
        try {
            v = Numbers.parseInt(value);
        } catch (NumberFormatException nfe) {
            v = 0;
            u = false;
            System.out.println("Skipping <member name=\"" + name + "\""
                    + " c:identifier=\"" + cIdentifier + "\""
                    + " value=\"" + value + "\""
                    + ">: Not an integer");
        }
        this.value = v;
        this.usable = u;
    }

    /**
     * Compare equality with another member of the same structure
     * @param other Another member
     * @return true iff both members share the same parent and have equal names
     */
    public boolean equals(Member other) {
        return (parent == other.parent) && name.equals(other.name);
    }
}
