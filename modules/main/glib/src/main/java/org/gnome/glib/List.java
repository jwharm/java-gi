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

package org.gnome.glib;

import io.github.jwharm.javagi.base.TransferOwnership;
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
 * Java wrapper for <a href="https://docs.gtk.org/glib/struct.List.html">GLib.List</a>
 * that implements {@link java.util.List}.
 *
 * @param <E> The element type must be a {@link MemorySegment}, a
 *            {@link String}, a primitive value, or implement the {@link Proxy}
 *            interface.
 */
public class List<E> extends AbstractSequentialList<E> implements Proxy {

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

    // The current head of the List. It is a mutable field, because add/remove
    // operations on an List can change/remove the head
    private ListNode head;

    // Install a cleaner when ownership is "container" (memory of item is not
    // managed) or "full"
    private final TransferOwnership ownership;

    /**
     * Create a new {@code GLib.List} wrapper.
     *
     * @param address   the memory address of the head element of the List
     * @param make      a function to construct element instances
     * @param free      a function to free element instances. If
     *                  {@code ownership} is "none" or "container", this can
     *                  safely be set to {@code null}.
     * @param ownership whether to free memory automatically
     */
    public List(MemorySegment address,
                Function<MemorySegment, E> make,
                Consumer<E> free,
                TransferOwnership ownership) {
        this.head = MemorySegment.NULL.equals(address) ? null
                : new ListNode(address);
        this.make = make;
        this.free = free;
        this.ownership = ownership;

        if (ownership != TransferOwnership.NONE) {
            var finalizer = new List.Finalizer<>(address, make, free, ownership);
            CLEANER.register(this, finalizer);
        }
    }

    /**
     * Create a wrapper for a new, empty {@code GLib.List}.
     *
     * @param make      a function to construct element instances
     * @param free      a function to free element instances. If
     *                  {@code ownership} is "none" or "container", this can
     *                  safely be set to {@code null}.
     * @param ownership whether to free memory automatically
     */
    public List(Function<MemorySegment, E> make,
                Consumer<E> free,
                TransferOwnership ownership) {
        this(null, make, free, ownership);
    }

    /**
     * Create a new {@code GLib.List} wrapper.
     *
     * @param address   the memory address of the head element of the List
     * @param make      a function to construct element instances
     * @param ownership whether to free memory automatically
     */
    public List(MemorySegment address,
                Function<MemorySegment, E> make,
                TransferOwnership ownership) {
        this(address, make, null, ownership);
    }

    /**
     * Returns a list iterator over the elements in this {@code GLib.List} (in
     * proper sequence).
     *
     * @param  index index of first element to be returned from the list
     *               iterator (by a call to the {@code next} method)
     * @return a list iterator over the elements in this list (in proper
     *         sequence).
     */
    @Override
    public @NotNull ListIterator<E> listIterator(int index) {
        return new ListIterator<>() {

            // Register the direction of the last iterator step
            enum Direction {
                FORWARD,
                BACKWARD
            }
            private Direction direction = Direction.FORWARD;

            private ListNode last = null;
            private int index = -1;

            @Override
            public boolean hasNext() {
                return last == null ? head != null : last.readNext() != null;
            }

            @Override
            public E next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                last = last == null ? head : last.readNext();
                index++;
                direction = Direction.FORWARD;
                if (last == null)
                    throw new IllegalStateException();
                return make.apply(last.readData());
            }

            @Override
            public int nextIndex() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return index + 1;
            }

            @Override
            public boolean hasPrevious() {
                return last != null && last.readPrev() != null;
            }

            @Override
            public E previous() {
                if (!hasPrevious())
                    throw new NoSuchElementException();
                last = last.readPrev();
                index--;
                direction = Direction.BACKWARD;
                if (last == null)
                    throw new IllegalStateException();
                return make.apply(last.readData());
            }

            @Override
            public int previousIndex() {
                if (!hasPrevious())
                    throw new NoSuchElementException();
                return index == -1 ? -1 : index - 1;
            }

            @Override
            public void remove() {
                if (last == null)
                    throw new IllegalStateException();
                ListNode node = last;
                switch(direction) {
                    case BACKWARD -> next();
                    case FORWARD -> previous();
                }
                head = ListNode.deleteLink(head, node);

                var data = node.readData();
                if (ownership == TransferOwnership.FULL && data != null) {
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
                if (ownership == TransferOwnership.FULL && data != null) {
                    if (free == null)
                        GLib.free(data);
                    else
                        free.accept(make.apply(data));
                }

                last.writeData(getAddress(e, arena));
            }

            @Override
            public void add(E e) {
                if (direction == Direction.BACKWARD) {
                    head = ListNode.insertBefore(head, last, getAddress(e, arena));
                    last = last.readPrev();
                } else {
                    ListNode next = last == null ? head : last.readNext();
                    head = ListNode.insertBefore(head, next, getAddress(e, arena));
                    next();
                }
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
        return ListNode.length(head);
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
     * @return the memory address of the current GLib.List head, or
     *         {@link MemorySegment#NULL} if the head is {@code null}.
     */
    @Override
    public MemorySegment handle() {
        return head == null ? MemorySegment.NULL : head.handle();
    }

    private static class ListNode extends ProxyInstance {

        /**
         * Create a ListNode proxy instance for the provided memory address.
         *
         * @param address the memory address of the native object
         */
        ListNode(MemorySegment address) {
            super(Interop.reinterpret(address, getMemoryLayout().byteSize()));
        }

        /**
         * The memory layout of the native struct.
         * @return the memory layout
         */
        static MemoryLayout getMemoryLayout() {
            return MemoryLayout.structLayout(
                    ValueLayout.ADDRESS.withName("data"),
                    ValueLayout.ADDRESS.withName("next"),
                    ValueLayout.ADDRESS.withName("prev")
            ).withName("GList");
        }

        static VarHandle DATA = getMemoryLayout().varHandle(
                MemoryLayout.PathElement.groupElement("data"));

        static VarHandle NEXT = getMemoryLayout().varHandle(
                MemoryLayout.PathElement.groupElement("next"));

        static VarHandle PREV = getMemoryLayout().varHandle(
                MemoryLayout.PathElement.groupElement("prev"));

        static MethodHandle g_list_delete_link = Interop.downcallHandle(
                "g_list_delete_link",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static MethodHandle g_list_insert_before = Interop.downcallHandle(
                "g_list_insert_before",
                FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static MethodHandle g_list_length = Interop.downcallHandle(
                "g_list_length", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS), false);

        static MethodHandle g_list_free = Interop.downcallHandle(
                "g_list_free", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS),
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
        ListNode readNext() {
            var result = (MemorySegment) NEXT.get(handle(), 0);
            return MemorySegment.NULL.equals(result)? null
                    : new ListNode(result);
        }

        /**
         * Read the value of the field {@code prev}.
         *
         * @return The value of the field {@code prev}
         */
        ListNode readPrev() {
            var result = (MemorySegment) PREV.get(handle(), 0);
            return MemorySegment.NULL.equals(result) ? null
                    : new ListNode(result);
        }

        /**
         * Removes the node link_ from the list and frees it.
         * Compare this to g_list_remove_link() which removes the node
         * without freeing it.
         *
         * @param  list a {@code GList}, this must point to the top of the list
         * @param  link node to delete from {@code list}
         * @return the (possibly changed) start of the {@code GList}
         */
        static ListNode deleteLink(ListNode list, ListNode link) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            var linkPtr = link == null ? MemorySegment.NULL : link.handle();
            try {
                MemorySegment result = (MemorySegment) g_list_delete_link
                        .invokeExact(listPtr, linkPtr);
                return MemorySegment.NULL.equals(result) ? null
                        : new ListNode(result);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }

        /**
         * Inserts a new element into the list before the given position.
         *
         * @param  list    a pointer to a {@code GList}, this must point to the
         *                 top of the list
         * @param  sibling the list element before which the new element is
         *                 inserted or {@code null} to insert at the end of the
         *                 list
         * @param  data    the data for the new element
         * @return the (possibly changed) start of the {@code GList}
         */
        static ListNode insertBefore(ListNode list,
                                            ListNode sibling,
                                            MemorySegment data) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            var sbPtr = sibling == null ? MemorySegment.NULL : sibling.handle();
            var dataPtr = data == null ? MemorySegment.NULL : data;
            try {
                MemorySegment result = (MemorySegment) g_list_insert_before
                        .invokeExact(listPtr, sbPtr, dataPtr);
                return MemorySegment.NULL.equals(result) ? null
                        : new ListNode(result);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }

        /**
         * Gets the number of elements in a {@code GList}.
         * <p>
         * This function iterates over the whole list to count its elements. Use
         * a {@code GQueue} instead of a GList if you regularly need the number
         * of items. To check whether the list is non-empty, it is faster to
         * check {@code list} against {@code null}.
         *
         * @param  list a {@code GList}, this must point to the top of the list
         * @return the number of elements in the {@code GList}
         */
        static int length(ListNode list) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            try {
                return (int) g_list_length.invokeExact(listPtr);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }

        /**
         * Frees all of the memory used by a {@code GList}. The freed elements
         * are returned to the slice allocator.
         *
         * @param list The first link of a {@code GList}.
         */
        static void free(ListNode list) {
            var listPtr = list == null ? MemorySegment.NULL : list.handle();
            try {
                g_list_free.invokeExact(listPtr);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    private record Finalizer<E>(MemorySegment address,
                                Function<MemorySegment, E> make,
                                Consumer<E> free,
                                TransferOwnership ownership) implements Runnable {
        public void run() {
            if (address == null || MemorySegment.NULL.equals(address))
                return;

            // The calls to GLib.free() and ListNode.free() must run on the
            // main thread, not in the Cleaner thread.
            SourceFunc action = () -> {
                if (ownership == TransferOwnership.FULL) {
                    var node = new ListNode(address);
                    do {
                        if (free == null)
                            GLib.free(node.readData());
                        else
                            free.accept(make.apply(node.readData()));
                        node = node.readNext();
                    } while (node != null);
                }

                ListNode.free(new ListNode(address));
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
