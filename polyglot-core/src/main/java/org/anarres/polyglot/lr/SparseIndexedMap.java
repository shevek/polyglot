/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.collect.UnmodifiableIterator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class SparseIndexedMap<K extends Indexed, V> extends AbstractMap<K, V> implements IndexedMap<K, V> {

    private final IndexedUniverse<K> universe;
    // private final List<V> data = new ArrayList<>();
    private final Int2ObjectOpenHashMap<V> data = new Int2ObjectOpenHashMap<>();

    public SparseIndexedMap(@Nonnull IndexedUniverse<K> universe) {
        this.universe = universe;
    }

    @Nonnull
    public IndexedUniverse<K> getUniverse() {
        return universe;
    }

    protected boolean isCompatibleObject(@CheckForNull Object in) {
        return universe.getItemType().isInstance(in);
    }

    @Override
    public V get(int index) {
        return data.get(index);
    }

    @Override
    public V get(K key) {
        return data.get(key.getIndex());
    }

    @Override
    @SuppressWarnings("unchecked")
    public V get(Object key) {
        if (!isCompatibleObject(key))
            return null;
        return get((K) key);
    }

    @Override
    public V put(K key, V value) {
        int index = key.getIndex();
        return data.put(index, value);
    }

    @Override
    public V remove(K key) {
        if (key == null)
            return null;
        return data.remove(key.getIndex());
    }

    @Override
    @SuppressWarnings("unchecked")
    public V remove(Object key) {
        if (!isCompatibleObject(key))
            return null;
        return remove((K) key);
    }

    @Override
    public boolean containsKey(Object _key) {
        Indexed key = (Indexed) _key;
        int index = key.getIndex();
        return data.containsKey(index) && isCompatibleObject(_key);
    }

    @Override
    public int size() {
        return data.size();
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    @Override
    public void clear() {
        data.clear();
    }

    private abstract class BaseSet<T> extends AbstractSet<T> {

        @Override
        public int size() {
            return SparseIndexedMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return SparseIndexedMap.this.isEmpty();
        }

        @Override
        public void clear() {
            SparseIndexedMap.this.clear();
        }
    }

    private static class KeyIterator<K extends Indexed> extends UnmodifiableIterator<K> {

        private final IndexedUniverse<K> universe;
        private final IntIterator it;

        public KeyIterator(IndexedUniverse<K> universe, IntIterator it) {
            this.universe = universe;
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public K next() {
            return universe.getItemByIndex(it.nextInt());
        }
    }

    @Override
    public Set<K> keySet() {
        return new BaseSet<K>() {
            @Override
            public boolean contains(Object o) {
                return containsKey(o);
            }

            @Override
            public Iterator<K> iterator() {
                return new KeyIterator<>(universe, data.keySet().iterator());
            }
        };
    }

    @Override
    public Collection<V> values() {
        return data.values();
    }

    private static class EntryIterator<K extends Indexed, V> extends UnmodifiableIterator<Entry<K, V>> {

        private final IndexedUniverse<K> universe;
        private final ObjectIterator<Int2ObjectMap.Entry<V>> it;
        // = data.int2ObjectEntrySet().fastIterator();

        public EntryIterator(IndexedUniverse<K> universe, ObjectIterator<Int2ObjectMap.Entry<V>> it) {
            this.universe = universe;
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Entry<K, V> next() {
            Int2ObjectMap.Entry<V> e = it.next();
            K key = universe.getItemByIndex(e.getIntKey());
            return new SimpleImmutableEntry<>(key, e.getValue());
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new BaseSet<Map.Entry<K, V>>() {
            @Override
            public boolean contains(Object o) {
                return data.containsValue(o);
            }

            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new EntryIterator<>(universe, data.int2ObjectEntrySet().fastIterator());
            }
        };
    }
}
