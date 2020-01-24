/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
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

        public Shift(@Nonnull LRItem item, @Nonnull LRState newState) {
            super(item);
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

        @Override
        public Indexed getTarget() {
            return getNewState();
        }

        @Override
        public String toString() {
            return super.toString() + " " + newState.getIndex();
        }
    }

    public static class Reduce extends LRAction {

        // private final CstAlternativeModel reduction;
        public Reduce(@Nonnull LRItem item) {
            super(item);
        }

        // public Reduce(CstAlternativeModel reduction) { this.reduction = reduction; }
        @Override
        public Action getAction() {
            return Action.REDUCE;
        }

        @Override
        public Indexed getTarget() {
            return getProductionAlternative();
        }

        @Override
        public String toString() {
            CstAlternativeModel reduction = getProductionAlternative();
            return super.toString() + " " + reduction.getName() + " (rule " + reduction.getIndex() + ")";
        }
    }

    public static class Accept extends LRAction {

        public Accept(LRItem item) {
            super(item);
        }

        @Override
        public Action getAction() {
            return Action.ACCEPT;
        }

        @Override
        public Indexed getTarget() {
            return null;
        }
    }

    /** Used to implement nonassociative rules. */
    @SuppressWarnings("JavaLangClash")
    public static class Error extends LRAction {

        public Error(LRItem item) {
            super(item);
        }

        @Override
        public Action getAction() {
            return Action.ERROR;
        }

        @Override
        public Indexed getTarget() {
            return null;
        }
    }

    private final LRItem item;

    protected LRAction(@Nonnull LRItem item) {
        this.item = Preconditions.checkNotNull(item, "LRItem was null.");
    }

    @Nonnull
    public abstract Action getAction();

    // Called by EncodedStateMachine.
    public abstract Indexed getTarget();

    @Nonnull
    public LRItem getItem() {
        return item;
    }

    @Nonnull
    public CstAlternativeModel getProductionAlternative() {
        return getItem().getProductionAlternative();
    }

    // This is used in a Map by LRActionMapBuilder to store the set of items causing a conflict.
    @Override
    public int hashCode() {
        return Objects.hashCode(getAction()) ^ Objects.hashCode(getTarget());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof LRAction))
            return false;
        LRAction o = (LRAction) obj;
        return Objects.equals(getTarget(), o.getTarget());
    }

    @Override
    public String toString() {
        return getAction().toString();
    }
}
