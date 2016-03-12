/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.Specifier;
import org.anarres.polyglot.model.UnaryOperator;
import org.anarres.polyglot.node.TIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It is universally true that the accepting production [S' -&gt; S ., $] must have index 1.
 *
 * @author shevek
 */
public abstract class LRItemUniverse<I extends LRItem> extends IndexedUniverse<I> {

    private static final Logger LOG = LoggerFactory.getLogger(LRItemUniverse.class);

    /** Constructs the extra S' -&gt; S production. */
    // Page 222.
    @Nonnull
    protected static CstAlternativeModel newInitialProduction(@Nonnull CstProductionModel rootProduction) {
        CstProductionModel production = new CstProductionModel(Integer.MAX_VALUE, new TIdentifier("Start"), ImmutableMultimap.<String, AnnotationModel>of());
        CstAlternativeModel alternative = CstAlternativeModel.forName(Integer.MAX_VALUE, production, new TIdentifier("<Start>"));
        CstElementModel element = new CstElementModel(
                new TIdentifier(rootProduction.getName()),
                Specifier.PRODUCTION,
                new TIdentifier(rootProduction.getName()),
                UnaryOperator.NONE);
        element.symbol = rootProduction;
        alternative.elements.add(element);

        return alternative;
    }
    protected final GrammarModel grammar;
    protected final CstAlternativeModel startProduction;

    public LRItemUniverse(@Nonnull Class<I> itemType, @Nonnull GrammarModel grammar) {
        super(itemType);
        this.grammar = grammar;
        this.startProduction = newInitialProduction(grammar.cstProductionRoot);
    }

    protected abstract void closure(@Nonnull Set<? super I> out, @Nonnull Queue<I> queue, @Nonnull I root, @Nonnull IntSet tmp);

    // Caching doesn't actually make this faster.
    /*
     private void _closure(@Nonnull Set<? super I> out, @Nonnull I root) {
     if (false) {
     closure(out, root);
     } else {
     @SuppressWarnings("unchecked")
     IndexedSet<I> cache = (IndexedSet<I>) root.closure;
     if (cache == null) {
     cache = new IndexedSet<>(this);
     closure(cache, root);
     root.closure = cache;
     }
     out.addAll(cache);
     }
     }
     */
    @Nonnull
    private ImmutableIndexedSet<? extends I> closure(@Nonnull I root) {
        // This allocates, but this method is only called once per build.
        MutableIndexedSet<I> out = new MutableIndexedSet<>(this);
        closure(out, new ArrayDeque<I>(), root, new IntOpenHashSet());
        // LOG.info("Closure of " + root + " is " + out);
        return out.toImmutableSet();
    }

    // @Nonnull protected abstract void _goto(@Nonnull Set<? super LRItem> out, @Nonnull Iterable<? extends LRItem> items, @Nonnull CstProductionSymbol symbol);
    /** This routine is meant to be allocation-free. */
    // @Override
    private void _goto(@Nonnull Set<? super I> out, @Nonnull LRState source, @Nonnull CstProductionSymbol symbol) {
        // LOG.info("Computing GOTO from " + items + " on " + symbol);
        // Preconditions.checkState(out.isEmpty(), "Output set not empty before GOTO.");
        // TODO: This allocates an iterator, which is the majority of our memory allocation.
        // If, instead, we require items to be an IndexedSet, we could walk the bitfield.
        Queue<I> tmpClosureQueue = new ArrayDeque<>();
        IntSet tmpClosureLookaheads = new IntOpenHashSet();
        // Passing source as an LRState rather than as an ImmutableIndexedSet saves
        // us a hash lookup on every call, but means that LRState has to expose
        // ImmutableIndexedSet rather than just Set to enable this allocation optimization.
        int[] indices = source.getItems().getIndices();
        // for (int index = indices.nextSetBit(0); index >= 0; index = indices.nextSetBit(index + 1)) {
        // for (I item : items) {   // Allocates an iterator.
        for (int index : indices) {
            I item = getItemByIndex(index);
            CstProductionSymbol productionSymbol = item.getSymbol();
            if (symbol.equals(productionSymbol)) {
                // The item we are looking for HAS to be the next item in the itemList.
                I follow = getItemByIndex(item.getIndex() + 1);
                // item.assertFollowedBy(follow);
                // LOG.info("Closing on " + follow);
                if (!out.contains(follow))  // Avoid allocating the Deque in the subclass.
                    closure(out, tmpClosureQueue, follow, tmpClosureLookaheads);
                Preconditions.checkState(tmpClosureQueue.isEmpty(), "Queue not empty.");
            }
        }
        // LOG.info("Computed " + out);
    }

    /**
     * Possibly adds a state to the automaton.
     *
     * @param automaton The automaton.
     * @param queue The queue on which to enqueue the new state, if added.
     * @param tmpSet The temporary, reusable LRItem set. Invariant: Empty on call.
     * @param in The starting state.
     * @param symbol The follow symbol.
     */
    // @ThreadSafe
    private void addState(@Nonnull LRAutomaton automaton, @Nonnull Queue<? super LRState> queue, @Nonnull MutableIndexedSet<I> tmpSet, @Nonnull LRState source, @Nonnull CstProductionSymbol symbol) {
        // tmpSet.clear();    // Not required, as empty by invariant.
        _goto(tmpSet, source, symbol);
        if (tmpSet.isEmpty())
            return;
        ImmutableIndexedSet<I> out = tmpSet.toImmutableSet();
        tmpSet.clear();
        LRState target = automaton.addStateAndTransition(source, out, symbol);
        if (target != null) {
            if ((target.getIndex() & 511) == 0) {
                LOG.debug("Created {} with {} remaining.", target.getName(), queue.size());
                // tmpSet.trim();  // This prevents us from continuously blowing the size of the array, but hurts the GC a lot.
            }
            queue.add(target);
        }
    }

    // @ThreadSafe
    private void buildThread(@Nonnull LRAutomaton automaton, @Nonnull BlockingQueue<LRState> queue) throws InterruptedException {
        // I think we do a lot of work repeating ourselves. Let's minimize that.
        // Invariant: A state is only "seen" when all its GOTO sets have been evaluated.
        // This doesn't make it any faster.
        // Set<Set<? extends I>> seenStates = new HashSet<>();
        // We allocate exactly one of these.
        MutableIndexedSet<I> tmpSet = new MutableIndexedSet<>(this);
        // Set<CstProductionSymbol> follow = new HashSet<>();   // This allocates.
        SymbolFilter follow = new SymbolFilter();
        for (;;) {
            LRState state = queue.poll(50, TimeUnit.MILLISECONDS);
            if (state == null)
                break;
            // if (seenStates.contains(set)) continue;
            // Invariant: 'tmp' is empty and unreferenced at the top of each loop.
            // Rather than walking over all grammar.tokens and grammar.cstProductions,
            // we walk the state set, and only handle those tokens which actually appear.
            // This optimization makes the loop about twice as fast.
            follow.clear(); // Make sure we process each follow symbol at most once.
            // for (LRItem item : state.getItems()) {   // Allocates an iterator.
            for (int index : state.getItems().getIndices()) {
                I item = getItemByIndex(index);
                CstProductionSymbol symbol = item.getSymbol();
                if (symbol == null)
                    continue;
                if (!follow.add(symbol))
                    continue;
                addState(automaton, queue, tmpSet, state, symbol);
            }
            // seenStates.add(set);
        }
    }

    // Page 224/227/232.
    @Nonnull
    protected LRAutomaton build(@Nonnull PolyglotExecutor executor, @Nonnull final LRAutomaton automaton) throws InterruptedException, ExecutionException {
        // A queue of lrstates for which we have not yet computed follows.
        // Using a PriorityBlockingQueue causes us to process shorter stacks first,
        // thus generating simpler shift-reduce conflict errors.
        // It's no slower than a LinkedBlockingQueue.
        final BlockingQueue<LRState> queue = new PriorityBlockingQueue<>(64, new Comparator<LRState>() {
            @Override
            public int compare(LRState o1, LRState o2) {
                return Integer.compare(o1.getStack().size(), o2.getStack().size());
            }
        });

        // This is S' -> S, which was added first in the constructor.
        final ImmutableIndexedSet<? extends I> initialItems = closure(getItemByIndex(0));
        LRState initialState = automaton.addState(initialItems, Collections.<CstProductionSymbol>emptyList());
        queue.add(initialState);

        // long tmStart = System.currentTimeMillis();
        executor.parallel(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                buildThread(automaton, queue);
                return null;
            }
        });
        // long tmElapsed = System.currentTimeMillis() - tmStart;
        /*
         LOG.debug("Created {} states in {} seconds at {}/second.",
         automaton.getStates().size(),
         tmElapsed / 1000,
         automaton.getStates().size() * 1000L / tmElapsed);
         */

        // LOG.debug("Building internal maps.");
        automaton.buildMaps();
        return automaton;
    }

    @Nonnull
    public abstract LRAutomaton build(@Nonnull PolyglotExecutor executor) throws InterruptedException, ExecutionException;
}
