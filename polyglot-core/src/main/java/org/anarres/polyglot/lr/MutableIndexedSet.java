/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class MutableIndexedSet<IndexedItem extends Indexed> extends AbstractIndexedSet<IndexedItem> {

    private static final Logger LOG = LoggerFactory.getLogger(MutableIndexedSet.class);
    private final IntOpenHashSet indices;

    public MutableIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe) {
        super(universe);
        // this.indices = new BitSet(universe.size());
        this.indices = new IntOpenHashSet();
    }

    /** Exposed to allow for allocation-free iteration. */
    @Nonnull
    public IntSet getIndices() {
        return indices;
    }

    @Override
    public boolean add(IndexedItem e) {
        if (!isCompatibleObject(e))
            throw new IllegalArgumentException("Cannot add " + ((e == null) ? "null" : e.getClass()) + "; expected " + universe.getItemType());
        int index = e.getIndex();
        // if (indices.get(index))
        // return false;
        // indices.set(index);
        // return true;
        return indices.add(index);
    }

    @Override
    public boolean remove(Object o) {
        if (!isCompatibleObject(o))
            return false;
        @SuppressWarnings("unchecked")
        IndexedItem i = (IndexedItem) o;
        int index = i.getIndex();
        return indices.remove(index);
        // if (!indices.get(index))
        // return false;
        // indices.clear(index);
        // return true;
    }

    private boolean isCompatibleIndexedSet(@CheckForNull Iterable<?> in) {
        if (!(in instanceof MutableIndexedSet<?>))
            return false;
        MutableIndexedSet<?> s = (MutableIndexedSet<?>) in;
        return s.universe == universe;
    }

    private boolean isContainedIn(@Nonnull BitSet haystack, @Nonnull BitSet needle) {
        for (int i = needle.nextSetBit(0); i >= 0; i = needle.nextSetBit(i + 1))
            if (!haystack.get(i))
                return false;
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends IndexedItem> c) {
        if (isCompatibleIndexedSet(c)) {
            MutableIndexedSet<?> s = (MutableIndexedSet<?>) c;
            // if (isContainedIn(indices, s.indices))
            // return false;
            // indices.or(s.indices);
            // return true;
            return indices.addAll(s.indices);
        }
        Preconditions.checkNotNull(c, "addAll: argument set was null.");
        return super.addAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        if (isCompatibleIndexedSet(c)) {
            MutableIndexedSet<?> s = (MutableIndexedSet<?>) c;
            // if (!indices.intersects(s.indices))
            // return false;
            // indices.andNot(s.indices);
            // return true;
            return indices.removeAll(s.indices);
        }
        return super.removeAll(c);
    }

    @Override
    public boolean contains(Object o) {
        // It's probably faster to call this second.
        // if (!isCompatibleObject(o)) return false;
        @SuppressWarnings("unchecked")
        IndexedItem i = (IndexedItem) o;
        int index = i.getIndex();
        return indices.contains(index) && isCompatibleObject(o);
    }

    private static final class BitSetItr<IndexedItem extends Indexed> implements Iterator<IndexedItem> {

        private final IndexedUniverse<IndexedItem> universe;
        private final BitSet indices;
        private int curr;
        private int next;

        public BitSetItr(@Nonnull IndexedUniverse<IndexedItem> universe, @Nonnull BitSet indices) {
            this.universe = universe;
            this.indices = indices;
            this.curr = -1;
            this.next = indices.nextSetBit(0);
        }

        @Override
        public boolean hasNext() {
            return next >= 0;
        }

        // This routine is a performance hotspot.
        @Override
        public IndexedItem next() {
            int curr = next;
            this.curr = curr;
            this.next = indices.nextSetBit(curr + 1);
            return universe.getItemByIndex(curr);
        }

        @Override
        public void remove() {
            indices.clear(curr);
        }
    }

    private static final class IntSetItr<IndexedItem extends Indexed> implements Iterator<IndexedItem> {

        private final IndexedUniverse<IndexedItem> universe;
        private final IntIterator it;

        public IntSetItr(IndexedUniverse<IndexedItem> universe, IntIterator it) {
            this.universe = universe;
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public IndexedItem next() {
            int index = it.nextInt();
            return universe.getItemByIndex(index);
        }

        @Override
        public void remove() {
            it.remove();
        }
    }

    @Override
    public Iterator<IndexedItem> iterator() {
        return new IntSetItr<>(getUniverse(), getIndices().iterator());
    }

    @Override
    public boolean isEmpty() {
        return indices.isEmpty();
    }

    @Override
    public int size() {
        return indices.size();
    }

    public boolean trim() {
        return indices.trim();
    }

    @Override
    public void clear() {
        indices.clear();
    }

    @Nonnull
    public ImmutableIndexedSet<IndexedItem> toImmutableSet() {
        return new ImmutableIndexedSet<>(getUniverse(), indices);
    }

    @Override
    public int hashCode() {
        return indices.hashCode();
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
        MutableIndexedSet<IndexedItem> s = (MutableIndexedSet<IndexedItem>) o;
        return universe == s.universe
                && indices.equals(s.indices);
    }
}
