/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import it.unimi.dsi.fastutil.ints.Int2IntMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenCustomHashMap;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizLabel;
import org.anarres.graphviz.builder.GraphVizScope;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.lr.IntHashStrategy;
import org.anarres.polyglot.lr.TokenMap;
import org.anarres.polyglot.lr.TokenSet;
import org.anarres.polyglot.lr.TokenUniverse;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.AnnotationUtils;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Deterministic Finite Automaton, built from an {@link NFA}.
 *
 * GraphViz renderings of this object tend to be big enough to cause
 * difficulties either for GraphViz or for the image viewer.
 *
 * @author shevek
 */
public class DFA implements GraphVizable, GraphVizScope {

    private static final Logger LOG = LoggerFactory.getLogger(DFA.class);
    // Needs to be mutable for minimization.
    public final List<State> states;

    /** The argument list may/will be mutated by reference. */
    public DFA(@Nonnull List<State> states) {
        this.states = states;
    }

    @TemplateProperty
    @Nonnull
    public List<? extends State> getStates() {
        return states;
    }

    // Int2ObjectArrayMap might be faster here.
    private static class IntToStateMultimap extends Int2ObjectOpenCustomHashMap<BitSet> {

        public IntToStateMultimap() {
            super(IntHashStrategy.INSTANCE);
        }

        public void put(@Nonnull int key, @Nonnull State dfaState) {
            BitSet value = get(key);
            if (value == null) {
                value = new BitSet();
                put(key, value);
            }
            value.set(dfaState.getIndex());
        }
    }

    private static final boolean DEBUG_MINIMIZE = false;
    private static final boolean DEBUG_REBUILD = false;

    private static void assertMinimizeKosher(@Nonnull Int2IntMap stateToGroup, @Nonnull IntToStateMultimap groupToStates) {
        for (Int2IntMap.Entry e : stateToGroup.int2IntEntrySet()) {
            BitSet states = groupToStates.get(e.getIntValue());
            if (!states.get(e.getIntKey()))
                throw new IllegalStateException("State " + e.getIntKey() + " not found in group " + e.getIntValue() + ": " + states);
        }
        for (Int2ObjectMap.Entry<BitSet> e : groupToStates.int2ObjectEntrySet()) {
            Integer group = e.getIntKey();
            BitSet groupStates = e.getValue();
            if (groupStates.isEmpty())
                throw new IllegalStateException("Group " + group + " is unexpectedly empty.");
            for (int dfaStateIdx = groupStates.nextSetBit(0); dfaStateIdx >= 0; dfaStateIdx = groupStates.nextSetBit(dfaStateIdx + 1)) {
                if (stateToGroup.get(dfaStateIdx) != group)
                    throw new IllegalStateException("State " + dfaStateIdx + " unexpectedly found in group " + group + ": " + groupStates);
            }
        }
    }

    @Nonnull
    public void minimize() {
        // IndexedUniverse universe = new IndexedUniverse(State.class, states);

        // Group indices: initially: Non-accepting = 0; all others = token index. -1 is invalid.
        int groupIndex = 0; // The next group index to allocate, in case token indices aren't sequential and dense (they aren't).
        Int2IntMap stateToGroup = new Int2IntOpenCustomHashMap(IntHashStrategy.INSTANCE);
        IntToStateMultimap groupToStates = new IntToStateMultimap();
        for (State state : getStates()) {
            int group = state.getAcceptTokenIndex();
            stateToGroup.put(state.getIndex(), group);
            groupToStates.put(group, state);
            groupIndex = Math.max(groupIndex, group + 1);
        }
        assertMinimizeKosher(stateToGroup, groupToStates);

        IntToStateMultimap dstGroupToStates = new IntToStateMultimap();

        boolean done;
        do {
            done = true;
            // We mutate this as we go, so let's copy it.
            for (BitSet groupStates : new ArrayList<>(groupToStates.values())) {
                if (DEBUG_MINIMIZE)
                    LOG.debug("Group " /* + e.getIntKey()*/ + ": " + groupStates);

                char start = 0;
                char end;
                do {
                    end = (char) 0xffff;

                    boolean transitionFound = false;
                    // For each state in the group:
                    for (int dfaStateIdx = groupStates.nextSetBit(0); dfaStateIdx >= 0; dfaStateIdx = groupStates.nextSetBit(dfaStateIdx + 1)) {
                        State dfaState = states.get(dfaStateIdx);

                        // We're looking for the shortest common interval in all transitions starting from 'start'.
                        Transition overlap = CharInterval.findFirstOverlappingInterval(dfaState.getTransitions(), start, end);
                        if (overlap != null) {
                            if (overlap.getStart() > start) {
                                // Consider the previous region only.
                                // But we didn't find an actual transition here.
                                end = (char) (overlap.getStart() - 1);
                            } else {
                                transitionFound = true;
                                if (overlap.getEnd() < end) {
                                    // And we might be talking about a restricted region.
                                    end = overlap.getEnd();
                                }
                            }
                        }
                    }

                    // If we found no transition on the interval, then everybody behaved the same.
                    if (transitionFound) {
                        if (DEBUG_MINIMIZE)
                            LOG.debug("Transition found on " + new CharInterval(start, end));
                        dstGroupToStates.clear();
                        for (int dfaStateIdx = groupStates.nextSetBit(0); dfaStateIdx >= 0; dfaStateIdx = groupStates.nextSetBit(dfaStateIdx + 1)) {
                            State dfaState = states.get(dfaStateIdx);
                            Transition transition = CharInterval.findFirstOverlappingInterval(dfaState.getTransitions(), start, end);
                            int dstGroup = (transition == null) ? -1 : stateToGroup.get(transition.destination.index);
                            if (DEBUG_MINIMIZE)
                                LOG.debug("State " + dfaState.index + ": " + transition + " to group " + dstGroup);
                            dstGroupToStates.put(dstGroup, dfaState);
                        }
                        if (dstGroupToStates.size() > 1) {
                            if (DEBUG_MINIMIZE)
                                LOG.debug("Group requires splitting: " + dstGroupToStates);
                            int partitionIndex = 0;
                            for (BitSet partitionStates : dstGroupToStates.values()) {
                                if (partitionIndex == 0) {
                                    // We drop the first partition back into the existing group, by reference.
                                    groupStates.clear();
                                    groupStates.or(partitionStates);
                                } else {
                                    // All other partitions become new groups.
                                    int group = groupIndex++;
                                    groupToStates.put(group, partitionStates);
                                    for (int partitionStateIdx = partitionStates.nextSetBit(0); partitionStateIdx >= 0; partitionStateIdx = partitionStates.nextSetBit(partitionStateIdx + 1))
                                        stateToGroup.put(partitionStateIdx, group);
                                }
                                partitionIndex++;
                            }
                            done = false;

                            if (DEBUG_MINIMIZE)
                                LOG.debug("After split: " + groupToStates);
                            assertMinimizeKosher(stateToGroup, groupToStates);
                        }
                    }

                    // Look for the next character range.
                    start = (char) (end + 1);
                } while (end != (char) 0xffff);
            }
        } while (!done);

        if (DEBUG_MINIMIZE) {
            LOG.debug("After initial: G-S: " + groupToStates);
            LOG.debug("After initial: S-G: " + stateToGroup);
        }

        BitSet stateToRemove = new BitSet(states.size());
        for (BitSet groupStates : groupToStates.values()) {
            int dfaStateKeep = groupStates.nextSetBit(0);
            for (int dfaStateRemove = groupStates.nextSetBit(dfaStateKeep + 1); dfaStateRemove >= 0; dfaStateRemove = groupStates.nextSetBit(dfaStateRemove + 1))
                stateToRemove.set(dfaStateRemove);
        }

        remap(stateToGroup, stateToRemove);
    }

    @Nonnull
    private State remapState(@Nonnull List<State> outStates, @Nonnull State in, @Nonnull Int2IntMap stateToGroup, @Nonnull Int2IntMap groupToIndex) {
        int group = stateToGroup.get(in.index);
        int index = groupToIndex.get(group);
        return outStates.get(index);
    }

    private void remap(@Nonnull Int2IntMap stateToGroup, @Nonnull BitSet stateToRemove) {
        if (DEBUG_REBUILD)
            LOG.debug("Removing " + stateToRemove);

        Int2IntMap groupToIndex = new Int2IntOpenCustomHashMap(IntHashStrategy.INSTANCE);
        List<State> outStates = new ArrayList<>(stateToGroup.size());
        for (State dfaState : states) {
            if (DEBUG_REBUILD)
                LOG.debug("Remapping " + dfaState);
            if (stateToRemove.get(dfaState.index))
                continue;
            int group = stateToGroup.get(dfaState.index);
            int index = outStates.size();
            outStates.add(new State(index, dfaState.nfaStates, dfaState.acceptToken));
            groupToIndex.put(group, index);
        }

        for (State dfaState : states) {
            if (stateToRemove.get(dfaState.index))
                continue;
            State outState = remapState(outStates, dfaState, stateToGroup, groupToIndex);
            for (Transition dfaTransition : dfaState.transitions) {
                State outDstState = remapState(outStates, dfaTransition.getDestination(), stateToGroup, groupToIndex);
                outState.addTransition(new Transition(dfaTransition.getStart(), dfaTransition.getEnd(), outDstState));
            }
            for (CustomTransition dfaTransition : dfaState.customTransitions) {
                State outDstState = remapState(outStates, dfaTransition.getDestination(), stateToGroup, groupToIndex);
                outState.addCustomTransition(dfaTransition.getCustomName(), outDstState);
            }
        }

        // We violated this invariant on the way in.
        for (State dfaState : outStates) {
            // TODO: Make sure that we don't have duplicate transitions here.
            Collections.sort(dfaState.transitions);
        }

        states.clear();
        states.addAll(outStates);
        if (DEBUG_REBUILD) {
            for (int i = 0; i < states.size(); i++) {
                if (states.get(i).index != i)
                    throw new IllegalStateException("Bad state at index " + i + ": " + states.get(i));
            }
            LOG.debug("States: " + states);
        }
    }

    @Override
    public void toGraphViz(GraphVizGraph graph) {
        for (int i = 0; i < states.size(); i++) {
            State state = states.get(i);
            GraphVizLabel label = graph.node(this, state).label();
            label.set(String.valueOf(i));
            if (state.acceptToken != null)
                label.append(" (").append(state.acceptToken.getName()).append(")");
            for (Transition transition : state.transitions) {
                label = graph.edge(this, state, transition.destination).label();
                if (!label.isEmpty())
                    label.append(", ");
                label.append(transition.toIntervalString());
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < states.size(); i++)
            buf.append(i).append(": ").append(states.get(i)).append("\n");
        return buf.toString();
    }

    /** A factory which transforms an {@link NFA} into a {@link DFA}. */
    public static class Builder {

        private final ErrorHandler errors;
        private final GrammarModel grammar;
        private final TokenMask mask;
        private final NFA nfa;

        public Builder(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar, @Nonnull TokenMask mask, @Nonnull NFA nfa) {
            this.errors = errors;
            this.grammar = grammar;
            this.mask = mask;
            this.nfa = Preconditions.checkNotNull(nfa, "NFA was null.");
        }

        // This is a parallel array and should go into the NFA?
        @Nonnull
        private BitSet[] buildEClosureCache() {
            BitSet[] eclosures = new BitSet[nfa.states.length];

            for (int nfaStateIdx = 0; nfaStateIdx < nfa.states.length; nfaStateIdx++) {
                // LOG.info("e-closure of " + nfaStateIdx);

                BitSet closure = new BitSet();
                buildEClosureCache(closure, nfaStateIdx);
                eclosures[nfaStateIdx] = closure;
            }

            return eclosures;
        }

        private void buildEClosureCache(@Nonnull BitSet out, @Nonnegative int nfaState) {
            // We could treat our existing walk as a cache here, but I don't think it's worth it.
            // However, it's worth making sure we don't recurse into any given path a second time.
            if (out.get(nfaState))
                return;
            out.set(nfaState);
            // LOG.info("includes " + nfaState);

            for (NFA.Transition transition : nfa.states[nfaState].transitions) {
                if (transition == null) // I'd like to say this was notreached, but it's handled rigorously.
                    continue;
                if (!transition.isEpsilon()) // custom transitions are never epsilon.
                    continue;
                buildEClosureCache(out, transition.destination);
            }
        }

        @Nonnull
        private BitSet eclosure(@Nonnull BitSet[] eclosures, @Nonnull BitSet nfaStates) {
            BitSet out = new BitSet();
            for (int i = nfaStates.nextSetBit(0); i >= 0; i = nfaStates.nextSetBit(i + 1))
                out.or(eclosures[i]);
            return out;
        }

        /**
         * Finds the lowest numbered token accepted by any of the given NFA states.
         *
         * @param nfaStates The set of NFA states from which to accept tokens.
         * @return the lowest numbered token accepted by an of the given NFA states.
         */
        @CheckForNull
        private TokenModel getAcceptForState(@Nonnull BitSet nfaStates) {
            TokenModel acceptToken = null;
            for (int nfaStateIdx = nfaStates.nextSetBit(0); nfaStateIdx >= 0; nfaStateIdx = nfaStates.nextSetBit(nfaStateIdx + 1)) {
                NFA.State nfaState = nfa.states[nfaStateIdx];
                if (nfaState.accept != null) {
                    TokenModel token = nfaState.accept;
                    if (acceptToken == null) {
                        acceptToken = token;
                    } else if (acceptToken.getIndex() > token.getIndex()) {
                        mask.mask(acceptToken, token);
                        acceptToken = token;
                    } else {
                        mask.mask(token, acceptToken);
                    }
                    // else if (LOG.isDebugEnabled())
                    // LOG.debug("Rejecting " + token + " in favour of previously declared " + acceptToken);
                }
            }
            mask.accept(acceptToken);
            return acceptToken;
        }

        @Nonnull
        private DFA.State getDfaState(@Nonnull List<DFA.State> dfaStates, @Nonnull Map<BitSet, DFA.State> dfaStateMap, @Nonnull BitSet dstNfaStates) {
            State dfaStateTarget = dfaStateMap.get(dstNfaStates);

            if (dfaStateTarget == null) {
                TokenModel acceptToken = getAcceptForState(dstNfaStates);
                dfaStateTarget = new State(dfaStates.size(), dstNfaStates, acceptToken);
                dfaStates.add(dfaStateTarget);
                dfaStateMap.put(dstNfaStates, dfaStateTarget);
            }

            return dfaStateTarget;
        }

        @Nonnull
        public DFA build() {
            BitSet[] eclosures = buildEClosureCache();
            List<DFA.State> dfaStates = new ArrayList<>();
            Map<BitSet, DFA.State> dfaStateMap = new HashMap<>();

            State dfaState = new State(0, eclosures[0], getAcceptForState(eclosures[0]));
            dfaStates.add(dfaState);
            dfaStateMap.put(eclosures[0], dfaState);

            for (int dfaStateIdx = 0; dfaStateIdx < dfaStates.size(); dfaStateIdx++) {
                dfaState = dfaStates.get(dfaStateIdx);

                char start = 0;
                char end;

                do {
                    BitSet dstNfaStates = new BitSet();
                    end = (char) 0xffff;
                    boolean transitionFound = false;

                    // We are iterating over a previously constructed closure.
                    for (int nfaStateIdx = dfaState.nfaStates.nextSetBit(0); nfaStateIdx >= 0; nfaStateIdx = dfaState.nfaStates.nextSetBit(nfaStateIdx + 1)) {
                        NFA.State nfaState = nfa.states[nfaStateIdx];
                        for (NFA.Transition nfaTransition : nfaState.transitions) {
                            if (nfaTransition == null)
                                continue;
                            // It's an epsilon-transition, and the destination is already included in nfaStates.
                            if (nfaTransition.isEpsilon())
                                continue;
                            if (nfaTransition.isCustom())
                                continue;

                            // We're looking for the shortest common interval in all transitions starting from 'start'.
                            NFA.CharSetTransition c = (NFA.CharSetTransition) nfaTransition;
                            CharInterval overlap = CharInterval.findFirstOverlappingInterval(c.chars.getIntervals(), start, end);
                            if (overlap != null) {
                                if (overlap.getStart() > start) {
                                    // Consider the previous region only.
                                    // But we didn't find an actual transition here.
                                    end = (char) (overlap.getStart() - 1);
                                } else {
                                    dstNfaStates.set(nfaTransition.destination);
                                    transitionFound = true;
                                    if (overlap.getEnd() < end) {
                                        // And we might be talking about a restricted region.
                                        end = overlap.getEnd();
                                    }
                                }
                            }

                        }
                    }

                    if (transitionFound) {
                        dstNfaStates = eclosure(eclosures, dstNfaStates);
                        State dfaStateTarget = getDfaState(dfaStates, dfaStateMap, dstNfaStates);
                        // These are generated in strictly ascending order.
                        dfaState.addTransition(new Transition(start, end, dfaStateTarget));
                    }

                    // Look for the next character range.
                    start = (char) (end + 1);
                } while (end != (char) 0xffff);

                for (int nfaStateIdx = dfaState.nfaStates.nextSetBit(0); nfaStateIdx >= 0; nfaStateIdx = dfaState.nfaStates.nextSetBit(nfaStateIdx + 1)) {
                    NFA.State nfaState = nfa.states[nfaStateIdx];
                    for (NFA.Transition nfaTransition : nfaState.transitions) {
                        if (!nfaTransition.isCustom())
                            continue;
                        NFA.CustomTransition c = (NFA.CustomTransition) nfaTransition;

                        BitSet dstNfaStates = new BitSet();
                        dstNfaStates.set(c.getDestination());
                        dstNfaStates = eclosure(eclosures, dstNfaStates);
                        State dfaStateTarget = getDfaState(dfaStates, dfaStateMap, dstNfaStates);
                        dfaState.addCustomTransition(c.getName(), dfaStateTarget);
                    }
                }

            }   // foreach dfaState

            return new DFA(dfaStates);
        }
    }

    // MUST use identity equality.
    public static class State implements Indexed {

        private final int index;
        private final BitSet nfaStates;
        public final TokenModel acceptToken;
        public final List<Transition> transitions = new ArrayList<>();
        private final List<CustomTransition> customTransitions = new ArrayList<>();

        public State(int index, BitSet nfaStates, @CheckForNull TokenModel accept) {
            this.index = index;
            this.nfaStates = nfaStates;
            this.acceptToken = accept;

            // LOG.debug("DFA state " + index + " represents " + nfaStates);
        }

        @TemplateProperty
        @Nonnegative
        @Override
        public int getIndex() {
            return index;
        }

        @CheckForNull
        public TokenModel getAcceptToken() {
            return acceptToken;
        }

        @TemplateProperty
        @CheckForNull
        public String getAcceptTokenName() {
            return (acceptToken == null) ? "" : acceptToken.getName();
        }

/*
        @TemplateProperty
        public boolean isAcceptTokenPredicated() {
            return (acceptToken == null) ? false : acceptToken.hasAnnotation(AnnotationName.LexerPredicated);
        }
*/

        @TemplateProperty
        @CheckForSigned
        public int getAcceptTokenIndex() {
            return (acceptToken == null) ? 0 : acceptToken.getIndex();
        }

        @TemplateProperty
        @Nonnull
        public List<? extends Transition> getTransitions() {
            return transitions;
        }

        private void addTransition(@Nonnull Transition transition) {
            if (!transitions.isEmpty())
                if (transition.compareTo(Iterables.getLast(transitions)) <= 0)
                    throw new IllegalArgumentException("Transition out of order: " + transition);
            transitions.add(transition);
        }

        public void addCustomTransition(@Nonnull String name, @Nonnull State destination) {
            this.customTransitions.add(new CustomTransition(destination, name));
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            buf.append("[").append(index).append("]:");
            if (acceptToken != null)
                buf.append("(").append(acceptToken.getName()).append(") ");
            for (Transition transition : transitions)
                buf.append(transition).append(",");
            for (CustomTransition customTransition : customTransitions)
                buf.append(customTransition).append(",");
            return buf.toString() /*+ " " + nfaStates*/;
        }
    }

    @Immutable
    public static final class Transition extends CharInterval {

        private final State destination;

        public Transition(char start, char end, @Nonnull State destination) {
            super(start, end);
            this.destination = destination;
        }

        /** Converts the char to valid Java code for constructing it. */
        @Nonnull
        private static String toCode(char c) {
            if (c < 128 && Character.isLetterOrDigit(c))
                return new String(new char[]{'\'', c, '\''});
            if ("!\"#$%&()*+,-./:;<=>?@[]^_`{|}~".indexOf(c) != -1)
                return new String(new char[]{'\'', c, '\''});
            switch (c) {
                case '\'':
                    return "'\\''";
                case '\\':
                    return "'\\\\'";
                case '\t':
                    return "'\\t'";
                case '\n':
                    return "'\\n'";
                case '\r':
                    return "'\\r'";
            }
            return Integer.toString(c);
        }

        @TemplateProperty
        @Nonnull
        public String getStartCode() {
            return toCode(getStart());
        }

        @TemplateProperty
        @Nonnull
        public String getEndCode() {
            return toCode(getEnd());
        }

        @Nonnull
        public State getDestination() {
            return destination;
        }

        @Override
        public int hashCode() {
            return getStart() << 16 ^ getEnd() ^ System.identityHashCode(destination);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (null == obj)
                return false;
            if (!getClass().equals(obj.getClass()))
                return false;
            Transition o = (Transition) obj;
            return getStart() == o.getStart()
                    && getEnd() == o.getEnd()
                    && destination == o.destination;
        }

        @Override
        public String toString() {
            return "[" + toIntervalString() + "]->" + getDestination().index;
        }

    }

    public static final class CustomTransition {

        private final State destination;
        private final String customName;

        public CustomTransition(State destination, String customName) {
            this.destination = destination;
            this.customName = customName;
        }

        public State getDestination() {
            return destination;
        }

        @Nonnull
        public String getCustomName() {
            return customName;
        }

        @Override
        public int hashCode() {
            return getDestination().hashCode() ^ getCustomName().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (null == obj)
                return false;
            if (!getClass().equals(obj.getClass()))
                return false;
            CustomTransition o = (CustomTransition) obj;
            return getDestination().equals(o.getDestination())
                    && getCustomName().equals(o.getCustomName());
        }

        @Override
        public String toString() {
            return getDestination().index + ":`" + getCustomName() + "`";
        }
    }

    /**
     * A data structure for detecting which {@link TokenModel tokens}
     * can never be matched by any DFA state.
     */
    public static class TokenMask extends TokenMap<TokenSet> {

        /**
         * Initially, all tokens are maskable, as none of them has an accepting state.
         *
         * @param universe The universe of tokens.
         */
        public TokenMask(@Nonnull TokenUniverse universe, @Nonnull String machineName) {
            super(universe);
            for (TokenModel token : universe.getItems()) {
                if (token.hasAnnotation(AnnotationName.LexerIgnore))
                    continue;
                if (!AnnotationUtils.isIncluded(token, AnnotationName.LexerInclude, AnnotationName.LexerExclude, machineName))
                    continue;
                if (token.hasAnnotation(AnnotationName.LexerAllowMasking))
                    continue;
                if (token instanceof TokenModel.EOF)
                    continue;
                put(token, new TokenSet(universe));
            }
        }

        public TokenMask(@Nonnull GrammarModel grammar, @Nonnull String machineName) {
            this(new TokenUniverse(grammar), machineName);
        }

        /**
         * If a given token has not yet been accepted by any state,
         * records that it has been masked in some state.
         *
         * @param masked The token which was masked.
         * @param by The token by which it was masked.
         */
        public void mask(@Nonnull TokenModel masked, @Nonnull TokenModel by) {
            // We call getDfaState() per nfa-set multiple times, but that can only call getAcceptForState once.
            // LOG.debug("Token " + masked + " masked by " + by);
            // if (masked.equals(by)) return;
            TokenSet set = get(masked);
            if (set != null)
                set.add(by);
        }

        /**
         * Records that a token was accepted by some state.
         * Throws away all masking information.
         *
         * @param accept The accepted token.
         */
        public void accept(@CheckForNull TokenModel accept) {
            remove(accept);
        }
    }
}
