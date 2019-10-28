/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.objects.Object2IntLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
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
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.PrecedenceComparator;
import org.anarres.polyglot.model.ProductionSymbol;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class LRAutomaton implements GraphVizable, GraphVizScope {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(LRAutomaton.class);
    @SuppressWarnings("UnusedVariable")
    private final String machineName;
    /** These are guaranteed indexed sequentially. */
    private final Map<ImmutableIndexedSet<? extends LRItem>, LRState> states = new LinkedHashMap<>();
    private List<String> errors;
    private final LRConflict.Map conflicts = new LRConflict.Map();
    private final Object lock = new Object();

    public LRAutomaton(@Nonnull String machineName) {
        this.machineName = machineName;
    }

    /** These are guaranteed indexed sequentially. */
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
    // Dragon book page 293: shift and reduce descriptions in yacc.
    @Nonnull
    /* pp */ void buildMaps(PrecedenceComparator precedenceComparator) {
        LRActionMapBuilder actionBuilder = new LRActionMapBuilder(precedenceComparator);    // NOTTHREADSAFE
        // @GuardedBy("errorMap")
        // Just for unification of identical errors.
        final Object2IntMap<String> errorMap = new Object2IntLinkedOpenHashMap<>();
        errorMap.defaultReturnValue(-1);

        for (LRState state : getStates()) {

            ACTION:
            {
                // MultimapBuilder.hashKeys().arrayListValues(1);
                actionBuilder.clear();
                for (LRItem item : state.getItems()) {
                    // LOG.info("Building action for " + getName() + " / " + item);
                    if (item.getIndex() == 1) { // [S' -> S, $] is always item 1.
                        actionBuilder.addAction(item, TokenModel.EOF.INSTANCE, new LRAction.Accept(item));
                    } else {
                        CstProductionSymbol symbol = item.getSymbol();
                        if (symbol == null) {
                            // Reduce on lookahead (LR1) or follow (LR0)
                            // LRAction.Reduce reduceAction = item.getProductionAlternative().reduceActionCache;
                            LRAction.Reduce reduceAction = item.getReduceAction();
                            for (TokenModel token : getLookaheads(item))
                                actionBuilder.addAction(item, token, reduceAction);
                            // transitionMap.get(item.getProductionAlternative().getProduction());
                        } else if (symbol.isTerminal()) {
                            // Shift.
                            LRState target = state.getTransitionMap().get(symbol);
                            // actionBuilder.addAction(item, (TokenModel) symbol, target.shiftActionCache);
                            actionBuilder.addAction(item, (TokenModel) symbol, new LRAction.Shift(item, target));
                        }
                    }
                }
                Collection<? extends LRConflict> stateConflicts = actionBuilder.getConflicts(state);
                state.actionMap = actionBuilder.toMap();
                state.conflicts = stateConflicts;
                conflicts.addConflicts(stateConflicts);
                actionBuilder.clear();
            }

            GOTO:
            {
                SortedMap<CstProductionModel, LRState> map = new TreeMap<>(CstProductionModel.IndexComparator.INSTANCE);
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
                Collections.sort(tokens, TokenModel.IndexComparator.INSTANCE);
                String error = Joiner.on(", ").join(Iterables.transform(tokens, ProductionSymbol.FUNCTION_GET_DESCRIPTIVE_NAME));
                int errorIndex = errorMap.getInt(error);
                if (errorIndex < 0) {
                    errorIndex = errorMap.size();
                    errorMap.put(error, errorIndex);
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
                        default:
                            // errorprone
                            break;
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
