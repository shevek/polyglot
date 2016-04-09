/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.diagnoser;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.lr.FirstFunction;
import org.anarres.polyglot.lr.LRAction;
import org.anarres.polyglot.lr.LRConflict;
import org.anarres.polyglot.lr.LRDiagnosis;
import org.anarres.polyglot.lr.LRItem;
import org.anarres.polyglot.lr.TokenSet;
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
public class AlgorithmicLRDiagnoser implements LRDiagnoser {

    private static final Logger LOG = LoggerFactory.getLogger(AlgorithmicLRDiagnoser.class);

    public static class Factory implements LRDiagnoser.Factory {

        @Override
        public LRDiagnoser newDiagnoser(GrammarModel grammar, Set<? extends Option> options) {
            return new AlgorithmicLRDiagnoser(grammar);
        }
    }
    private static final boolean DEBUG = false;
    private final GrammarModel grammar;
    private final FirstFunction firstFunction;
    private final Multimap<CstProductionModel, CstAlternativeModel> productionUsage = HashMultimap.create();

    public AlgorithmicLRDiagnoser(@Nonnull GrammarModel grammar) {
        this.grammar = grammar;
        this.firstFunction = new FirstFunction(grammar);

        for (CstProductionModel production : grammar.getCstProductions()) {
            for (CstAlternativeModel alternative : production.getAlternatives().values()) {
                for (CstElementModel element : alternative.getElements()) {
                    CstProductionSymbol symbol = element.getSymbol();
                    if (symbol instanceof CstProductionModel) {
                        productionUsage.put((CstProductionModel) symbol, alternative);
                    }
                }
            }
        }
    }

    private static class Context {

        // TODO: ConcurrentNavigableMap<Object, Object> x = new ConcurrentSkipListMap<>();
        // private final Queue<State> queue = new PriorityQueue<>();
        private final Queue<State> queue = new LinkedList<>();
        // private final Int2IntMap seenWithToken = new Int2IntOpenHashMap();
        // private final Int2IntMap seenWithRoot = new Int2IntOpenHashMap();
        private final Object2IntMap<CstElementModel> seenWithToken = new Object2IntOpenHashMap<>();
        private final Object2IntMap<CstElementModel> seenWithRoot = new Object2IntOpenHashMap<>();
        /** The first (shallowest) state in which we saw the token. */
        private State tokenState = null;
    }

    @SuppressFBWarnings("SE_NO_SERIALVERSIONID")
    private static class State extends ArrayList<LRDiagnosis.ItemFrame> implements Comparable<State> {

        @Nonnull
        private transient final CstAlternativeModel needleAlternative;
        @CheckForNull
        private transient final TokenModel token;
        @Nonnegative
        private transient final int elementCount;

        public State(
                @Nonnull State parent,
                @Nonnull LRDiagnosis.ItemFrame current,
                @CheckForNull TokenModel token) {
            super(parent.size() + 1);
            addAll(parent);
            add(current);
            this.needleAlternative = current.getAlternative();
            this.token = token;
            this.elementCount = parent.elementCount + needleAlternative.getElements().size();
        }

        public State(
                @Nonnull LRDiagnosis.ItemFrame current,
                @CheckForNull TokenModel token) {
            super(1);
            add(current);
            this.needleAlternative = current.getAlternative();
            this.token = token;
            this.elementCount = needleAlternative.getElements().size();
        }

        @Nonnull
        public LRDiagnosis.Path toPath() {
            LRDiagnosis.Path path = new LRDiagnosis.Path(this);
            Collections.reverse(path);
            return path;
        }

        @Override
        public int compareTo(State o) {
            // Prefer token-less states.
            if (this.token != null) {
                if (o.token == null)
                    return 1;
            } else if (this.token == null) {
                if (o.token != null)
                    return -1;
            }

            // Prefer shorter stack in diagnostics.
            int cmp = Integer.compare(size(), o.size());
            if (cmp != 0)
                return cmp;

            // Prefer smaller element count in diagnostics.
            return Integer.compare(elementCount, o.elementCount);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            for (LRDiagnosis.ItemFrame frame : this) {
                buf.append(frame.getAlternative().getName()).append(' ');
            }
            buf.append(" / ").append(needleAlternative.getName());
            return buf.toString();
        }
    }

    @SuppressWarnings("unused")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private boolean isSeen(@Nonnull Int2IntMap seenAtDepth, @Nonnull State state, @Nonnull CstAlternativeModel parentAlternative) {
        // CstProductionModel parentProduction = parentAlternative.getProduction();
        int parentDepth = seenAtDepth.get(parentAlternative.getIndex());
        if (parentDepth != 0) {
            if (parentDepth < state.size()) {
                if (DEBUG)
                    LOG.debug("    Previously (partial) seen " + parentAlternative.getName() + " at depth " + parentDepth + "; not interested in depth " + state.size());
                return true;
            }
        }
        seenAtDepth.put(parentAlternative.getIndex(), state.size());
        return false;
    }

    @SuppressWarnings("unused")
    @SuppressFBWarnings("UPM_UNCALLED_PRIVATE_METHOD")
    private boolean isSeen(@Nonnull Object2IntMap<CstElementModel> seenAtDepth, @Nonnull State state, @Nonnull CstElementModel parentElement) {
        // CstProductionModel parentProduction = parentAlternative.getProduction();
        int parentDepth = seenAtDepth.getInt(parentElement);
        if (parentDepth != 0) {
            if (parentDepth < state.size()) {
                if (DEBUG)
                    LOG.debug("    Previously (partial) seen " + parentElement + " at depth " + parentDepth + "; not interested in depth " + state.size());
                return true;
            }
        }
        seenAtDepth.put(parentElement, state.size());
        return false;
    }

    @CheckForNull
    private LRDiagnosis.Path diagnose(@Nonnull Context context) {
        TokenSet tokens = new TokenSet(firstFunction.getUniverse());

        String prefix = "    ";

        STATE:
        while (!context.queue.isEmpty()) {
            if (context.queue.size() > 1000000)
                break;

            State state = context.queue.remove();

            // Discard any state which is just REALLY SLOW to find the token.
            // This MAY cause a failure to find a path, but saves our RAM.
            if (context.tokenState != null)
                if (state.token != null)
                    if (state.size() > context.tokenState.size() + 1)
                        continue;

            CstAlternativeModel needleAlternative = state.needleAlternative;
            CstProductionModel needleProduction = needleAlternative.getProduction();
            TokenModel token = state.token;

            if (DEBUG) {
                LOG.debug("Entering " + state);
                LOG.debug(prefix + "At depth " + state.size() + " looking for " + ((token == null) ? "<root>" : token.getName()));
            }

            if (token == null) {
                if (needleProduction == grammar.cstProductionRoot) {
                    if (DEBUG)
                        LOG.debug(prefix + "Found root; returning.");
                    return state.toPath();
                }
            }

            Object2IntMap<CstElementModel> seenAtDepth = (token != null) ? context.seenWithToken : context.seenWithRoot;

            PARENT_ALTERNATIVE:
            for (CstAlternativeModel parentAlternative : productionUsage.get(needleProduction)) {
                /*
                 if (isSeen(seenAtDepth, state, parentAlternative))
                 continue;
                 */

                if (DEBUG)
                    LOG.debug("  Searching " + parentAlternative + " for " + needleProduction.getName());

                int parentElementIndex = 0;
                for (CstElementModel parentElement : parentAlternative.getElements()) {
                    if (parentElement.getSymbol() == needleProduction) {
                        if (isSeen(seenAtDepth, state, parentElement))
                            continue;

                        // if (DEBUG) LOG.debug(prefix + "Found " + parentElement);
                        if (token != null) {
                            // We have not yet found our token, so we need to find it in a FIRST() set.
                            tokens.clear();
                            boolean epsilon = firstFunction.addFirst(tokens, parentAlternative.getElements(), parentElementIndex + 1);
                            if (DEBUG)
                                LOG.debug(prefix + "FIRST tokens after " + (parentElementIndex + 1) + " are " + tokens + " ; epsilon=" + epsilon);
                            if (tokens.contains(token)) {
                                if (DEBUG)
                                    LOG.debug(prefix + "Token: Found following " + token.getName() + "; now searching for root");
                                LRDiagnosis.ItemFrame frame = new LRDiagnosis.ItemFrame(parentAlternative, parentElementIndex, "Token '" + token.getName() + "' in FIRST("
                                        + parentAlternative.elements.subList(parentElementIndex + 1, parentAlternative.elements.size())
                                        // + "'" + parentAlternative.getName() + "', " + (parentElementIndex + 1)
                                        + ")");
                                State child = new State(state, frame, null);
                                // Not necessarily the fastest path to the root, but the fastest path to the token.
                                context.queue.clear();
                                context.queue.add(child);
                                if (context.tokenState == null)
                                    context.tokenState = child;
                                // else notreached, due to the clear()
                                continue STATE;
                            } else if (epsilon) {
                                LRDiagnosis.ItemFrame frame = new LRDiagnosis.ItemFrame(parentAlternative, parentElementIndex, "Element followed by possible epsilon; inspecting parents");
                                // If the production was empty, then it may be used in a parent
                                // production which contains the token.
                                if (DEBUG)
                                    LOG.debug(prefix + "Reduced to epsilon; searching parent for token.");
                                context.queue.add(new State(state, frame, token));
                            } else {
                                // This alternative cannot be followed by the token in that parent. Noop.
                            }
                        } else {
                            // if (DEBUG) LOG.debug(prefix + "Searching parent for root.");
                            // We have found our token and we are aiming for the root.
                            LRDiagnosis.ItemFrame frame = new LRDiagnosis.ItemFrame(parentAlternative, parentElementIndex);
                            context.queue.add(new State(state, frame, null));
                            continue PARENT_ALTERNATIVE;
                        }
                    }
                    parentElementIndex++;
                }

            }
        }

        // LOG.warn("Failed to find a path to root.");
        if (context.tokenState != null) {
            LRDiagnosis.Path path = new LRDiagnosis.Path(context.tokenState);
            path.add(new LRDiagnosis.MessageFrame("Failed to find a path to root."));
            Collections.reverse(path);
            return path;
        }
        return null;
    }

    @Override
    public LRDiagnosis diagnose(@Nonnull LRConflict conflict) {
        LRDiagnosis out = new LRDiagnosis(conflict);

        // LOG.debug("Diagnosing\n" + conflict);
        TokenModel token = conflict.getToken();
        /*
         DEBUG = (token.getName().equals("tok_lpar"));
         if (!DEBUG)
         return out;
         */
        for (Map.Entry<? extends LRAction, ? extends LRItem> e : conflict.getItems().entrySet()) {
            LRAction action = e.getKey();

            CstAlternativeModel alternative = e.getValue().getProductionAlternative();

            if (DEBUG)
                LOG.debug("Justifying " + action + " on " + token.getName());
            Context context = new Context();
            LRDiagnosis.ItemFrame frame = new LRDiagnosis.ItemFrame(alternative, e.getValue().getPosition(), action + " on " + token.getName());

            LRDiagnosis.Path path = null;
            switch (action.getAction()) {
                case SHIFT:
                    // context.seenWithRoot.put(alternative.getIndex(), 1);
                    context.queue.add(new State(frame, null));
                    path = diagnose(context);
                    break;
                case REDUCE:
                    // context.seenWithToken.put(alternative.getIndex(), 1);
                    context.queue.add(new State(frame, token));
                    path = diagnose(context);
                    break;
                default:
                    // Avoid FB warning.
                    break;
            }

            if (path == null)
                path = new LRDiagnosis.Path(Arrays.asList(new LRDiagnosis.MessageFrame("Failed to find a useful diagnosis.")));
            // LRDiagnosis.Path path = new LRDiagnosis.Path(context.result == null ? Collections.<LRDiagnosis.Frame>emptyList() : context.result);
            out.put(action, path);
        }

        // LOG.info("Diagnosis is:\n" + out);
        return out;
    }
}
