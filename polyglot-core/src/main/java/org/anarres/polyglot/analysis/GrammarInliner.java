/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import java.util.HashSet;
import java.util.Set;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Performs explicitly requested inlinings.
 *
 * @author shevek
 */
public class GrammarInliner implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GrammarInliner.class);
    private static final boolean DEBUG = false;

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public GrammarInliner(ErrorHandler errors, GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Override
    public void run() {
        Set<CstAlternativeModel> inlineAlternatives = new HashSet<>();
        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            if (cstProduction.hasAnnotation(AnnotationName.Inline)) {
                inlineAlternatives.addAll(cstProduction.getAlternatives().values());
                continue;
            }
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                if (cstAlternative.hasAnnotation(AnnotationName.Inline)) {
                    inlineAlternatives.add(cstAlternative);
                    continue;
                }
            }
        }

        if (!inlineAlternatives.isEmpty()) {
            Inliner inliner = new Inliner(errors, grammar);
            // This is opportunistic. We actually don't care.
            boolean success = inliner.substitute(inlineAlternatives);
            if (success)
                if (DEBUG)
                    LOG.debug("Performed explicit inlining.");
        }
    }

}
