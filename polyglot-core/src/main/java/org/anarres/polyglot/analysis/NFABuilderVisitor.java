/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.dfa.CharSet;
import org.anarres.polyglot.dfa.NFA;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.AnnotationUtils;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.HelperModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.node.AAlternateMatcher;
import org.anarres.polyglot.node.AConcatMatcher;
import org.anarres.polyglot.node.ACustomMatcher;
import org.anarres.polyglot.node.ADifferenceMatcher;
import org.anarres.polyglot.node.AHelper;
import org.anarres.polyglot.node.AHelperMatcher;
import org.anarres.polyglot.node.AIntervalMatcher;
import org.anarres.polyglot.node.ALiteralMatcher;
import org.anarres.polyglot.node.APlusMatcher;
import org.anarres.polyglot.node.AQuestionMatcher;
import org.anarres.polyglot.node.AStarMatcher;
import org.anarres.polyglot.node.AToken;
import org.anarres.polyglot.node.AUnionMatcher;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.PMatcher;
import org.anarres.polyglot.node.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class NFABuilderVisitor extends MatcherParserVisitor {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(NFABuilderVisitor.class);

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public NFABuilderVisitor(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        super(errors);
        this.errors = errors;
        this.grammar = grammar;
    }

    private static void notreached() {
        throw new IllegalStateException();
    }

    private char getChar(@Nonnull Node node, @CheckForNull Token location, @Nonnull String description) {
        Object o = getOut(node);
        if (o instanceof Character)
            return (Character) o;
        if (o instanceof String) {
            String s = (String) o;
            if (s.length() > 1)
                errors.addError(location, "Expected character, not string of length " + s.length() + " in " + description + ": " + s);
            return s.charAt(0);
        }
        throw new IllegalStateException("What is " + o.getClass() + " for " + node.getClass());
    }

    @Nonnull
    private CharSet getCharSet(@Nonnull Node node, @CheckForNull Token location, @Nonnull String description) {
        // TODO: Warn if not a CharSet - could be a helper which referred to an NFA.
        Object o = getOut(node);
        if (o instanceof CharSet)
            return (CharSet) o;
        if (o instanceof Character)
            return new CharSet((Character) o);
        if (o instanceof String) {
            String s = (String) o;
            // This failure is optional; CharSet can be constructed from a String.
            if (s.length() > 1)
                errors.addError(location, "Expected character or charset, not string of length " + s.length() + " in " + description + ": " + s);
            return new CharSet(s.charAt(0));
        }
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
            return NFA.forCharSet((CharSet) o);
        if (o instanceof Character)
            return NFA.forString(Character.toString((Character) o));
        if (o instanceof String)
            return NFA.forString((String) o);
        throw new IllegalStateException("What is " + o.getClass() + " for " + node.getClass());
    }

    @Override
    public void outALiteralMatcher(ALiteralMatcher node) {
        // LOG.info("ALiteralMatcher " + Integer.toString(c.charValue()) + " -> CharSet.");
        setOut(node, getOut(node.getLiteral()));   // Character or String.
    }

    @Override
    public void outAIntervalMatcher(AIntervalMatcher node) {
        char left = getChar(node.getLeft(), node.getOp(), "left endpoint of interval");
        char right = getChar(node.getRight(), node.getOp(), "right endpoint of interval");
        setOut(node, new CharSet(left, right));
    }

    @Override
    public void outAUnionMatcher(AUnionMatcher node) {
        // LOG.info("Union at " + node.getOp().getLine());
        CharSet left = getCharSet(node.getLeft(), node.getOp(), "left hand side of union");
        CharSet right = getCharSet(node.getRight(), node.getOp(), "right hand side of union");
        setOut(node, left.union(right));
    }

    @Override
    public void outADifferenceMatcher(ADifferenceMatcher node) {
        // LOG.info("Difference at " + node.getOp().getLine());
        CharSet left = getCharSet(node.getLeft(), node.getOp(), "left hand side of difference");
        CharSet right = getCharSet(node.getRight(), node.getOp(), "right hand side of difference");
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
    public void outACustomMatcher(ACustomMatcher node) {
        String name = node.getCustomName().getText();
        name = name.substring(1, name.length() - 2);
        setOut(node, NFA.forCustomMatcher(name));
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
        Object o = getOut(node.getMatcher());
        HelperModel.Value value;
        if (o instanceof Character)
            value = new CharSet((Character) o);
        else if (o instanceof String) {
            String s = (String) o;
            if (s.length() == 1)
                value = new CharSet(s.charAt(0));
            else
                value = NFA.forString(s);
        } else {
            value = (HelperModel.Value) o;
        }
        // LOG.info(name + " -> " + value);
        helper.value = value;
    }

    @Override
    public void outAToken(AToken node) {
        String name = node.getName().getText();
        TokenModel token = grammar.getToken(name);
        if (token == null)
            throw new IllegalStateException("No such token " + name);
        if (token.hasAnnotation(AnnotationName.LexerIgnore))
            return;
        if (AnnotationUtils.isAnnotated(token, AnnotationName.LexerExclude, null))
            return;

        NFA nfa = getNFA(node.getMatcher());
        // nfa.states[nfa.states.length - 1].accept = token;
        nfa = nfa.accept(token);
        setOut(node, nfa);
        token.nfa = nfa;

        // LOG.info(name + " -> " + nfa);
    }
}
