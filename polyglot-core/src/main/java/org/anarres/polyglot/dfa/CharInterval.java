/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

/**
 *
 * @author shevek
 */
@Immutable
public class CharInterval implements Comparable<CharInterval> {

    @CheckForNull
    public static <T extends CharInterval> T findFirstOverlappingInterval(@Nonnull List<? extends T> intervals, char start, char end) {
        int low = 0;
        int high = intervals.size() - 1;
        T result = null;

        while (high >= low) {
            int middle = (high + low) >>> 1;

            T candidate = intervals.get(middle);

            if (start <= candidate.getEnd()) {
                if (end >= candidate.getStart()) {
                    result = candidate;
                    // we continue, to find the lowest matching interval!
                }

                high = middle - 1;
            } else {
                low = middle + 1;
            }
        }

        return result;
    }

    private final char start;
    private final char end;

    /**
     * An interval (range) of characters.
     *
     * @param start The start of the range, inclusive.
     * @param end The end of the range, inclusive.
     */
    public CharInterval(char start, char end) {
        this.start = start;
        this.end = end;
    }

    public CharInterval(char c) {
        this(c, c);
    }

    public char getStart() {
        return start;
    }

    public char getEnd() {
        return end;
    }

    public boolean overlaps(char start, char end) {
        return (getStart() <= end) && (getEnd() >= start);
    }

    public boolean overlaps(@Nonnull CharInterval o) {
        return overlaps(o.getStart(), o.getEnd());
    }

    @Override
    public int compareTo(CharInterval o) {
        return Integer.compare(start, o.start);
    }

    @Nonnull
    public static String c(char c) {
        if ((c >= 32) && (c < 127))
            return Character.toString(c);
        else
            return Integer.toString(c);
    }

    @Nonnull
    public String toIntervalString() {
        if (start < end) {
            return c(start) + " .. " + c(end);
        } else {
            return c(start);
        }
    }

    @Override
    public String toString() {
        return toIntervalString();
    }
}
