/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

import com.google.common.primitives.Chars;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.anarres.polyglot.model.HelperModel;

/**
 *
 * @author shevek
 */
public class CharSet implements HelperModel.Value {

    private final List<Interval> intervals = new ArrayList<>();

    public CharSet(char c) {
        this.intervals.add(new Interval(c, c));
    }

    public CharSet(char start, char end) {
        this.intervals.add(new Interval(start, end));
    }

    private CharSet(@Nonnull Collection<? extends Interval> intervals) {
        this.intervals.addAll(intervals);
    }

    public CharSet(@Nonnull CharSet o) {
        this(o.intervals);
    }

    @CheckForNull
    public Interval findFirstOverlappingInterval(char start, char end) {
        int low = 0;
        int high = intervals.size() - 1;
        Interval result = null;

        while (high >= low) {
            int middle = (high + low) >>> 1;

            Interval candidate = intervals.get(middle);

            if (start <= candidate.end) {
                if (end >= candidate.start) {
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

    private void remove(@Nonnull Interval interval) {
        intervals.remove(interval);
    }

    private void add(@Nonnull Interval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            Interval iv = intervals.get(i);

            if (iv.start > interval.start) {
                intervals.add(i, interval);
                return;
            }
        }

        intervals.add(interval);
    }

    @Nonnull
    public CharSet union(@Nonnull CharSet chars) {
        CharSet result = new CharSet(this);

        for (Interval interval : chars.intervals) {
            for (;;) {
                Interval overlap = result.findFirstOverlappingInterval(
                        // Extend the search by one in each direction so we find a contiguous range.
                        Chars.checkedCast(Math.max(0, interval.start - 1)),
                        Chars.checkedCast(Math.min(0xffff, interval.end + 1))
                );
                if (overlap == null)
                    break;
                result.remove(overlap);
                interval = new Interval(
                        Chars.checkedCast(Math.min(interval.start, overlap.start)),
                        Chars.checkedCast(Math.max(interval.end, overlap.end))
                );
            }

            result.add(interval);
        }

        return result;
    }

    @Nonnull
    public CharSet diff(@Nonnull CharSet chars) {
        CharSet result = new CharSet(this);

        for (Interval interval : chars.intervals) {
            for (;;) {
                Interval overlap = result.findFirstOverlappingInterval(interval.start, interval.end);
                if (overlap == null)
                    break;
                result.remove(overlap);
                if (overlap.start < interval.start) {
                    result.add(new Interval(overlap.start, Chars.checkedCast(interval.start - 1)));
                }
                if (overlap.end > interval.end) {
                    result.add(new Interval(Chars.checkedCast(interval.end + 1), overlap.end));
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (Interval interval : intervals) {
            buf.append("[").append(interval).append("] ");
        }

        return buf.toString();
    }

    @Immutable
    public static class Interval {

        public final char start;
        public final char end;

        /**
         * An interval (range) of characters.
         *
         * @param start The start of the range, inclusive.
         * @param end The end of the range, inclusive.
         */
        public Interval(char start, char end) {
            this.start = start;
            this.end = end;
        }

        @Nonnull
        public static String c(char c) {
            if ((c >= 32) && (c < 127))
                return Character.toString(c);
            else
                return Integer.toString(c);
        }

        @Override
        public String toString() {
            if (start < end) {
                return c(start) + " .. " + c(end);
            } else {
                return c(start);
            }
        }
    }
}
