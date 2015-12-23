/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.Comparator;
import javax.annotation.Nonnegative;

/**
 *
 * @author shevek
 */
public interface Indexed {

    public static class IndexComparator implements Comparator<Indexed> {

        public static final IndexComparator INSTANCE = new IndexComparator();

        @Override
        public int compare(Indexed o1, Indexed o2) {
            return Integer.compare(o1.getIndex(), o2.getIndex());
        }
    }

    @Nonnegative
    public int getIndex();

}
