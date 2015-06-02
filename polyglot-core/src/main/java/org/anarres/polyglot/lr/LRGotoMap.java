/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import org.anarres.polyglot.model.CstProductionModel;

/**
 *
 * @author shevek
 */
@Deprecated
public class LRGotoMap extends HashMap<CstProductionModel, LRState> {

    // TOOD: do we need a Builder and/or a Table or SetMultimap here?
    @Override
    public LRState put(CstProductionModel key, LRState value) {
        Preconditions.checkNotNull(key, "TokenModel was null.");
        Preconditions.checkNotNull(value, "LRAction was null.");
        // LOG.info("AddAction: " + key + " -> " + value);
        LRState prev = super.put(key, value);
        // Detect and log shift-reduce conflicts.
        if (prev != null)
            if (!prev.equals(value))
                throw new IllegalArgumentException("Conflict: " + prev + " != " + value);
        return prev;
    }
}
