/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.Arrays;
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
/* pp */ class DenseIndexedMap<K extends Indexed, V> implements IndexedMap<K, V> {

    private final Object[] data;

    public DenseIndexedMap(@Nonnegative int size) {
        this.data = new Object[size];
    }

    public DenseIndexedMap(@Nonnull IndexedUniverse<K> universe) {
        this(universe.size());
    }

    @SuppressWarnings(value = "unchecked")
    public V get(@Nonnull int index) {
        return (V) data[index];
    }

    @Override
    public V get(@Nonnull K key) {
        return get(key.getIndex());
    }

    @Override
    public V put(@Nonnull K key, V value) {
        int index = key.getIndex();
        @SuppressWarnings("unchecked")
        V out = (V) data[index];
        data[index] = value;
        return out;
    }

    @Override
    public V remove(@Nonnull K key) {
        int index = key.getIndex();
        // for (int i = data.size() - 1; i < index; i++) data.add(null);
        @SuppressWarnings("unchecked")
        V value = (V) data[index];
        data[index] = null;
        return value;
    }

    @Override
    public void clear() {
        Arrays.fill(data, null);
    }

}
