/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class LRState implements Indexed {

    private static final Logger LOG = LoggerFactory.getLogger(LRState.class);
    // private final LRAutomaton automaton;
    private final int index;
    private final List<? extends CstProductionSymbol> stack;
    private final ImmutableIndexedSet<? extends LRItem> items;
    private final Map<CstProductionSymbol, LRState> transitionMap = new HashMap<>();
    /* pp */ SortedMap<TokenModel, LRAction> actionMap;
    /* pp */ SortedMap<CstProductionModel, LRState> gotoMap;
    /* pp */ int errorIndex;
    /* pp */ Collection<? extends LRConflict> conflicts;

    // Cache
    // /* pp */ final LRAction.Shift shiftActionCache = new LRAction.Shift(this);

    public LRState(
            // @Nonnull LRAutomaton automaton,
            @Nonnull List<? extends CstProductionSymbol> stack,
            @Nonnegative int index,
            @Nonnull ImmutableIndexedSet<? extends LRItem> items) {
        // this.automaton = automaton;
        this.index = index;
        this.stack = stack;
        this.items = Preconditions.checkNotNull(items, "Items was null.");
    }

    // @Nonnull public LRAutomaton getAutomaton() { return automaton; }
    @Override
    public int getIndex() {
        return index;
    }

    @Nonnull
    @TemplateProperty
    public String getName() {
        return "S" + getIndex();
    }

    @Nonnull
    public List<? extends CstProductionSymbol> getStack() {
        return stack;
    }

    @Nonnull
    public ImmutableIndexedSet<? extends LRItem> getItems() {
        return items;
    }

    @Nonnull
    public Map<? extends CstProductionSymbol, ? extends LRState> getTransitionMap() {
        return transitionMap;
    }

    public boolean addTransition(@Nonnull CstProductionSymbol symbol, @Nonnull LRState state) {
        LRState prev = transitionMap.put(
                Preconditions.checkNotNull(symbol, "Transition symbol was null."),
                Preconditions.checkNotNull(state, "Transition target state was null."));
        if (prev != null)
            if (!prev.equals(state))
                throw new IllegalArgumentException("Conflict: " + prev + " != " + state);
        return prev == null;
    }

    // Page 227/234.
    // @Nonnull
    @TemplateProperty
    public Map<TokenModel, LRAction> getActionMap() {
        return actionMap;
    }

    // @Nonnull
    @TemplateProperty
    public Map<CstProductionModel, LRState> getGotoMap() {
        return gotoMap;
    }

    public int getErrorIndex() {
        return errorIndex;
    }

    public void toStringBuilder(@Nonnull StringBuilder buf) {
        buf.append("State ").append(getName()).append("\n");

        STACK:
        buf.append("    Stack is ");
        for (CstProductionSymbol symbol : stack)
            buf.append(symbol.getName()).append(' ');
        buf.append("\n");

        ITEMS:
        {
            for (LRItem item : getItems()) {
                buf.append("    Item is ");
                item.toStringBuilder(buf);
                buf.append("\n");
            }
        }

        TRANSITIONS:
        for (Map.Entry<? extends CstProductionSymbol, ? extends LRState> e : getTransitionMap().entrySet())
            buf.append("    Transition is ").append(e.getKey()).append(" -> ").append(e.getValue().getName()).append("\n");

        ACTIONS:
        if (getActionMap() != null) {
            Multimap<LRAction, TokenModel> actionInverse = HashMultimap.create();
            for (Map.Entry<? extends TokenModel, ? extends LRAction> e : getActionMap().entrySet())
                actionInverse.put(e.getValue(), e.getKey());
            for (Map.Entry<LRAction, Collection<TokenModel>> e : actionInverse.asMap().entrySet())
                buf.append("    Action is ").append(e.getKey()).append(" on ").append(e.getValue()).append("\n");
            /*
             LRAction alsoAction = null;
             Set<TokenModel> alsoTokens = new HashSet<>();
             for (Map.Entry<? extends TokenModel, ? extends LRAction> e : getActionMap().entrySet()) {
             if (e.getValue().equals(alsoAction)) {
             alsoTokens.add(e.getKey());
             continue;
             }
             if (!alsoTokens.isEmpty()) {
             buf.append("      (Also on  ").append(alsoTokens).append(")\n");
             alsoTokens.clear();
             }
             alsoAction = e.getValue();
             buf.append("    Action is ").append(e.getKey()).append(" -> ").append(e.getValue()).append("\n");
             }
             if (!alsoTokens.isEmpty()) {
             buf.append("      (Also on  ").append(alsoTokens).append(")\n");
             }
             */
        } else {
            buf.append("    No ActionMap.\n");
        }

        GOTO:
        if (getGotoMap() != null) {
            // LRState alsoGoto = null;
            // Set<String> alsoProductions = new HashSet<>();
            for (Map.Entry<? extends CstProductionModel, ? extends LRState> e : getGotoMap().entrySet()) {
                /*
                 if (e.getValue().equals(alsoGoto)) {
                 alsoProductions.add(e.getKey().getName());
                 continue;
                 }
                 if (!alsoProductions.isEmpty()) {
                 buf.append("      (Also on  ").append(alsoProductions).append(")\n");
                 alsoProductions.clear();
                 }
                 alsoGoto = e.getValue();
                 */
                buf.append("    Goto is ").append(e.getKey().getName()).append(" -> ").append(e.getValue().getName()).append("\n");
            }
            // if (!alsoProductions.isEmpty()) { buf.append("      (Also on  ").append(alsoProductions).append(")\n"); }
        } else {
            buf.append("    No GotoMap.\n");
        }

        CONFLICTS:
        if (conflicts != null) {
            for (LRConflict conflict : conflicts) {
                conflict.toStringBuilderBody(buf);
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toStringBuilder(buf);
        return buf.toString();
    }
}
