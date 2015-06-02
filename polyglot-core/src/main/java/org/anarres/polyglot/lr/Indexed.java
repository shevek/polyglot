/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.Nonnegative;

/**
 *
 * @author shevek
 */
public interface Indexed {

    public static class Comparator implements java.util.Comparator<Indexed> {

        public static final Comparator INSTANCE = new Comparator();

        @Override
        public int compare(Indexed o1, Indexed o2) {
            return Integer.compare(o1.getIndex(), o2.getIndex());
        }
    }

    @Nonnegative
    public int getIndex();

}
