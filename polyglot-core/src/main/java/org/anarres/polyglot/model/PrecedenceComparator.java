/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * An explicit comparator for a partial order.
 *
 * @author shevek
 */
public class PrecedenceComparator {

    public static enum Result {
        LOWER, EQUAL, HIGHER, INCOMPARABLE
    }

    private static final class Key {

        private final String high;
        private final String low;

        public Key(String high, String low) {
            this.high = Preconditions.checkNotNull(high, "High-precedence was null.");
            this.low = Preconditions.checkNotNull(low, "Low-precedence was null.");
        }

        @Override
        public int hashCode() {
            return (high.hashCode() << 1) ^ low.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (null == obj)
                return false;
            if (!(obj instanceof Key))
                return false;
            Key o = (Key) obj;
            return high.equals(o.high) && low.equals(o.low);
        }
    }

    private final ObjectSet<Key> data = new ObjectOpenHashSet<>();

    public void add(@Nonnull List<String> chain) {
        for (int i = 0; i < chain.size() - 1; i++) {
            for (int j = i + 1; j < chain.size(); j++) {
                data.add(new Key(chain.get(i), chain.get(j)));
            }
        }
    }

    @Nonnull
    public Result compare(@CheckForNull String o1, @CheckForNull String o2) {
        if (o1 == null)
            return Result.INCOMPARABLE;
        if (o2 == null)
            return Result.INCOMPARABLE;
        if (o1.equals(o2))
            return Result.EQUAL;
        if (data.contains(new Key(o1, o2)))
            return Result.HIGHER;
        if (data.contains(new Key(o2, o1)))
            return Result.LOWER;
        return Result.INCOMPARABLE;
    }
}
