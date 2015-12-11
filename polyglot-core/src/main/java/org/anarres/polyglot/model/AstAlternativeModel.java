/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.node.AAstAlternative;
import org.anarres.polyglot.node.AElement;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.Token;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class AstAlternativeModel extends AbstractNamedJavaModel implements AstModel {

    @Nonnull
    public static AstAlternativeModel forNode(@Nonnull AstProductionModel production, @Nonnull AAstAlternative node) {
        Token location = location(production, node.getName(), node.getJavadocComment(), Iterables.getFirst(node.getElements(), null));
        AstAlternativeModel model = new AstAlternativeModel(production, location, node.getName(), annotations(node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    private final AstProductionModel production;
    @CheckForNull
    private final TIdentifier alternativeName;
    public final List<AstElementModel> elements = new ArrayList<>();
    public final List<AstElementModel> externals = new ArrayList<>();
    private final Multimap<String, AnnotationModel> annotations;
    // Cached
    private final String javaTypeName;

    public AstAlternativeModel(@Nonnull AstProductionModel production, @Nonnull Token location, @CheckForNull TIdentifier name, Multimap<String, AnnotationModel> annotations) {
        // TODO: This is a really bad choice for Location as it points to the production not the elements.
        super(location, name(production, name));
        this.production = production;
        this.alternativeName = name;
        this.annotations = annotations;

        StringBuilder buf = new StringBuilder("A");
        if (alternativeName != null)
            buf.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, alternativeName.getText()));
        buf.append(CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, production.getName()));
        this.javaTypeName = buf.toString();
    }

    @Override
    public String getSourceName() {
        if (alternativeName != null)
            return alternativeName.getText();
        return getProduction().getSourceName();   // Must be unqualified.
    }

    @Nonnull
    public AstProductionModel getProduction() {
        return production;
    }

    @CheckForNull
    public TIdentifier getAlternativeName() {
        return alternativeName;
    }

    @Override
    public String getJavaTypeName() {
        return javaTypeName;
    }

    @Nonnull
    @TemplateProperty
    public List<? extends AstElementModel> getElements() {
        return elements;
    }

    @Nonnull
    @TemplateProperty
    public List<? extends AstElementModel> getElementsReversed() {
        List<AstElementModel> elements = new ArrayList<>(getElements());
        Collections.reverse(elements);
        return elements;
    }

    @Nonnull
    @TemplateProperty
    public List<? extends AstElementModel> getExternals() {
        return externals;
    }

    @Override
    public Multimap<String, AnnotationModel> getAnnotations() {
        return annotations;
    }

    @Override
    public AAstAlternative toNode() {
        List<AElement> elements = new ArrayList<>();
        for (AstElementModel e : getElements())
            elements.add(e.toNode());
        for (AstElementModel e : getExternals())
            elements.add(e.toNode());
        return new AAstAlternative(newJavadocCommentToken(), alternativeName, elements, toAnnotations(annotations));
    }
}
