/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2023 Jan-Willem Harmannij
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

package io.github.jwharm.javagi.annotations;

import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.github.jwharm.javagi.types.Types;

/**
 * Register the annotated type as a GObject signal on the enclosing class.
 * <p>
 * The signal declaration must be a functional interface. Example usage:
 * 
 * <pre>{@code
 * 
 * public class Example extends GObject {
 *     @Signal
 *     public interface ExampleSignal {
 *         public boolean run();
 *     }
 * }
 * }</pre>
 * 
 * Alternatively, extend an existing functional interface:
 * 
 * <pre>{@code
 * 
 * public class Example extends GObject {
 *     @Signal
 *     public interface ExampleSignal extends BooleanSupplier {};
 * }
 * }</pre>
 * 
 * When the enclosing class is registered as a GObject using
 * {@link Types#register()}, the signal will be registered for that class, using
 * the parameters of the annotated type.
 * <p>
 * The default name of the signal is derived from the type name, translating
 * camel-case ("ExampleSignal") to kebab-case ("example-signal"). It is possible
 * to override this with the {@link #name()} parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(TYPE)
public @interface Signal {
    /**
     * The name of the signal. The default name is translated from the type name.
     */
    String name() default "";

    /**
     * Invoke the object method handler in the first emission stage.
     */
    boolean runFirst() default false;

    /**
     * Invoke the object method handler in the third emission stage.
     */
    boolean runLast() default false;

    /**
     * Invoke the object method handler in the last emission stage.
     */
    boolean runCleanup() default false;

    /**
     * Signals being emitted for an object while currently being in emission for
     * this very object will not be emitted recursively, but instead cause the first
     * emission to be restarted.
     */
    boolean noRecurse() default false;

    /**
     * This signal supports “::detail” appendices to the signal name upon handler
     * connections and emissions.
     */
    boolean detailed() default false;

    /**
     * Action signals are signals that may freely be emitted on alive objects from
     * user code via {@code g_signal_emit()} and friends, without the need of being
     * embedded into extra code that performs pre or post emission adjustments on
     * the object. They can also be thought of as object methods which can be called
     * generically by third-party code.
     */
    boolean action() default false;

    /**
     * No emissions hooks are supported for this signal.
     */
    boolean noHooks() default false;

    /**
     * Varargs signal emission will always collect the arguments, even if there are
     * no signal handlers connected.
     * 
     * @since 2.30
     */
    boolean mustCollect() default false;

    /**
     * The signal is deprecated and will be removed in a future version. A warning
     * will be generated if it is connected while running with
     * G_ENABLE_DIAGNOSTIC=1.
     * 
     * @since 2.32
     */
    boolean deprecated() default false;
}
