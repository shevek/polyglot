/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.lr.Indexed;
import org.anarres.polyglot.node.ACstAlternative;
import org.anarres.polyglot.node.ACstProduction;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TTokArrow;
import org.anarres.polyglot.output.TemplateProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A nonterminal symbol.
 *
 * This class is final so that a cast to {@link AstProductionSymbol} is compile-time illegal.
 *
 * @author shevek
 */
public final class CstProductionModel extends AbstractNamedModel implements CstProductionSymbol, Indexed {

    @SuppressWarnings("UnusedVariable")
    private static final Logger LOG = LoggerFactory.getLogger(CstProductionModel.class);

    @Nonnull
    public static CstProductionModel forNode(@Nonnull ErrorHandler errors, @Nonnegative int index, @Nonnull ACstProduction node) {
        CstProductionModel model = new CstProductionModel(index, node.getName(), annotations(errors, node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    private final int index;
    /** @see CstAlternativeModel#transformExpressions */
    public final List<CstTransformPrototypeModel> transformPrototypes = new ArrayList<>();
    // TODO: Switch to ArrayList and use linear search for lookup-by-name. Make LR1ItemUniverse do for(int i) iteration.
    public final Map<String, CstAlternativeModel> alternatives = new LinkedHashMap<>();
    /* pp */ int alternativeIndex = 0;

    public CstProductionModel(@Nonnegative int index, @Nonnull TIdentifier name, @Nonnull Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
        this.index = index;
    }

    /** CstProductionModel uses index for the GOTO table. */
    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public List<CstTransformPrototypeModel> getTransformPrototypes() {
        return transformPrototypes;
    }

    /**
     * Returns the transform prototype at the given index.
     *
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
    public Collection<? extends CstAlternativeModel> getAlternatives() {
        return alternatives.values();
    }

    public void addAlternative(@Nonnull CstAlternativeModel alternative) {
        Object prev = alternatives.put(alternative.getName(), alternative);
        if (prev != null)
            throw new IllegalStateException("Clobbered CST alternative " + prev);
    }

    public boolean removeAlterative(@Nonnull CstAlternativeModel alternative) {
        return alternatives.remove(alternative.getName()) != null;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    /** Passing null asks if ignored for all machines. */
    public boolean isIgnored(@CheckForNull String machineName) {
        if (!AnnotationUtils.isIncluded(this, AnnotationName.ParserInclude, AnnotationName.ParserExclude, machineName))
            return true;
        if (AnnotationUtils.isAnnotated(this, AnnotationName.ParserIgnore, machineName))
            return true;
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
        return new ACstProduction(
                newJavadocCommentToken(),
                toNameToken(),
                new TTokArrow(),
                transform,
                alternatives,
                toAnnotations(getAnnotations()));
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
