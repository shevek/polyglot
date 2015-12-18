/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import java.util.Collections;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AAnnotation;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.TIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A wrapper around a {@link TokenModel terminal} or {@link CstProductionModel nonterminal} in a concrete production.
 *
 * @author shevek
 */
// TODO: Extends AbstractElementModel.
public class CstElementModel extends AbstractElementModel<CstProductionSymbol> {

    private static final Logger LOG = LoggerFactory.getLogger(CstElementModel.class);

    @Nonnull
    public static CstElementModel forNode(@Nonnull AElement node) {
        return new CstElementModel(
                name(node),
                Specifier.toSpecifier(node.getSpecifier()),
                node.getSymbolName(),
                UnaryOperator.toUnaryOperator(node.getUnOp()));
    }

    @Nonnull
    public static CstElementModel forToken(@Nonnull TokenModel token) {
        CstElementModel out = new CstElementModel(token.toNameToken(), Specifier.TOKEN, token.toNameToken(), UnaryOperator.NONE);
        out.symbol = token;
        return out;
    }

    @Nonnull
    public static TIdentifier name(@Nonnull AElement node) {
        TIdentifier identifier = node.getName();
        if (identifier == null)
            identifier = node.getSymbolName();
        return identifier;
    }

    public CstElementModel(TIdentifier name, Specifier specifier, TIdentifier symbolName, UnaryOperator unaryOperator) {
        super(name, specifier, symbolName, unaryOperator);
    }

    @Nonnull
    public CstProductionModel getCstProduction() {
        return Preconditions.checkNotNull((CstProductionModel) getSymbol(), "Symbol was null.");
    }

    @Override
    public String getJavaTypeName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public AElement toNode() {
        TIdentifier nameToken = Objects.equals(getName(), getSymbolName()) ? null : toNameToken();
        // LOG.info("name=" + getName() + ", symbolName=" + getSymbolName() + ", nameToken=" + nameToken);
        return new AElement(
                newJavadocCommentToken(),
                nameToken,
                toSpecifier(),
                new TIdentifier(getSymbolName(), getLocation()),
                getUnaryOperator().newUnOp(),
                Collections.<AAnnotation>emptyList());
    }

    @Override
    public String toString() {
        String name = getName();
        String symbolName = getSymbolName();
        if (name.equals(symbolName))
            return name;
        return "[" + name + "]:" + symbolName;
    }
}
