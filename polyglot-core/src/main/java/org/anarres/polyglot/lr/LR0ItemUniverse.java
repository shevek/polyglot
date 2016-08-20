/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnull;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is designed to enable allocation-free manipulation of LR0Items.
 *
 * @author shevek
 */
public class LR0ItemUniverse extends LRItemUniverse<LR0Item> {

    private static final Logger LOG = LoggerFactory.getLogger(LR0ItemUniverse.class);
    /** A map from CstAlternativeModel to LR0Item(CstAlternativeModel, 0), to avoid allocation. */
    private final Map<CstAlternativeModel, LR0Item> itemMapInitial = new HashMap<>();

    public LR0ItemUniverse(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) {
        super(LR0Item.class, grammar, cstProductionRoot);

        IgnoredProductionsSet ignoredProductions = getIgnoredProductions();

        addAlternative(startProduction);
        for (CstProductionModel production : grammar.cstProductions.values()) {
            if (ignoredProductions.isIgnored(production))
                continue;
            for (CstAlternativeModel alternative : production.alternatives.values()) {
                if (ignoredProductions.isIgnored(alternative))
                    continue;
                addAlternative(alternative);
            }
        }
    }

    private void addAlternative(@Nonnull CstAlternativeModel alternative) {
        for (int i = 0; i < alternative.elements.size() + 1; i++) {
            LR0Item item = new LR0Item(size(), alternative, i);
            if (i == 0)
                itemMapInitial.put(alternative, item);
            itemList.add(item);
        }
    }

    @Nonnull
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    public LR0Item findZeroItem(@Nonnull CstAlternativeModel alternative) {
        return itemMapInitial.get(alternative);
    }

    /** This routine is meant to be allocation-free. */
    // Page 224.
    @Override
    protected void closure(Set<? super LR0Item> out, Queue<LR0Item> queue, LR0Item root, IntSet tmp) {
        // Invariant: Queue contains all unwalked items (and possibly some duplicates).
        // When an item is removed from the queue, it is added to the result.
        IgnoredProductionsSet ignoredProductions = getIgnoredProductions();
        queue.add(root);
        for (;;) {
            LR0Item item = queue.poll();
            if (item == null)
                break;
            if (!out.add(item))
                continue;
            CstProductionSymbol symbol = item.getSymbol();
            if (!(symbol instanceof CstProductionModel))
                continue;
            // LOG.info("Closing over item " + item);
            CstProductionModel subproduction = (CstProductionModel) symbol;
            for (CstAlternativeModel subalternative : subproduction.alternatives.values()) {
                if (ignoredProductions.isIgnored(subalternative))
                    continue;
                // Allocation-free version of new LR0Item(subalternative, 0);
                LR0Item subitem = findZeroItem(subalternative);
                if (!out.contains(subitem))
                    queue.add(subitem);
            }
            // LOG.info("Closure is now " + out);
        }
    }

    @Override
    public LRAutomaton build(PolyglotExecutor executor) throws InterruptedException, ExecutionException {
        return build(executor, new LR0Automaton(grammar, cstProductionRoot, getIgnoredProductions()));
    }
}
