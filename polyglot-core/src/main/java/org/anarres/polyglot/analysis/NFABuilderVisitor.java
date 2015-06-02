/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import java.util.List;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.dfa.NFA;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.StateModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.node.AAlternateMatcher;
import org.anarres.polyglot.node.ACharMatcher;
import org.anarres.polyglot.node.AConcatMatcher;
import org.anarres.polyglot.node.ADifferenceMatcher;
import org.anarres.polyglot.node.AHelper;
import org.anarres.polyglot.node.AHelperMatcher;
import org.anarres.polyglot.node.AIntervalMatcher;
import org.anarres.polyglot.node.APlusMatcher;
import org.anarres.polyglot.node.AQuestionMatcher;
import org.anarres.polyglot.node.AStarMatcher;
import org.anarres.polyglot.node.AStringMatcher;
import org.anarres.polyglot.node.AToken;
import org.anarres.polyglot.node.AUnionMatcher;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PMatcher;
import org.anarres.polyglot.dfa.CharSet;
import org.anarres.polyglot.node.AGrammar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class NFABuilderVisitor extends MatcherParserVisitor {

    private static final Logger LOG = LoggerFactory.getLogger(NFABuilderVisitor.class);

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public NFABuilderVisitor(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    private static void notreached() {
        throw new IllegalStateException();
    }

    @Nonnull
    private CharSet getCharSet(@Nonnull Node node) {
        // TODO: Warn if not a CharSet - could be a helper which referred to an NFA.
        Object o = getOut(node);
        if (o instanceof CharSet)
            return (CharSet) o;
        if (node instanceof AHelperMatcher)
            throw new IllegalStateException("What is " + o.getClass() + " for helper " + ((AHelperMatcher) node).getHelperName().getText());
        throw new IllegalStateException("What is " + o.getClass() + " for " + node.getClass());
    }

    @Nonnull
    private NFA getNFA(@Nonnull Node node) {
        // TODO: Warn if not a CharSet - could be a helper which referred to an NFA.
        Object o = getOut(node);
        if (o == null)
            throw new NullPointerException("No NFA for " + node.getClass());
        if (o instanceof NFA)
            return ((NFA) o);
        if (o instanceof CharSet)
            return new NFA((CharSet) o);
        throw new IllegalStateException("What is " + o.getClass() + " for " + node.getClass());
    }

    @Override
    public void outACharMatcher(ACharMatcher node) {
        Character c = (Character) getOut(node.getChar());
        // LOG.info("ACharMatcher " + Integer.toString(c.charValue()) + " -> CharSet.");
        setOut(node, new CharSet(c.charValue()));
    }

    @Override
    public void outAIntervalMatcher(AIntervalMatcher node) {
        Character left = (Character) getOut(node.getLeft());
        Character right = (Character) getOut(node.getRight());
        setOut(node, new CharSet(left.charValue(), right.charValue()));
    }

    @Override
    public void outAUnionMatcher(AUnionMatcher node) {
        // LOG.info("Union at " + node.getOp().getLine());
        CharSet left = getCharSet(node.getLeft());
        CharSet right = getCharSet(node.getRight());
        setOut(node, left.union(right));
    }

    @Override
    public void outADifferenceMatcher(ADifferenceMatcher node) {
        // LOG.info("Difference at " + node.getOp().getLine());
        CharSet left = getCharSet(node.getLeft());
        CharSet right = getCharSet(node.getRight());
        setOut(node, left.diff(right));
    }

    @Override
    public void outAHelperMatcher(AHelperMatcher node) {
        String name = node.getHelperName().getText();
        HelperModel helper = grammar.getHelper(name);
        if (helper == null) {
            errors.addError(node.getHelperName(), "No such helper '" + name + "'.");
            setOut(node, new CharSet(' '));
            return;
        }
        setOut(node, helper.value);
    }

    @Override
    public void outAStringMatcher(AStringMatcher node) {
        String text = parse(node.getString());
        setOut(node, new NFA(text));
    }

    @Override
    public void outAPlusMatcher(APlusMatcher node) {
        setOut(node, getNFA(node.getMatcher()).oneOrMore());
    }

    @Override
    public void outAQuestionMatcher(AQuestionMatcher node) {
        setOut(node, getNFA(node.getMatcher()).zeroOrOne());
    }

    @Override
    public void outAStarMatcher(AStarMatcher node) {
        setOut(node, getNFA(node.getMatcher()).zeroOrMore());
    }

    @Override
    public void outAConcatMatcher(AConcatMatcher node) {
        List<PMatcher> matchers = node.getMatchers();
        switch (matchers.size()) {
            case 0:
                setOut(node, new NFA());    // This should really be an empty charset.
                break;
            case 1:
                notreached();
                // This might still be a CharSet.
                setOut(node, getOut(matchers.get(0)));
                break;
            default:
                NFA nfa = getNFA(matchers.get(0));
                for (int i = 1; i < matchers.size(); i++)
                    nfa = nfa.concatenate(getNFA(matchers.get(i)));
                setOut(node, nfa);
                break;
        }
    }

    @Override
    public void outAAlternateMatcher(AAlternateMatcher node) {
        List<PMatcher> matchers = node.getMatchers();
        switch (matchers.size()) {
            case 0:
                notreached();
                setOut(node, new NFA());
                break;
            case 1:
                notreached();
                // This might still be a CharSet.
                setOut(node, getOut(matchers.get(0)));
                break;
            default:
                NFA nfa = getNFA(matchers.get(0));
                for (int i = 1; i < matchers.size(); i++)
                    nfa = nfa.alternate(getNFA(matchers.get(i)));
                setOut(node, nfa);
                break;
        }
    }

    @Override
    public void outAHelper(AHelper node) {
        String name = node.getName().getText();
        HelperModel helper = grammar.getHelper(name);
        if (helper == null)
            throw new IllegalStateException("No such helper " + name);
        HelperModel.Value value = (HelperModel.Value) getOut(node.getMatcher());
        // LOG.info(name + " -> " + value);
        helper.value = value;
    }

    @Override
    public void outAToken(AToken node) {
        NFA nfa = getNFA(node.getMatcher());

        String name = node.getName().getText();
        TokenModel token = grammar.getToken(name);
        if (token == null)
            throw new IllegalStateException("No such token " + name);

        // nfa.states[nfa.states.length - 1].accept = token;
        nfa = nfa.accept(token);
        setOut(node, nfa);
        token.nfa = nfa;

        // LOG.info(name + " -> " + nfa);
    }

    @Override
    public void outAGrammar(AGrammar node) {
        // Build the NFA for each state.
        for (StateModel state : grammar.states.values()) {

            // LOG.info("Building for state " + state.getName());
            NFA stateNfa = null;
            for (TokenModel token : grammar.tokens.values()) {
                // Out of all the tokens which "start" in that state.
                if (token.transitions.containsKey(state)
                        || (token.transitions.isEmpty() && state.isInitialState())) {
                    // LOG.info("Token " + token.getName() + " in state " + state.getName());
                    NFA tokenNfa = token.nfa;
                    if (stateNfa == null)
                        stateNfa = tokenNfa;
                    else
                        stateNfa = stateNfa.merge(tokenNfa);
                }
            }

            // TODO: Check stateNfa is non-null.
            state.nfa = stateNfa;

            // LOG.info(state.getName() + " -> " + stateNfa);
        }
    }

}
