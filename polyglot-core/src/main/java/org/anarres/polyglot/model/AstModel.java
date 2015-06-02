/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public interface AstModel {

    @Nonnull
    public String getJavadocComment();

    @Nonnull
    public abstract String getJavaTypeName();

    @Nonnull
    public String getJavaFieldName();

    @Nonnull
    public String getJavaMethodName();

    @Nonnull
    public Multimap<String, AnnotationModel> getAnnotations();
}
