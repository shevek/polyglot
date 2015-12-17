/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

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
        states[0].transitions.add(new Transition(null, 1));
        states[1] = new State();
    }

    /** Constructs an NFA which accepts on any char in the CharSet. */
    public NFA(@Nonnull CharSet chars) {
        this(2);
        states[0] = new State();
        states[0].transitions.add(new Transition(chars, 1));
        states[1] = new State();
    }

    public NFA(@Nonnull String string) {
        this(string.length() + 1);

        for (int i = 0; i < string.length(); i++) {
            states[i] = new State();
            states[i].transitions.add(new Transition(new CharSet(string.charAt(i)), i + 1));
        }

        states[string.length()] = new State();
    }

    public NFA(@Nonnull NFA nfa) {
        this(nfa.states.length);

        for (int i = 0; i < nfa.states.length; i++) {
            // Clones the state, but not the transitions.
            states[i] = nfa.states[i].offsetBy(0);
        }
    }

    @Nonnull
    public NFA zeroOrMore() {
        NFA nfa = new NFA(states.length + 2);
        nfa.states[0] = new State();
        nfa.states[0].transitions.add(new Transition(null, 1));
        nfa.states[0].transitions.add(new Transition(null, states.length + 1));

        for (int i = 0; i < states.length; i++) {
            nfa.states[i + 1] = states[i].offsetBy(1);
        }

        nfa.states[states.length].transitions.add(new Transition(null, 1));
        nfa.states[states.length].transitions.add(new Transition(null, states.length + 1));

        nfa.states[states.length + 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA zeroOrOne() {
        NFA nfa = new NFA(states.length + 2);
        nfa.states[0] = new State();
        nfa.states[0].transitions.add(new Transition(null, 1));
        nfa.states[0].transitions.add(new Transition(null, states.length + 1));

        for (int i = 0; i < states.length; i++) {
            nfa.states[i + 1] = states[i].offsetBy(1);
        }

        nfa.states[states.length].transitions.add(new Transition(null, states.length + 1));

        nfa.states[states.length + 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA oneOrMore() {
        NFA nfa = new NFA(states.length + 2);
        nfa.states[0] = new State();
        nfa.states[0].transitions.add(new Transition(null, 1));

        for (int i = 0; i < states.length; i++) {
            nfa.states[i + 1] = states[i].offsetBy(1);
        }

        nfa.states[states.length].transitions.add(new Transition(null, 1));
        nfa.states[states.length].transitions.add(new Transition(null, states.length + 1));

        nfa.states[states.length + 1] = new State();

        return nfa;
    }

    @Nonnull
    public NFA concatenate(@Nonnull NFA next) {
        NFA nfa = new NFA(states.length + next.states.length - 1);

        for (int i = 0; i < states.length - 1; i++) {
            nfa.states[i] = states[i].offsetBy(0);
        }

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
        nfa.states[states.length - 1].transitions.add(new Transition(null, states.length + next.states.length - 1));

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
                new Transition(null, states.length + next.states.length - 1));

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
                    label.append(transition.chars == null ? "e" : transition.chars.toString());
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

    public static class State {

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

    public static class Transition {

        @CheckForNull
        public final CharSet chars;
        @Nonnegative
        public final int destination;

        public Transition(@CheckForNull CharSet chars, @Nonnegative int destination) {
            this.chars = chars;
            this.destination = destination;
        }

        @Nonnull
        public Transition offsetBy(int destinationOffset) {
            if (destinationOffset == 0)
                return this;
            return new Transition(chars, destination + destinationOffset);
        }

        @Override
        public String toString() {
            return destination + ":{" + chars + "}";
        }
    }
}
