/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class ImmutableIndexedSet<IndexedItem extends Indexed> extends AbstractIndexedSet<IndexedItem> {

    private static boolean immutable() {
        throw new UnsupportedOperationException("Cannot mutate an immutable set.");
    }

    @Nonnull
    public static int[] toSortedIntArray(@Nonnull IntSet indices) {
        int[] tmp = indices.toIntArray();
        Arrays.sort(tmp);
        return tmp;
    }

    @Nonnull
    public static IntSet toIntSet(@Nonnull Iterable<? extends Indexed> items) {
        // We go via IntSet to guarantee uniqueness of the integers.
        IntSet indices = new IntOpenHashSet();
        for (Indexed item : items)
            indices.add(item.getIndex());
        return indices;
    }

    private final int[] indices;
    private final int hashCode;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ImmutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull int[] indices) {
        super(universe);
        this.indices = indices;
        this.hashCode = Arrays.hashCode(indices);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ImmutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull IntSet indices) {
        this(universe, toSortedIntArray(indices));
    }

    public ImmutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull Iterable<? extends IndexedItem> items) {
        this(universe, toIntSet(items));
    }

    @SuppressWarnings("unchecked")
    public ImmutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull IndexedItem... items) {
        this(universe, Arrays.asList(items));
    }

    @Nonnull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public int[] getIndices() {
        return indices;
    }

    @Override
    public boolean add(IndexedItem e) {
        return immutable();
    }

    @Override
    public boolean remove(Object o) {
        return immutable();
    }

    @Override
    public boolean contains(Object o) {
        if (!isCompatibleObject(o))
            return false;
        @SuppressWarnings("unchecked")
        IndexedItem i = (IndexedItem) o;
        int index = i.getIndex();
        return Arrays.binarySearch(indices, index) >= 0;
    }

    private static final class Itr<IndexedItem extends Indexed> implements Iterator<IndexedItem> {

        private final IndexedUniverse<IndexedItem> universe;
        private final int[] indices;
        private int index;

        public Itr(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull int[] indices) {
            this.universe = universe;
            this.indices = indices;
            this.index = -1;
        }

        @Override
        public boolean hasNext() {
            return index < (indices.length - 1);
        }

        // This routine is a performance hotspot.
        @Override
        public IndexedItem next() {
            int curr = indices[++index];
            return universe.getItemByIndex(curr);
        }

        @Override
        public void remove() {
            immutable();
        }
    }

    @Override
    public Iterator<IndexedItem> iterator() {
        return new Itr<>(getUniverse(), getIndices());
    }

    @Override
    public int size() {
        return indices.length;
    }

    @Override
    public void clear() {
        immutable();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (null == o)
            return false;
        if (!getClass().equals(o.getClass()))
            return false;
        @SuppressWarnings("unchecked")
        ImmutableIndexedSet<IndexedItem> s = (ImmutableIndexedSet<IndexedItem>) o;
        return universe == s.universe
                && hashCode == s.hashCode
                && Arrays.equals(indices, s.indices);
    }
}
