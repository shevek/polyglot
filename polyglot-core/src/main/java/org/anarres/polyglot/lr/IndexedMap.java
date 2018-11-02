/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public interface IndexedMap<K extends Indexed, V> {

    @CheckForNull
    public V get(@Nonnegative int index);

    @CheckForNull
    public V get(@Nonnull K key);

    @CheckForNull
    public V put(@Nonnull K key, V value);

    @CheckForNull
    public V remove(@Nonnull K key);

    public void clear();
}
