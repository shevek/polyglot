/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.AbstractSet;
import javax.annotation.CheckForNull;

/**
 *
 * @author shevek
 */
public abstract class AbstractIndexedSet<IndexedItem extends Indexed> extends AbstractSet<IndexedItem> implements IndexedSet<IndexedItem> {

    protected final IndexedUniverse<IndexedItem> universe;

    public AbstractIndexedSet(IndexedUniverse<IndexedItem> universe) {
        this.universe = universe;
    }

    @Override
    public IndexedUniverse<IndexedItem> getUniverse() {
        return universe;
    }

    protected boolean isCompatibleObject(@CheckForNull Object in) {
        return universe.getItemType().isInstance(in);
    }

}
