/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.node.ACstAlternative;
import org.anarres.polyglot.node.ACstProduction;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TTokArrow;

/**
 * A nonterminal symbol.
 *
 * This class is final so that a cast to {@link AstProductionSymbol} is compile-time illegal.
 *
 * @author shevek
 */
public final class CstProductionModel extends AbstractNamedModel implements CstProductionSymbol, Indexed {

    @Nonnull
    public static CstProductionModel forNode(@Nonnegative int index, @Nonnull ACstProduction node) {
        return new CstProductionModel(index, node.getName());
    }

    private final int index;
    /** @see CstAlternativeModel#transformExpressions */
    public final List<CstTransformPrototypeModel> transformPrototypes = new ArrayList<>();
    public final Map<String, CstAlternativeModel> alternatives = new TreeMap<>();
    /* pp */ int alternativeIndex = 0;

    public CstProductionModel(@Nonnegative int index, @Nonnull TIdentifier name) {
        super(name);
        this.index = index;
    }

    /** CstProductionModel uses index for the GOTO table. */
    @Override
    public int getIndex() {
        return index;
    }

    /**
     * @see #getTransformPrototype(int)
     * @see CstAlternativeModel#getTransformExpressions()
     * @see CstAlternativeModel#getTransformExpression(int)
     * @return The list of transform prototypes.
     */
    @Nonnull
    @TemplateProperty("parser.vm")
    public List<CstTransformPrototypeModel> getTransformPrototypes() {
        return transformPrototypes;
    }

    /**
     * @see #getTransformPrototypes()
     * @see CstAlternativeModel#getTransformExpressions()
     * @see CstAlternativeModel#getTransformExpression(int)
     * @param index The index of the transform prototype to return.
     * @return The transform prototype.
     */
    @Nonnull
    public CstTransformPrototypeModel getTransformPrototype(int index) {
        return transformPrototypes.get(index);
    }

    @Nonnull
    @TemplateProperty("parser.vm")
    public Map<String, CstAlternativeModel> getAlternatives() {
        return alternatives;
    }

    public void addAlternative(@Nonnull CstAlternativeModel alternative) {
        alternatives.put(alternative.getName(), alternative);
    }

    public boolean removeAlterative(@Nonnull CstAlternativeModel alternative) {
        return alternatives.remove(alternative.getName()) != null;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @Override
    public ACstProduction toNode() {
        List<AElement> transform = new ArrayList<>();
        for (CstTransformPrototypeModel e : getTransformPrototypes())
            transform.add(e.toNode());
        List<ACstAlternative> alternatives = new ArrayList<>();
        for (Map.Entry<String, CstAlternativeModel> e : this.alternatives.entrySet())
            alternatives.add(e.getValue().toNode());
        return new ACstProduction(toNameToken(), new TTokArrow(), transform, alternatives);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append(getName()).append(" { ->");
        for (CstTransformPrototypeModel transform : getTransformPrototypes())
            buf.append(' ').append(transform);
        buf.append(" }");
        return buf.toString();
    }
}
