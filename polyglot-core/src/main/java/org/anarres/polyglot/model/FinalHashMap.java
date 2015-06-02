/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import java.util.TreeMap;

/**
 *
 * @author shevek
 */
public class FinalHashMap<K, V> extends TreeMap<K, V> {

    @Override
    public V put(K key, V value) {
        Object prev = super.put(key, value);
        if (prev != null)
            throw new IllegalArgumentException("Refusing to overwrite " + key);
        return null;
    }

}
