/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class LRMapBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LRMapBuilder.class);

    private final LRAutomaton automaton;
    private final LRState state;
    // I think this is too sparse to be worth extending TokenMap.
    private final ImmutableSortedMap.Builder<TokenModel, LRAction> actions = ImmutableSortedMap.orderedBy(TokenModel.Comparator.INSTANCE);
    // While we might imagine that this is lossy over <TokenModel, LRItem, LRAction>,
    // the extra information is useless, and we have to elide it manually.
    private final Table<TokenModel, LRAction, LRItem> actionItems = HashBasedTable.create();

    public LRMapBuilder(LRAutomaton automaton, LRState state) {
        this.automaton = automaton;
        this.state = state;
    }

    @Nonnull
    public void addAction(@Nonnull LRItem item, @Nonnull TokenModel key, @Nonnull LRAction value) {
        Preconditions.checkNotNull(key, "TokenModel was null.");
        Preconditions.checkNotNull(value, "LRAction was null.");
        actions.put(key, value);
        actionItems.put(key, value, item);
    }

    public Collection<? extends LRConflict> getConflicts(@Nonnull LRConflict.Map conflicts) {
        Set<LRConflict> out = new HashSet<>();
        for (Map.Entry<TokenModel, Map<LRAction, LRItem>> e : actionItems.rowMap().entrySet()) {
            if (e.getValue().size() > 1) {
                LRConflict conflict = new LRConflict(state, e.getKey(), e.getValue());
                if (out.add(conflict))
                    conflicts.addConflict(conflict);
            }
        }
        return out;
    }

    public void run() {

        ACTION:
        {
            // MultimapBuilder.hashKeys().arrayListValues(1);
            for (LRItem item : state.getItems()) {
                // LOG.info("Building action for " + getName() + " / " + item);
                if (item.getIndex() == 1) { // [S' -> S, $] is always item 1.
                    addAction(item, TokenModel.EOF.INSTANCE, new LRAction.Accept());
                } else {
                    CstProductionSymbol symbol = item.getSymbol();
                    if (symbol == null) {
                        // Reduce on lookahead (LR1) or follow (LR0)
                        LRAction.Reduce reduceAction = item.getProductionAlternative().reduceActionCache;
                        for (TokenModel token : automaton.getLookaheads(item))
                            addAction(item, token, reduceAction);
                        // transitionMap.get(item.getProductionAlternative().getProduction());
                    } else if (symbol.isTerminal()) {
                        // Shift.
                        LRState target = state.getTransitionMap().get(symbol);
                        addAction(item, (TokenModel) symbol, target.shiftActionCache);
                    }
                }
            }
            state.actionMap = actions.build();
            state.conflicts = getConflicts(conflicts);
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
}
