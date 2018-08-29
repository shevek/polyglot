/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class LR1ItemUniverse extends LRItemUniverse<LR1Item> {

    private static class TokenMap<V> {

        private final Object[] data;

        public TokenMap(@Nonnegative int size) {
            this.data = new Object[size];
        }

        @Nonnull
        @SuppressWarnings("unchecked")
        public V get(@Nonnull int index) {
            return (V) data[index];
        }

        @Nonnull
        public V get(@Nonnull TokenModel token) {
            return get(token.getIndex());
        }

        public void put(@Nonnull TokenModel token, @Nonnull V value) {
            data[token.getIndex()] = value;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(LR1ItemUniverse.class);
    // private static final boolean DEBUG = false;
    private final FirstFunction firstFunction;
    // private final FollowFunction followFunction = new FollowFunction(grammar, firstFunction);
    private final Map<CstAlternativeModel, TokenMap<LR1Item>> itemMapInitial = new HashMap<>();

    public LR1ItemUniverse(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) {
        super(LR1Item.class, grammar, cstProductionRoot);

        IgnoredProductionsSet ignoredProductions = getIgnoredProductions();
        this.firstFunction = new FirstFunction(grammar, ignoredProductions);

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
        TokenMap<LR1Item> itemMapLocal = new TokenMap<>(firstFunction.getUniverse().size());
        itemMapInitial.put(alternative, itemMapLocal);

        addAlternative(itemMapLocal, alternative, TokenModel.EOF.INSTANCE);
        for (TokenModel lookahead : grammar.tokens.values()) {
            addAlternative(itemMapLocal, alternative, lookahead);
        }
    }

    private void addAlternative(@Nonnull TokenMap<LR1Item> itemMapLocal, CstAlternativeModel alternative, @Nonnull TokenModel lookahead) {
        for (int i = 0; i < alternative.elements.size() + 1; i++) {
            LR1Item item = new LR1Item(size(), alternative, i, lookahead);
            if (i == 0)
                itemMapLocal.put(lookahead, item);
            itemList.add(item);
        }
    }

    // At worst linear in the size of FIRST(alternative.elements[0]) or something like that.
    @Nonnull
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private LR1Item findZeroItem(@Nonnull CstAlternativeModel alternative, @Nonnull TokenModel lookahead) {
        LR1Item item = itemMapInitial.get(alternative).get(lookahead);
        // Preconditions.checkState(alternative == item.getProductionAlternative(), "Wrong production in zero-item.");
        // Preconditions.checkState(lookahead == item.getLookahead(), "Wrong lookahead in zero-item.");
        // LOG.info(alternative + " / " + lookahead + " -> " + item);
        return item;
        /*
         for (int i = itemMapInitial.get(alternative); ; i++) {
         LR1Item item = itemList.get(i);
         if (item.getProduction() != alternative)
         throw new IllegalStateException("Wrong CstAlternativeModel while searching for item.");
         if (item.getPosition() != 0)
         throw new IllegalStateException("Wrong index while searching for item.");
         if (item.getLookahead() == lookahead)
         return item;
         }
         */
    }

    @Nonnull
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private LR1Item findZeroItem(@Nonnull CstAlternativeModel alternative, @Nonnull int lookahead) {
        return itemMapInitial.get(alternative).get(lookahead);
    }

    /**
     * Adds first(WXYZa) to out.
     *
     * WXYZ is held in elements.subList(start).
     * a is the given terminal.
     *
     * @param out the set into which to add tokens.
     * @param firstFunction The FirstFunction.
     * @param elements The vector of symbols to analyze.
     * @param start the starting point in the vector.
     * @param terminal the terminal considered to be at the end of the list of productions, with null representing end of input.
     * @return true iff the symbol sequence was nullable.
     */
    private void addFirst(@Nonnull Set<TokenModel> out, @Nonnull List<? extends CstElementModel> elements, @Nonnegative int start, @Nonnull TokenModel terminal) {
        if (firstFunction.addFirst(out, elements, start))
            out.add(terminal);
    }

    /** This routine is meant to be allocation-free. */
    @Override
    protected void closure(Set<? super LR1Item> out, Queue<LR1Item> queue, LR1Item root, IntSet lookaheads) {
        // Invariant: Queue contains all unwalked items (and possibly some duplicates).
        // When an item is removed from the queue, it is added to the result.
        // TokenUniverse tokenUniverse = firstFunction.getUniverse();
        IgnoredProductionsSet ignoredProductions = getIgnoredProductions();
        queue.add(root);
        for (;;) {
            LR1Item item = queue.poll();
            if (item == null)
                break;
            if (!out.add(item))
                continue;
            CstProductionSymbol symbol = item.getSymbol();
            if (!(symbol instanceof CstProductionModel))
                continue;
            CstAlternativeModel alternative = item.getProductionAlternative();
            CstProductionModel subproduction = (CstProductionModel) symbol;
            // if (DEBUG) LOG.info("Closing over " + item);
            for (CstAlternativeModel subalternative : subproduction.alternatives.values()) {
                if (ignoredProductions.isIgnored(subalternative))
                    continue;
                lookaheads.clear();
                if (firstFunction.addFirst(lookaheads, alternative.getElements(), item.getPosition() + 1))  // Walk the remainder of the parent, hence the +1
                    lookaheads.add(item.getLookahead().getIndex());    // If everything was nullable, add the terminal.
                // if (DEBUG) LOG.info("    FIRST(" + item + " = " + subalternative.elements + " ,, " + alternative.elements.subList(item.getPosition() + 1, alternative.elements.size()) + ") = " + lookaheads);

                // TODO: We could iterate the bitset directly here.
                // for (TokenModel lookahead : lookaheads) {
                // for (int lookahead = lookaheads.nextSetBit(0); lookahead >= 0; lookahead = lookaheads.nextSetBit(lookahead + 1)) {
                // The only trouble with fastutil is that we can't iterate without allocating.
                for (IntIterator it = lookaheads.iterator(); it.hasNext(); /* */) {
                    // for (int lookahead : lookaheads) {
                    int lookahead = it.nextInt();
                    // TokenModel lookahead = tokenUniverse.getItemByIndex(i);
                    // Allocation-free version of new LR1Item(subalternative, 0, lookahead);
                    LR1Item subitem = findZeroItem(subalternative, lookahead);
                    if (!out.contains(subitem)) {
                        // if (DEBUG) LOG.info("      Enqueued " + subitem);
                        queue.add(subitem);
                    }
                }
            }
        }
    }

    @Override
    public LRAutomaton build(PolyglotExecutor executor) throws InterruptedException, ExecutionException {
        return build(executor, new LR1Automaton(cstProductionRoot));
    }
}
