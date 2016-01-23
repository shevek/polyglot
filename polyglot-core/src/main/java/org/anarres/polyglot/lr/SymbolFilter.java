/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.TokenModel;

/**
 * This is a high-speed, allocation-free equivalent of a HashSet for CstProductionSymbols.
 *
 * @author shevek
 */
public class SymbolFilter {

    private final IntSet indices = new IntOpenHashSet();

    /**
     * Adds a CstProductionSymbol to this filter.
     *
     * Since we know that the only two "real" concrete subclasses of CstProductionSymbol
     * are CstProductionSymbol and TokenModel, we map them to integer range
     * [0, inf) and [-1, -inf) respectively. That allows us to use
     * a fastutil IntSet for the store, which is a cheap implementation.
     *
     * @param symbol The symbol to add.
     * @return true if this symbol is unseen; false if it was previously added.
     */
    public boolean add(@Nonnull CstProductionSymbol symbol) {
        int index;
        if (symbol instanceof CstProductionModel)
            index = ((CstProductionModel) symbol).getIndex();
        else
            index = -1 - ((TokenModel) symbol).getIndex();
        return indices.add(index);
    }

    public void clear() {
        indices.clear();
    }
}
