/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import java.util.HashSet;
import java.util.Set;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class GrammarSimplifier implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GrammarSimplifier.class);
    private static final boolean DEBUG = false;

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public GrammarSimplifier(ErrorHandler errors, GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Override
    public void run() {
        Object2IntOpenHashMap<CstProductionModel> cstProductionReferenceCount = new Object2IntOpenHashMap<>();

        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives()) {
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    if (!cstElement.isTerminal()) {
                        cstProductionReferenceCount.addTo(cstElement.getCstProduction(), 1);
                    }
                }
            }
        }

        Set<CstAlternativeModel> inlineAlternatives = new HashSet<>();
        for (Object2IntMap.Entry<CstProductionModel> e : cstProductionReferenceCount.object2IntEntrySet()) {
            CstProductionModel cstProduction = e.getKey();
            if (DEBUG)
                LOG.debug(cstProduction.getName() + " -> " + e.getIntValue() + " uses of " + cstProduction.getAlternatives().size() + " alts.");
            // This runs after GrammarNormalizer, so we are safe against creating explosion due to optional items in rules.
            // So we could use && or || in this conditional, depending on how aggressive we are.
            // It turns out that && makes the world a lot slower.
            // if (e.getIntValue() > 1 || cstProduction.getAlternatives().size() > 1)
            // Experimentally, this setup seems to reduce both state count and build time.
            if (e.getIntValue() != 1)
                continue;
            if (cstProduction.getAlternatives().size() != 1)
                continue;
            inlineAlternatives.addAll(cstProduction.getAlternatives());
        }
        if (!inlineAlternatives.isEmpty()) {
            Inliner inliner = new Inliner(errors, grammar);
            // This is opportunistic. We actually don't care.
            boolean success = inliner.substitute(inlineAlternatives);
            if (success)
                if (DEBUG)
                    LOG.debug("Grammar simplified.");
        }
    }

}
