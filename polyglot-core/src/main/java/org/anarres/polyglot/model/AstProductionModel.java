/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.node.AAstAlternative;
import org.anarres.polyglot.node.AAstProduction;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * This class is final so that a cast to {@link CstProductionSymbol} is compile-time illegal.
 *
 * @author shevek
 */
public final class AstProductionModel extends AbstractNamedJavaModel implements AstProductionSymbol {

    @Nonnull
    public static AstProductionModel forNode(@Nonnull ErrorHandler errors, @Nonnull AAstProduction node) {
        AstProductionModel model = new AstProductionModel(node.getName(), annotations(errors, node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    public final Map<String, AstAlternativeModel> alternatives = new TreeMap<>();

    public AstProductionModel(TIdentifier name, Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
    }

    @Override
    @TemplateProperty("parser.vm")
    public String getJavaTypeName() {
        return "P" + CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getName());
    }

    @Nonnull
    @TemplateProperty
    public List<AstAlternativeModel> getAlternatives() {
        return new ArrayList<>(alternatives.values());
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    /**
     * Returns the single alternative of this production, if it is a singleton.
     * Otherwise, returns null.
     *
     * This method is used to decide whether to emit element references as
     * the Java type of the production or of the alternative.
     *
     * @return the single alternative of this production, if it is a singleton.
     */
    @CheckForNull
    // @TemplateProperty
    public AstAlternativeModel getSingletonAlternative() {
        if (alternatives.size() != 1)
            return null;
        return alternatives.values().iterator().next();
    }

    @Override
    public AAstProduction toNode() {
        List<AAstAlternative> alternatives = new ArrayList<>();
        for (Map.Entry<String, AstAlternativeModel> e : this.alternatives.entrySet())
            alternatives.add(e.getValue().toNode());
        return new AAstProduction(
                newJavadocCommentToken(),
                toNameToken(),
                alternatives,
                toAnnotations(getAnnotations()));
    }
}
