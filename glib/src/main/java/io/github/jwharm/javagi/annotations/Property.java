package io.github.jwharm.javagi.annotations;

import org.gnome.gobject.ParamSpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Property {
    String name();
    Class<? extends ParamSpec> type() default ParamSpec.class;
    boolean readable() default true;
    boolean writable() default true;
    boolean construct() default false;
    boolean constructOnly() default false;
    boolean explicitNotify() default false;
    boolean deprecated() default false;
}
