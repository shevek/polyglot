/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.Nonnull;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
public class TokenUniverse extends IndexedUniverse<TokenModel> {

    public TokenUniverse(@Nonnull Iterable<? extends TokenModel> tokens) {
        super(TokenModel.class);
        addItemAtIndex(TokenModel.EOF.INSTANCE);
        for (TokenModel token : tokens)
            addItemAtIndex(token);
    }

    public TokenUniverse(@Nonnull GrammarModel grammar) {
        this(grammar.tokens.values());
    }
}