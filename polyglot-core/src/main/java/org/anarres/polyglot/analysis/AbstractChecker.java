/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
@Deprecated // Not used.
public class AbstractChecker implements Runnable {

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public AbstractChecker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Override
    public void run() {
        for (AstProductionModel astProduction : grammar.astProductions.values()) {
            if (astProduction.abstractAlternative == null)
                continue;
        }
    }

}
