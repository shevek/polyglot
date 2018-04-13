/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Table;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.CheckForSigned;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.CstAlternativeModel;
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
    private final Object2IntMap<TokenModel> actionPrecedence = new Object2IntOpenHashMap<>();
    // While we might imagine that this is lossy over <TokenModel, LRItem, LRAction>,
    // the extra information is useless, and we have to elide it manually.
    private final Table<TokenModel, LRAction, LRItem> actionItems = HashBasedTable.create();

    /** Negative = lower priority. Positive = higher priority. */
    @CheckForSigned
    private int getActionPrecedence(@Nonnull LRItem item) {
        CstAlternativeModel cstAlternative = item.getProductionAlternative();
        AnnotationModel annotation = cstAlternative.getAnnotation(AnnotationName.ParserPrecedence);
        if (annotation == null)
            return 0;
        String value = annotation.getValue();
        // This is grandfathered in.
        if (Strings.isNullOrEmpty(value))
            return 1;
        return Integer.parseInt(value);
    }

    @Nonnull
    public void addAction(@Nonnull LRItem item, @Nonnull TokenModel key, @Nonnull LRAction value) {
        Preconditions.checkNotNull(key, "TokenModel was null.");
        Preconditions.checkNotNull(value, "LRAction was null.");

        int currPrecedence = getActionPrecedence(item);
        int prevPrecedence = actionPrecedence.getInt(key);
        if (currPrecedence > prevPrecedence) {
            actionPrecedence.put(key, currPrecedence);
            actions.remove(key);
            actionItems.row(key).clear();
        } else if (currPrecedence < prevPrecedence) {
            return;
        }

        actions.put(key, value);
        actionItems.put(key, value, item);
    }

    public void clear() {
        actions.clear();
        actionPrecedence.clear();
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
        return ImmutableSortedMap.copyOf(actions, TokenModel.IndexComparator.INSTANCE);
    }
}
