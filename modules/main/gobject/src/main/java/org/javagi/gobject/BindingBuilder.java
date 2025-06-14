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

package org.javagi.gobject;

import org.gnome.gobject.*;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Function;

import static org.gnome.gobject.BindingFlags.*;

/**
 * Builder class to setup a GObject property binding.
 *
 * @param <S> type of the source property
 * @param <T> type of the target property
 *
 * @see GObject#bindProperty(String, GObject, String)
 */
public class BindingBuilder<S, T> {
    private GObject source;
    private String sourceProperty;
    private GObject target;
    private String targetProperty;
    private Function<S, T> transformTo;
    private Function<T, S> transformFrom;
    private final Set<BindingFlags> flags = EnumSet.noneOf(BindingFlags.class);

    /**
     * Create a BindingBuilder to construct a GObject property binding.
     *
     * @see Binding
     */
    public BindingBuilder() {
    }

    /**
     * Create a BindingBuilder to construct a GObject property binding.
     *
     * @param source         the source {@code GObject}
     * @param sourceProperty the property on the source object to bind
     * @param target         the target {@code GObject}
     * @param targetProperty the property on {@code target} to bind
     *
     * @see Binding
     */
    public BindingBuilder(GObject source, String sourceProperty, GObject target, String targetProperty) {
        this.source = source;
        this.sourceProperty = sourceProperty;
        this.target = target;
        this.targetProperty = targetProperty;
    }

    /**
     * Set the source {@code GObject}
     */
    public BindingBuilder<S, T> source(GObject sourceObject) {
        this.source = sourceObject;
        return this;
    }

    /**
     * Set the property on the source object to bind
     */
    public BindingBuilder<S, T> sourceProperty(String sourceProperty) {
        this.sourceProperty = sourceProperty;
        return this;
    }

    /**
     * Set the target {@code GObject}
     */
    public BindingBuilder<S, T> target(GObject targetObject) {
        this.target = targetObject;
        return this;
    }

    /**
     * the property on {@code target} to bind
     */
    public BindingBuilder<S, T> targetProperty(String targetProperty) {
        this.targetProperty = targetProperty;
        return this;
    }

    /**
     * Bidirectional binding; if either the
     *   property of the source or the property of the target changes,
     *   the other is updated.
     */
    public BindingBuilder<S, T> bidirectional() {
        flags.add(BIDIRECTIONAL);
        return this;
    }

    /**
     * Synchronize the values of the source and
     *   target properties when creating the binding; the direction of
     *   the synchronization is always from the source to the target.
     */
    public BindingBuilder<S, T> syncCreate() {
        flags.add(SYNC_CREATE);
        return this;
    }

    /**
     * If the two properties being bound are
     *   booleans, setting one to {@code true} will result in the other being
     *   set to {@code false} and vice versa. This flag will only work for
     *   boolean properties, and cannot be used when passing custom
     *   transformation functions.
     */
    public BindingBuilder<S, T> invertBoolean() {
        flags.add(INVERT_BOOLEAN);
        return this;
    }

    /**
     * Set the transformation function from this GObject to the {@code target},
     * or {@code null} to use the default.
     */
    public BindingBuilder<S, T> transformTo(Function<S, T> transformTo) {
        this.transformTo = transformTo;
        return this;
    }

    /**
     * Set the transformation function from the {@code target} to this GObject,
     * or {@code null} to use the default
     */
    public BindingBuilder<S, T> transformFrom(Function<T, S> transformFrom) {
        this.transformFrom = transformFrom;
        return this;
    }

    /**
     * Create the property binding.
     */
    public Binding build() {
        if (flags.contains(INVERT_BOOLEAN)
                && (transformFrom != null || transformTo != null)) {
            throw new IllegalArgumentException("The INVERT_BOOLEAN flag cannot "
                    + "be used when passing custom transformation functions");
        }

        BindingTransformFunc to = transformTo == null ? null : this::applyTransformTo;
        BindingTransformFunc from = transformFrom == null ? null : this::applyTransformFrom;

        Binding binding = source.bindPropertyFull(sourceProperty, target,
                targetProperty, flags, to, from);

        /*
         * The binding is automatically removed when the Binding object is
         * destroyed, which means it cannot be discarded by the user. Increase
         * the refcount to fix this.
         */
        binding.ref();

        return binding;
    }

    @SuppressWarnings("unchecked") // Catches and handles ClassCaseException
    private boolean applyTransformTo(Binding binding, Value from, Value to) {
        S s;
        try {
            s = (S) ValueUtil.valueToObject(from);
        } catch (ClassCastException cce) {
            s = null;
        }

        if (s == null)
            return false;

        ValueUtil.objectToValue(transformTo.apply(s), to);
        return true;
    }

    @SuppressWarnings("unchecked") // Catches and handles ClassCaseException
    private boolean applyTransformFrom(Binding binding, Value from, Value to) {
        T t;
        try {
            t = (T) ValueUtil.valueToObject(from);
        } catch (ClassCastException cce) {
            t = null;
        }

        if (t == null)
            return false;

        return ValueUtil.objectToValue(transformFrom.apply(t), to);
    }
}
