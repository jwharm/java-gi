package io.github.jwharm.javagi.model;

import io.github.jwharm.javagi.JavaGI;
import io.github.jwharm.javagi.generator.Conversions;

public class Member extends GirElement {

    public String cIdentifier;
    int value;

    public Member(GirElement parent, String name, String cIdentifier, String value) {
        super(parent);
        this.cIdentifier = cIdentifier;
        if (name != null) {
            this.name = Conversions.prefixDigits(name);
        }
        try {
            this.value = Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            if (JavaGI.DISPLAY_WARNINGS) {
                System.out.println("Skipping <member name=\"" + name + "\"" 
                        + " c:identifier=\"" + cIdentifier + "\"" 
                        + " value=\"" + value + "\"" 
                        + ">: Not an integer");
            }
        }
    }
}
