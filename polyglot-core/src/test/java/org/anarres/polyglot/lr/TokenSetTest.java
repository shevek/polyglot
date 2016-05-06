/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.anarres.polyglot.model.AnnotationModel;
import org.junit.Test;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.node.ALiteralMatcher;
import org.anarres.polyglot.node.TIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static org.junit.Assert.*;

/**
 *
 * @author shevek
 */
public class TokenSetTest {

    private static final Logger LOG = LoggerFactory.getLogger(TokenSetTest.class);

    @Test
    public void testSet() {
        List<TokenModel> tokens = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tokens.add(new TokenModel(i + 1,
                    new TIdentifier("tok" + i),
                    new ALiteralMatcher(), null,
                    ImmutableMultimap.<String, AnnotationModel>of()
            ));
        }
        TokenUniverse universe = new TokenUniverse(tokens);
        TokenSet set = new TokenSet(universe);

        LOG.info("Empty: " + set);
        assertEquals(0, set.size());
        set.add(TokenModel.EOF.INSTANCE);

        LOG.info("Null: " + set);
        assertEquals(1, set.size());
        assertEquals(TokenModel.EOF.INSTANCE, Iterables.getOnlyElement(set));

        set.clear();
        assertEquals(0, set.size());
        set.addAll(Arrays.<TokenModel>asList(TokenModel.EOF.INSTANCE));
        LOG.info("Null: " + set);
        assertEquals(1, set.size());
        assertEquals(TokenModel.EOF.INSTANCE, Iterables.getOnlyElement(set));
    }

}
