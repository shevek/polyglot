/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.collect.AbstractIterator;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class TokenMap<V> extends AbstractMap<TokenModel, V> {

    private final TokenUniverse universe;
    private final List<V> values = new ArrayList<>();

    public TokenMap(@Nonnull TokenUniverse universe) {
        this.universe = universe;
        for (int i = 0; i < universe.size(); i++)
            values.add(null);
    }

    @Override
    public V get(Object _key) {
        if (!(_key instanceof TokenModel))
            return null;
        TokenModel key = (TokenModel) _key;
        int index = key.getIndex();
        return /* values.size() <= index ? null : */ values.get(index);
    }

    @Override
    public V put(TokenModel key, V value) {
        int index = key.getIndex();
        V prev = values.get(index);
        values.set(index, value);
        return prev;
    }

    @Override
    public V remove(Object _key) {
        if (!(_key instanceof TokenModel))
            return null;
        TokenModel key = (TokenModel) _key;
        int index = key.getIndex();
        // for (int i = values.size() - 1; i < index; i++) values.add(null);
        V prev = values.get(index);
        values.set(index, null);
        return prev;
    }

    @Override
    public boolean containsKey(Object key) {
        return get(key) != null;
    }

    @Override
    public int size() {
        int size = 0;
        for (int i = 0; i < values.size(); i++)
            if (values.get(i) != null)
                size++;
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < values.size(); i++)
            if (values.get(i) != null)
                return false;
        return true;
    }

    @Override
    public void clear() {
        values.clear();
    }

    private abstract class BaseSet<T> extends AbstractSet<T> {

        @Override
        public int size() {
            return TokenMap.this.size();
        }

        @Override
        public boolean isEmpty() {
            return TokenMap.this.isEmpty();
        }
    }

    private abstract class BaseIterator<T> extends AbstractIterator<T> {

        private int i = -1;

        @Override
        protected T computeNext() {
            while (++i < values.size()) {
                V value = values.get(i);
                if (value != null)
                    return computeNext(i, value);
            }
            return endOfData();
        }

        protected abstract T computeNext(@Nonnegative int index, @Nonnull V value);
    }

    @Override
    public Set<TokenModel> keySet() {
        return new BaseSet<TokenModel>() {

            @Override
            public boolean contains(Object o) {
                return containsKey(o);
            }

            @Override
            public Iterator<TokenModel> iterator() {
                return new BaseIterator<TokenModel>() {
                    @Override
                    protected TokenModel computeNext(int index, V value) {
                        return universe.getItemByIndex(index);
                    }
                };
            }
        };
    }

    @Override
    public Set<Entry<TokenModel, V>> entrySet() {
        return new BaseSet<Map.Entry<TokenModel, V>>() {
            @Override
            public Iterator<Entry<TokenModel, V>> iterator() {
                return new BaseIterator<Entry<TokenModel, V>>() {
                    @Override
                    protected Entry<TokenModel, V> computeNext(int index, V value) {
                        return new SimpleImmutableEntry<>(universe.getItemByIndex(index), value);
                    }
                };
            }

        };
    }
}
