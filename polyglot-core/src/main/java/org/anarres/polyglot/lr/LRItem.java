/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementAssociativity;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;

/**
 *
 * @author shevek
 */
public interface LRItem extends Indexed {

    /** The left hand side. */
    @Nonnull
    public CstProductionModel getProduction();

    /** The right hand side. */
    @Nonnull
    public CstAlternativeModel getProductionAlternative();

    /** The location of the dot. */
    @Nonnegative
    public int getPosition();

    /** Returns the element at the position (after the dot), or null. */
    @CheckForNull
    public CstElementModel getElement();

    /** If the dot is at the end, i.e. this is a reduce action, returns UNSPECIFIED. */
    @Nonnull
    public CstElementAssociativity getAssociativity();

    /** Returns the symbol of the element at the position (after the dot), or null. */
    @CheckForNull
    public CstProductionSymbol getSymbol();

    public void assertFollowedBy(@Nonnull LRItem follow);

    public void toStringBuilderWithoutLookahead(@Nonnull StringBuilder buf);

    public void toStringBuilder(@Nonnull StringBuilder buf);
}
