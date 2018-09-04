/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.Arrays;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 * A dense function from an indexed key to a value.
 *
 * This map is not iterable.
 *
 * @see TokenMap
 * @author shevek
 */
/* pp */ class DenseIndexedMap<K extends Indexed, V> {

    private final Object[] data;

    public DenseIndexedMap(@Nonnegative int size) {
        this.data = new Object[size];
    }

    public DenseIndexedMap(@Nonnull IndexedUniverse<K> universe) {
        this(universe.size());
    }

    @Nonnull
    @SuppressWarnings(value = "unchecked")
    public V get(@Nonnull int index) {
        return (V) data[index];
    }

    @Nonnull
    public V get(@Nonnull K key) {
        return get(key.getIndex());
    }

    public void put(@Nonnull K key, @Nonnull V value) {
        data[key.getIndex()] = value;
    }

    @CheckForNull
    public V remove(@Nonnull K key) {
        int index = key.getIndex();
        // for (int i = data.size() - 1; i < index; i++) data.add(null);
        V value = (V) data[index];
        data[index] = null;
        return value;
    }

    // @Override
    public void clear() {
        Arrays.fill(data, null);
    }

}
