/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.Nonnull;
import org.anarres.polyglot.model.TokenModel;

/**
 *
 * @author shevek
 */
// Could make this more efficient.
public class TokenSet extends MutableIndexedSet<TokenModel> {

    public TokenSet(@Nonnull TokenUniverse universe) {
        super(universe);
    }

    @Override
    protected boolean isCompatibleObject(Object in) {
        return in instanceof TokenModel;
    }
}