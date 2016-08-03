/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AbstractNamedModel;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 *
 * @author shevek
 */
public class AnnotationChecker implements Runnable {

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public AnnotationChecker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    private void check(@Nonnull AbstractNamedModel m) {
        for (AnnotationModel a : m.getAnnotations().values()) {
            AnnotationName annotationName = AnnotationName.forText(a.getName());
            if (annotationName == null)
                continue;
            if (annotationName.isTarget(m.getClass()))
                continue;
            errors.addError(m.getLocation(), "Annotation type " + annotationName + " is not applicable to model type " + m.getClass().getSimpleName());
        }
    }

    @Override
    public void run() {
        for (AstProductionModel astProduction : grammar.getAstProductions()) {
            check(astProduction);
        }
    }

}
