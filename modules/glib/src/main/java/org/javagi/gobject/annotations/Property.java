/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2025 Jan-Willem Harmannij
 *
 * SPDX-License-Identifier: LGPL-2.1-or-later
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package org.javagi.gobject.annotations;

import org.gnome.gobject.ParamSpec;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The {@code @Property} annotation is used to indicate that a method (or pair
 * of methods) is a property, to set a property name and flags, or to specify
 * that a pair of get- and set-methods are not properties (using
 * {@code skip=false}).
 * <p>
 * Always set the {@code @Property} annotation with the same parameters on both
 * the get- and set-method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Property {
    String NOT_SET = "JAVA-GI-DEFAULT-PLACEHOLDER-VALUE";

    String name() default "";
    Class<? extends ParamSpec> type() default ParamSpec.class;
    boolean skip() default false;
    boolean readable() default true;
    boolean writable() default true;
    boolean construct() default false;
    boolean constructOnly() default false;
    boolean explicitNotify() default false;
    boolean deprecated() default false;
    String minimumValue() default NOT_SET;
    String maximumValue() default NOT_SET;
    String defaultValue() default NOT_SET;
}
