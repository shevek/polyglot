/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.analysis;

import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.model.AstElementModel;
import org.anarres.polyglot.model.AstProductionSymbol;
import org.anarres.polyglot.model.CstAlternativeModel;
import org.anarres.polyglot.model.CstProductionModel;
import org.anarres.polyglot.model.CstTransformExpressionModel;
import org.anarres.polyglot.model.CstTransformPrototypeModel;
import org.anarres.polyglot.model.GrammarModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class TypeChecker implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(TypeChecker.class);

    @Nonnull
    private static String toSymbolName(@CheckForNull AstProductionSymbol symbol) {
        if (symbol == null)
            return "Null";
        else
            return symbol.getName();
    }

    @Nonnull
    private static String toTypeName(@Nonnull AstProductionSymbol symbol, boolean list) {
        String name = toSymbolName(symbol);
        if (list)
            return "[" + name + "]";
        else
            return name;
    }

    private static boolean isList(@Nonnull CstTransformExpressionModel expression) {
        return expression instanceof CstTransformExpressionModel.List;
    }

    private static boolean isAssignableFrom(@Nonnull AstProductionSymbol target, @CheckForNull AstProductionSymbol source) {
        if (source == null)
            return true;
        boolean ret = target == source;
        // LOG.info(target + " <- " + source);
        return ret;
    }

    private final ErrorHandler errors;
    private final GrammarModel grammar;
    private final CstTransformExpressionModel.Visitor<CstAlternativeModel, AstProductionSymbol, RuntimeException> expressionVisitor = new CstTransformExpressionModel.Visitor<CstAlternativeModel, AstProductionSymbol, RuntimeException>() {

        @Override
        public AstProductionSymbol visitNull(CstTransformExpressionModel.Null expression, CstAlternativeModel input) throws RuntimeException {
            return null;
        }

        @Override
        public AstProductionSymbol visitReference(CstTransformExpressionModel.Reference expression, CstAlternativeModel input) throws RuntimeException {
            return expression.getTransform().getSymbol();
        }

        @Override
        public AstProductionSymbol visitNew(CstTransformExpressionModel.New expression, CstAlternativeModel input) throws RuntimeException {

            ARGUMENTS:
            {
                List<AstElementModel> parameters = expression.astAlternative.elements;

                if (expression.getArguments().size() != parameters.size()) {
                    errors.addError(expression.getLocation(), "AST alternative " + expression.astAlternative.getName()
                            + " has " + parameters.size() + " fields,"
                            + " but constructed with " + expression.getArguments().size() + " arguments.");
                    break ARGUMENTS;
                }

                for (int i = 0; i < parameters.size(); i++) {
                    AstElementModel parameter = parameters.get(i);
                    CstTransformExpressionModel argument = expression.getArgument(i);
                    AstProductionSymbol result = argument.apply(this, input);
                    // LOG.info(parameter + " <<-- " + result + " list " + isList(argument) + ": " + argument + ":: " + argument.getClass());
                    if (!isAssignableFrom(parameter.symbol, result) || isList(argument) != parameter.isList())
                        errors.addError(argument.getLocation(), "In transform of CST alternative " + input.getName() + ": Cannot pass argument of type " + toTypeName(result, isList(argument)) + " for parameter " + parameter + " of " + expression.astAlternative.getName());
                    else if (!parameter.isNullable() && argument.isNullableValue())
                        errors.addError(argument.getLocation(), "In transform of CST alternative " + input.getName() + ": Cannot pass nullable argument for parameter " + parameter + " of " + expression.astAlternative.getName());

                    // Allow construction with an empty list.
                    if (isList(argument) && result == null)
                        ((CstTransformExpressionModel.List) argument).elementType = parameter.symbol;
                }
            }

            return expression.astAlternative.getProduction();
        }

        @Override
        public AstProductionSymbol visitList(CstTransformExpressionModel.List expression, CstAlternativeModel input) throws RuntimeException {
            AstProductionSymbol symbol = null;
            for (CstTransformExpressionModel item : expression.getItems()) {
                AstProductionSymbol result = item.apply(this, input);

                if (isList(item)) {
                    errors.addError(item.getLocation(), "In transform of CST alternative " + input.getName() + ": Nested lists are not permitted.");
                    continue;
                }

                if (symbol == null)
                    symbol = result;

                if (!isAssignableFrom(symbol, result)) {
                    errors.addError(item.getLocation(), "In transform of CST alternative " + input.getName() + ": Cannot make a heterogenous list of " + toTypeName(symbol, false) + " and " + toTypeName(result, isList(item)));
                    continue;
                }

            }

            expression.elementType = symbol;
            return symbol;
        }
    };

    public TypeChecker(@Nonnull ErrorHandler errors, @Nonnull GrammarModel grammar) {
        this.errors = errors;
        this.grammar = grammar;
    }

    @Override
    public void run() {
        for (CstProductionModel cstProduction : grammar.getCstProductions()) {
            for (CstAlternativeModel cstAlternative : cstProduction.getAlternatives().values()) {
                if (cstAlternative.getTransformExpressions().size() != cstProduction.getTransformPrototypes().size()) {
                    errors.addError(cstAlternative.getLocation(), "Production " + cstProduction.getName() + " has " + cstProduction.getTransformPrototypes().size() + " transformation prototypes"
                            + " but alternative " + cstAlternative.getName() + " has " + cstAlternative.getTransformExpressions().size() + " transform expressions.");
                    continue;
                }

                for (int i = 0; i < cstAlternative.getTransformExpressions().size(); i++) {
                    CstTransformPrototypeModel transformPrototype = cstProduction.getTransformPrototype(i);
                    CstTransformExpressionModel transformExpression = cstAlternative.getTransformExpression(i);
                    AstProductionSymbol result = transformExpression.apply(expressionVisitor, cstAlternative);
                    // LOG.info("transformPrototype.getSymbol() = " + transformPrototype.getSymbol() + "@" + System.identityHashCode(transformPrototype.getSymbol()) + "@" + transformPrototype.getSymbol().getLocation().getLine() + ":" + transformPrototype.getSymbol().getLocation().getPos());
                    // LOG.info("result = " + result + "@" + System.identityHashCode(result) + "@" + result.getLocation().getLine() + ":" + result.getLocation().getPos());
                    if (!isAssignableFrom(transformPrototype.getSymbol(), result)
                            || transformPrototype.isList() != isList(transformExpression))
                        errors.addError(transformExpression.getLocation(), "Transform expression " + i + " of type " + toTypeName(result, isList(transformExpression)) + " does not satisfy transform prototype " + transformPrototype);
                    else if (!transformPrototype.isNullable() && transformExpression.isNullableValue())
                        errors.addError(transformExpression.getLocation(), "Transform expression '" + transformExpression + "' is nullable, and does not satisfy non-nullable transform prototype '" + transformPrototype + "'.");
                }
            }
        }
    }
}
