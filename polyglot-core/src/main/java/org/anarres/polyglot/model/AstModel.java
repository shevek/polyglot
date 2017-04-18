/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;

/**
 * The set of models of things which might appear in the AST.
 *
 * @author shevek
 */
public interface AstModel extends AnnotatedModel {

    @Nonnull
    @TemplateProperty
    public String getJavadocComment();

    @Nonnull
    @TemplateProperty
    public abstract String getJavaTypeName();

    @Nonnull
    @TemplateProperty
    public String getJavaFieldName();

    @Nonnull
    @TemplateProperty
    public String getJavaMethodName();
}
