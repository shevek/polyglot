/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public interface AstModel {

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

    @Nonnull
    @TemplateProperty
    public Multimap<String, ? extends AnnotationModel> getAnnotations();
}
