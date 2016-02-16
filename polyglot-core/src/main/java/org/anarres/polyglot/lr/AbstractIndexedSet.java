/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import java.util.AbstractSet;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public abstract class AbstractIndexedSet<IndexedItem extends Indexed> extends AbstractSet<IndexedItem> implements IndexedSet<IndexedItem> {

    protected final IndexedUniverse<IndexedItem> universe;

    public AbstractIndexedSet(@Nonnull IndexedUniverse<IndexedItem> universe) {
        this.universe = Preconditions.checkNotNull(universe, "IndexedUniverse was null.");
    }

    @Override
    public IndexedUniverse<IndexedItem> getUniverse() {
        return universe;
    }

    protected boolean isCompatibleObject(@CheckForNull Object in) {
        return universe.getItemType().isInstance(in);
    }

}
