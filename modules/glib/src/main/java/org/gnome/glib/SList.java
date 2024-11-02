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

import io.github.jwharm.javagi.base.Proxy;
import io.github.jwharm.javagi.base.ProxyInstance;
import io.github.jwharm.javagi.interop.Interop;
import org.jetbrains.annotations.NotNull;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.util.AbstractSequentialList;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;

import static io.github.jwharm.javagi.interop.Interop.getAddress;

/**
 * Java wrapper for <a href="https://docs.gtk.org/glib/struct.SList.html">GLib.SList</a>
 * that implements {@link java.util.List}.
 * <p>
 * Because SList is a singly-linked list, attempts to navigate the list
 * backwards will throw an {@code UnsupportedOperationException}.
 *
 * @param <E> The element type must be a {@link MemorySegment}, a
 *            {@link String}, a primitive value, or implement the {@link Proxy}
 *            interface.
 */
public class SList<E> extends AbstractSequentialList<E> implements Proxy {

    static {
        GLib.javagi$ensureInitialized();
    }

    // Used to dispose the list and, optionally, its items
    private static final Cleaner CLEANER = Cleaner.create();

    // The Arena is used to allocate native Strings
    private final Arena arena = Arena.ofAuto();

    // Used to construct a Java instance for a native object
    private final Function<MemorySegment, E> make;

    // Used to free a removed object
    private final Consumer<E> free;

    // The current head of the SList. It is a mutable field, because add/remove
    // operations on an SList can change/remove the head
    private SListNode head;

    // Ownership is "container" (memory of item is not managed) or "full"
    private final boolean fullOwnership;

    /**
     * Create a new {@code GLib.SList} wrapper.
     *
     * @param address       the memory address of the head element of the SList
     * @param make          a function to construct element instances
     * @param free          a function to free element instances. If
     *                      {@code fullOwnership} is {@code false}, this can
     *                      safely be set to {@code null}.
     * @param fullOwnership whether to free element instances automatically
     */
    public SList(MemorySegment address,
                 Function<MemorySegment, E> make,
                 Consumer<E> free,
                 boolean fullOwnership) {
        this.head = MemorySegment.NULL.equals(address) ? null
                : new SListNode(address);
        this.make = make;
        this.free = free;
        this.fullOwnership = fullOwnership;

        var finalizer = new Finalizer<>(address, make, free, fullOwnership);
        CLEANER.register(this, finalizer);
    }

    /**
     * Create a wrapper for a new, empty {@code GLib.SList}.
     *
     * @param make          a function to construct element instances
     * @param free          a function to free element instances. If
     *                      {@code fullOwnership} is {@code false}, this can
     *                      safely be set to {@code null}.
     * @param fullOwnership whether to free element instances automatically
     */
    public SList(Function<MemorySegment, E> make,
                 Consumer<E> free,
                 boolean fullOwnership) {
        this(null, make, free, fullOwnership);
    }

    /**
     * Create a new {@code GLib.SList} wrapper.
     *
     * @param address       the memory address of the head element of the SList
     * @param make          a function to construct element instances
     * @param fullOwnership whether to free element instances automatically
     *                      with {@link GLib#free}
     */
    public SList(MemorySegment address,
                 Function<MemorySegment, E> make,
                 boolean fullOwnership) {
        this(address, make, null, fullOwnership);
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
                var data = last == null ? null : last.readData();
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

                if (fullOwnership && data != null) {
                    if (free == null)
                        GLib.free(data);
                    else
                        free.accept(make.apply(data));
                }
            }

            @Override
            public void set(E e) {
                if (last == null)
                    throw new IllegalStateException();

                var data = last.readData();
                if (fullOwnership && data != null) {
                    if (free == null)
                        GLib.free(data);
                    else
                        free.accept(make.apply(data));
                }

                last.writeData(getAddress(e, arena));
            }

            @Override
            public void add(E e) {
                var next = peek();
                if (index <= 0)
                    head = SListNode.insertBefore(head, next, getAddress(e, arena));
                else {
                    prev = SListNode.insertBefore(last, next, getAddress(e, arena));
                    if (prev == null)
                        throw new IllegalStateException();
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
    private static class SListNode extends ProxyInstance {

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
        static MemoryLayout getMemoryLayout() {
            return MemoryLayout.structLayout(
                    ValueLayout.ADDRESS.withName("data"),
                    ValueLayout.ADDRESS.withName("next")
            ).withName("GSList");
        }

        static VarHandle DATA = getMemoryLayout().varHandle(
                MemoryLayout.PathElement.groupElement("data"));

        static VarHandle NEXT = getMemoryLayout().varHandle(
                MemoryLayout.PathElement.groupElement("next"));

        static MethodHandle g_slist_delete_link = Interop.downcallHandle(
                "g_slist_delete_link",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static MethodHandle g_slist_insert_before = Interop.downcallHandle(
                "g_slist_insert_before",
                FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static MethodHandle g_slist_length = Interop.downcallHandle(
                "g_slist_length", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS), false);

        static MethodHandle g_slist_free = Interop.downcallHandle(
                "g_slist_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
                false);

        /**
         * Read the value of the field {@code data}.
         *
         * @return The value of the field {@code data}
         */
        MemorySegment readData() {
            return (MemorySegment) DATA.get(handle(), 0);
        }

        /**
         * Write a value in the field {@code data}.
         *
         * @param data The new value for the field {@code data}
         */
        void writeData(MemorySegment data) {
            DATA.set(handle(), 0, (data == null ? MemorySegment.NULL : data));
        }

        /**
         * Read the value of the field {@code next}.
         *
         * @return The value of the field {@code next}
         */
        SListNode readNext() {
            var result = (MemorySegment) NEXT.get(handle(), 0);
            return MemorySegment.NULL.equals(result) ? null
                    : new SListNode(result);
        }

        /**
         * Removes the node link_ from the list and frees it. Compare this to
         * g_slist_remove_link() which removes the node without freeing it.
         * <p>
         * Removing arbitrary nodes from a singly-linked list requires time that
         * is proportional to the length of the list (ie. O(n)). If you find
         * yourself using g_slist_delete_link() frequently, you should consider
         * a different data structure, such as the doubly-linked {@code GList}.
         *
         * @param  list a {@code GSList}
         * @param  link node to delete
         * @return the new head of {@code list}
         */
        static SListNode deleteLink(SListNode list, SListNode link) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            var linkPtr = link == null ? MemorySegment.NULL : link.handle();
            try {
                MemorySegment result = (MemorySegment) g_slist_delete_link
                        .invokeExact(listPtr, linkPtr);
                return MemorySegment.NULL.equals(result) ? null
                        : new SListNode(result);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }

        /**
         * Inserts a node before {@code sibling} containing {@code data}.
         *
         * @param  slist   a {@code GSList}
         * @param  sibling node to insert {@code data} before
         * @param  data    data to put in the newly-inserted node
         * @return the new head of the list.
         */
        static SListNode insertBefore(SListNode slist,
                                      SListNode sibling,
                                      MemorySegment data) {
            var listPtr = slist == null ? MemorySegment.NULL : slist.handle();
            var sbPtr = sibling == null ? MemorySegment.NULL : sibling.handle();
            var dataPtr = data == null ? MemorySegment.NULL : data;
            try {
                MemorySegment result = (MemorySegment) g_slist_insert_before
                        .invokeExact(listPtr, sbPtr, dataPtr);
                return MemorySegment.NULL.equals(result) ? null
                        : new SListNode(result);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }

        /**
         * Gets the number of elements in a {@code GSList}.
         * <p>
         * This function iterates over the whole list to count its elements. To
         * check whether the list is non-empty, it is faster to check
         * {@code list} against {@code null}.
         *
         * @param  list a {@code GSList}
         * @return the number of elements in the {@code GSList}
         */
        static int length(SListNode list) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            try {
                return (int) g_slist_length.invokeExact(listPtr);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }

        /**
         * Frees all of the memory used by a {@code GSList}. The freed elements
         * are returned to the slice allocator.
         *
         * @param list The first link of a {@code GSList}.
         */
        static void free(SListNode list) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            try {
                g_slist_free.invokeExact(listPtr);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    private record Finalizer<E>(MemorySegment address,
                                Function<MemorySegment, E> make,
                                Consumer<E> free,
                                boolean fullOwnership) implements Runnable {
        public void run() {
            if (address == null || MemorySegment.NULL.equals(address))
                return;

            // The calls to GLib.free() and SListNode.free() must run on the
            // main thread, not in the Cleaner thread.
            SourceFunc action = () -> {
                if (fullOwnership) {
                    var node = new SListNode(address);
                    do {
                        if (free == null)
                            GLib.free(node.readData());
                        else
                            free.accept(make.apply(node.readData()));
                        node = node.readNext();
                    } while (node != null);
                }

                SListNode.free(new SListNode(address));
                return GLib.SOURCE_REMOVE;
            };

            var mainContext = MainContext.default_();
            if (mainContext != null)
                mainContext.invoke(action);
            else
                action.run();
        }
    }
}
