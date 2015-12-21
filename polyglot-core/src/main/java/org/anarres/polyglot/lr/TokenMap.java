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
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class TokenMap<V> extends AbstractMap<TokenModel, V> {

    private final TokenUniverse universe;
    // private final List<V> values = new ArrayList<>();
    private final Int2ObjectOpenHashMap<V> values = new Int2ObjectOpenHashMap<>();

    public TokenMap(@Nonnull TokenUniverse universe) {
        this.universe = universe;
    }

    @Override
    public V get(Object _key) {
        if (!(_key instanceof TokenModel))
            return null;
        TokenModel key = (TokenModel) _key;
        int index = key.getIndex();
        return values.get(index);
    }

    @Override
    public V put(TokenModel key, V value) {
        int index = key.getIndex();
        return values.put(index, value);
    }

    @Override
    public V remove(Object _key) {
        if (!(_key instanceof TokenModel))
            return null;
        TokenModel key = (TokenModel) _key;
        int index = key.getIndex();
        // for (int i = values.size() - 1; i < index; i++) values.add(null);
        return values.remove(index);
    }

    @Override
    public boolean containsKey(Object _key) {
        if (!(_key instanceof TokenModel))
            return false;
        TokenModel key = (TokenModel) _key;
        int index = key.getIndex();
        return values.containsKey(index);
    }

    @Override
    public int size() {
        return values.size();
    }

    @Override
    public boolean isEmpty() {
        return values.isEmpty();
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

        @Override
        public void clear() {
            TokenMap.this.clear();
        }
    }

    private static class KeyIterator extends UnmodifiableIterator<TokenModel> {

        private final TokenUniverse universe;
        private final IntIterator it;

        public KeyIterator(TokenUniverse universe, IntIterator it) {
            this.universe = universe;
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public TokenModel next() {
            return universe.getItemByIndex(it.nextInt());
        }
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
                return new KeyIterator(universe, values.keySet().iterator());
            }
        };
    }

    @Override
    public Collection<V> values() {
        return values.values();
    }

    private static class EntryIterator<V> extends UnmodifiableIterator<Entry<TokenModel, V>> {

        private final TokenUniverse universe;
        private final ObjectIterator<Int2ObjectMap.Entry<V>> it;
        // = values.int2ObjectEntrySet().fastIterator();

        public EntryIterator(TokenUniverse universe, ObjectIterator<Int2ObjectMap.Entry<V>> it) {
            this.universe = universe;
            this.it = it;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Entry<TokenModel, V> next() {
            Int2ObjectMap.Entry<V> e = it.next();
            TokenModel key = universe.getItemByIndex(e.getIntKey());
            return new SimpleImmutableEntry<>(key, e.getValue());
        }
    }

    @Override
    public Set<Entry<TokenModel, V>> entrySet() {
        return new BaseSet<Map.Entry<TokenModel, V>>() {
            @Override
            public boolean contains(Object o) {
                return values.containsValue(o);
            }

            @Override
            public Iterator<Entry<TokenModel, V>> iterator() {
                return new EntryIterator<>(universe, values.int2ObjectEntrySet().fastIterator());
            }
        };
    }
}
