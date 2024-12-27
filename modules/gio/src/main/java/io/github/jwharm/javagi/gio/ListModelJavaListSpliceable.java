package io.github.jwharm.javagi.gio;

import org.gnome.gobject.GObject;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface ListModelJavaListSpliceable<E extends GObject> extends ListModelJavaListMutable<E> {

    void splice(int position, int nRemovals, @Nullable E[] additions);
    void splice(int position, int nRemovals, @NotNull Collection<? extends E> additions);

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, E[])}.
     */
    @Override
    default void clear() {
        splice(0, size(), (E[]) null);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default E set(int index, E element) {
        return ListModelJavaListMutable.super.set(index, element);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default void add(int index, E element) {
        ListModelJavaListMutable.super.add(index, element);
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default boolean addAll(@NotNull Collection<? extends E> c) {
        splice(size(), 0, Objects.requireNonNull(c));
        return !c.isEmpty();
    }

    /**
     * {@inheritDoc}
     *
     * @apiNote This implementation delegates to {@link #splice(int, int, Collection)}.
     */
    @Override
    default boolean addAll(int index, @NotNull Collection<? extends E> c) {
        splice(index, 0, Objects.requireNonNull(c));
        return !c.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default @NotNull List<E> subList(int fromIndex, int toIndex) {
        return new SubList<>(this, fromIndex, toIndex);
    }

    @ApiStatus.Internal
    class SubList<E extends GObject, List extends ListModelJavaListSpliceable<E>> extends ListModelJavaListMutable.SubList<E, List> implements ListModelJavaListSpliceable<E> {
        public SubList(List list, int fromIndex, int toIndex) {
            super(list, fromIndex, toIndex);
        }

        @Override
        public void splice(int position, int nRemovals, @Nullable E[] additions) {
            if (position < 0 || nRemovals < 0 || position + nRemovals > size())
                throw new IndexOutOfBoundsException();
            list.splice(position + fromIndex, nRemovals, additions);
            toIndex -= nRemovals;
            toIndex += additions == null ? 0 : additions.length;
        }

        @Override
        public void splice(int position, int nRemovals, @NotNull Collection<? extends E> additions) {
            if (position < 0 || nRemovals < 0 || position + nRemovals > size())
                throw new IndexOutOfBoundsException();
            list.splice(position + fromIndex, nRemovals, additions);
            toIndex -= nRemovals;
            toIndex += additions.size();
        }
    }
}
