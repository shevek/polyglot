/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AbstractElementModel;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.AstProductionSymbol;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.Specifier;
import org.anarres.polyglot.model.TokenModel;
import org.anarres.polyglot.model.UnaryOperator;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This normalization process attempts to avoid generating erasing productions,
 * as they tend to make the grammar be LR(k &gt; 1).
 *
 * @author shevek
 */
public class GrammarNormalizer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GrammarNormalizer.class);
    private static final String SUFFIX_LIST = "$list";
    private static final String SUFFIX_WITH = "$with_";
    private static final String SUFFIX_WITHOUT = "$no_";

    private static class CstAlternativeSubstitute {

        private final StringBuilder name = new StringBuilder();
        private final List<CstElementModel> elements = new ArrayList<>();
        private final List<CstTransformExpressionModel> transformExpressions = new ArrayList<>();

        public CstAlternativeSubstitute(@Nonnull CstAlternativeModel alternative) {
            // We can't use alternative.getSourceName() here because
            // two elements may have the same source name.
            // We need at least the alternativeIndex.
            TIdentifier name = alternative.getAlternativeName();
            if (name != null)
                this.name.append(name.getText());
            else
                this.name.append('$').append(alternative.getAlternativeIndex());
            this.transformExpressions.addAll(alternative.getTransformExpressions());
        }

        public CstAlternativeSubstitute(@Nonnull CstAlternativeSubstitute source) {
            this.name.append(source.name);
            this.elements.addAll(source.elements);
            this.transformExpressions.addAll(source.transformExpressions);
        }

        @Nonnull
        public CstAlternativeModel toCstAlternative(@Nonnull GrammarModel grammar, @Nonnull CstProductionModel production, @Nonnull Token location) {
            CstAlternativeModel out = CstAlternativeModel.forName(grammar.cstAlternativeIndex++, production, new TIdentifier(name.toString(), location));
            out.elements.addAll(elements);
            out.transformExpressions.addAll(transformExpressions);
            return out;
        }
    }

    private final ErrorHandler errors;
    private final GrammarModel grammar;
    private final ExpressionSubstituteVisitor substituteVisitor = new ExpressionSubstituteVisitor();
    private final CstTransformExpressionModel.Visitor<CstElementModel, Void, RuntimeException> relinkVisitor = new CstTransformExpressionModel.AbstractVisitor<CstElementModel, Void, RuntimeException>() {

        @Override
        public Void visitReference(CstTransformExpressionModel.Reference expression, CstElementModel input) throws RuntimeException {
            if (expression.element == input) {
                // TODO: This is a pretty horrible hack.
                if (!ReferenceLinker.linkTransform(errors, expression))
                    errors.addError(expression.getLocation(), "Internal error: Failed to relink expression after normalization.");
                // LOG.info("Relinked " + expression + " which now refers to " + input + "; transform is now " + expression.transform);
            }
            return null;
        }
    };

    public GrammarNormalizer(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    /** Converts 'expression' to an AListExpression '[expression]'. */
    @Nonnull
    /* pp */ static CstTransformExpressionModel.List newListExpression(@Nonnull CstTransformExpressionModel e0) {
        CstTransformExpressionModel.List out = new CstTransformExpressionModel.List(e0.getLocation());
        out.addTransformExpression(e0);
        return out;
    }

    @Nonnull
    /* pp */ static CstTransformExpressionModel.List newListExpression(@Nonnull CstTransformExpressionModel e0, @Nonnull CstTransformExpressionModel e1) {
        CstTransformExpressionModel.List out = newListExpression(e0);
        out.addTransformExpression(e1);
        return out;
    }

    @Nonnull
    /* pp */ static CstTransformExpressionModel.Reference newReferenceExpression(@Nonnull CstElementModel element, @Nonnull CstTransformPrototypeModel transform) {
        // TIdentifier transformName = (transform != null) ? transform.toNameToken() : null;
        CstTransformExpressionModel.Reference out = new CstTransformExpressionModel.Reference(element.toNameToken(), transform.toNameToken());
        out.element = Preconditions.checkNotNull(element, "CstElementModel was null.");
        out.transform = Preconditions.checkNotNull(transform, "CstTransformPrototypeModel was null.");
        return out;
    }

    @Nonnull
    private CstAlternativeModel newBaseAlternative(@Nonnull CstProductionModel production, @Nonnull CstElementModel element) {
        Token location = element.getLocation();
        CstAlternativeModel out = CstAlternativeModel.forName(grammar.cstAlternativeIndex++, production, new TIdentifier("base", location));
        CstElementModel outElement = new CstElementModel(new TIdentifier("tail", location), element.getSpecifier(), element.symbol.toNameToken(), UnaryOperator.NONE);
        outElement.symbol = element.symbol;
        out.elements.add(outElement);

        // Now we generate enough copies of the expression to satisfy the transform prototype.
        if (element.symbol instanceof TokenModel) {
            TokenModel token = (TokenModel) element.symbol;
            out.transformExpressions.add(newListExpression(newReferenceExpression(outElement, token.getTransformPrototype())));
        } else {
            CstProductionModel prod = (CstProductionModel) element.symbol;
            for (CstTransformPrototypeModel transform : prod.getTransformPrototypes()) {
                out.transformExpressions.add(newListExpression(newReferenceExpression(outElement, transform)));
            }
        }

        return out;
    }

    /**
     * If we have a CST element foo*, where CST production foo { -> a b } then
     *
     * Token -> [tail.token], [head.token, tail.token]
     * Production -> [tail.a] [tail.b], [head.a tail.a] [head.b tail.b]
     */
    @Nonnull
    private CstAlternativeModel newListAlternative(@Nonnull CstProductionModel production, @Nonnull CstElementModel element) {
        Token location = element.getLocation();
        CstAlternativeModel out = CstAlternativeModel.forName(grammar.cstAlternativeIndex++, production, new TIdentifier("list", location));

        CstElementModel outHeadElement = new CstElementModel(new TIdentifier("head", location), Specifier.PRODUCTION, production.toNameToken(), UnaryOperator.NONE);
        outHeadElement.symbol = production;
        out.elements.add(outHeadElement);

        CstElementModel outTailElement = new CstElementModel(new TIdentifier("tail", location), element.getSpecifier(), element.symbol.toNameToken(), UnaryOperator.NONE);
        outTailElement.symbol = element.symbol;
        out.elements.add(outTailElement);

        if (element.isTerminal()) {
            TokenModel token = element.getToken();
            // This should have been created by newProduction()->newTransformPrototype() below.
            CstTransformPrototypeModel transform = Iterables.getOnlyElement(production.transformPrototypes);
            out.transformExpressions.add(newListExpression(
                    newReferenceExpression(outHeadElement, transform),
                    newReferenceExpression(outTailElement, token.getTransformPrototype())));
        } else {
            CstProductionModel prod = element.getCstProduction();
            for (int i = 0; i < prod.getTransformPrototypes().size(); i++) {
                CstTransformPrototypeModel listTransform = production.getTransformPrototype(i);
                CstTransformPrototypeModel itemTransform = prod.getTransformPrototype(i);
                out.transformExpressions.add(newListExpression(
                        newReferenceExpression(outHeadElement, listTransform),
                        newReferenceExpression(outTailElement, itemTransform)));
            }
        }

        return out;
    }

    /** Constructs { -> symbol* }. */
    @Nonnull
    private CstTransformPrototypeModel newListTransformPrototype(@Nonnull TIdentifier name, @Nonnull AstProductionSymbol symbol) {
        Specifier specifier = symbol instanceof TokenModel ? Specifier.TOKEN : Specifier.PRODUCTION;
        CstTransformPrototypeModel out = new CstTransformPrototypeModel(name, specifier, symbol.toNameToken(), UnaryOperator.STAR);
        out.symbol = symbol;
        return out;
    }

    @Nonnull
    private CstProductionModel newListProduction(@Nonnull String name, @Nonnull CstElementModel element) {
        Token location = element.getLocation();
        CstProductionModel out = new CstProductionModel(grammar.cstProductionIndex++, new TIdentifier(name, location));

        if (element.symbol instanceof TokenModel) {
            TokenModel token = (TokenModel) element.symbol;
            out.transformPrototypes.add(newListTransformPrototype(token.getTransformPrototype().toNameToken(), token));
        } else {
            CstProductionModel prod = (CstProductionModel) element.symbol;
            for (CstTransformPrototypeModel transform : prod.getTransformPrototypes())
                out.transformPrototypes.add(newListTransformPrototype(transform.toNameToken(), transform.symbol));
        }

        return out;
    }

    @Nonnull
    private CstProductionModel getListProduction(@Nonnull CstElementModel element) {
        String name = element.getSymbolName() + SUFFIX_LIST;
        // Since we use unique suffices, we can safely use the grammar as a store for synthetic productions.
        CstProductionModel production = grammar.getCstProduction(name);
        if (production == null) {
            production = newListProduction(name, element);
            production.addAlternative(newBaseAlternative(production, element));
            production.addAlternative(newListAlternative(production, element));
            grammar.addCstProduction(production);
        }
        return production;
    }

    /**
     * If the element is something like foo*, rewrites it to be foo$star,
     * referring to a new production.
     *
     * Rewrites the CstElementModel to avoid quantifiers.
     */
    @Nonnull
    private void addSyntheticProductions(@Nonnull CstAlternativeModel cstAlternative) {

        List<CstAlternativeSubstitute> substitutes = new ArrayList<>();
        substitutes.add(new CstAlternativeSubstitute(cstAlternative));

        for (CstElementModel cstElement : cstAlternative.getElements()) {
            switch (cstElement.getUnaryOperator()) {
                case STAR:
                case PLUS:
                    CstProductionModel listSymbol = getListProduction(cstElement);
                    cstElement.symbol = listSymbol;
                    for (CstTransformExpressionModel transformExpression : cstAlternative.getTransformExpressions()) {
                        // cstElement.symbol -> listSymbol.getTransformPrototypes()
                        transformExpression.apply(relinkVisitor, cstElement);
                    }
                    break;
                default:
                    // We're just using switch as a typesafe equality test.
                    break;
            }

            switch (cstElement.getUnaryOperator()) {
                case QUESTION:
                case STAR:
                    // Produce one production with, and one without the item.
                    // First, double up.
                    int size = substitutes.size();
                    for (int i = 0; i < size; i++)
                        substitutes.add(new CstAlternativeSubstitute(substitutes.get(i)));
                    // Now, without
                    ExpressionSubstituteVisitor.SubstitutionMap map = new ExpressionSubstituteVisitor.SubstitutionMap();
                    for (CstTransformPrototypeModel transformPrototype : cstElement.symbol.getTransformPrototypes()) {
                        // LOG.info("Substituting for " + transformPrototype + " from " + cstElement.symbol);
                        map.addSubstitution(cstElement, transformPrototype, new CstTransformExpressionModel.Null(cstElement.getLocation()));
                    }
                    // LOG.info("SubstitutionMap is " + map);
                    for (int i = 0; i < size; i++) {
                        CstAlternativeSubstitute substitute = substitutes.get(i);
                        substitute.name.append(SUFFIX_WITHOUT).append(cstElement.getSourceName());

                        for (int j = 0; j < substitute.transformExpressions.size(); j++) {
                            CstTransformExpressionModel transformExpression = substitute.transformExpressions.get(j);
                            substitute.transformExpressions.set(j, transformExpression.apply(substituteVisitor, map));
                        }
                    }
                    for (int i = 0; i < size; i++) {
                        CstAlternativeSubstitute substitute = substitutes.get(i + size);
                        substitute.name.append(SUFFIX_WITH).append(cstElement.getSourceName());
                        substitute.elements.add(cstElement);
                    }
                    break;
                default:
                    for (CstAlternativeSubstitute substitute : substitutes)
                        substitute.elements.add(cstElement);
                    break;
            }

            // We could remove the unop, or we could ignore it henceforth.
            // This cstElement is now shared between several alternatives.
            cstElement.setUnaryOperator(UnaryOperator.NONE);
        }

        if (substitutes.size() != 1) {
            CstProductionModel cstProduction = cstAlternative.getProduction();
            // i.e. if we had at least one star or questionmark operator.
            if (!cstProduction.removeAlterative(cstAlternative))
                throw new IllegalStateException("Failed to remove CST alternative " + cstAlternative.getName());

            for (CstAlternativeSubstitute substitute : substitutes) {
                cstProduction.addAlternative(substitute.toCstAlternative(grammar, cstProduction, cstAlternative.getLocation()));
            }
        }
    }

    private void normalize(@Nonnull AbstractElementModel<?> element) {
        switch (element.getUnaryOperator()) {
            case PLUS:
                element.setUnaryOperator(UnaryOperator.STAR);
                break;
            default:
                // Just a type-safe equality test.
                break;
        }
    }

    @Override
    public void run() {
        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            for (CstTransformPrototypeModel cstTransformPrototype : cstProduction.getTransformPrototypes()) {
                normalize(cstTransformPrototype);
            }
            for (CstAlternativeModel cstAlternative : new ArrayList<>(cstProduction.getAlternatives().values())) {
                // LOG.info("Normalizing " + cstAlternative);
                addSyntheticProductions(cstAlternative);
            }
        }

        for (AstProductionModel astProduction : grammar.getAstProductions()) {
            for (AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                for (AstElementModel astElement : astAlternative.getElements()) {
                    normalize(astElement);
                }
            }
        }
    }
}
