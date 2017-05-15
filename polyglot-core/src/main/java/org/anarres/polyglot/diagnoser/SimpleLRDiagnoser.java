/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.diagnoser;

import com.google.common.base.Throwables;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.lr.FirstFunction;
import org.anarres.polyglot.lr.IgnoredProductionsSet;
import org.anarres.polyglot.lr.LRAction;
import org.anarres.polyglot.lr.LRConflict;
import org.anarres.polyglot.lr.LRDiagnosis;
import org.anarres.polyglot.lr.LRItem;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Don't even.
 *
 * @author shevek
 */
public class SimpleLRDiagnoser implements LRDiagnoser {

    private static final Logger LOG = LoggerFactory.getLogger(SimpleLRDiagnoser.class);
    private static final boolean DEBUG = false;

    @Nonnull
    private static IntSet[] newIntSetArray(@Nonnegative int size) {
        IntSet[] out = new IntSet[size];
        for (int i = 0; i < size; i++)
            out[i] = new IntOpenHashSet();
        return out;
    }

    @Nonnull
    private static String indent(@Nonnegative int depth, @Nonnegative int matched) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < depth; i++)
            buf.append("  ");
        buf.append("[").append(matched).append("] ");
        return buf.toString();
    }
    private final GrammarModel grammar;
    private final CstProductionModel cstProductionRoot;
    // private final LR0ItemUniverse universe;
    private final IgnoredProductionsSet ignoredProductions;
    private final FirstFunction firstFunction;

    public SimpleLRDiagnoser(GrammarModel grammar, CstProductionModel cstProductionRoot) {
        this.grammar = grammar;
        this.cstProductionRoot = cstProductionRoot;
        // this.universe = new LR0ItemUniverse(grammar, cstProductionRoot);
        this.ignoredProductions = new IgnoredProductionsSet(grammar, cstProductionRoot);
        this.firstFunction = new FirstFunction(grammar, ignoredProductions);
    }

    private class Context {

        private final LRDiagnosis diagnosis;
        private final LRConflict conflict;
        private final List<? extends CstProductionSymbol> cstSymbolPath;
        private final IntSet itemAlternatives = new IntOpenHashSet();
        // private final IntSet[] cstProductionsSeen;
        private final IntSet[] cstProductionsSeenAtDepth;
        private final Deque<LRDiagnosis.ItemFrame> stack = new ArrayDeque<>();
        private long lastDumpTime = System.currentTimeMillis();

        public Context(@Nonnull LRDiagnosis diagnosis, @Nonnull LRConflict conflict) {
            this.diagnosis = diagnosis;
            this.conflict = conflict;
            this.cstSymbolPath = conflict.getState().getStack();
            for (LRItem item : conflict.getItems().values())
                itemAlternatives.add(item.getProductionAlternative().getIndex());
            this.cstProductionsSeenAtDepth = newIntSetArray(cstSymbolPath.size() + 1);
        }

        /**
         * If we've entered a particular CST production at a particular cstSymbolPathIndex, there's
         * often no point entering it at the same depth again.
         */
        // We need to break symmetry against all the possible orders in which we can stack up zero-position recursive alts of a single prod on the stack.
        // e.g.
        // A = . A op0 B
        // -> A = . A op1 B
        //   -> A = . A op2 B
        public boolean enter(@Nonnull CstProductionModel cstProduction, @Nonnegative int cstSymbolPathIndex) {
            if (ignoredProductions.isIgnored(cstProduction)) {
                if (DEBUG)
                    LOG.info("Production ignored: " + cstProduction);
                return false;
            }
            if (!cstProductionsSeenAtDepth[cstSymbolPathIndex].add(cstProduction.getIndex())) {
                if (DEBUG)
                    LOG.info("Already seen . " + cstProduction.getName() + " at cstSymbolPathIndex " + cstSymbolPathIndex);
                return false;
            }
            return true;
        }

        public boolean enter(@Nonnull CstAlternativeModel cstAlternative) {
            if (ignoredProductions.isIgnored(cstAlternative)) {
                if (DEBUG)
                    LOG.info("Alternative ignored: " + cstAlternative.getName());
                return false;
            }
            return true;
        }

        public void push(@Nonnull CstAlternativeModel cstAlternative, @Nonnegative int position, @Nonnegative int cstSymbolStackIndex) {
            stack.addLast(new LRDiagnosis.ItemFrame(cstAlternative, position));
            // If we enter an alt at 0, then there's no point entering any other alt on that prod this round.
            // This is our primary symmetry-break.
        }

        public void pop() {
            LRDiagnosis.ItemFrame frame = stack.removeLast();
        }

        public void leave(@Nonnull CstAlternativeModel cstAlternative) {
        }

        public void leave(@Nonnull CstProductionModel cstProduction) {
        }
    }

    private boolean found(@Nonnull Context context, @Nonnull CstAlternativeModel cstAlternative, @Nonnegative int cstElementIndex) {
        LOG.info("Reached limit, checking if " + cstAlternative + " in " + context.itemAlternatives + "\n" + new LRDiagnosis.Path(context.stack));
        if (!context.itemAlternatives.rem(cstAlternative.getIndex()))
            return false;

        for (Map.Entry<? extends LRAction, ? extends LRItem> e : context.conflict.getItems().entrySet()) {
            if (cstAlternative == e.getValue().getProductionAlternative()) {
                LOG.info("Found!");
                context.push(cstAlternative, cstElementIndex, context.stack.size());
                context.diagnosis.put(e.getKey(), new LRDiagnosis.Path(context.stack));
                context.pop();
                return true;
            }
        }

        throw new IllegalStateException("Found item in itemAlternatives but not in LRConflict.");
    }

    private void search(@Nonnull Context context, @Nonnegative int depth, @Nonnegative int cstSymbolPathIndex, @Nonnull CstProductionModel cstProduction) {
        long now = System.currentTimeMillis();
        if (context.lastDumpTime < now - 2000) {
            LOG.info("Stack is\n" + new LRDiagnosis.Path(context.stack));
            context.lastDumpTime = now;
        }

        if (!context.enter(cstProduction, cstSymbolPathIndex)) {
            if (DEBUG)
                LOG.info(indent(depth, cstSymbolPathIndex) + "Not entering " + cstProduction + " at " + cstSymbolPathIndex);
            return;
        }

        try {
            if (DEBUG)
                LOG.info(indent(depth, cstSymbolPathIndex) + "Enter production " + cstProduction.getName() + " at " + cstSymbolPathIndex + " looking for " + context.itemAlternatives.size() + " more targets.");

            OPTIMIZE:
            {
                // We should be able to do this for productions as well as tokens,
                // with an appropriate definition or subclass of FirstFunction.
                CstProductionSymbol cstSymbolExpect = context.cstSymbolPath.get(cstSymbolPathIndex);
                Set<? extends TokenModel> cstTokensExpect = firstFunction.apply(cstSymbolExpect);   // Might be a token.
                Set<? extends TokenModel> cstTokensActual = firstFunction.apply(cstProduction);
                if (Collections.disjoint(cstTokensActual, cstTokensExpect)) {
                    if (DEBUG)
                        LOG.info(indent(depth, cstSymbolPathIndex) + "First disjoint: expect=" + cstTokensExpect + ", actual=" + cstTokensActual);
                    return;
                }
            }

            if (false) {
                for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                    LOG.info(indent(depth, cstSymbolPathIndex) + "Alt of production " + cstProduction.getName() + " is " + cstAlternative.getName());
                }
            }

            ALTERNATIVE:
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                // We have to check seen on alternatives, because sometimes we need to go twice around a list production.
                if (!context.enter(cstAlternative)) {
                    LOG.info(indent(depth, cstSymbolPathIndex) + "Not entering alternative " + cstAlternative.getName());
                    continue;
                }
                if (DEBUG)
                    LOG.info(indent(depth, cstSymbolPathIndex) + "Enter alternative " + cstAlternative.getName());

                if (cstSymbolPathIndex == context.cstSymbolPath.size()) {
                    if (found(context, cstAlternative, 0))
                        return;
                    if (DEBUG)
                        LOG.info(indent(depth, cstSymbolPathIndex) + "Continuing (too deep) out of alternative " + cstAlternative.getName());
                    continue ALTERNATIVE;
                }

                ELEMENT:
                for (int i = 0; i < cstAlternative.getElements().size(); i++) {
                    CstElementModel cstElement = cstAlternative.getElement(i);
                    CstProductionSymbol cstSymbolActual = cstElement.getSymbol();
                    CstProductionSymbol cstSymbolExpect = context.cstSymbolPath.get(cstSymbolPathIndex + i);
                    if (DEBUG)
                        LOG.info(indent(depth, cstSymbolPathIndex + i) + "Element " + cstAlternative.getName() + ".[" + i + "] is " + cstSymbolActual.getName() + ", expect=" + cstSymbolExpect.getName());

                    // Yay, a match.
                    if (cstSymbolActual == cstSymbolExpect) {
                        // if (DEBUG)
                        // LOG.info(indent(depth, cstSymbolPathIndex + i) + "Element " + cstAlternative.getName() + ".[" + i + "] Match.");

                        if (cstSymbolPathIndex + i + 1 == context.cstSymbolPath.size()) {
                            if (found(context, cstAlternative, i + 1))
                                return;
                            if (DEBUG)
                                LOG.info(indent(depth, cstSymbolPathIndex) + "Continuing (too deep, within) out of alternative " + cstAlternative.getName());
                            continue ALTERNATIVE;
                        }
                        // TODO: We also need to recurse into it, in case it's a left-recursive application.
                        if (DEBUG)
                            LOG.info(indent(depth, cstSymbolPathIndex) + "Element matches; continuing to next element after match in " + cstAlternative.getName());
                        continue ELEMENT;
                    }
                    // if (DEBUG)
                    // LOG.info(indent(depth, cstSymbolPathIndex + i) + "Element " + cstAlternative.getName() + ".[" + i + "] No match.");
                    // No match, and it's a terminal. Reject.
                    if (cstSymbolActual.isTerminal()) {
                        if (DEBUG)
                            LOG.info(indent(depth, cstSymbolPathIndex + i) + "Breaking because terminal " + cstSymbolActual.getName() + " cannot match " + cstSymbolExpect.getName() + ".");
                        break;
                    }
                    context.push(cstAlternative, i, cstSymbolPathIndex + i);
                    search(context, depth + 1, cstSymbolPathIndex + i, (CstProductionModel) cstSymbolActual);
                    if (context.itemAlternatives.isEmpty()) {
                        if (DEBUG)
                            LOG.info(indent(depth, cstSymbolPathIndex + i) + "Returning due to completion.");
                        return;
                    }
                    context.pop();
                    // No match, and it's a non-terminal. Reject.
                    // if (DEBUG)
                    // LOG.info(indent(depth, cstSymbolPathIndex + i) + "Element " + cstAlternative.getName() + ".[" + i + "] After sub-match.");
                    break;
                }
                if (DEBUG)
                    LOG.info(indent(depth, cstSymbolPathIndex) + "Exit alternative " + cstAlternative.getName());
                context.leave(cstAlternative);
            }
        } finally {
            // if (DEBUG)
            // LOG.info(indent(depth, cstSymbolPathIndex) + "Exit production " + cstProduction.getName() + " at " + cstSymbolPathIndex);
            context.leave(cstProduction);
        }
    }

    @Override
    public LRDiagnosis diagnose(@Nonnull LRConflict conflict) {
        LRDiagnosis out = new LRDiagnosis(conflict);

        try {
            Context context = new Context(out, conflict);
            search(context, 0, 0, cstProductionRoot);
        } catch (Exception e) {
            LOG.warn("Failed", e);
            throw Throwables.propagate(e);
        }

        return out;
    }

}
