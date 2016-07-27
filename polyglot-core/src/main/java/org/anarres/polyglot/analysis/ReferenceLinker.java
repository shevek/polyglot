/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AbstractElementModel;
import org.anarres.polyglot.model.AnnotationName;
import org.anarres.polyglot.model.AstAlternativeModel;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionModel;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstElementModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstProductionSymbol;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.ExternalModel;
import org.anarres.polyglot.model.GrammarModel;
import org.anarres.polyglot.model.ProductionSymbol;
import org.anarres.polyglot.model.Specifier;
import org.anarres.polyglot.model.TokenModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Links all elements and transforms to their respective targets, and moves externals to a separate data structure.
 *
 * @author shevek
 */
public class ReferenceLinker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(ReferenceLinker.class);

    /* pp */ static boolean linkTransform(@Nonnull ErrorHandler errors, @Nonnull CstAlternativeModel location, @Nonnull CstTransformExpressionModel.Reference expression) {
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
            errors.addError(expression.getLocation(), "In transform of alternative '" + location.getName() + "',"
                    + " CST production '" + symbol.getName() + "' was never transformed to '" + transformName + "'.");
            return false;
        } else {
            switch (symbol.getTransformPrototypes().size()) {
                case 0:
                    errors.addError(expression.getLocation(), "In transform of alternative '" + location.getName() + "',"
                            + " Expression cannot refer to CST production '" + symbol.getName() + "', which was transformed to void.");
                    return false;
                case 1:
                    expression.transform = Iterables.getOnlyElement(symbol.getTransformPrototypes());
                    // LOG.info("Found default transform " + expression.transform + " in " + symbol.getClass().getSimpleName() + " " + symbol);
                    // expression.transformIndex = 0;
                    return true;
                default:
                    errors.addError(expression.getLocation(), "In transform of alternative '" + location.getName() + "',"
                            + " Expression refers to CST production '" + symbol.getName() + "' with multiple transformations, but does not specify one.");
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
                errors.addError(expression.getLocation(), "In transform of alternative '" + cstAlternative.getName() + "',"
                        + " reference '" + elementName + "' does not match any production element"
                        + "; element names are " + Joiner.on(", ").join(cstAlternative.getElements()));
                return null;
            }

            TRANSFORM:
            {
                if (!linkTransform(errors, cstAlternative, expression))
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
                    errors.addError(expression.getLocation(), "In CST alternative " + cstAlternative.getName() + ": No such AST production '" + productionName + "' for New.");
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
                        errors.addError(expression.getLocation(), "In CST alternative " + cstAlternative.getName() + ": No such AST alternative '" + alternativeName + "' for New.");
                        return null;
                    }
                    expression.astAlternative = astAlternative;
                } else {
                    if (astProduction.alternatives.size() != 1) {
                        errors.addError(expression.getLocation(), "In CST alternative " + cstAlternative.getName() + ": AST production '" + astProduction.getName() + "' has multiple alternatives. Please specify one, so I don't have to guess.");
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

    private static class Linkage<S extends ProductionSymbol> {

        private final String symbolDesc;
        private final S symbol;
        private final boolean legal;

        public Linkage(@Nonnull String symbolDesc, @Nonnull S symbol, boolean legal) {
            this.symbolDesc = symbolDesc;
            this.symbol = symbol;
            this.legal = legal;
        }

        public void toStringBuilder(StringBuilder buf, boolean name) {
            buf.append(symbolDesc);
            if (name)
                buf.append(" '").append(symbol.getName()).append("'");
            buf.append(" defined at ").append(ErrorHandler.toLocationString(symbol.getLocation()));
        }

        public String toString(boolean name) {
            StringBuilder buf = new StringBuilder();
            toStringBuilder(buf, name);
            return buf.toString();
        }

        @Override
        public String toString() {
            return toString(false);
        }
    }

    private static class Linkages<S extends ProductionSymbol> extends ArrayList<Linkage<S>> {

        private int legalCount;
        @SuppressFBWarnings("SE_BAD_FIELD")
        private Linkage<S> legalLinkage = null;

        public Linkages() {
            super(Specifier.values().length - 1);
        }

        @Override
        public boolean add(Linkage<S> e) {
            if (e.legal) {
                legalCount++;
                legalLinkage = e;
            }

            return super.add(e);
        }

        private String toString(boolean name) {
            switch (size()) {
                case 0:
                    return "<no linkages>";
                case 1:
                    return get(0).toString(name);
                default:
                    StringBuilder buf = new StringBuilder();
                    for (int i = 0; i < size() - 1; i++) {
                        if (i > 0)
                            buf.append(", ");
                        get(i).toStringBuilder(buf, name);
                    }
                    buf.append(" or ");
                    get(size() - 1).toStringBuilder(buf, name);
                    return buf.toString();
            }
        }

        @Override
        public String toString() {
            return toString(false);
        }
    }

    @SuppressWarnings("unchecked")  // Casts of TokenModel or ExternalModel to S are unchecked, but correct in this instance.
    private <S extends ProductionSymbol> void linkElement(@Nonnull AbstractElementModel<S> element, Map<? extends String, ? extends S> productions, @Nonnull String productionDesc, @Nonnull String prefix, @Nonnull String targetDesc, boolean externalsLegal) {
        String symbolName = element.getSymbolName();

        Linkages<S> linkages = new Linkages<>();

        TOKEN:
        if (element.getSpecifier().isAllowed(Specifier.TOKEN)) {
            TokenModel token = grammar.getToken(symbolName);
            if (token == null)
                break TOKEN;
            linkages.add(new Linkage<>(token.isIgnored() ? "ignored token" : "token", (S) token, !token.isIgnored()));
        }

        PRODUCTION:
        if (element.getSpecifier().isAllowed(Specifier.PRODUCTION)) {
            S production = productions.get(symbolName);
            if (production == null)
                break PRODUCTION;
            linkages.add(new Linkage<>(productionDesc, production, true));
        }

        EXTERNAL:
        if (element.getSpecifier().isAllowed(Specifier.EXTERNAL)) {
            ExternalModel external = grammar.getExternal(symbolName);
            if (external == null)
                break EXTERNAL;
            linkages.add(new Linkage<>((externalsLegal ? "" : "illegal ") + "external", (S) external, externalsLegal));
        }

        switch (linkages.legalCount) {
            case 1:
                element.symbol = linkages.legalLinkage.symbol;
                return;
            case 0:
                if (linkages.isEmpty())
                    errors.addError(element.getLocation(), prefix + ": No such token" + (externalsLegal ? ", external" : "") + " or " + productionDesc + " '" + symbolName + "' for " + targetDesc + " " + element.getName() + "'.");
                else
                    errors.addError(element.getLocation(), prefix + ": Name '" + symbolName + "' in " + targetDesc + " cannot reference " + linkages.toString(true));
                break;
            default:
                errors.addError(element.getLocation(), prefix + ": Ambiguous name '" + symbolName + "' in " + targetDesc + " could reference either " + linkages.toString(false));
                break;
        }
    }

    private void linkCstTransformPrototype(@Nonnull CstTransformPrototypeModel element, @Nonnull String prefix) {
        linkElement(element, grammar.astProductions, "AST production", prefix, "transform prototype", false);
    }

    private void linkCstElement(@Nonnull CstElementModel element, @Nonnull String prefix) {
        linkElement(element, grammar.cstProductions, "CST production", prefix, "CST element", false);
    }

    private void linkAstElement(@Nonnull AstElementModel element, @Nonnull String prefix) {
        linkElement(element, grammar.astProductions, "AST production", prefix, "AST element", true);
    }

    @Override
    public void run() {

        CST:
        {
            // All known weak productions are in the weak set.
            // As we discover a reference from a strong production to any weak production,
            // we enqueue the weak production. This causes subsidiary weak productions to
            // be linked, and so forth.
            // At the end, we may, if we choose, remove all remaining weak productions.
            Set<CstProductionModel> cstProductionsRoot = new HashSet<>(grammar.getCstProductionRoots());
            Set<CstProductionModel> cstProductionsWeak = new HashSet<>();
            Deque<CstProductionModel> cstProductionsTodo = new ArrayDeque<>(cstProductionsRoot);
            for (CstProductionModel cstProduction : grammar.getCstProductions()) {
                if (!cstProductionsRoot.contains(cstProduction)) {
                    if (cstProduction.hasAnnotation(AnnotationName.Weak))
                        cstProductionsWeak.add(cstProduction);
                    else
                        cstProductionsTodo.add(cstProduction);
                }
            }
            // Now every production is either weak, or TODO.

            for (;;) {
                CstProductionModel cstProduction = cstProductionsTodo.poll();
                if (cstProduction == null)
                    break;
                {
                    String prefix = "In CST production " + cstProduction.getName();
                    for (CstTransformPrototypeModel transformPrototype : cstProduction.getTransformPrototypes()) {
                        linkCstTransformPrototype(transformPrototype, prefix);
                    }
                }
                for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                    String prefix = "In CST alternative " + cstAlternative.getName();
                    for (CstElementModel cstElement : cstAlternative.getElements()) {
                        linkCstElement(cstElement, prefix);
                        CstProductionSymbol symbol = cstElement.symbol;
                        if (cstProductionsWeak.remove(symbol))  // doubles as an instanceof check.
                            cstProductionsTodo.add((CstProductionModel) symbol);
                    }
                    for (CstTransformExpressionModel transformExpression : cstAlternative.getTransformExpressions()) {
                        transformExpression.apply(expressionVisitor, cstAlternative);
                    }
                }
            }

            for (CstProductionModel cstProduction : cstProductionsWeak) {
                LOG.info("Discarded unreferenced weak production " + cstProduction.getName());
                grammar.removeCstProduction(cstProduction);
            }
        }

        AST:
        {
            // TODO: Possibly require all unreferenced AST symbols to be annotated @Weak?
            // TODO: Possibly discard unreferenced AST symbols? (Probably not.)
            for (AstProductionModel astProduction : grammar.getAstProductions()) {
                for (AstAlternativeModel astAlternative : astProduction.getAlternatives()) {
                    // for (AstElementModel astElement : astAlternative.getElements()) { linkAstElement(astElement); }
                    String prefix = "In AST alternative " + astAlternative.getName();
                    for (Iterator<AstElementModel> it = astAlternative.elements.iterator(); it.hasNext(); /* */) {
                        AstElementModel astElement = it.next();
                        linkAstElement(astElement, prefix);
                        if (astElement.symbol instanceof ExternalModel) {
                            it.remove();
                            astAlternative.externals.add(astElement);
                        }
                    }
                }
            }
        }
    }
}
