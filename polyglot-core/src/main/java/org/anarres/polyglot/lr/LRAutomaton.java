/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizEdge;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizLabel;
import org.anarres.graphviz.builder.GraphVizScope;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.PolyglotExecutor;
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
public abstract class LRAutomaton implements GraphVizable, GraphVizScope {

    private static final Logger LOG = LoggerFactory.getLogger(LRAutomaton.class);
    private final Map<ImmutableIndexedSet<? extends LRItem>, LRState> states = new LinkedHashMap<>();
    private List<String> errors;
    private final LRConflict.Map conflicts = new LRConflict.Map();
    private final Object lock = new Object();

    @Nonnull
    @TemplateProperty
    public Collection<? extends LRState> getStates() {
        return states.values();
    }

    @Nonnull
    private LRState getState(@Nonnull ImmutableIndexedSet<? extends LRItem> items) {
        LRState state = states.get(items);
        if (state == null)
            throw new IllegalStateException("No state for " + items);
        return state;
    }

    @Nonnull
    public LRState addState(@Nonnull ImmutableIndexedSet<? extends LRItem> items, List<? extends CstProductionSymbol> stack) {
        LRState state = new LRState(stack, states.size(), items);
        states.put(items, state);
        return state;
    }

    // @ThreadSafe
    @CheckForNull
    public LRState addStateAndTransition(@Nonnull LRState source, @Nonnull ImmutableIndexedSet<? extends LRItem> targetItems, @Nonnull CstProductionSymbol symbol) {
        // public boolean addState(@Nonnull ImmutableIndexedSet<? extends LRItem> items) {
        Preconditions.checkArgument(!targetItems.isEmpty(), "Cannot add state with no items.");
        synchronized (lock) {
            LRState target = states.get(targetItems);
            boolean added = false;
            if (target == null) {
                List<CstProductionSymbol> stack = new ArrayList<>(source.getStack().size() + 1);
                stack.addAll(source.getStack());
                stack.add(symbol);
                target = addState(targetItems, stack);
                added = true;
            }
            source.addTransition(symbol, target);
            return added ? target : null;
        }
    }

    @TemplateProperty("parser.vm")
    public List<? extends String> getErrors() {
        return errors;
    }

    @Nonnull
    public LRConflict.Map getConflicts() {
        return conflicts;
    }

    @Nonnull
    protected abstract Iterable<? extends TokenModel> getLookaheads(@Nonnull LRItem item);

    // TODO: Make multithreaded.
    @Nonnull
    /* pp */ void buildMaps(@Nonnull PolyglotExecutor executor) {
        // @GuardedBy("errorMap")
        final Map<String, Integer> errorMap = new LinkedHashMap<>();

        for (LRState state : getStates()) {

            ACTION:
            {
                // MultimapBuilder.hashKeys().arrayListValues(1);
                LRMapBuilder mapBuilder = new LRMapBuilder();    // NOTTHREADSAFE
                for (LRItem item : state.getItems()) {
                    // LOG.info("Building action for " + getName() + " / " + item);
                    if (item.getIndex() == 1) { // [S' -> S, $] is always item 1.
                        mapBuilder.addAction(item, TokenModel.EOF.INSTANCE, new LRAction.Accept());
                    } else {
                        CstProductionSymbol symbol = item.getSymbol();
                        if (symbol == null) {
                            // Reduce on lookahead (LR1) or follow (LR0)
                            LRAction.Reduce reduceAction = item.getProductionAlternative().reduceActionCache;
                            for (TokenModel token : getLookaheads(item))
                                mapBuilder.addAction(item, token, reduceAction);
                            // transitionMap.get(item.getProductionAlternative().getProduction());
                        } else if (symbol.isTerminal()) {
                            // Shift.
                            LRState target = state.getTransitionMap().get(symbol);
                            mapBuilder.addAction(item, (TokenModel) symbol, target.shiftActionCache);
                        }
                    }
                }
                Collection<? extends LRConflict> stateConflicts = mapBuilder.getConflicts(state);
                state.actionMap = mapBuilder.toMap();
                state.conflicts = stateConflicts;
                conflicts.addConflicts(stateConflicts);
            }

            GOTO:
            {
                SortedMap<CstProductionModel, LRState> map = new TreeMap<>(CstProductionModel.Comparator.INSTANCE);
                for (Map.Entry<? extends CstProductionSymbol, ? extends LRState> e : state.getTransitionMap().entrySet()) {
                    CstProductionSymbol symbol = e.getKey();
                    if (symbol.isTerminal())
                        continue;
                    LRState target = e.getValue();
                    map.put((CstProductionModel) symbol, target);
                }
                state.gotoMap = map;
            }

            ERROR:
            {
                List<TokenModel> tokens = new ArrayList<>(state.getActionMap().keySet());
                Collections.sort(tokens, TokenModel.Comparator.INSTANCE);
                String error = "Expected " + tokens;
                Integer errorIndex;
                synchronized (errorMap) {
                    errorIndex = errorMap.get(error);
                    if (errorIndex == null) {
                        errorIndex = errorMap.size();
                        errorMap.put(error, errorIndex);
                    }
                }
                state.errorIndex = errorIndex;
            }

        }

        // Relies on LinkedHashMap's insertion order.
        this.errors = new ArrayList<>(errorMap.keySet());
    }

    @Override
    public void toGraphViz(GraphVizGraph graph) {
        for (LRState state : getStates()) {
            GraphVizLabel label = graph.node(this, state).label();
            label.set(state.getName()).append("\n");
            for (LRItem item : state.getItems()) {
                if (item.getPosition() > 0)
                    if (item.getIndex() > 0)
                        label.append(item).append("\n");
                if (label.length() > 1024) {
                    label.append("... (total " + state.getItems().size() + " items)\n");
                    break;
                }
            }

            TRANSITION:
            if (true) {
                for (Map.Entry<? extends CstProductionSymbol, ? extends LRState> e : state.getTransitionMap().entrySet()) {
                    CstProductionSymbol symbol = e.getKey();
                    LRState target = e.getValue();
                    GraphVizEdge edge = graph.edge(this, state, target).label(symbol.getName());
                    // edge.color("green");
                }
            }

            ACTION:
            if (false) {
                for (Map.Entry<TokenModel, LRAction> e : state.getActionMap().entrySet()) {
                    TokenModel token = e.getKey();
                    LRAction action = e.getValue();
                    switch (action.getAction()) {
                        case SHIFT: {
                            LRAction.Shift shiftAction = (LRAction.Shift) action;
                            LRState newState = shiftAction.getNewState();
                            GraphVizEdge edge = graph.edge(this, state, newState);
                            edge.label(token.getName());
                            edge.color("blue");
                            break;
                        }
                    }
                }
            }

        }

        CONFLICT:
        {
            for (Map.Entry<?, LRConflict> e : conflicts.entrySet()) {
                LRConflict conflict = e.getValue();
                LRState state = conflict.getState();
                graph.node(this, state).color("red").label().append(conflict);
            }
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + states.size() + " states)";
    }
}
