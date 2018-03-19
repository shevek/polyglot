/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import it.unimi.dsi.fastutil.ints.IntHash;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class IntHashStrategy implements IntHash.Strategy {

    public static final IntHash.Strategy INSTANCE = new IntHashStrategy();

    // Interesting, but a few percent slower.
    // @Nonnull public static IntOpenCustomHashSet newIntSet() { return new IntOpenCustomHashSet(INSTANCE); }
    @Nonnull
    public static IntOpenHashSet newIntSet() {
        return new IntOpenHashSet();
    }

    /** Based on http://www.burtleburtle.net/bob/hash/integer.html */
    @Override
    public int hashCode(int a) {
        a = (a + 0x7ed55d16) + (a << 12);
        a = (a ^ 0xc761c23c) ^ (a >> 19);
        a = (a + 0x165667b1) + (a << 5);
        a = (a + 0xd3a2646c) ^ (a << 9);
        a = (a + 0xfd7046c5) + (a << 3);
        a = (a ^ 0xb55a4f09) ^ (a >> 16);
        return a;
    }

    @Override
    public boolean equals(int a, int b) {
        return a == b;
    }

}
