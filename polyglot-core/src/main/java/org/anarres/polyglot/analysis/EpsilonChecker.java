/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.lr.FirstFunction;
import org.anarres.polyglot.lr.IgnoredProductionsSet;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class EpsilonChecker implements Runnable {

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public EpsilonChecker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Override
    public void run() {
        FirstFunction firstFunction = new FirstFunction(grammar, IgnoredProductionsSet.EMPTY);
        for (CstProductionModel cstProduction : grammar.cstProductions.values()) {
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    if (cstElement.isTerminal())
                        continue;
                    if (cstElement.getUnaryOperator().isNullable()) {
                        if (firstFunction.isNullable(cstElement.getCstProduction())) {
                            errors.addError(cstElement.getLocation(), "Optional CST element '" + cstElement.getName() + "' references a nullable production '" + cstElement.getCstProduction() + "' and will lead to a shift-reduce error.");
                        }
                    }
                }
            }
        }
    }

}
