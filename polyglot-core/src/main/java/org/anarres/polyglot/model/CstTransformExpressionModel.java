/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;
import org.anarres.polyglot.node.AListExpression;
import org.anarres.polyglot.node.ANewExpression;
import org.anarres.polyglot.node.ANullExpression;
import org.anarres.polyglot.node.AReferenceExpression;
import org.anarres.polyglot.node.PExpression;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TKwNew;
import org.anarres.polyglot.node.TTokLsquare;
import org.anarres.polyglot.node.Token;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author shevek
 */
public abstract class CstTransformExpressionModel extends AbstractModel {

    private static final Logger LOG = LoggerFactory.getLogger(CstTransformExpressionModel.class);

    public interface Visitor<I, O, X extends Exception> {

        public O visitNull(@Nonnull Null expression, I input) throws X;

        public O visitReference(@Nonnull Reference expression, I input) throws X;

        public O visitNew(@Nonnull New expression, I input) throws X;

        public O visitList(@Nonnull List expression, I input) throws X;
    }

    public static class AbstractVisitor<I, O, X extends Exception> implements Visitor<I, O, X> {

        public O visitDefault(@Nonnull CstTransformExpressionModel expression, I input) throws X {
            return null;
        }

        @Override
        public O visitNull(Null expression, I input) throws X {
            return visitDefault(expression, input);
        }

        @Override
        public O visitReference(Reference expression, I input) throws X {
            return visitDefault(expression, input);
        }

        @Override
        public O visitNew(New expression, I input) throws X {
            for (CstTransformExpressionModel e : expression.getArguments())
                e.apply(this, input);
            return visitDefault(expression, input);
        }

        @Override
        public O visitList(List expression, I input) throws X {
            for (CstTransformExpressionModel e : expression.getItems())
                e.apply(this, input);
            return visitDefault(expression, input);
        }
    }

    public interface Container {

        public void addTransformExpression(@Nonnull CstTransformExpressionModel expression);
    }

    /** This avoids having CstTransformExpressionModel be generic. */
    public static abstract class Base<Node> extends CstTransformExpressionModel {

        public Base(@Nonnull Token location) {
            super(location);
        }
    }

    public static class Null extends Base<ANullExpression> {

        public Null(@Nonnull Token location) {
            super(location);
        }

        @Override
        public <I, O, X extends Exception> O apply(Visitor<I, O, X> visitor, I input) throws X {
            return visitor.visitNull(this, input);
        }

        @Override
        public ANullExpression toNode() {
            return new ANullExpression();
        }

        @Override
        protected void toStringBuilder(StringBuilder buf) {
            buf.append("Null");
        }

        @Override
        public String toString() {
            return "Null";
        }
    }

    public static class Reference extends Base<AReferenceExpression> {

        private final TIdentifier elementName;
        private final TIdentifier transformName;
        public CstElementModel element;
        public CstTransformPrototypeModel transform;
        /* The index into CstProductionModel.transformPrototype or CstAlternativeModel.transformExpressions. */
        // public int transformIndex;
        // private final Throwable created = new Exception();

        public Reference(@Nonnull TIdentifier elementName, @CheckForNull TIdentifier transformName) {
            super(elementName);
            this.elementName = Preconditions.checkNotNull(elementName, "Element name was null.");
            this.transformName = transformName;
        }

        @Nonnull
        public String getElementName() {
            // if (element != null) return element.getName();
            return elementName.getText();
        }

        @TemplateProperty
        public CstElementModel getElement() {
            return Preconditions.checkNotNull(element, "Element of %s was null on read.", this);
        }

        @CheckForNull
        public String getTransformName() {
            // if (transform != null) return transform.getName();
            if (transformName == null)
                return null;
            return transformName.getText();
        }

        @TemplateProperty
        public CstTransformPrototypeModel getTransform() {
            return Preconditions.checkNotNull(transform, "Transform of %s was null on read for element %s", this, element);
        }

        @Override
        public boolean isListValue() {
            return getTransform().getUnaryOperator().isList();
        }

        @Override
        public <I, O, X extends Exception> O apply(Visitor<I, O, X> visitor, I input) throws X {
            return visitor.visitReference(this, input);
        }

        @Override
        public AReferenceExpression toNode() {
            TIdentifier elementName = element != null ? element.toNameToken() : this.elementName.clone();
            TIdentifier transformName = transform != null ? transform.toNameToken() : this.transformName != null ? this.transformName.clone() : null;
            return new AReferenceExpression(elementName, transformName);
        }

        @Override
        protected void toStringBuilder(StringBuilder buf) {
            if (element != null)
                buf.append(element.getName());
            else
                buf.append('"').append(getElementName()).append('"');

            if (transform != null)
                buf.append('.').append(transform.getName());
            else if (getTransformName() != null)
                buf.append(".\"").append(getTransformName()).append('"');
        }
    }

    public static class New extends Base<ANewExpression> implements Container {

        public final java.util.List<CstTransformExpressionModel> arguments = new ArrayList<>();
        public final TIdentifier productionName;
        public final TIdentifier alternativeName;
        public AstAlternativeModel astAlternative;

        public New(@Nonnull TIdentifier productionName, @CheckForNull TIdentifier alternativeName) {
            super(productionName);
            this.productionName = productionName;
            this.alternativeName = alternativeName;
        }

        @Override
        public void addTransformExpression(CstTransformExpressionModel expression) {
            arguments.add(expression);
        }

        @Nonnull
        public String getProductionName() {
            return productionName.getText();
        }

        @CheckForNull
        public String getAlternativeName() {
            if (alternativeName == null)
                return null;
            return alternativeName.getText();
        }

        @Nonnull
        public AstAlternativeModel getAstAlternative() {
            return astAlternative;
        }

        @TemplateProperty
        public java.util.List<CstTransformExpressionModel> getArguments() {
            return arguments;
        }

        @Nonnull
        public CstTransformExpressionModel getArgument(int i) {
            return arguments.get(i);
        }

        @Override
        public <I, O, X extends Exception> O apply(Visitor<I, O, X> visitor, I input) throws X {
            return visitor.visitNew(this, input);
        }

        @Override
        public ANewExpression toNode() {
            java.util.List<PExpression> out = new ArrayList<>();
            for (CstTransformExpressionModel e : getArguments())
                out.add(e.toNode());
            TIdentifier productionName = astAlternative != null ? astAlternative.getProduction().toNameToken() : this.productionName.clone();
            TIdentifier alternativeName = astAlternative != null ? astAlternative.toNameToken() : this.alternativeName != null ? this.alternativeName.clone() : null;
            return new ANewExpression(productionName, alternativeName, out, new TKwNew());
        }

        @Override
        protected void toStringBuilder(StringBuilder buf) {
            buf.append("New ");
            if (astAlternative != null)
                buf.append(astAlternative.getProduction().getName());
            else
                buf.append('"').append(getProductionName()).append('"');
            if (astAlternative != null)
                buf.append(".").append(astAlternative.getSourceName());
            else if (getAlternativeName() != null)
                buf.append(".\"").append(getAlternativeName()).append('"');
            buf.append('(');
            toStringBuilder(buf, getArguments());
            buf.append(')');
        }
    }

    public static class List extends Base<AListExpression> implements Container {

        private final java.util.List<CstTransformExpressionModel> items = new ArrayList<>();
        public AstProductionSymbol elementType;

        public List(@Nonnull Token location) {
            super(location);
        }

        @Override
        public void addTransformExpression(CstTransformExpressionModel expression) {
            items.add(expression);
        }

        @TemplateProperty
        public java.util.List<CstTransformExpressionModel> getItems() {
            return items;
        }

        @Nonnull
        public String getJavaTypeName() {
            if (elementType == null)
                return "Object";    // List<Null>.
            return AstElementModel.getJavaTypeName(elementType);
        }

        @Override
        public boolean isListValue() {
            return true;
        }

        @Override
        public <I, O, X extends Exception> O apply(Visitor<I, O, X> visitor, I input) throws X {
            return visitor.visitList(this, input);
        }

        @Override
        public AListExpression toNode() {
            java.util.List<PExpression> out = new ArrayList<>();
            for (CstTransformExpressionModel e : getItems())
                out.add(e.toNode());
            return new AListExpression(out, new TTokLsquare(getLocation()));
        }

        @Override
        protected void toStringBuilder(StringBuilder buf) {
            buf.append('[');
            toStringBuilder(buf, getItems());
            buf.append(']');
        }
    }

    public CstTransformExpressionModel(@Nonnull Token location) {
        super(location);
    }

    @Nonnull
    @TemplateProperty
    public String getType() {
        return getClass().getSimpleName().toLowerCase();
    }

    /**
     * If we are adding this to a list, do we use add() or addAll()?
     *
     * Yes if this is an explicit list OR if it's a list-typed variable (reference to a list).
     */
    @TemplateProperty
    public boolean isListValue() {
        return false;
    }

    public abstract <I, O, X extends Exception> O apply(Visitor<I, O, X> visitor, I input) throws X;

    @Nonnull
    public abstract PExpression toNode();

    protected void toStringBuilder(@Nonnull StringBuilder buf, @Nonnull java.util.List<? extends CstTransformExpressionModel> l) {
        boolean b = false;
        for (CstTransformExpressionModel e : l) {
            if (b)
                buf.append(", ");
            else
                b = true;
            e.toStringBuilder(buf);
        }
    }

    protected abstract void toStringBuilder(@Nonnull StringBuilder buf);

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        toStringBuilder(buf);
        return buf.toString();
    }
}
