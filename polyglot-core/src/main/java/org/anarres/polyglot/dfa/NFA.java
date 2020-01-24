/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizGraph;
import org.anarres.graphviz.builder.GraphVizLabel;
import org.anarres.graphviz.builder.GraphVizScope;
import org.anarres.graphviz.builder.GraphVizable;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.TokenModel;

/**
 * A Nondeterministic Finite Automaton.
 *
 * These NFA objects satisfy several invariants:
 * <ol>
 * <li>[INITIAL] The initial state is state 0.
 * <li>[FINAL] The terminal state, if there is one (i.e. non-merge NFAs), is state [length - 1].
 * <li>[INTO] There are no internal transitions INTO state 0.
 * <li>[OUTOF] There are no internal transitions OUT OF state [length - 1].
 * </ol>
 * I think the model works if either of these conditions is relaxed,
 * but not if both are relaxed.
 * Relaxing condition [INTO] prevents us from doing efficient merges by
 * just merging transition lists, as a kleene-star could return to state 0
 * and then follow an alternative (merged) NFA.
 * I'm not currently sure what condition [OUTOF] buys us as long as we
 * are otherwise careful with NFAs, but it doesn't cost us that much.
 *
 * GraphViz renderings of this object tend to be big enough to cause
 * difficulties either for GraphViz or for the image viewer.
 *
 * @author shevek
 */
public class NFA implements HelperModel.Value, GraphVizable, GraphVizScope {

    @Nonnull
    public final State[] states;

    private NFA(@Nonnegative int size) {
        // System.out.print(".");
        states = new State[size];
    }

    /** Constructs an NFA which accepts on empty. */
    public NFA() {
        // I think this can be condensed to 1 state.
        this(2);
        states[0] = new State();
        states[0].transitions.add(new CharSetTransition(null, 1));
        states[1] = new State();
    }

    /** Constructs an NFA which accepts on any char in the CharSet. */
    @Nonnull
    public static NFA forCharSet(@Nonnull CharSet chars) {
        NFA out = new NFA(2);
        out.states[0] = new State();
        out.states[0].transitions.add(new CharSetTransition(chars, 1));
        out.states[1] = new State();
        return out;
    }

    @Nonnull
    public static NFA forString(@Nonnull String string) {
        NFA out = new NFA(string.length() + 1);

        for (int i = 0; i < string.length(); i++) {
            out.states[i] = new State();
            out.states[i].transitions.add(new CharSetTransition(new CharSet(string.charAt(i)), i + 1));
        }

        out.states[string.length()] = new State();

        return out;
    }

    @Nonnull
    public static NFA forCustomMatcher(@Nonnull String name) {
        NFA out = new NFA(2);
        out.states[0] = new State();
        out.states[0].transitions.add(new CustomTransition(name, 1));
        out.states[1] = new State();
        return out;
    }

    @Nonnull
    public NFA zeroOrMore() {
        NFA nfa = new NFA(states.length + 2);
        nfa.states[0] = new State();
        nfa.states[0].transitions.add(new CharSetTransition(null, 1));
        nfa.states[0].transitions.add(new CharSetTransition(null, states.length + 1));

        for (int i = 0; i < states.length; i++) {
            nfa.states[i + 1] = states[i].offsetBy(1);
        }

        nfa.states[states.length].transitions.add(new CharSetTransition(null, 1));
        nfa.states[states.length].transitions.add(new CharSetTransition(null, states.length + 1));

        nfa.states[states.length + 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA zeroOrOne() {
        NFA nfa = new NFA(states.length + 2);
        nfa.states[0] = new State();
        nfa.states[0].transitions.add(new CharSetTransition(null, 1));
        nfa.states[0].transitions.add(new CharSetTransition(null, states.length + 1));

        for (int i = 0; i < states.length; i++) {
            nfa.states[i + 1] = states[i].offsetBy(1);
        }

        nfa.states[states.length].transitions.add(new CharSetTransition(null, states.length + 1));

        nfa.states[states.length + 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA oneOrMore() {
        NFA nfa = new NFA(states.length + 2);
        nfa.states[0] = new State();
        nfa.states[0].transitions.add(new CharSetTransition(null, 1));

        for (int i = 0; i < states.length; i++) {
            nfa.states[i + 1] = states[i].offsetBy(1);
        }

        nfa.states[states.length].transitions.add(new CharSetTransition(null, 1));
        nfa.states[states.length].transitions.add(new CharSetTransition(null, states.length + 1));

        nfa.states[states.length + 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA concatenate(@Nonnull NFA next) {
        NFA nfa = new NFA(states.length + next.states.length - 1);

        for (int i = 0; i < states.length - 1; i++) {
            nfa.states[i] = states[i].offsetBy(0);
        }

        // Copies next[INITIAL] in place of this.[FINAL].
        for (int i = 0; i < next.states.length; i++) {
            nfa.states[states.length + i - 1] = next.states[i].offsetBy(states.length - 1);
        }

        return nfa;
    }

    @Nonnull
    public NFA alternate(@Nonnull NFA next) {
        NFA nfa = new NFA(states.length + next.states.length);

        // Copy the first NFA.
        for (int i = 0; i < states.length; i++) {
            nfa.states[i] = states[i].offsetBy(0);
        }
        // Outgoing transition from first NFA to final state.
        nfa.states[states.length - 1].transitions.add(new CharSetTransition(null, states.length + next.states.length - 1));

        // Append outgoing transitions from state 0 of the second NFA to state 0 of the first NFA.
        for (Transition transition : next.states[0].transitions) {
            nfa.states[0].transitions.add(transition.offsetBy(states.length - 1));
        }
        // Copy the remaining states of the second NFA.
        for (int i = 1; i < next.states.length; i++) {
            nfa.states[states.length + i - 1] = next.states[i].offsetBy(states.length - 1);
        }

        // Outgoing transition from second NFA to final state.
        nfa.states[states.length + next.states.length - 2].transitions.add(
                new CharSetTransition(null, states.length + next.states.length - 1));

        // Final state.
        nfa.states[states.length + next.states.length - 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA merge(@Nonnull NFA next) {
        NFA nfa = new NFA(states.length + next.states.length - 1);

        // Copy the first NFA.
        for (int i = 0; i < states.length; i++) {
            nfa.states[i] = states[i].offsetBy(0);
        }
        // Append outgoing transitions from state 0 of the second NFA to state 0 of the first NFA.
        for (Transition transition : next.states[0].transitions) {
            nfa.states[0].transitions.add(transition.offsetBy(states.length - 1));
        }
        // Copy the remaining states of the second NFA.
        for (int i = 1; i < next.states.length; i++) {
            nfa.states[states.length + i - 1] = next.states[i].offsetBy(states.length - 1);
        }

        return nfa;
    }

    /** We have to copy the entire NFA as we mutate one of the states. */
    @Nonnull
    public NFA accept(@Nonnull TokenModel token) {
        NFA nfa = new NFA(states.length);
        for (int i = 0; i < states.length; i++)
            nfa.states[i] = states[i].offsetBy(0);
        nfa.states[nfa.states.length - 1].accept = token;
        return nfa;
    }

    @Override
    public void toGraphViz(GraphVizGraph graph) {
        for (int i = 0; i < states.length; i++) {
            State state = states[i];
            GraphVizLabel label = graph.node(this, state).label();
            label.set(String.valueOf(i));
            if (state.accept != null)
                label.append(" (").append(state.accept.getName()).append(")");
            for (Transition transition : state.transitions) {
                if (transition != null) {
                    label = graph.edge(this, state, states[transition.destination]).label();
                    if (!label.isEmpty())
                        label.append(", ");
                    if (transition instanceof CharSetTransition) {
                        CharSetTransition c = (CharSetTransition) transition;
                        label.append(c.chars == null ? "e" : c.chars.toString());
                    } else {
                        // CustomTransition c = (CustomTransition) transition;
                        label.append("!");
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < states.length; i++) {
            buf.append(i).append(":");
            states[i].toStringBuilder(buf);
            buf.append("\n");
        }
        return buf.toString();
    }

    /* pp */ static class State {

        @CheckForNull
        public TokenModel accept;
        public final List<Transition> transitions = new ArrayList<>();

        @Nonnull
        public State offsetBy(@Nonnegative int destinationOffset) {
            State out = new State();
            out.accept = accept;
            for (Transition transition : transitions)
                out.transitions.add(transition.offsetBy(destinationOffset));
            return out;
        }

        public void toStringBuilder(@Nonnull StringBuilder buf) {
            if (accept != null)
                buf.append("(").append(accept).append(") ");
            for (Transition transition : transitions)
                buf.append(" ").append(transition);
        }

        @Override
        public String toString() {
            StringBuilder buf = new StringBuilder();
            toStringBuilder(buf);
            return buf.toString();
        }
    }

    /* pp */ static class CharSetTransition extends Transition {

        @CheckForNull
        public final CharSet chars;

        public CharSetTransition(CharSet chars, int destination) {
            super(destination);
            this.chars = chars;
        }

        @Override
        public boolean isEpsilon() {
            return chars == null;
        }

        @Override
        public boolean isCustom() {
            return false;
        }

        @Override
        public Transition offsetBy(int destinationOffset) {
            if (destinationOffset == 0)
                return this;
            return new CharSetTransition(chars, destination + destinationOffset);
        }

        @Override
        public String toString() {
            return destination + ":{" + chars + "}";
        }
    }

    /* pp */ static class CustomTransition extends Transition {

        private final String name;

        public CustomTransition(@Nonnull String name, @Nonnegative int destination) {
            super(destination);
            this.name = Preconditions.checkNotNull(name, "Name was null.");
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean isEpsilon() {
            return false;
        }

        @Override
        public boolean isCustom() {
            return true;
        }

        @Override
        public Transition offsetBy(int destinationOffset) {
            if (destinationOffset == 0)
                return this;
            return new CustomTransition(name, destination + destinationOffset);
        }

        @Override
        public String toString() {
            return destination + ":!";
        }
    }

    /* pp */ static abstract class Transition {

        @Nonnegative
        public final int destination;

        public Transition(@Nonnegative int destination) {
            this.destination = destination;
        }

        @Nonnegative
        public int getDestination() {
            return destination;
        }

        /** Returns true iff this is an epsilon transition. */
        public abstract boolean isEpsilon();

        public abstract boolean isCustom();

        @Nonnull
        public abstract Transition offsetBy(int destinationOffset);
    }
}
