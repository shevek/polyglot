/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public interface IndexedMap<K extends Indexed, V> {

    public V get(@Nonnull K key);

    public V put(@Nonnull K key, V value);

    public V remove(@Nonnull K key);

    public void clear();
}
