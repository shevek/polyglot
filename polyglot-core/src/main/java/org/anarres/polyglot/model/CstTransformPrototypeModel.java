/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public class CstTransformPrototypeModel extends AbstractElementModel<AstProductionSymbol> {

    private static final Logger LOG = LoggerFactory.getLogger(CstTransformPrototypeModel.class);

    @Nonnull
    public static CstTransformPrototypeModel forNode(@Nonnull AElement node) {
        return new CstTransformPrototypeModel(
                name(node),
                Specifier.toSpecifier(node.getSpecifier()),
                node.getSymbolName(),
                UnaryOperator.toUnaryOperator(node.getUnOp()));
    }

    public CstTransformPrototypeModel(@Nonnull TIdentifier name, @Nonnull Specifier specifier, @Nonnull TIdentifier symbolName, @Nonnull UnaryOperator unaryOperator) {
        super(name, specifier, symbolName, unaryOperator);
        // LOG.info(name.getText() + ":" + symbolName.getText() + "." + unaryOperator + "@" + System.identityHashCode(this), new Exception());
    }

    @Nonnull
    public AstProductionModel getAstProduction() {
        return Preconditions.checkNotNull((AstProductionModel) getSymbol(), "Symbol was null.");
    }

    @Override
    @TemplateProperty
    public String getJavaTypeName() {
        return AstElementModel.getJavaTypeName(getSymbol());
    }

    @Override
    public AElement toNode() {
        TIdentifier nameToken = Objects.equals(getName(), getSymbolName()) ? null : toNameToken();
        return new AElement(
                newJavadocCommentToken(),
                nameToken,
                toSpecifier(),
                new TIdentifier(getSymbolName(), getLocation()),
                getUnaryOperator().newUnOp());
    }
}
