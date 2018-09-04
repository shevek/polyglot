/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.Preconditions;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;

/**
 * Checks for conflicts in the grammar.
 * 
 * <ul>
 * <li>No two expressions of an alternative may reference the same element/transform pair.
 * <li>Alternatives foo.barbaz and foobar.baz can generate the same Java type name.
 * </ul>
 *
 * @author shevek
 */
public class ConflictChecker implements Runnable {

    private final ErrorHandler errors;
    private final GrammarModel grammar;

    public ConflictChecker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    private static class TransformVisitor extends CstTransformExpressionModel.AbstractVisitor<Void, Void, RuntimeException> {

        private static class Reference {

            private final CstElementModel element;
            private final CstTransformPrototypeModel transform;

            public Reference(CstElementModel element, CstTransformPrototypeModel transform) {
                this.element = Preconditions.checkNotNull(element, "CstElementModel was null.");
                this.transform = Preconditions.checkNotNull(transform, "CstTransformPrototypeModel was null.");
            }

            @Override
            public int hashCode() {
                return element.hashCode() ^ transform.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (null == obj)
                    return false;
                if (!getClass().equals(obj.getClass()))
                    return false;
                Reference o = (Reference) obj;
                return element.equals(o.element) && transform.equals(o.transform);
            }

        }

        private final ErrorHandler errors;
        private final Set<Reference> transforms = new HashSet<>();

        public TransformVisitor(@Nonnull ErrorHandler errors) {
            this.errors = errors;
        }

        @Override
        public Void visitReference(CstTransformExpressionModel.Reference expression, Void input) throws RuntimeException {
            Reference key = new Reference(expression.element, expression.transform);
            if (!transforms.add(key))
                errors.addError(expression.getLocation(), "Element " + expression + " has already been used in a transform of this CST alternative.");
            return super.visitReference(expression, input);
        }
    }

    @Override
    public void run() {
        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives()) {
                // Check for duplicate use of an element/transform pair.
                TransformVisitor visitor = new TransformVisitor(errors);
                for (CstTransformExpressionModel transformExpression : cstAlternative.getTransformExpressions()) {
                    transformExpression.apply(visitor, null);
                }
            }
        }

        Map<String, AstAlternativeModel> javaTypeNameMap = new HashMap<>();
        for (AstProductionModel astProduction : grammar.getAstProductions()) {
            for (AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                String javaTypeName = astAlternative.getJavaTypeName();
                AstAlternativeModel prev = javaTypeNameMap.put(javaTypeName, astAlternative);
                if (prev != null) {
                    errors.addError(astAlternative.getLocation(), "Two AST elements generate the same Java type name: '" + astAlternative.getName() + "' and '" + prev.getName() + "'.");
                }
                // for (AstElementModel astElement : astAlternative.getElements()) { astElement.getJavaFieldName(); }
            }
        }
    }
}
