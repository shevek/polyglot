/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import org.anarres.polyglot.analysis.MatcherParserVisitor;
import org.anarres.polyglot.node.AExternal;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TString;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class ExternalModel extends AbstractNamedJavaModel implements AstProductionSymbol {

    @Nonnull
    public static ExternalModel forNode(@Nonnull AExternal node) {
        ExternalModel model = new ExternalModel(node.getName(), node.getExternalType(), annotations(node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    private final TString externalType;
    // Cached
    private final String javaTypeName;

    public ExternalModel(@Nonnull TIdentifier name, @Nonnull TString externalType, Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
        this.externalType = externalType;
        this.javaTypeName = MatcherParserVisitor.parse(externalType);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @TemplateProperty("alternative.vm")
    public boolean isPrimitive() {
        return getJavaTypeName().toLowerCase().equals(getJavaTypeName());
    }

    @Override
    public String getJavaTypeName() {
        return javaTypeName;
    }

    @Override
    public Node toNode() {
        return new AExternal(
                newJavadocCommentToken(),
                toNameToken(),
                externalType.clone(),
                toAnnotations(getAnnotations()));
    }
}
