/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.analysis.MatcherParserVisitor;
import org.anarres.polyglot.node.AExternal;
import org.anarres.polyglot.node.Node;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class ExternalModel extends AbstractNamedJavaModel implements AstProductionSymbol {

    @Nonnull
    public static ExternalModel forNode(@Nonnull ErrorHandler errors, @Nonnull AExternal node) {
        String javaTypeName = MatcherParserVisitor.parse(errors, node.getExternalType());
        ExternalModel model = new ExternalModel(node.getName(), javaTypeName, annotations(errors, node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    // Cached
    private final String javaTypeName;

    public ExternalModel(@Nonnull TIdentifier name, @Nonnull String javaTypeName, Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
        this.javaTypeName = javaTypeName;
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    @TemplateProperty("alternative.vm")
    public boolean isPrimitive() {
        return getJavaTypeName().toLowerCase().equals(getJavaTypeName());
    }

    @TemplateProperty("alternative.vm")
    public boolean isPrimitiveBoolean() {
        return "boolean".equals(getJavaTypeName());
    }

    @Override
    public String getJavaTypeName() {
        return javaTypeName;
    }

    @Override
    public String getJavaMethodName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getName());
    }

    @Override
    public Node toNode() {
        return new AExternal(
                newJavadocCommentToken(),
                toNameToken(),
                MatcherParserVisitor.escape(getJavaTypeName()),
                toAnnotations(getAnnotations()));
    }
}
