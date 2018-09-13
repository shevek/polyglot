/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.lr;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Table;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.PrecedenceComparator;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
/* pp */ class LRActionMapBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(LRActionMapBuilder.class);
    private static final boolean DEBUG = false;
    private final PrecedenceComparator precedenceComparator;
    // I think this is too sparse to be worth extending TokenMap.
    private final Map<TokenModel, LRAction> actions = new HashMap<>();
    // While we might imagine that this is lossy over <TokenModel, LRItem, LRAction>,
    // the extra information is useless, and we have to elide it manually.
    private final Table<TokenModel, LRAction, LRItem> actionItems = HashBasedTable.create();

    public LRActionMapBuilder(@Nonnull PrecedenceComparator precedenceComparator) {
        this.precedenceComparator = precedenceComparator;
    }

    // Appel, Modern Compiler Implementation in C, page 73.
    @Nonnull
    private PrecedenceComparator.Result compareRules(@Nonnull CstAlternativeModel r0, CstAlternativeModel r1) {
        if (r0.equals(r1))
            return PrecedenceComparator.Result.EQUAL;
        DEPRECATED:
        {
            boolean r0_high = r0.hasAnnotation(AnnotationName.ParserPrecedence);
            boolean r1_high = r1.hasAnnotation(AnnotationName.ParserPrecedence);
            if (r0_high && !r1_high)
                return PrecedenceComparator.Result.HIGHER;
            else if (r1_high && !r0_high)
                return PrecedenceComparator.Result.LOWER;
        }
        PrecedenceComparator.Result result = precedenceComparator.compare(r0.getPrecedence(), r1.getPrecedence());
        if (DEBUG)
            LOG.debug("CompareRules: " + result);
        return result;
    }

    @CheckForNull
    private LRAction resolveShiftReduceConflict(@Nonnull LRAction shiftAction, @Nonnull LRAction reduceAction) {
        switch (compareRules(shiftAction.getProductionAlternative(), reduceAction.getProductionAlternative())) {
            case LOWER:
                return reduceAction;
            case HIGHER:
                return shiftAction;
            case EQUAL:
                break;
            case INCOMPARABLE:
            default:
                // If we return null here, then a token cannot have associativity if both rules don't have a precedence.
                // return null;
                break;
        }
        if (DEBUG)
            LOG.debug("Associativity = " + shiftAction.getItem().getAssociativity());
        // For rules which are of equal precedence:
        switch (shiftAction.getItem().getAssociativity()) {
            case LEFT:
                return reduceAction;
            case RIGHT:
                return shiftAction;
            case NONE:
                return new LRAction.Error(shiftAction.getItem());
            case UNSPECIFIED:
            default:
                return null;
        }
    }

    @CheckForNull
    private LRAction resolveReduceReduceConflict(@Nonnull LRAction reduceAction0, @Nonnull LRAction reduceAction1) {
        switch (compareRules(reduceAction0.getProductionAlternative(), reduceAction1.getProductionAlternative())) {
            case LOWER:
                return reduceAction1;
            case HIGHER:
                return reduceAction0;
            default:
                if (reduceAction0.getProductionAlternative().equals(reduceAction1.getProductionAlternative()))
                    return reduceAction0;
                return null;
        }
    }

    @Nonnull
    public void addAction(@Nonnull LRItem item, @Nonnull TokenModel key, @Nonnull LRAction value) {
        Preconditions.checkNotNull(key, "TokenModel was null.");
        Preconditions.checkNotNull(value, "LRAction was null.");

        LRAction prev = actions.get(key);
        LRAction curr;
        if (prev != null) {
            switch (prev.getAction()) {
                case ERROR:
                    // A previous rule was nonassociative, we'll leave it as error.
                    return;
                case ACCEPT:
                    // The TokenModel must be an EOF, so it can only be an ACCEPT/ACCEPT conflict.
                    Preconditions.checkState(key == TokenModel.EOF.INSTANCE, "ACCEPT conflict on a non-EOF token.");
                    return;
                default:
                    // errorprone
                    break;
            }

            switch (value.getAction()) {
                case SHIFT:
                    // There's no such thing as a shift-shift conflict.
                    if (prev.getAction() == LRAction.Action.SHIFT)
                        return;
                    curr = resolveShiftReduceConflict(value, prev);
                    break;
                case REDUCE:
                    if (prev.getAction() == LRAction.Action.SHIFT)
                        curr = resolveShiftReduceConflict(prev, value);
                    else
                        curr = resolveReduceReduceConflict(prev, value);
                    break;

                default:
                    throw new IllegalStateException("Unexpected conflict on " + value);
            }
            if (DEBUG) {
                LOG.debug("Value = " + value + " on " + item);
                LOG.debug("Prev = " + prev + " on " + item);
                LOG.debug("Resolved = " + curr);
            }
            if (curr == prev)
                return; // We resolved it, and we're not changing anything.
            if (curr == null) {
                // We failed to resolved it. Let's remember why.
                actionItems.put(key, value, item);
                return;
            }
            actionItems.remove(key, prev);  // We resolved it, and it's not curr any more.
        } else {
            // No conflict.
            curr = value;
            actionItems.put(key, curr, item);
        }

        actions.put(key, curr);
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
        // Here, we remove all the explicit ERROR productions put into the table by the @NonAssociative annotations.
        for (Iterator<LRAction> it = actions.values().iterator(); it.hasNext(); /**/) {
            LRAction action = it.next();
            // LOG.debug("Evaluating " + action);
            if (action.getAction() == LRAction.Action.ERROR)
                it.remove();
        }
        return ImmutableSortedMap.copyOf(actions, TokenModel.IndexComparator.INSTANCE);
    }
}
