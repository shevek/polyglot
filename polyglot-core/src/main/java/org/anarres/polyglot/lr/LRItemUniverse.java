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
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.PolyglotExecutor;
import org.anarres.polyglot.analysis.StartChecker;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.Specifier;
import org.anarres.polyglot.model.TokenModel;
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
                UnaryOperator.NONE,
                ImmutableMultimap.<String, AnnotationModel>of());
        element.symbol = rootProduction;
        alternative.elements.add(element);

        return alternative;
    }
    protected final GrammarModel grammar;
    protected final CstProductionModel cstProductionRoot;
    protected final CstAlternativeModel startProduction;
    private final String machineName;
    private final IgnoredProductionsSet ignoredProductions;

    public LRItemUniverse(@Nonnull Class<I> itemType, @Nonnull GrammarModel grammar, @Nonnull CstProductionModel cstProductionRoot) {
        super(itemType);
        this.grammar = grammar;
        this.cstProductionRoot = cstProductionRoot;
        this.startProduction = newInitialProduction(cstProductionRoot);
        this.machineName = StartChecker.getMachineName(cstProductionRoot);
        this.ignoredProductions = new IgnoredProductionsSet(grammar, cstProductionRoot);
    }

    @Nonnull
    public String getMachineName() {
        return machineName;
    }

    @Nonnull
    public IgnoredProductionsSet getIgnoredProductions() {
        return ignoredProductions;
    }

    protected abstract void closure(@Nonnull MutableIndexedSet<? super I> out, @Nonnull Queue<I> queue, @Nonnull I root, @Nonnull IntSet tmp);

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
    /** Only used for the closure of the root element. */
    @Nonnull
    private ImmutableIndexedSet<? extends I> closure(@Nonnull I root) {
        // This allocates, but this method is only called once per build.
        MutableIndexedSet<I> out = new MutableIndexedSet<>(this);
        closure(out, new ArrayDeque<I>(), root, new IntOpenHashSet());
        // LOG.info("Closure of " + root + " is " + out);
        return out.toImmutableSet();
    }

    /**
     * This routine is meant to be allocation-free.
     * Returns the next index in sourceItems which contains an item with a DIFFERENT symbol.
     */
    @Nonnegative
    private int _goto(@Nonnull MutableIndexedSet<? super I> out, @Nonnull Queue<I> tmpClosureQueue, @Nonnull IntSet tmpClosureLookaheads, @Nonnull LRItem[] sourceItems, @Nonnegative int sourceItemIndex, @Nonnull CstProductionSymbol symbol) {
        // LOG.info("Computing GOTO from " + items + " on " + symbol);
        // Preconditions.checkState(out.isEmpty(), "Output set not empty before GOTO.");
        // TODO: This allocates an iterator, which is the majority of our memory allocation.
        // If, instead, we require items to be an IndexedSet, we could walk the bitfield.
        for (int i = sourceItemIndex; i < sourceItems.length; i++) {
            LRItem item = sourceItems[i];
            CstProductionSymbol productionSymbol = item.getSymbol();
            // If the item after the dot is the symbol we are interested in,
            // we step past it, and add to the new closure.
            if (symbol.equals(productionSymbol)) {
                // The item we are looking for HAS to be the next item in the itemList.
                I follow = getItemByIndex(item.getIndex() + 1);
                // item.assertFollowedBy(follow);
                // LOG.info("Closing on " + follow);
                if (!out.contains(follow))  // Avoid allocating the Deque in the subclass. TODO: But we don't, any more. We allocate it here.
                    closure(out, tmpClosureQueue, follow, tmpClosureLookaheads);
                Preconditions.checkState(tmpClosureQueue.isEmpty(), "Queue not empty.");
            } else {
                return i;
            }
        }
        return sourceItems.length;
        // LOG.info("Computed " + out);
    }

    /**
     * Possibly adds a state to the automaton.
     *
     * From a given source state, for a given closure of LRItems generated
     * from the source state and the chosen symbol, we call this routine to add
     * the state after the transition.
     *
     * @param automaton The automaton.
     * @param queue The queue on which to enqueue the new state, if added.
     * @param source The starting state.
     * @param targetItems The symbols comprisong the ending state.
     * @param symbol The transition symbol, terminal or nonterminal.
     */
    // @ThreadSafe
    private void addState(@Nonnull LRAutomaton automaton, @Nonnull Queue<? super LRState> queue, @Nonnull LRState source, @Nonnull ImmutableIndexedSet<I> targetItems, @Nonnull CstProductionSymbol symbol) {
        LRState target = automaton.addStateAndTransition(source, targetItems, symbol);
        if (target != null) {
            if ((target.getIndex() & 511) == 0) {
                if (LOG.isDebugEnabled()) {
                    StringBuilder buf = new StringBuilder("[ ");
                    target.toStringBuilderStack(buf);
                    buf.append("]");
                    LOG.debug("Created {} with {} remaining: {}", target.getName(), queue.size(), buf);
                    // tmpSet.trim();  // This prevents us from continuously blowing the size of the array, but hurts the GC a lot.
                }
            }
            queue.add(target);
        }
    }

    private static class LRItemSymbolComparator implements Comparator<LRItem> {

        public static final LRItemSymbolComparator INSTANCE = new LRItemSymbolComparator();
        private static final int LEFT_SORTS_FIRST = -1;
        private static final int RIGHT_SORTS_FIRST = 1;

        @Override
        public int compare(LRItem o1, LRItem o2) {
            CstProductionSymbol s1 = o1.getSymbol();
            CstProductionSymbol s2 = o2.getSymbol();
            if (s1 == s2)
                return 0;
            // Nulls sort first.
            if (s1 == null)
                return LEFT_SORTS_FIRST;
            if (s2 == null)
                return RIGHT_SORTS_FIRST;

            if (s1 instanceof TokenModel) {
                if (s2 instanceof TokenModel) {
                    return Integer.compare(s1.getIndex(), s2.getIndex());
                } else {
                    // Tokens sort first.
                    return LEFT_SORTS_FIRST;
                }
            } else {
                if (s2 instanceof TokenModel) {
                    return RIGHT_SORTS_FIRST;
                } else {
                    return Integer.compare(s1.getIndex(), s2.getIndex());
                }
            }
        }
    }

    // @ThreadSafe
    private void buildThread(@Nonnull LRAutomaton automaton, @Nonnull BlockingQueue<LRState> queue) throws InterruptedException {
        // I think we do a lot of work repeating ourselves. Let's minimize that.
        // Invariant: A state is only "seen" when all its GOTO sets have been evaluated.
        // This doesn't make it any faster.
        // Set<Set<? extends I>> seenStates = new HashSet<>();
        // We allocate exactly one of these.
        MutableIndexedSet<I> tmpTargetItems = new MutableIndexedSet<>(this);
        Queue<I> tmpClosureQueue = new ArrayDeque<>();
        IntSet tmpClosureLookaheads = new IntOpenHashSet();
        // Set<CstProductionSymbol> follow = new HashSet<>();   // This allocates.
        for (;;) {
            LRState source = queue.poll(50, TimeUnit.MILLISECONDS);
            if (source == null)
                break;
            // if (seenStates.contains(set)) continue;
            // Invariant: 'tmp' is empty and unreferenced at the top of each loop.
            // Rather than walking over all grammar.tokens and grammar.cstProductions,
            // we walk the state set, and only handle those tokens which actually appear.
            // This optimization makes the loop about twice as fast.
            // The combination of this loop and the loop in addState()->_goto() is effectively quadratic.
            // We use sorting to avoid the quadratic nature.

            LRItem[] sourceItems = source.getItemsAsArray();   // TODO: Read into an array allocated in this frame; keep a length.
            Arrays.sort(sourceItems, LRItemSymbolComparator.INSTANCE);
            // for (int index : state.getItems().getIndices()) {
            // I item = getItemByIndex(index);
            int sourceItemIndex = 0;
            while (sourceItemIndex < sourceItems.length) {
                CstProductionSymbol symbol = sourceItems[sourceItemIndex].getSymbol();
                if (symbol == null) {
                    sourceItemIndex++;
                    continue;
                }
                // The sort guarantees we process each potential 'next' symbol at most once.
                sourceItemIndex = _goto(tmpTargetItems, tmpClosureQueue, tmpClosureLookaheads, sourceItems, sourceItemIndex, symbol);
                if (tmpTargetItems.isEmpty())
                    return;
                ImmutableIndexedSet<I> targetItems = tmpTargetItems.toImmutableSet();
                tmpTargetItems.clear();
                addState(automaton, queue, source, targetItems, symbol);
            }
            // seenStates.add(set);
        }
    }

    @Nonnull
    protected abstract LRAutomaton newAutomaton();

    // Page 224/227/232.
    @Nonnull
    public LRAutomaton build(@Nonnull PolyglotExecutor executor) throws InterruptedException, ExecutionException {
        // A queue of lrstates for which we have not yet computed follows.
        // Using a PriorityBlockingQueue causes us to process shorter stacks first,
        // thus generating simpler shift-reduce conflict errors.
        // It's no slower than a LinkedBlockingQueue.
        final LRAutomaton automaton = newAutomaton();

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
        automaton.buildMaps(grammar.precedenceComparator);
        return automaton;
    }
}
