/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.HashMultimap;
import java.util.Collections;
import java.util.List;
import org.anarres.polyglot.Option;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.Specifier;
import org.anarres.polyglot.model.UnaryOperator;
import org.anarres.polyglot.node.TIdentifier;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class JavaHelperTest {

    private static final Logger LOG = LoggerFactory.getLogger(JavaHelperTest.class);

    private void testLexFormat(String text, int size) {
        AstProductionModel production = new AstProductionModel(new TIdentifier("prod"), HashMultimap.<String, AnnotationModel>create());
        AstAlternativeModel alternative = new AstAlternativeModel(production, production.getLocation(), new TIdentifier("alt"), HashMultimap.<String, AnnotationModel>create());
        production.alternatives.put(alternative.getName(), alternative);
        AstElementModel element = new AstElementModel(new TIdentifier("el"), Specifier.ANY, new TIdentifier("sym"), UnaryOperator.NONE, HashMultimap.<String, AnnotationModel>create());
        alternative.elements.add(element);

        GrammarModel grammar = new GrammarModel();
        grammar.addAstProduction(production);

        JavaHelper helper = new JavaHelper(Collections.<Option>emptySet(), grammar, null);
        LOG.info("In: " + text);
        List<? extends Object> out = helper.lexFormat(alternative, text);
        LOG.info("Out: " + out);
        assertEquals(size, out.size());
    }

    @Test
    public void testLexFormat() {
        testLexFormat("", 0);
        testLexFormat("%%", 1);
        testLexFormat("%{el}", 1);
        testLexFormat("%{el}foo%{el}bar%%%{el}%%%%", 6);
        testLexFormat("foo bar baz %% qux", 1);
        testLexFormat("%{el} %{el} %<%<%>%> baz %% qux", 9);
        testLexFormat("ELSE%{el}\n", 3);
    }
}
