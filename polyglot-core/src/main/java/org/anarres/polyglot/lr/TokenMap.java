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
public class TokenMap<V> extends SparseIndexedMap<TokenModel, V> {

    public TokenMap(@Nonnull TokenUniverse universe) {
        super(universe);
    }

    @Override
    protected boolean isCompatibleObject(Object in) {
        return in instanceof TokenModel;
    }
}
