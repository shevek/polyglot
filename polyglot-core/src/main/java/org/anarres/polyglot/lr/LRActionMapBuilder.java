/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class LRActionMapBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LRActionMapBuilder.class);
    // I think this is too sparse to be worth extending TokenMap.
    private final Map<TokenModel, LRAction> actions = new HashMap<>();
    // While we might imagine that this is lossy over <TokenModel, LRItem, LRAction>,
    // the extra information is useless, and we have to elide it manually.
    private final Table<TokenModel, LRAction, LRItem> actionItems = HashBasedTable.create();

    @Nonnull
    public void addAction(@Nonnull LRItem item, @Nonnull TokenModel key, @Nonnull LRAction value) {
        Preconditions.checkNotNull(key, "TokenModel was null.");
        Preconditions.checkNotNull(value, "LRAction was null.");
        actions.put(key, value);
        actionItems.put(key, value, item);
    }

    public void clear() {
        actions.clear();
        actionItems.clear();
    }

    @Nonnull
    public Collection<? extends LRConflict> getConflicts(@Nonnull LRState state) {
        Set<LRConflict> out = new HashSet<>();
        for (Map.Entry<TokenModel, Map<LRAction, LRItem>> e : actionItems.rowMap().entrySet()) {
            if (e.getValue().size() > 1) {
                out.add(new LRConflict(state, e.getKey(), e.getValue()));
            }
        }
        return out;
    }

    @Nonnull
    public SortedMap<TokenModel, LRAction> toMap() {
        return ImmutableSortedMap.copyOf(actions, TokenModel.Comparator.INSTANCE);
    }
}
