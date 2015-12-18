/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 * A reference to either a {@link TokenModel} or an {@link AstProductionModel}.
 *
 * @see AstProductionSymbol
 * @author shevek
 */
public class AstElementModel extends AbstractElementModel<AstProductionSymbol> implements AstModel {

    /**
     * Returns the most specific possible java type name for the symbol.
     *
     * This is the first applicable alternative of:
     * <ul>
     * <li>The java type name of the singleton alternative of the production.
     * <li>The java type name of the production, if not a singleton.
     * <li>The java type name of the token.
     * </ul>
     *
     * @param symbol The AstProductionSymbol for which to construct a type name.
     * @return the most specific possible java type name for the symbol.
     */
    @Nonnull
    public static String getJavaTypeName(@Nonnull AstProductionSymbol symbol) {
        // If we have a production with a single alterntive, use that alternative type for all elements.
        // Putting this into AstProductionModel would really screw up template generation.
        if (symbol instanceof AstProductionModel) {
            AstProductionModel production = (AstProductionModel) symbol;
            AstAlternativeModel alternative = production.getSingletonAlternative();
            if (alternative != null)
                return alternative.getJavaTypeName();
        }
        return symbol.getJavaTypeName();
    }

    @Nonnull
    public static AstElementModel forNode(@Nonnull AElement node) {
        AstElementModel model = new AstElementModel(
                name(node),
                Specifier.toSpecifier(node.getSpecifier()),
                node.getSymbolName(),
                UnaryOperator.toUnaryOperator(node.getUnOp()),
                annotations(node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    @Nonnull
    public static AstElementModel forToken(@Nonnull TokenModel token) {
        AstElementModel out = new AstElementModel(token.toNameToken(), Specifier.TOKEN, token.toNameToken(), UnaryOperator.NONE, HashMultimap.<String, AnnotationModel>create());
        out.symbol = token;
        return out;
    }

    private final Multimap<String, AnnotationModel> annotations;

    public AstElementModel(@Nonnull TIdentifier name, @Nonnull Specifier specifier, @Nonnull TIdentifier symbolName, @Nonnull UnaryOperator unaryOperator, Multimap<String, AnnotationModel> annotations) {
        super(name, specifier, symbolName, unaryOperator);
        this.annotations = annotations;
    }

    @Nonnull
    public AstProductionModel getAstProduction() {
        return Preconditions.checkNotNull((AstProductionModel) getSymbol(), "Symbol was null.");
    }

    @Override
    @TemplateProperty
    public String getJavaTypeName() {
        return getJavaTypeName(getSymbol());
    }

    @Override
    public Multimap<String, AnnotationModel> getAnnotations() {
        return annotations;
    }

    @Override
    public AElement toNode() {
        TIdentifier nameToken = Objects.equals(getName(), getSymbolName()) ? null : toNameToken();
        return new AElement(
                newJavadocCommentToken(),
                nameToken,
                toSpecifier(),
                new TIdentifier(getSymbolName(), getLocation()),
                getUnaryOperator().newUnOp(),
                toAnnotations(getAnnotations()));
    }
}
