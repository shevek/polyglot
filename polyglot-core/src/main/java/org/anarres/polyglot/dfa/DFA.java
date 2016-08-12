/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.BitSet;
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
import org.anarres.polyglot.lr.TokenMap;
import org.anarres.polyglot.lr.TokenSet;
import org.anarres.polyglot.lr.TokenUniverse;
import org.anarres.polyglot.model.AnnotationName;
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
    public final List<? extends State> states;

    public DFA(@Nonnull List<? extends State> states) {
        this.states = states;
    }

    @TemplateProperty
    @Nonnull
    public List<? extends State> getStates() {
        return states;
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

            for (NFA.Transition transition : nfa.states[nfaState].transitions) {
                if (transition == null)
                    continue;
                if (transition.chars != null)
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

                    for (int nfaStateIdx = dfaState.nfaStates.nextSetBit(0); nfaStateIdx >= 0; nfaStateIdx = dfaState.nfaStates.nextSetBit(nfaStateIdx + 1)) {
                        NFA.State nfaState = nfa.states[nfaStateIdx];
                        for (NFA.Transition nfaTransition : nfaState.transitions) {
                            if (nfaTransition == null)
                                continue;
                            // It's an epsilon-transition, and the destination is already included in nfaStates.
                            if (nfaTransition.chars == null)
                                continue;

                            // We're looking for the shortest common interval in all transitions starting from 'start'.
                            CharInterval overlap = CharInterval.findFirstOverlappingInterval(nfaTransition.chars.getIntervals(), start, end);
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
                        State dfaStateTarget = dfaStateMap.get(dstNfaStates);

                        if (dfaStateTarget == null) {
                            // TODO: Compute accept here.
                            TokenModel acceptToken = getAcceptForState(dstNfaStates);
                            dfaStateTarget = new State(dfaStates.size(), dstNfaStates, acceptToken);
                            dfaStates.add(dfaStateTarget);
                            dfaStateMap.put(dstNfaStates, dfaStateTarget);

                        }

                        // These are generated in strictly ascending order.
                        dfaState.addTransition(new Transition(start, end, dfaStateTarget));
                    }

                    // Look for the next character range.
                    start = (char) (end + 1);
                } while (end != (char) 0xffff);
            }

            return new DFA(dfaStates);
        }
    }

    public static class State {

        private final int index;
        private final BitSet nfaStates;
        public final TokenModel acceptToken;
        public final List<Transition> transitions = new ArrayList<>();

        public State(int index, BitSet nfaStates, @CheckForNull TokenModel accept) {
            this.index = index;
            this.nfaStates = nfaStates;
            this.acceptToken = accept;
        }

        @TemplateProperty
        @Nonnegative
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

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            if (acceptToken != null)
                buf.append("(").append(acceptToken.getName()).append(") ");
            for (Transition transition : transitions)
                buf.append(transition).append(",");
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
            return getDestination().index + ":[" + toIntervalString() + "]";
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
        public TokenMask(@Nonnull TokenUniverse universe) {
            super(universe);
            for (TokenModel token : universe.getItems()) {
                if (token.hasAnnotation(AnnotationName.LexerIgnore))
                    continue;
                if (token.hasAnnotation(AnnotationName.LexerAllowMasking))
                    continue;
                if (token instanceof TokenModel.EOF)
                    continue;
                put(token, new TokenSet(universe));
            }
        }

        public TokenMask(@Nonnull GrammarModel grammar) {
            this(new TokenUniverse(grammar));
        }

        /**
         * If a given token has not yet been accepted by any state,
         * records that it has been masked in some state.
         *
         * @param masked The token which was masked.
         * @param by The token by which it was masked.
         */
        public void mask(@Nonnull TokenModel masked, @Nonnull TokenModel by) {
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
