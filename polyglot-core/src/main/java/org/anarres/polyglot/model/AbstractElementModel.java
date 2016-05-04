/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Multimap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.AProductionSpecifier;
import org.anarres.polyglot.node.ATokenSpecifier;
import org.anarres.polyglot.node.PSpecifier;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
// -> AbstractSymbolReferenceModel?
public abstract class AbstractElementModel<S extends ProductionSymbol> extends AbstractNamedJavaModel {

    @Nonnull
    public static TIdentifier name(@Nonnull AElement node) {
        TIdentifier identifier = node.getName();
        if (identifier == null)
            identifier = node.getSymbolName();
        return identifier;
    }

    private final Specifier specifier;
    private final TIdentifier symbolName;
    private UnaryOperator unaryOperator;
    public S symbol;

    public AbstractElementModel(
            @Nonnull TIdentifier name,
            @Nonnull Specifier specifier,
            @Nonnull TIdentifier symbolName,
            @Nonnull UnaryOperator unaryOperator,
            @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
        this.specifier = specifier;
        this.symbolName = symbolName;
        this.unaryOperator = unaryOperator;
    }

    @Nonnull
    public Specifier getSpecifier() {
        return specifier;
    }

    @Nonnull
    public String getSymbolName() {
        if (symbol != null)
            return symbol.getName();
        return symbolName.getText();
    }

    @Nonnull
    public UnaryOperator getUnaryOperator() {
        return unaryOperator;
    }

    public void setUnaryOperator(@Nonnull UnaryOperator unaryOperator) {
        this.unaryOperator = unaryOperator;
    }

    public S getSymbol() {
        return symbol;
    }

    public boolean isTerminal() {
        Preconditions.checkNotNull(symbol, "Symbol was null.");
        // Why not symbol.isTerminal()?
        return symbol instanceof TokenModel;
    }

    @Nonnull
    public TokenModel getToken() {
        return Preconditions.checkNotNull((TokenModel) symbol, "Symbol was null.");
    }

    @Override
    public String getJavaMethodName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getName());
    }

    @TemplateProperty
    public boolean isNullable() {
        return getUnaryOperator().isNullable();
    }

    @TemplateProperty
    public boolean isList() {
        return getUnaryOperator().isList();
    }

    @CheckForNull
    protected PSpecifier toSpecifier() {
        ProductionSymbol s = getSymbol();
        if (s == null)
            return null;
        if (s.isTerminal())
            return new ATokenSpecifier();
        return new AProductionSpecifier();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        String name = getName();
        String symbolName = getSymbolName();
        if (!name.equals(symbolName))
            buf.append("[").append(name).append("]:");
        buf.append(symbolName).append(getUnaryOperator().getText());
        return buf.toString();
    }
}
