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
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.HelperModel;

/**
 *
 * @author shevek
 */
public class CharSet implements HelperModel.Value {

    private final List<CharInterval> intervals = new ArrayList<>();

    public CharSet(char c) {
        this.intervals.add(new CharInterval(c));
    }

    public CharSet(char start, char end) {
        this.intervals.add(new CharInterval(start, end));
    }

    private CharSet(@Nonnull Collection<? extends CharInterval> intervals) {
        this.intervals.addAll(intervals);
    }

    public CharSet(@Nonnull CharSet o) {
        this(o.intervals);
    }

    public CharSet(@Nonnull String s) {
        for (int i = 0; i < s.length(); i++)
            add(new CharInterval(s.charAt(i)));
    }

    @Nonnull
    public List<? extends CharInterval> getIntervals() {
        return intervals;
    }

    private void remove(@Nonnull CharInterval interval) {
        intervals.remove(interval);
    }

    private void add(@Nonnull CharInterval interval) {
        for (int i = 0; i < intervals.size(); i++) {
            CharInterval iv = intervals.get(i);

            if (iv.getStart() > interval.getStart()) {
                intervals.add(i, interval);
                return;
            }
        }

        intervals.add(interval);
    }

    @Nonnull
    public CharSet union(@Nonnull CharSet chars) {
        CharSet result = new CharSet(this);

        for (CharInterval interval : chars.intervals) {
            for (;;) {
                CharInterval overlap = CharInterval.findFirstOverlappingInterval(
                        result.getIntervals(),
                        // Extend the search by one in each direction so we find a contiguous range.
                        Chars.checkedCast(Math.max(0, interval.getStart() - 1)),
                        Chars.checkedCast(Math.min(0xffff, interval.getEnd() + 1))
                );
                if (overlap == null)
                    break;
                result.remove(overlap);
                interval = new CharInterval(
                        Chars.checkedCast(Math.min(interval.getStart(), overlap.getStart())),
                        Chars.checkedCast(Math.max(interval.getEnd(), overlap.getEnd()))
                );
            }

            result.add(interval);
        }

        return result;
    }

    @Nonnull
    public CharSet diff(@Nonnull CharSet chars) {
        CharSet result = new CharSet(this);

        for (CharInterval interval : chars.intervals) {
            for (;;) {
                CharInterval overlap = CharInterval.findFirstOverlappingInterval(result.getIntervals(), interval.getStart(), interval.getEnd());
                if (overlap == null)
                    break;
                result.remove(overlap);
                if (overlap.getStart() < interval.getStart()) {
                    result.add(new CharInterval(overlap.getStart(), Chars.checkedCast(interval.getStart() - 1)));
                }
                if (overlap.getEnd() > interval.getEnd()) {
                    result.add(new CharInterval(Chars.checkedCast(interval.getEnd() + 1), overlap.getEnd()));
                }
            }
        }

        return result;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();

        for (CharInterval interval : intervals) {
            buf.append("[").append(interval).append("] ");
        }

        return buf.toString();
    }
}
