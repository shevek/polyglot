/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import java.util.Set;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public interface IndexedSet<IndexedItem extends Indexed> extends Set<IndexedItem> {

    /** Exposed to allow for allocation-free iteration. */
    @Nonnull
    public IndexedUniverse<IndexedItem> getUniverse();
}
