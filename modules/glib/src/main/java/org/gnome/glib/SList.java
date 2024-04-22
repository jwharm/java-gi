/* Java-GI - Java language bindings for GObject-Introspection-based libraries
 * Copyright (C) 2022-2024 Jan-Willem Harmannij
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

package org.gnome.glib;

import io.github.jwharm.javagi.base.ManagedInstance;
import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.interop.Interop;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.util.AbstractSequentialList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Function;

/**
 * Java wrapper for <a href="https://docs.gtk.org/glib/struct.SList.html">GLib.SList</a>
 * that implements {@link java.util.List}.
 * <p>
 * Because SList is a singly-linked list, attempts to navigate the list
 * backwards will throw an {@code UnsupportedOperationException}.
 *
 * @param <E> The element type must be a {@link MemorySegment}, a
 *            {@link String}, or implement the {@link Proxy} interface.
 */
public class SList<E> extends AbstractSequentialList<E> implements Proxy {

    static {
        GLib.javagi$ensureInitialized();
    }

    // The Arena is used to allocate native Strings
    private final Arena arena = Arena.ofAuto();

    // Used to construct a Java instance for a native object
    private final Function<MemorySegment, E> make;

    // The current head of the SList. It is a mutable field, because add/remove
    // operations on an SList can change/remove the head
    private SListNode head;

    /**
     * Create a new {@code GLib.SList} wrapper.
     *
     * @param address the memory address of the head element of the SList
     * @param make a function to construct element instances
     */
    public SList(MemorySegment address, Function<MemorySegment, E> make) {
        this.head = MemorySegment.NULL.equals(address) ? null
                : new SListNode(address);
        this.make = make;
    }

    /**
     * Create a wrapper for a new, empty {@code GLib.SList}.
     *
     * @param make a function to construct element instances
     */
    public SList(Function<MemorySegment, E> make) {
        this(null, make);
    }

    /**
     * Returns a list iterator over the elements in this {@code GLib.SList} (in
     * proper sequence).
     * <p>
     * Because SList is a singly-linked list, the following iterator methods
     * will throw an {@code UnsupportedOperationException}:
     * <ul>
     *     <li>{@link ListIterator#hasPrevious()}
     *     <li>{@link ListIterator#previous()}
     *     <li>{@link ListIterator#previousIndex()}
     * </ul>
     *
     * @param  index index of first element to be returned from the list
     *               iterator (by a call to the {@code next} method)
     * @return a list iterator over the elements in this list (in proper
     *         sequence).
     */
    @Override
    public @NotNull ListIterator<E> listIterator(int index) {

        return new ListIterator<>() {

            /*
             * last = the SList that was returned last
             * prev = the SList that was returned before the last
             */
            private SListNode prev = null, last = null;

            private int index = -1;

            private SListNode peek() {
                return last == null ? head : last.readNext();
            }

            @Override
            public boolean hasNext() {
                return peek() != null;
            }

            @Override
            public E next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                prev = last;
                last = peek();
                index++;
                var address = last == null ? null : last.readData();
                return address == null ? null : make.apply(address);
            }

            @Override
            public int nextIndex() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return index + 1;
            }

            @Override
            public boolean hasPrevious() {
                throw new UnsupportedOperationException();
            }

            @Override
            public E previous() {
                throw new UnsupportedOperationException();
            }

            @Override
            public int previousIndex() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void remove() {
                switch(index) {
                    case -1 -> throw new IllegalStateException();
                    case  0 -> {
                        head = SListNode.deleteLink(head, last);
                        last = null;
                    }
                    case  1 -> {
                        head = SListNode.deleteLink(head, last);
                        last = head;
                    }
                    default -> {
                        prev = SListNode.deleteLink(prev, last);
                        last = prev;
                    }
                }
                index--;
            }

            @Override
            public void set(E e) {
                if (last == null)
                    throw new IllegalStateException();
                last.writeData(getAddress(e));
            }

            @Override
            public void add(E e) {
                var next = peek();
                if (index <= 0)
                    head = SListNode.insertBefore(head, next, getAddress(e));
                else {
                    prev = SListNode.insertBefore(last, next, getAddress(e));
                    last = prev.readNext();
                }
                index++;
            }
        };
    }

    /**
     * Retrieve the size of the list. This is an expensive operation for
     * long lists, because the entire length must be traversed.
     *
     * @return the length of the list
     */
    @Override
    public int size() {
        return SListNode.length(head);
    }

    /**
     * Checks if the list has no elements (the head of the list is
     * {@code null}).
     *
     * @return whether the list is empty
     */
    @Override
    public boolean isEmpty() {
        return head == null;
    }

    private MemorySegment getAddress(Object o) {
        return switch (o) {
            case MemorySegment m -> m;
            case String s -> arena.allocateFrom(s);
            case Proxy p -> p.handle();
            default -> throw new IllegalArgumentException("Not a MemorySegment, String or Proxy");
        };
    }

    /**
     * Returns the memory address of the head of the list. This address can
     * change if the list is modified.
     *
     * @return the memory address of the current GLib.SList head, or
     *         {@link MemorySegment#NULL} if the head is {@code null}.
     */
    @Override
    public MemorySegment handle() {
        return head == null ? MemorySegment.NULL : head.handle();
    }

    /**
     * Represents a GLib.SList instance in native memory
     */
    private static class SListNode extends ManagedInstance {

        /**
         * Create a SListNode proxy instance for the provided memory address.
         *
         * @param address the memory address of the native object
         */
        public SListNode(MemorySegment address) {
            super(Interop.reinterpret(address, getMemoryLayout().byteSize()));
        }

        /**
         * The memory layout of the native struct.
         *
         * @return the memory layout
         */
        public static MemoryLayout getMemoryLayout() {
            return MemoryLayout.structLayout(
                    ValueLayout.ADDRESS.withName("data"),
                    ValueLayout.ADDRESS.withName("next")
            ).withName("GSList");
        }

        /**
         * Read the value of the field {@code data}.
         *
         * @return The value of the field {@code data}
         */
        public MemorySegment readData() {
            return (MemorySegment) getMemoryLayout()
                    .varHandle(MemoryLayout.PathElement.groupElement("data")).get(handle(), 0);
        }

        /**
         * Write a value in the field {@code data}.
         *
         * @param data The new value for the field {@code data}
         */
        public void writeData(MemorySegment data) {
            getMemoryLayout().varHandle(MemoryLayout.PathElement.groupElement("data"))
                    .set(handle(), 0, (data == null ? MemorySegment.NULL : data));
        }

        /**
         * Read the value of the field {@code next}.
         *
         * @return The value of the field {@code next}
         */
        public SListNode readNext() {
            var _result = (MemorySegment) getMemoryLayout()
                    .varHandle(MemoryLayout.PathElement.groupElement("next")).get(handle(), 0);
            return MemorySegment.NULL.equals(_result) ? null : new SListNode(_result);
        }

        /**
         * Removes the node link_ from the list and frees it.
         * Compare this to g_slist_remove_link() which removes the node
         * without freeing it.
         * <p>
         * Removing arbitrary nodes from a singly-linked list requires time
         * that is proportional to the length of the list (ie. O(n)). If you
         * find yourself using g_slist_delete_link() frequently, you should
         * consider a different data structure, such as the doubly-linked
         * {@code GList}.
         *
         * @param  list a {@code GSList}
         * @param  link node to delete
         * @return the new head of {@code list}
         */
        public static SListNode deleteLink(SListNode list, SListNode link) {
            MemorySegment _result;
            try {
                FunctionDescriptor _fdesc = FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS);
                _result = (MemorySegment) Interop.downcallHandle("g_slist_delete_link", _fdesc, false)
                        .invokeExact(
                                (MemorySegment) (list == null ? MemorySegment.NULL : list.handle()),
                                (MemorySegment) (link == null ? MemorySegment.NULL : link.handle()));
            } catch (Throwable _err) {
                throw new AssertionError("Unexpected exception occurred: ", _err);
            }
            return MemorySegment.NULL.equals(_result) ? null : new SListNode(_result);
        }

        /**
         * Inserts a node before {@code sibling} containing {@code data}.
         *
         * @param  slist   a {@code GSList}
         * @param  sibling node to insert {@code data} before
         * @param  data    data to put in the newly-inserted node
         * @return the new head of the list.
         */
        public static @NotNull SListNode insertBefore(SListNode slist, SListNode sibling, @Nullable MemorySegment data) {
            MemorySegment _result;
            try {
                FunctionDescriptor _fdesc = FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS);
                _result = (MemorySegment) Interop.downcallHandle("g_slist_insert_before", _fdesc, false)
                        .invokeExact(
                                (MemorySegment) (slist == null ? MemorySegment.NULL : slist.handle()),
                                (MemorySegment) (sibling == null ? MemorySegment.NULL : sibling.handle()),
                                (MemorySegment) (data == null ? MemorySegment.NULL : data));
            } catch (Throwable _err) {
                throw new AssertionError("Unexpected exception occurred: ", _err);
            }
            return new SListNode(_result);
        }

        /**
         * Gets the number of elements in a {@code GSList}.
         * <p>
         * This function iterates over the whole list to
         * count its elements. To check whether the list is non-empty, it is faster to
         * check {@code list} against {@code null}.
         *
         * @param  list a {@code GSList}
         * @return the number of elements in the {@code GSList}
         */
        public static int length(SListNode list) {
            int _result;
            try {
                FunctionDescriptor _fdesc = FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS);
                _result = (int) Interop.downcallHandle("g_slist_length", _fdesc, false).invokeExact(
                        (MemorySegment) (list == null ? MemorySegment.NULL : list.handle()));
            } catch (Throwable _err) {
                throw new AssertionError("Unexpected exception occurred: ", _err);
            }
            return _result;
        }
    }
}
