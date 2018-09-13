/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.node.ACstAlternative;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.PExpression;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TTokArrow;
import org.anarres.polyglot.node.Token;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A derivation for a nonterminal symbol, consisting of {@link CstElementModel elements}.
 *
 * @author shevek
 */
public final class CstAlternativeModel extends AbstractNamedModel implements Indexed, CstTransformExpressionModel.Container {

    private static final Logger LOG = LoggerFactory.getLogger(CstAlternativeModel.class);

    @Nonnull
    public static CstAlternativeModel forNode(@Nonnull ErrorHandler errors, int index, @Nonnull CstProductionModel production, @Nonnull ACstAlternative node) {
        Token location = location(production, node.getName(), node.getJavadocComment(), Iterables.getFirst(node.getElements(), null));
        TIdentifier name = node.getName();
        // if (name == null) name = new TIdentifier("$" + cstProduction.alternativeIndex++, cstProduction.getLocation());
        int alternativeIndex = production.alternativeIndex++;
        CstAlternativeModel model = new CstAlternativeModel(index, production, location, name, alternativeIndex, annotations(errors, node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    @Nonnull
    public static CstAlternativeModel forName(@Nonnegative int index, @Nonnull CstProductionModel production, @Nonnull TIdentifier name, @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        int alternativeIndex = production.alternativeIndex++;
        return new CstAlternativeModel(index, production, name, name, alternativeIndex, annotations);
    }

    @Nonnull
    public static CstAlternativeModel forName(@Nonnegative int index, @Nonnull CstProductionModel production, @Nonnull TIdentifier name) {
        return forName(index, production, name, ImmutableMultimap.<String, AnnotationModel>of());
    }

    @Nonnull
    private static String name(@Nonnull CstProductionModel production, @CheckForNull TIdentifier name, @Nonnegative int alternativeIndex) {
        String text = production.getName();
        if (name != null)
            text = text + "." + name.getText();
        else
            text = text + ".$" + alternativeIndex;
        return text;
    }

    private final int index;
    private final CstProductionModel production;
    @CheckForNull
    private final TIdentifier alternativeName;
    private final int alternativeIndex;
    public final List<CstElementModel> elements = new ArrayList<>();
    /** @see CstProductionModel#transformPrototypes */
    public final List<CstTransformExpressionModel> transformExpressions = new ArrayList<>(1);   // We very rarely get more than one.
    // public final LRAction.Reduce reduceActionCache = new LRAction.Reduce(this);
    @CheckForNull
    private final String precedence;

    private CstAlternativeModel(@Nonnegative int index, @Nonnull CstProductionModel production, @Nonnull Token location, @CheckForNull TIdentifier name, @Nonnegative int alternativeIndex, Multimap<String, ? extends AnnotationModel> annotations) {
        // TODO: This is a really bad choice for Location as it points to the production not the elements.
        super(location, name(production, name, alternativeIndex), annotations);
        this.index = index;
        this.production = Preconditions.checkNotNull(production, "CstProductionModel was null.");
        this.alternativeName = name;
        this.alternativeIndex = alternativeIndex;

        AnnotationModel annotation = getAnnotation(AnnotationName.Precedence);
        if (annotation == null)
            annotation = production.getAnnotation(AnnotationName.Precedence);
        this.precedence = annotation == null ? null : annotation.getValue();
    }

    /**
     * The global index of this alternative in the grammar.
     * CstAlternativeModel uses index for reduction rules in the parser.
     *
     * @return The global index of this alternative in the grammar.
     */
    @Override
    public int getIndex() {
        return index;
    }

    /*
     @Override
     public String getJavaTypeName() {
     StringBuilder buf = new StringBuilder("A");
     if (node.getName() != null)
     buf.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, node.getName().getText()));
     buf.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, production.getName()));
     return buf.toString();
     }
     */
    @Nonnull
    public CstProductionModel getProduction() {
        return production;
    }

    /**
     * The index of this alternative in the production.
     *
     * @return The index of this alternative in the production.
     */
    public int getAlternativeIndex() {
        return alternativeIndex;
    }

    @CheckForNull
    public TIdentifier getAlternativeName() {
        return alternativeName;
    }

    @Override
    public String getSourceName() {
        if (alternativeName != null)
            return alternativeName.getText();
        return getProduction().getSourceName();   // Must be unqualified.
        // return "$" + alternativeIndex;
    }

    @Nonnull
    public CstElementModel getElement(@Nonnegative int i) {
        return elements.get(i);
    }

    @Nonnull
    @TemplateProperty
    public List<? extends CstElementModel> getElements() {
        return elements;
    }

    @TemplateProperty
    public List<? extends CstElementModel> getElementsReversed() {
        return Lists.reverse(getElements());
    }

    /**
     * @see #getTransformExpression(int)
     * @see CstProductionModel#getTransformPrototypes()
     * @see CstProductionModel#getTransformPrototype(int)
     * @return The list of transform expressions.
     */
    @TemplateProperty
    public List<CstTransformExpressionModel> getTransformExpressions() {
        return transformExpressions;
    }

    /**
     * @see #getTransformExpressions()
     * @see CstProductionModel#getTransformPrototypes()
     * @see CstProductionModel#getTransformPrototype(int)
     * @param index The index of the expression to return.
     * @return The expression.
     */
    @TemplateProperty
    public CstTransformExpressionModel getTransformExpression(int index) {
        return transformExpressions.get(index);
    }

    @Override
    public void addTransformExpression(CstTransformExpressionModel expression) {
        transformExpressions.add(expression);
    }

    // To be looked up in PrecedenceComparator.
    @CheckForNull
    public String getPrecedence() {
        return precedence;
    }

    @Nonnull
    @TemplateProperty("parser.vm")  // For naming reduction functions.
    public String getJavaMethodName() {
        StringBuilder buf = new StringBuilder("A");
        if (alternativeName != null)
            buf.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, alternativeName.getText()));
        buf.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, production.getName()));
        if (alternativeName == null && production.alternatives.size() > 1)
            buf.append("$").append(alternativeIndex);
        // LOG.info(this + " -> " + buf);
        return buf.toString();
    }

    /** Passing null asks if ignored for all machines. */
    public boolean isIgnored(@CheckForNull String machineName) {
        if (!AnnotationUtils.isIncluded(this, AnnotationName.ParserInclude, AnnotationName.ParserExclude, machineName))
            return true;
        if (AnnotationUtils.isAnnotated(this, AnnotationName.ParserIgnore, machineName))
            return true;
        return getProduction().isIgnored(machineName);
    }

    @Override
    public ACstAlternative toNode() {
        List<AElement> elements = new ArrayList<>();
        for (CstElementModel e : getElements())
            elements.add(e.toNode());
        List<PExpression> transform = new ArrayList<>();
        for (CstTransformExpressionModel e : getTransformExpressions())
            transform.add(e.toNode());
        return new ACstAlternative(
                newJavadocCommentToken(),
                alternativeName,
                elements,
                new TTokArrow(),
                transform,
                toAnnotations(getAnnotations()));
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        if (precedence != null)
            buf.append("@Precedence('").append(precedence).append("') ");
        buf.append(getName()).append(" =");
        for (CstElementModel e : getElements())
            buf.append(' ').append(e);
        buf.append(" { ->");
        for (CstTransformExpressionModel e : getTransformExpressions())
            buf.append(' ').append(e);
        buf.append(" }");
        return buf.toString();
    }
}
