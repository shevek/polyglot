/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.dfa;

import com.google.common.collect.ImmutableMultimap;
import java.io.File;
import javax.annotation.Nonnull;
import org.anarres.graphviz.builder.GraphVizUtils;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.StateModel;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.node.ALiteralMatcher;
import org.anarres.polyglot.node.TIdentifier;
import org.junit.Test;

/**
 *
 * @author shevek
 */
public class DFATest {

    @Nonnull
    private TokenModel addToken(@Nonnull GrammarModel grammar, @Nonnull String... texts) {
        int index = grammar.tokenIndex++;
        TokenModel token = new TokenModel(index, new TIdentifier("t_" + index), new ALiteralMatcher(), null, ImmutableMultimap.<String, AnnotationModel>of());
        grammar.tokens.put(token.getName(), token);

        NFA nfa = NFA.forString(texts[0]);
        for (int i = 1; i < texts.length; i++)
            nfa = nfa.alternate(NFA.forString(texts[i]));
        token.nfa = nfa.accept(token);
        return token;
    }

    @Test
    public void testDFA() throws Exception {
        File dir = new File("build/test-outputs");
        dir.mkdirs();

        GrammarModel grammar = new GrammarModel();
        StateModel state = new StateModel(grammar.stateIndex++, new TIdentifier("DEFAULT"));
        grammar.states.put(state.getName(), state);

        // don't use 'a' or 'z', it makes debugging harder.
        addToken(grammar, "foo", "yoo");
        addToken(grammar, "bbr");
        addToken(grammar, "bbp");
        addToken(grammar, "byo", "foobyo");

        NFA nfa = null;
        for (TokenModel token : grammar.tokens.values()) {
            if (nfa == null)
                nfa = token.nfa;
            else
                nfa = nfa.merge(token.nfa);
        }
        GraphVizUtils.toGraphVizFile(new File(dir, "nfa.dot"), nfa);

        ErrorHandler errors = new ErrorHandler();
        DFA.Builder builder = new DFA.Builder(errors, grammar, new DFA.TokenMask(grammar, "machineName"), nfa);
        DFA dfa = builder.build();

        GraphVizUtils.toGraphVizFile(new File(dir, "dfa.dot"), dfa);

        dfa.minimize();
        GraphVizUtils.toGraphVizFile(new File(dir, "dfa-minimized.dot"), dfa);
    }
}
