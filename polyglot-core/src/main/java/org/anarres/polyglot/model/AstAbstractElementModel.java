/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
@Deprecated // Not used
public class AstAbstractElementModel implements AstModel {

    private final String name;
    private final String javadocComment;
    private final String javaTypeName;
    private final UnaryOperator unaryOperator;

    public AstAbstractElementModel(
            @Nonnull String name,
            @CheckForNull String javadocComment,
            @Nonnull String javaTypeName,
            @Nonnull UnaryOperator unaryOperator) {
        this.name = name;
        this.javadocComment = javadocComment;
        this.javaTypeName = javaTypeName;
        this.unaryOperator = unaryOperator;
    }

    @Override
    public String getJavadocComment() {
        return javadocComment;
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Override
    public String getJavaTypeName() {
        return javaTypeName;
    }

    @Nonnull
    public UnaryOperator getUnaryOperator() {
        return unaryOperator;
    }

    @Override
    public String getJavaFieldName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, getName());
    }

    @Override
    public String getJavaMethodName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getName());
    }

    @Override
    public Multimap<String, ? extends AnnotationModel> getAnnotations() {
        return HashMultimap.create();
    }

    @TemplateProperty
    public boolean isNullable() {
        return getUnaryOperator().isNullable();
    }

    @TemplateProperty
    public boolean isList() {
        return getUnaryOperator().isList();
    }

}
