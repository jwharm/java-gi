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

import org.javagi.base.Out;
import org.javagi.base.Proxy;
import org.javagi.interop.Interop;
import org.javagi.interop.MemoryCleaner;
import java.lang.Integer;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.javagi.base.TransferOwnership.*;
import static org.javagi.interop.Interop.getAddress;
import static java.lang.foreign.MemorySegment.NULL;

/**
 * The {@code GHashTable} struct is an opaque data structure to represent a
 * hash table. The keys and values of the Java class can be pointers
 * ({@link MemorySegment} objects), strings, primitive values or native objects
 * (implementing the {@link Proxy} interface).
 * <p>
 * This class is intended to help Java developers deal with native functions
 * that require or return a GHashTable. It is not meant to be used as a
 * replacement for Java's own HashMap.
 */
public class HashTable<K,V> extends AbstractMap<K,V> implements Proxy {
    static {
        GLib.javagi$ensureInitialized();
    }

    private final MemorySegment handle;

    // The Arena is used to allocate native Strings
    private final Arena arena = Arena.ofAuto();

    // Used to construct a Java instance for a native object
    private final Function<MemorySegment, K> makeKey;
    private final Function<MemorySegment, V> makeValue;

    /**
     * Create a HashTable proxy instance for the provided memory address.
     *
     * @param address   the memory address of the native object
     * @param makeKey   function that creates a K from a pointer
     * @param makeValue function that creates a V from a pointer
     */
    public HashTable(MemorySegment address,
                     Function<MemorySegment, K> makeKey,
                     Function<MemorySegment, V> makeValue) {
        this.handle = address;
        this.makeKey = makeKey;
        this.makeValue = makeValue;
    }

    /**
     * Create a HashTable proxy instance for the provided memory address. The
     * hashtable contains {@code MemorySegment} keys and values.
     *
     * @param address the memory address of the native object
     */
    @SuppressWarnings("unchecked")
    public HashTable(MemorySegment address) {
        this(address, k -> (K) k, v -> (V) v);
    }

    public MemorySegment handle() {
        return handle;
    }

    /**
     * @inheritDoc
     */
    @Override
    public @NotNull Set<Entry<K, V>> entrySet() {
        final var iterator = getKeys().iterator();
        return new AbstractSet<>() {
            @Override
            public @NotNull Iterator<Entry<K, V>> iterator() {
                return new Iterator<>() {
                    K key = null;

                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Entry<K, V> next() {
                        key = iterator.next();
                        V value = lookup(key);
                        return new AbstractMap.SimpleEntry<>(key, value);
                    }

                    @Override
                    public void remove() {
                        if (key == null)
                            throw new IllegalStateException("No key to remove");

                        HashTable.this.remove_(key);
                    }
                };
            }

            @Override
            public int size() {
                return HashTable.this.size();
            }

            @Override
            public boolean add(Entry<K, V> kvEntry) {
                V current = lookup(kvEntry.getKey());
                if (current != null && current.equals(kvEntry.getValue()))
                    return false;

                replace_(kvEntry.getKey(), kvEntry.getValue());
                return true;
            }
        };
    }

    /**
     * @inheritDoc
     */
    @Override
    public V put(K key, V value) {
        V prev = get(key);
        return replace_(key, value) ? null : prev;
    }

    /**
     * Get the GType of the HashTable class
     *
     * @return the GType
     */
    public static Type getType() {
        return Interop.getType("g_hash_table_get_type");
    }

    /**
     * Creates a new {@code GHashTable} with a reference count of 1.
     * <p>
     * Hash values returned by {@code hashFunc} are used to determine where keys
     * are stored within the {@code GHashTable} data structure. The g_direct_hash(),
     * g_int_hash(), g_int64_hash(), g_double_hash() and g_str_hash()
     * functions are provided for some common types of keys.
     * If {@code hashFunc} is {@code null}, g_direct_hash() is used.
     * <p>
     * {@code keyEqualFunc} is used when looking up keys in the {@code GHashTable}.
     * The g_direct_equal(), g_int_equal(), g_int64_equal(), g_double_equal()
     * and g_str_equal() functions are provided for the most common types
     * of keys. If {@code keyEqualFunc} is {@code null}, keys are compared directly in
     * a similar fashion to g_direct_equal(), but without the overhead of
     * a function call. {@code keyEqualFunc} is called with the key from the hash table
     * as its first parameter, and the user-provided key to check against as
     * its second.
     *
     * @param hashFunc a function to create a hash value from a key
     * @param keyEqualFunc a function to check two keys for equality
     * @return a new {@code GHashTable}
     */
    public static HashTable<MemorySegment, MemorySegment> new_(HashFunc hashFunc, EqualFunc keyEqualFunc) {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_new.invokeExact(
                    (MemorySegment) (hashFunc == null ? NULL : hashFunc.toCallback(Arena.global())),
                    (MemorySegment) (keyEqualFunc == null ? NULL : keyEqualFunc.toCallback(Arena.global())));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        var _instance = NULL.equals(_result) ? null : new HashTable<MemorySegment, MemorySegment>(_result);
        if (_instance != null) {
            MemoryCleaner.takeOwnership(_instance);
            MemoryCleaner.setBoxedType(_instance, HashTable.getType());
        }
        return _instance;
    }

    /**
     * This is a convenience function for using a {@code GHashTable} as a set.  It
     * is equivalent to calling g_hash_table_replace() with {@code key} as both the
     * key and the value.
     * <p>
     * In particular, this means that if {@code key} already exists in the hash table, then
     * the old copy of {@code key} in the hash table is freed and {@code key} replaces it in the
     * table.
     * <p>
     * When a hash table only ever contains keys that have themselves as the
     * corresponding value it is able to be stored more efficiently.  See
     * the discussion in the section description.
     * <p>
     * Starting from GLib 2.40, this function returns a boolean value to
     * indicate whether the newly added value was already in the hash table
     * or not.
     *
     * @param key a key to insert
     * @return {@code true} if the key did not exist yet
     */
    public boolean add(@Nullable K key) {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_add.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result != 0;
    }

    /**
     * Checks if {@code key} is in this GLib.HashTable.
     *
     * @param key a key to check
     * @return {@code true} if {@code key} is in this GLib.HashTable, {@code false} otherwise.
     */
    public boolean contains(@Nullable K key) {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_contains.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result != 0;
    }

    /**
     * Destroys all keys and values in the {@code GHashTable} and decrements its
     * reference count by 1. If keys and/or values are dynamically allocated,
     * you should either free them first or create the {@code GHashTable} with destroy
     * notifiers using g_hash_table_new_full(). In the latter case the destroy
     * functions you supplied will be called on all keys and values during the
     * destruction phase.
     */
    public void destroy() {
        try {
            MethodHandles.g_hash_table_destroy.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Calls the given function for key/value pairs in the {@code GHashTable}
     * until {@code predicate} returns {@code true}. The function is passed the key
     * and value of each pair, and the given {@code userData} parameter. The
     * hash table may not be modified while iterating over it (you can't
     * add/remove items).
     * <p>
     * Note, that hash tables are really only optimized for forward
     * lookups, i.e. g_hash_table_lookup(). So code that frequently issues
     * g_hash_table_find() or g_hash_table_foreach() (e.g. in the order of
     * once per every entry in a hash table) should probably be reworked
     * to use additional or different data structures for reverse lookups
     * (keep in mind that an O(n) find/foreach operation issued for all n
     * values in a hash table ends up needing O(n*n) operations).
     *
     * @param predicate function to test the key/value pairs for a certain property
     * @return The value of the first key/value pair is returned,
     *     for which {@code predicate} evaluates to {@code true}. If no pair with the
     *     requested property is found, {@code null} is returned.
     */
    public V find(HRFunc predicate) {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _result;
            try {
                _result = (MemorySegment) MethodHandles.g_hash_table_find.invokeExact(handle(),
                        (MemorySegment) (predicate == null ? NULL : predicate.toCallback(_arena)),
                        NULL);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            return makeValue.apply(_result);
        }
    }

    /**
     * Calls the given function for each of the key/value pairs in the
     * {@code GHashTable}.  The function is passed the key and value of each
     * pair, and the given {@code userData} parameter.  The hash table may not
     * be modified while iterating over it (you can't add/remove
     * items). To remove all items matching a predicate, use
     * g_hash_table_foreach_remove().
     * <p>
     * The order in which g_hash_table_foreach() iterates over the keys/values in
     * the hash table is not defined.
     * <p>
     * See g_hash_table_find() for performance caveats for linear
     * order searches in contrast to g_hash_table_lookup().
     *
     * @param func the function to call for each key/value pair
     */
    public void foreach(HFunc func) {
        try (var _arena = Arena.ofConfined()) {
            try {
                MethodHandles.g_hash_table_foreach.invokeExact(handle(),
                        (MemorySegment) (func == null ? NULL : func.toCallback(_arena)),
                        NULL);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
        }
    }

    /**
     * Calls the given function for each key/value pair in the
     * {@code GHashTable}. If the function returns {@code true}, then the key/value
     * pair is removed from the {@code GHashTable}. If you supplied key or
     * value destroy functions when creating the {@code GHashTable}, they are
     * used to free the memory allocated for the removed keys and values.
     * <p>
     * See {@code GHashTableIter} for an alternative way to loop over the
     * key/value pairs in the hash table.
     *
     * @param func the function to call for each key/value pair
     * @return the number of key/value pairs removed
     */
    public int foreachRemove(HRFunc func) {
        try (var _arena = Arena.ofConfined()) {
            int _result;
            try {
                _result = (int) MethodHandles.g_hash_table_foreach_remove.invokeExact(handle(),
                        (MemorySegment) (func == null ? NULL : func.toCallback(_arena)),
                        NULL);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            return _result;
        }
    }

    /**
     * Calls the given function for each key/value pair in the
     * {@code GHashTable}. If the function returns {@code true}, then the key/value
     * pair is removed from the {@code GHashTable}, but no key or value
     * destroy functions are called.
     * <p>
     * See {@code GHashTableIter} for an alternative way to loop over the
     * key/value pairs in the hash table.
     *
     * @param func the function to call for each key/value pair
     * @return the number of key/value pairs removed.
     */
    public int foreachSteal(HRFunc func) {
        try (var _arena = Arena.ofConfined()) {
            int _result;
            try {
                _result = (int) MethodHandles.g_hash_table_foreach_steal.invokeExact(handle(),
                        (MemorySegment) (func == null ? NULL : func.toCallback(_arena)),
                        NULL);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            return _result;
        }
    }

    /**
     * Retrieves every key inside this GLib.HashTable. The returned data is valid
     * until changes to the hash release those keys.
     * <p>
     * This iterates over every entry in the hash table to build its return value.
     * To iterate over the entries in a {@code GHashTable} more efficiently, use a
     * {@code GHashTableIter}.
     *
     * @return a {@code GList} containing all the keys
     *     inside the hash table. The content of the list is owned by the
     *     hash table and should not be modified or freed. Use g_list_free()
     *     when done using the list.
     */
    public List<K> getKeys() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_get_keys.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return new List<>(_result, makeKey, null, CONTAINER);
    }

    /**
     * Retrieves every key inside this GLib.HashTable, as an array.
     * <p>
     * The returned array is {@code null}-terminated but may contain {@code null} as a
     * key.  Use {@code length} to determine the true length if it's possible that
     * {@code null} was used as the value for a key.
     * <p>
     * Note: in the common case of a string-keyed {@code GHashTable}, the return
     * value of this function can be conveniently cast to (const gchar **).
     * <p>
     * This iterates over every entry in the hash table to build its return value.
     * To iterate over the entries in a {@code GHashTable} more efficiently, use a
     * {@code GHashTableIter}.
     * <p>
     * You should always free the return result with g_free().  In the
     * above-mentioned case of a string-keyed hash table, it may be
     * appropriate to use g_strfreev() if you call g_hash_table_steal_all()
     * first to transfer ownership of the keys.
     *
     * @return a
     *   {@code null}-terminated array containing each key from the table.
     */
    public MemorySegment[] getKeysAsArray() {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _lengthPointer = _arena.allocate(ValueLayout.JAVA_INT);
            _lengthPointer.set(ValueLayout.JAVA_INT, 0L, 0);
            Out<Integer> length = new Out<>();
            MemorySegment _result;
            try {
                _result = (MemorySegment) MethodHandles.g_hash_table_get_keys_as_array.invokeExact(
                        handle(), _lengthPointer);
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            length.set(_lengthPointer.get(ValueLayout.JAVA_INT, 0));
            return Interop.getAddressArrayFrom(_result, length.get(), NONE);
        }
    }

    /**
     * Retrieves every key inside this GLib.HashTable, as a {@code GPtrArray}.
     * The returned data is valid until changes to the hash release those keys.
     * <p>
     * This iterates over every entry in the hash table to build its return value.
     * To iterate over the entries in a {@code GHashTable} more efficiently, use a
     * {@code GHashTableIter}.
     * <p>
     * You should always unref the returned array with g_ptr_array_unref().
     *
     * @return a {@code GPtrArray} containing each key from
     * the table. Unref with with g_ptr_array_unref() when done.
     */
    public MemorySegment[] getKeysAsPtrArray() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_get_keys_as_ptr_array.invokeExact(
                    handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return Interop.getAddressArrayFrom(Interop.dereference(_result), new PtrArray(_result).readLen(), NONE);
    }

    /**
     * Retrieves every value inside this GLib.HashTable. The returned data
     * is valid until this GLib.HashTable is modified.
     * <p>
     * This iterates over every entry in the hash table to build its return value.
     * To iterate over the entries in a {@code GHashTable} more efficiently, use a
     * {@code GHashTableIter}.
     *
     * @return a {@code GList} containing all the values
     *     inside the hash table. The content of the list is owned by the
     *     hash table and should not be modified or freed. Use g_list_free()
     *     when done using the list.
     */
    public List<V> getValues() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_get_values.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return new List<>(_result, makeValue, null, CONTAINER);
    }

    /**
     * Retrieves every value inside this GLib.HashTable, as a {@code GPtrArray}.
     * The returned data is valid until changes to the hash release those values.
     * <p>
     * This iterates over every entry in the hash table to build its return value.
     * To iterate over the entries in a {@code GHashTable} more efficiently, use a
     * {@code GHashTableIter}.
     * <p>
     * You should always unref the returned array with g_ptr_array_unref().
     *
     * @return a {@code GPtrArray} containing each value from
     * the table. Unref with with g_ptr_array_unref() when done.
     */
    public MemorySegment[] getValuesAsPtrArray() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_get_values_as_ptr_array.invokeExact(
                    handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return Interop.getAddressArrayFrom(Interop.dereference(_result), new PtrArray(_result).readLen(), NONE);
    }

    /**
     * Inserts a new key and value into a {@code GHashTable}.
     * <p>
     * If the key already exists in the {@code GHashTable} its current
     * value is replaced with the new value. If you supplied a
     * {@code valueDestroyFunc} when creating the {@code GHashTable}, the old
     * value is freed using that function. If you supplied a
     * {@code keyDestroyFunc} when creating the {@code GHashTable}, the passed
     * key is freed using that function.
     * <p>
     * Starting from GLib 2.40, this function returns a boolean value to
     * indicate whether the newly added value was already in the hash table
     * or not.
     *
     * @param key a key to insert
     * @param value the value to associate with the key
     * @return {@code true} if the key did not exist yet
     */
    public boolean insert(@Nullable K key, @Nullable V value) {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_insert.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)),
                    (MemorySegment) (value == null ? NULL : getAddress(value, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result != 0;
    }

    /**
     * Looks up a key in a {@code GHashTable}. Note that this function cannot
     * distinguish between a key that is not present and one which is present
     * and has the value {@code null}. If you need this distinction, use
     * g_hash_table_lookup_extended().
     *
     * @param key the key to look up
     * @return the associated value, or {@code null} if the key is not found
     */
    public V lookup(@Nullable K key) {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_lookup.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return makeValue.apply(_result);
    }

    /**
     * Looks up a key in the {@code GHashTable}, returning the original key and the
     * associated value and a {@code gboolean} which is {@code true} if the key was found. This
     * is useful if you need to free the memory allocated for the original key,
     * for example before calling g_hash_table_remove().
     * <p>
     * You can actually pass {@code null} for {@code lookupKey} to test
     * whether the {@code null} key exists, provided the hash and equal functions
     * of this GLib.HashTable are {@code null}-safe.
     *
     * @param lookupKey the key to look up
     * @param origKey return location for the original key
     * @param value return location for the value associated
     * with the key
     * @return {@code true} if the key was found in the {@code GHashTable}
     */
    public boolean lookupExtended(@Nullable K lookupKey,
                                  @Nullable Out<K> origKey, @Nullable Out<V> value) {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _origKeyPointer = _arena.allocate(ValueLayout.ADDRESS);
            MemorySegment _valuePointer = _arena.allocate(ValueLayout.ADDRESS);
            int _result;
            try {
                _result = (int) MethodHandles.g_hash_table_lookup_extended.invokeExact(handle(),
                        (MemorySegment) (lookupKey == null ? NULL : getAddress(lookupKey, arena)),
                        (MemorySegment) (origKey == null ? NULL : _origKeyPointer),
                        (MemorySegment) (value == null ? NULL : _valuePointer));
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            if (origKey != null) {
                origKey.set(makeKey.apply(_origKeyPointer.get(ValueLayout.ADDRESS, 0)));
            }
            if (value != null) {
                value.set(makeValue.apply(_valuePointer.get(ValueLayout.ADDRESS, 0)));
            }
            return _result != 0;
        }
    }

    /**
     * Creates a new {@code GHashTable} like g_hash_table_new_full() with a reference
     * count of 1.
     * <p>
     * It inherits the hash function, the key equal function, the key destroy function,
     * as well as the value destroy function, from this GLib.HashTable.
     * <p>
     * The returned hash table will be empty; it will not contain the keys
     * or values from this GLib.HashTable.
     *
     * @return a new {@code GHashTable}
     */
    public HashTable<K,V> newSimilar() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_new_similar.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        var _instance = NULL.equals(_result) ? null
                : new HashTable<>(_result, makeKey, makeValue);
        if (_instance != null) {
            MemoryCleaner.takeOwnership(_instance);
            MemoryCleaner.setBoxedType(_instance, HashTable.getType());
        }
        return _instance;
    }

    /**
     * Atomically increments the reference count of this GLib.HashTable by one.
     * This function is MT-safe and may be called from any thread.
     *
     * @return the passed in {@code GHashTable}
     */
    public HashTable<K,V> ref() {
        MemorySegment _result;
        try {
            _result = (MemorySegment) MethodHandles.g_hash_table_ref.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        MemoryCleaner.takeOwnership(this);
        MemoryCleaner.setBoxedType(this, HashTable.getType());
        return this;
    }

    /**
     * Removes a key and its associated value from a {@code GHashTable}.
     * <p>
     * If the {@code GHashTable} was created using g_hash_table_new_full(), the
     * key and value are freed using the supplied destroy functions, otherwise
     * you have to make sure that any dynamically allocated values are freed
     * yourself.
     * <p>
     * This method calls {@code g_hash_table_remove()}. It does not override or
     * replace {@link AbstractMap#remove(Object)}.
     *
     * @param key the key to remove
     * @return {@code true} if the key was found and removed from the {@code GHashTable}
     */
    public boolean remove_(@Nullable K key) {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_remove.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result != 0;
    }

    /**
     * Removes all keys and their associated values from a {@code GHashTable}.
     * <p>
     * If the {@code GHashTable} was created using g_hash_table_new_full(),
     * the keys and values are freed using the supplied destroy functions,
     * otherwise you have to make sure that any dynamically allocated
     * values are freed yourself.
     */
    public void removeAll() {
        try {
            MethodHandles.g_hash_table_remove_all.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Inserts a new key and value into a {@code GHashTable} similar to
     * g_hash_table_insert(). The difference is that if the key
     * already exists in the {@code GHashTable}, it gets replaced by the
     * new key. If you supplied a {@code valueDestroyFunc} when creating
     * the {@code GHashTable}, the old value is freed using that function.
     * If you supplied a {@code keyDestroyFunc} when creating the
     * {@code GHashTable}, the old key is freed using that function.
     * <p>
     * Starting from GLib 2.40, this function returns a boolean value to
     * indicate whether the newly added value was already in the hash table
     * or not.
     * <p>
     * This method calls {@code g_hash_table_replace()}. It does not override or
     * replace {@link Map#replace(Object, Object)}.
     *
     * @param key a key to insert
     * @param value the value to associate with the key
     * @return {@code true} if the key did not exist yet
     */
    public boolean replace_(@Nullable K key, @Nullable V value) {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_replace.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)),
                    (MemorySegment) (value == null ? NULL : getAddress(value, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result != 0;
    }

    /**
     * Returns the number of elements contained in the {@code GHashTable}.
     *
     * @return the number of key/value pairs in the {@code GHashTable}.
     */
    public int size() {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_size.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result;
    }

    /**
     * Removes a key and its associated value from a {@code GHashTable} without
     * calling the key and value destroy functions.
     *
     * @param key the key to remove
     * @return {@code true} if the key was found and removed from the {@code GHashTable}
     */
    public boolean steal(@Nullable K key) {
        int _result;
        try {
            _result = (int) MethodHandles.g_hash_table_steal.invokeExact(handle(),
                    (MemorySegment) (key == null ? NULL : getAddress(key, arena)));
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
        return _result != 0;
    }

    /**
     * Removes all keys and their associated values from a {@code GHashTable}
     * without calling the key and value destroy functions.
     */
    public void stealAll() {
        try {
            MethodHandles.g_hash_table_steal_all.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    /**
     * Looks up a key in the {@code GHashTable}, stealing the original key and the
     * associated value and returning {@code true} if the key was found. If the key was
     * not found, {@code false} is returned.
     * <p>
     * If found, the stolen key and value are removed from the hash table without
     * calling the key and value destroy functions, and ownership is transferred to
     * the caller of this method, as with g_hash_table_steal(). That is the case
     * regardless whether {@code stolenKey} or {@code stolenValue} output parameters are
     * requested.
     * <p>
     * You can pass {@code null} for {@code lookupKey}, provided the hash and equal functions
     * of this GLib.HashTable are {@code null}-safe.
     * <p>
     * The dictionary implementation optimizes for having all values identical to
     * their keys, for example by using g_hash_table_add(). Before 2.82, when
     * stealing both the key and the value from such a dictionary, the value was
     * {@code null}. Since 2.82, the returned value and key will be the same.
     *
     * @param lookupKey the key to look up
     * @param stolenKey return location for the
     *    original key
     * @param stolenValue return location
     *    for the value associated with the key
     * @return {@code true} if the key was found in the {@code GHashTable}
     */
    public boolean stealExtended(@Nullable K lookupKey,
                                 @Nullable Out<K> stolenKey, @Nullable Out<V> stolenValue) {
        try (var _arena = Arena.ofConfined()) {
            MemorySegment _stolenKeyPointer = _arena.allocate(ValueLayout.ADDRESS);
            MemorySegment _stolenValuePointer = _arena.allocate(ValueLayout.ADDRESS);
            int _result;
            try {
                _result = (int) MethodHandles.g_hash_table_steal_extended.invokeExact(handle(),
                        (MemorySegment) (lookupKey == null ? NULL : getAddress(lookupKey, arena)),
                        (MemorySegment) (stolenKey == null ? NULL : _stolenKeyPointer),
                        (MemorySegment) (stolenValue == null ? NULL : _stolenValuePointer));
            } catch (Throwable _err) {
                throw new AssertionError(_err);
            }
            if (stolenKey != null) {
                stolenKey.set(makeKey.apply(_stolenKeyPointer.get(ValueLayout.ADDRESS, 0)));
            }
            if (stolenValue != null) {
                stolenValue.set(makeValue.apply(_stolenValuePointer.get(ValueLayout.ADDRESS, 0)));
            }
            return _result != 0;
        }
    }

    /**
     * Atomically decrements the reference count of this GLib.HashTable by one.
     * If the reference count drops to 0, all keys and values will be
     * destroyed, and all memory allocated by the hash table is released.
     * This function is MT-safe and may be called from any thread.
     */
    public void unref() {
        try {
            MethodHandles.g_hash_table_unref.invokeExact(handle());
        } catch (Throwable _err) {
            throw new AssertionError(_err);
        }
    }

    private static final class MethodHandles {
        static final MethodHandle g_hash_table_add = Interop.downcallHandle("g_hash_table_add",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_contains = Interop.downcallHandle(
                "g_hash_table_contains", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_destroy = Interop.downcallHandle(
                "g_hash_table_destroy", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_find = Interop.downcallHandle("g_hash_table_find",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_foreach = Interop.downcallHandle(
                "g_hash_table_foreach", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_foreach_remove = Interop.downcallHandle(
                "g_hash_table_foreach_remove", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_foreach_steal = Interop.downcallHandle(
                "g_hash_table_foreach_steal", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_get_keys = Interop.downcallHandle(
                "g_hash_table_get_keys", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_get_keys_as_array = Interop.downcallHandle(
                "g_hash_table_get_keys_as_array", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_get_keys_as_ptr_array = Interop.downcallHandle(
                "g_hash_table_get_keys_as_ptr_array", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_get_values = Interop.downcallHandle(
                "g_hash_table_get_values", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_get_values_as_ptr_array = Interop.downcallHandle(
                "g_hash_table_get_values_as_ptr_array", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_insert = Interop.downcallHandle(
                "g_hash_table_insert", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_lookup = Interop.downcallHandle(
                "g_hash_table_lookup", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_lookup_extended = Interop.downcallHandle(
                "g_hash_table_lookup_extended", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                false);

        static final MethodHandle g_hash_table_new = Interop.downcallHandle("g_hash_table_new",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_new_full = Interop.downcallHandle(
                "g_hash_table_new_full", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                false);

        static final MethodHandle g_hash_table_new_similar = Interop.downcallHandle(
                "g_hash_table_new_similar", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_ref = Interop.downcallHandle("g_hash_table_ref",
                FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_remove = Interop.downcallHandle(
                "g_hash_table_remove", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_remove_all = Interop.downcallHandle(
                "g_hash_table_remove_all", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_replace = Interop.downcallHandle(
                "g_hash_table_replace", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_size = Interop.downcallHandle("g_hash_table_size",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_steal = Interop.downcallHandle("g_hash_table_steal",
                FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_steal_all = Interop.downcallHandle(
                "g_hash_table_steal_all", FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_steal_all_keys = Interop.downcallHandle(
                "g_hash_table_steal_all_keys", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_steal_all_values = Interop.downcallHandle(
                "g_hash_table_steal_all_values", FunctionDescriptor.of(ValueLayout.ADDRESS,
                        ValueLayout.ADDRESS), false);

        static final MethodHandle g_hash_table_steal_extended = Interop.downcallHandle(
                "g_hash_table_steal_extended", FunctionDescriptor.of(ValueLayout.JAVA_INT,
                        ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS),
                false);

        static final MethodHandle g_hash_table_unref = Interop.downcallHandle("g_hash_table_unref",
                FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), false);
    }
}
