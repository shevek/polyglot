/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.Objects;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.output.TemplateProperty;
import org.anarres.polyglot.runtime.AbstractParser;

/**
 *
 * @author shevek
 */
public abstract class LRAction {

    // Do not reorder, the parser relies on the ordinal() of these.
    public enum Action {

        SHIFT {
                    @Override
                    public int getOpcode() {
                        return AbstractParser.SHIFT;
                    }
                },
        REDUCE {
                    @Override
                    public int getOpcode() {
                        return AbstractParser.REDUCE;
                    }
                },
        ACCEPT {
                    @Override
                    public int getOpcode() {
                        return AbstractParser.ACCEPT;
                    }
                },
        ERROR {
                    @Override
                    public int getOpcode() {
                        return AbstractParser.ERROR;
                    }
                };

        public abstract int getOpcode();
    }

    public static class Shift extends LRAction {

        private final LRState newState;

        public Shift(@Nonnull LRState newState) {
            this.newState = newState;
        }

        @Override
        public Action getAction() {
            return Action.SHIFT;
        }

        @Nonnull
        public LRState getNewState() {
            return newState;
        }

        @Nonnull
        @Override
        public Indexed getTarget() {
            return newState;
        }

        @Override
        public String toString() {
            return super.toString() + " " + newState.getIndex();
        }
    }

    public static class Reduce extends LRAction {

        private final CstAlternativeModel reduction;

        public Reduce(@Nonnull CstAlternativeModel reduction) {
            this.reduction = reduction;
        }

        @Override
        public Action getAction() {
            return Action.REDUCE;
        }

        @Nonnull
        public CstAlternativeModel getRule() {
            return reduction;
        }

        @Nonnull
        @Override
        public Indexed getTarget() {
            return reduction;
        }

        @Override
        public String toString() {
            return super.toString() + " " + reduction.getName() + " (rule " + reduction.getIndex() + ")";
        }
    }

    public static class Accept extends LRAction {

        @Override
        public Action getAction() {
            return Action.ACCEPT;
        }

        @Override
        public Indexed getTarget() {
            return null;
        }
    }

    @Nonnull
    public abstract Action getAction();

    @TemplateProperty
    public abstract Indexed getTarget();

    @Override
    public int hashCode() {
        return Objects.hashCode(getAction()) ^ Objects.hashCode(getTarget());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (null == obj)
            return false;
        if (!getClass().equals(obj.getClass()))
            return false;
        LRAction o = (LRAction) obj;
        return Objects.equals(getTarget(), o.getTarget());
    }

    @Override
    public String toString() {
        return getAction().toString();
    }
}
