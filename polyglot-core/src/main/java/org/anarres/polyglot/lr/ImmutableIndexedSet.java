/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.IntCollection;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.Iterator;
import javax.annotation.Nonnull;

/**
 * An immutable hash set of {@link Indexed} items.
 *
 * This class violates the traditional contract for hashCode() and equals()
 * on collections, and is only equal to identical instances of
 * ImmutableIndexedSet.
 *
 * @author shevek
 */
public class ImmutableIndexedSet<IndexedItem extends Indexed> extends AbstractIndexedSet<IndexedItem> {

    private static boolean immutable() {
        throw new UnsupportedOperationException("Cannot mutate an immutable set.");
    }

    @Nonnull
    public static int[] toSortedIntArray(@Nonnull IntCollection indices) {
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

    private static long toBloomFilter(@Nonnull int[] indices) {
        long out = 0;
        for (int index : indices)
            out |= (1L << (index & (Long.SIZE - 1)));
        return out;
    }

    // There's no point having a bloom filter here, as we never call contains().
    // private final long bloomFilter;
    private final int[] indices;
    private final int hashCode;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ImmutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull int[] indices) {
        super(universe);
        // this.bloomFilter = toBloomFilter(indices);
        this.indices = indices;
        this.hashCode = Arrays.hashCode(indices);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ImmutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull IntCollection indices) {
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
        @SuppressWarnings("unchecked")
        IndexedItem i = (IndexedItem) o;
        int index = i.getIndex();
        return Arrays.binarySearch(indices, index) >= 0 && isCompatibleObject(o);
    }

    private static final class Itr<IndexedItem extends Indexed> implements Iterator<IndexedItem> {

        private final IndexedUniverse<IndexedItem> universe;
        private final int[] indices;
        private int index;

        public Itr(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull int[] indices) {
            this.universe = universe;
            this.indices = indices;
            this.index = 0;
        }

        @Override
        public boolean hasNext() {
            return index < indices.length;
        }

        // This routine is a performance hotspot.
        @Override
        public IndexedItem next() {
            int curr = indices[index++];
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
    public Object[] toArray() {
        int size = size();
        Object[] out = new Object[size];
        for (int i = 0; i < size; i++)
            out[i] = universe.getItemByIndex(indices[i]);
        return out;
    }

    @Override
    public <T> T[] toArray(T[] a) {
        int size = size();
        T[] out = a.length >= size
                ? a
                : (T[]) java.lang.reflect.Array.newInstance(a.getClass().getComponentType(), size);
        for (int i = 0; i < size; i++)
            out[i] = (T) universe.getItemByIndex(indices[i]);
        return out;
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
        ImmutableIndexedSet<?> s = (ImmutableIndexedSet<?>) o;
        return universe == s.universe
                && hashCode == s.hashCode
                && Arrays.equals(indices, s.indices);
    }
}
