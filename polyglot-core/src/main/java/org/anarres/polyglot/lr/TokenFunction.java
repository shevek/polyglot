/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.TokenModel;

/**
 * A dense function from a token to a value which is not iterable.
 *
 * @see TokenMap
 * @author shevek
 */
/* pp */ class TokenFunction<V> {

    private final Object[] data;

    public TokenFunction(@Nonnegative int size) {
        this.data = new Object[size];
    }

    @Nonnull
    @SuppressWarnings(value = "unchecked")
    public V get(@Nonnull int index) {
        return (V) data[index];
    }

    @Nonnull
    public V get(@Nonnull TokenModel token) {
        return get(token.getIndex());
    }

    public void put(@Nonnull TokenModel token, @Nonnull V value) {
        data[token.getIndex()] = value;
    }

}
