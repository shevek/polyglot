/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.lr.LRAction;
import org.anarres.polyglot.node.ACstAlternative;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.PExpression;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TTokArrow;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A derivation for a nonterminal symbol, consisting of {@link CstElementModel elements}.
 *
 * @author shevek
 */
public class CstAlternativeModel extends AbstractNamedModel implements Indexed, CstTransformExpressionModel.Container {

    private static final Logger LOG = LoggerFactory.getLogger(CstAlternativeModel.class);

    @Nonnull
    public static CstAlternativeModel forNode(int index, CstProductionModel production, ACstAlternative node) {
        TIdentifier name = node.getName();
        // if (name == null) name = new TIdentifier("$" + cstProduction.alternativeIndex++, cstProduction.getLocation());
        return forName(index, production, name);
    }

    @Nonnull
    public static CstAlternativeModel forName(@Nonnegative int index, @Nonnull CstProductionModel production, @CheckForNull TIdentifier name) {
        int alternativeIndex = production.alternativeIndex++;
        return new CstAlternativeModel(index, production, name, alternativeIndex);
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
    public final List<CstTransformExpressionModel> transformExpressions = new ArrayList<>();
    public final LRAction.Reduce reduceActionCache = new LRAction.Reduce(this);

    private CstAlternativeModel(@Nonnegative int index, @Nonnull CstProductionModel production, @CheckForNull TIdentifier name, @Nonnegative int alternativeIndex) {
        // TODO: This is a really bad choice for Location as it points to the production not the elements.
        super(location(production, name), name(production, name, alternativeIndex));
        this.index = index;
        this.production = Preconditions.checkNotNull(production, "CstProductionModel was null.");
        this.alternativeName = name;
        this.alternativeIndex = alternativeIndex;
    }

    /** CstAlternativeModel uses index for reduction rules in the parser. */
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

    @CheckForNull
    public TIdentifier getAlternativeName() {
        return alternativeName;
    }

    @Override
    public String getSourceName() {
        if (alternativeName != null)
            return alternativeName.getText();
        return getProduction().getSourceName();   // Must be unqualified.
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

    @Override
    public ACstAlternative toNode() {
        List<AElement> elements = new ArrayList<>();
        for (CstElementModel e : getElements())
            elements.add(e.toNode());
        List<PExpression> transform = new ArrayList<>();
        for (CstTransformExpressionModel e : getTransformExpressions())
            transform.add(e.toNode());
        return new ACstAlternative(alternativeName, elements, new TTokArrow(), transform);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
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
