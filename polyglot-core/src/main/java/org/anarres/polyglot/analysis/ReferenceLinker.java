/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import java.util.Map;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AbstractElementModel;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.ProductionSymbol;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class ReferenceLinker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceLinker.class);

    /* pp */ static boolean linkTransform(@Nonnull ErrorHandler errors, @Nonnull CstTransformExpressionModel.Reference expression) {
        String transformName = expression.getTransformName();
        CstProductionSymbol symbol = expression.element.getSymbol();
        // We previously found that the element does not refer to a useful symbol.
        // We bail here so that we don't either NPE, or produce a duplicate error message.
        // TODO: This may cause an issue for relinking.
        if (symbol == null)
            return false;
        if (transformName != null) {
            for (CstTransformPrototypeModel transformPrototype : symbol.getTransformPrototypes()) {
                if (transformName.equals(transformPrototype.getName())) {
                    expression.transform = transformPrototype;
                    // LOG.info("Found transform " + transformPrototype + " in " + symbol);
                    // expression.transformIndex = i;
                    return true;
                }
            }
            errors.addError(expression.getLocation(), "CST production '" + symbol.getName() + "' was never transformed to '" + transformName + "'.");
            return false;
        } else {
            switch (symbol.getTransformPrototypes().size()) {
                case 0:
                    errors.addError(expression.getLocation(), "Expression cannot refer to CST production '" + symbol.getName() + "', which was transformed to void.");
                    return false;
                case 1:
                    expression.transform = Iterables.getOnlyElement(symbol.getTransformPrototypes());
                    // LOG.info("Found default transform " + expression.transform + " in " + symbol.getClass().getSimpleName() + " " + symbol);
                    // expression.transformIndex = 0;
                    return true;
                default:
                    errors.addError(expression.getLocation(), "Expression refers to CST production '" + symbol.getName() + "' with multiple transformations, but does not specify one.");
                    return false;
            }
        }
    }

    private final ErrorHandler errors;
    private final GrammarModel grammar;
    private final CstTransformExpressionModel.Visitor<CstAlternativeModel, Void, RuntimeException> expressionVisitor = new CstTransformExpressionModel.AbstractVisitor<CstAlternativeModel, Void, RuntimeException>() {
        @Override
        public Void visitReference(CstTransformExpressionModel.Reference expression, CstAlternativeModel cstAlternative) throws RuntimeException {

            ELEMENT:
            {
                String elementName = expression.getElementName();
                for (CstElementModel element : cstAlternative.getElements()) {
                    if (elementName.equals(element.getName())) {
                        expression.element = element;
                        break ELEMENT;
                    }
                }
                errors.addError(expression.getLocation(), "No such element '" + elementName + "' in transform of alternative '" + cstAlternative.getName() + "'; existing are " + Joiner.on(", ").join(cstAlternative.getElements()));
                return null;
            }

            TRANSFORM:
            {
                if (!linkTransform(errors, expression))
                    return null;
            }

            return super.visitReference(expression, cstAlternative);
        }

        @Override
        public Void visitNew(CstTransformExpressionModel.New expression, CstAlternativeModel cstAlternative) throws RuntimeException {
            String productionName = expression.getProductionName();
            AstProductionModel astProduction = grammar.astProductions.get(productionName);
            PRODUCTION:
            {
                if (astProduction == null) {
                    errors.addError(expression.getLocation(), "No such AST production '" + productionName + "' for New.");
                    return null;
                }
            }

            ALTERNATIVE:
            {
                String alternativeSourceName = expression.getAlternativeName();
                if (alternativeSourceName != null) {
                    String alternativeName = AstAlternativeModel.name(astProduction, alternativeSourceName);
                    // LOG.info("Looking for alternativeName = " + alternativeName);
                    AstAlternativeModel astAlternative = astProduction.alternatives.get(alternativeName);
                    if (astAlternative == null) {
                        errors.addError(expression.getLocation(), "No such AST alternative '" + alternativeName + "' for New.");
                        return null;
                    }
                    expression.astAlternative = astAlternative;
                } else {
                    if (astProduction.alternatives.size() != 1) {
                        errors.addError(expression.getLocation(), "AST production '" + astProduction.getName() + "' has multiple alternatives. Please specify one, so I don't have to guess.");
                        return null;
                    }
                    expression.astAlternative = Iterables.getOnlyElement(astProduction.alternatives.values());
                }
            }

            return super.visitNew(expression, cstAlternative);
        }

    };

    public ReferenceLinker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    private <S extends ProductionSymbol> void linkElement(@Nonnull AbstractElementModel<S> element, Map<? extends String, ? extends S> productions, @Nonnull String productionDesc, @Nonnull String targetDesc) {
        String symbolName = element.getSymbolName();

        TokenModel token = null;
        if (element.getSpecifier().isTokenAllowed())
            token = grammar.getToken(symbolName);

        S production = null;
        if (element.getSpecifier().isProductionAllowed())
            production = productions.get(symbolName);

        if (production != null) {
            if (token == null || token.isIgnored()) {
                element.symbol = production;
                return;
            }
            errors.addError(element.getLocation(), "Ambiguous name '" + symbolName + "' in " + targetDesc + " could reference either"
                    + " production at " + ErrorHandler.toLocationString(production.getLocation())
                    + " or token at " + ErrorHandler.toLocationString(token.getLocation()) + ".");
        } else if (token != null) {
            if (!token.isIgnored()) {
                @SuppressWarnings("unchecked")
                S symbol = (S) token;
                element.symbol = symbol;
                return;
            }
            errors.addError(element.getLocation(), "Cannot reference ignored token '" + symbolName + "' for " + targetDesc + ".");
        } else {
            errors.addError(element.getLocation(), "No such token or " + productionDesc + " '" + symbolName + "' for " + targetDesc + ".");
        }
    }

    private void linkCstTransformPrototype(@Nonnull CstTransformPrototypeModel element) {
        linkElement(element, grammar.astProductions, "AST production", "transform prototype");
    }

    private void linkCstElement(@Nonnull CstElementModel element) {
        linkElement(element, grammar.cstProductions, "CST production", "CST element");
    }

    private void linkAstElement(@Nonnull AstElementModel element) {
        linkElement(element, grammar.astProductions, "AST production", "AST element");
    }

    @Override
    public void run() {
        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            for (CstTransformPrototypeModel transformPrototype : cstProduction.getTransformPrototypes()) {
                linkCstTransformPrototype(transformPrototype);
            }
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                for (CstElementModel cstElement : cstAlternative.getElements()) {
                    linkCstElement(cstElement);
                }
                for (CstTransformExpressionModel transformExpression : cstAlternative.getTransformExpressions()) {
                    transformExpression.apply(expressionVisitor, cstAlternative);
                }
            }
        }

        for (AstProductionModel astProduction : grammar.getAstProductions()) {
            for (AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                for (AstElementModel astElement : astAlternative.getElements()) {
                    linkAstElement(astElement);
                }
            }
        }
    }
}
