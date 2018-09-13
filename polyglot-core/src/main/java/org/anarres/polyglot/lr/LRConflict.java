/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementAssociativity;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class LRConflict {

    /** Ignore the key type in this Map. */
    public static class Map extends HashMap<IntSet, LRConflict> {

        public void addConflict(@Nonnull LRConflict conflict) {
            IntSet key = ImmutableIndexedSet.toIntSet(conflict.getItems().values());
            LRConflict prev = get(key);
            if (prev != null)
                if (prev.getState().getStack().size() <= conflict.getState().getStack().size())
                    return;
            put(key, conflict);
        }

        public void addConflicts(@Nonnull Collection<? extends LRConflict> conflicts) {
            for (LRConflict conflict : conflicts)
                addConflict(conflict);
        }

        @Nonnull
        public java.util.Set<? extends CstAlternativeModel> getConflictingAlternatives() {
            java.util.Set<CstAlternativeModel> out = new HashSet<>();
            for (LRConflict conflict : values())
                conflict.getConflictingAlternatives(out);
            return out;
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (LRConflict e : values()) {
                e.toStringBuilder(buf);
                // e.getState().toStringBuilder(buf);
            }
            buf.append("Proposed inlining:");
            for (CstAlternativeModel alternative : getConflictingAlternatives())
                buf.append(' ').append(alternative.getName());
            return buf.toString();
        }
    }

    private final LRState state;
    private final TokenModel token;
    private final java.util.Map<? extends LRAction, ? extends LRItem> items;

    public LRConflict(@Nonnull LRState state, @Nonnull TokenModel token, @Nonnull java.util.Map<? extends LRAction, ? extends LRItem> items) {
        this.state = state;
        this.token = token;
        this.items = items;
    }

    @Nonnull
    public LRState getState() {
        return state;
    }

    @Nonnull
    public TokenModel getToken() {
        return token;
    }

    @Nonnull
    public java.util.Map<? extends LRAction, ? extends LRItem> getItems() {
        return items;
    }

    private void getConflictingAlternatives(@Nonnull Collection<? super CstAlternativeModel> out) {
        for (java.util.Map.Entry<? extends LRAction, ? extends LRItem> e : items.entrySet()) {
            switch (e.getKey().getAction()) {
                // case ACCEPT:
                case REDUCE:
                    out.add(e.getValue().getProductionAlternative());
                    break;
                default:
                    // We're only using switch as a typesafe equals().
                    break;
            }
        }
    }

    @Nonnull
    public java.util.Set<? extends CstAlternativeModel> getConflictingAlternatives() {
        java.util.Set<CstAlternativeModel> out = new HashSet<>();
        getConflictingAlternatives(out);
        return out;
    }

    private static void toStringBuilderHeader(@Nonnull StringBuilder buf, @Nonnull LRState state, @Nonnull TokenModel token) {
        buf.append("In state ");
        buf.append(state.getName()).append(": ");
        buf.append("[ ");
        state.toStringBuilderStack(buf);
        buf.append("], on token ").append(token.getName());
    }

    public void toStringBuilderBody(@Nonnull StringBuilder buf) {
        for (java.util.Map.Entry<? extends LRAction, ? extends LRItem> e : items.entrySet()) {
            buf.append("    ");
            // f.getValue().toStringBuilderWithoutLookahead(buf);
            e.getValue().toStringBuilder(buf);
            buf.append(" (").append(e.getKey().getAction()).append(")");
            String precedence = e.getKey().getProductionAlternative().getPrecedence();
            if (precedence != null)
                buf.append(" (precedence=").append(precedence).append(")");
            CstElementAssociativity associativity = e.getKey().getItem().getAssociativity();
            if (associativity != null)
                buf.append(" (associativity=").append(associativity).append(")");
            buf.append("\n");
        }
    }

    public void toStringBuilder(@Nonnull StringBuilder buf) {
        toStringBuilderHeader(buf, state, token);
        buf.append(":\n");
        toStringBuilderBody(buf);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toStringBuilder(buf);
        return buf.toString();
    }
}
