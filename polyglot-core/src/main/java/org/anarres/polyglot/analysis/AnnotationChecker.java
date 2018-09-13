/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.Joiner;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AbstractNamedModel;
import org.anarres.polyglot.model.AnnotationModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.TokenModel;

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
            errors.addError(a.getLocation(), "Annotation type " + annotationName + " is not applicable to model type " + m.getClass().getSimpleName() + " '" + m.getName() + "'; only " + Joiner.on(", ").join(annotationName.getTargets()) + ".");
        }
    }

    @Override
    public void run() {
        for (TokenModel token : grammar.getTokens()) {
            check(token);
        }

        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            check(cstProduction);
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives()) {
                String precedence = cstAlternative.getPrecedence();
                if (precedence != null) {
                    if (!grammar.precedenceComparator.isLegal(precedence)) {
                        errors.addError(cstAlternative.getLocation(), "Precedence '" + precedence + "' is not known.");
                    }
                }
                check(cstAlternative);
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    check(cstElement);
                }
            }
        }

        for (AstProductionModel astProduction : grammar.getAstProductions()) {
            check(astProduction);
            for (AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                check(astAlternative);
                for (AstElementModel astElement : astAlternative.getElements()) {
                    check(astElement);
                }
            }
        }
    }

}
