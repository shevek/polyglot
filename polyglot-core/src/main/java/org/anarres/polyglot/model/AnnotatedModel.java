/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.Multimap;
import java.util.Collection;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public interface AnnotatedModel extends Model {

    @Nonnull
    @TemplateProperty
    public Multimap<String, ? extends AnnotationModel> getAnnotations();

    public boolean hasAnnotation(@Nonnull AnnotationName name);

    @Nonnull
    public Collection<? extends AnnotationModel> getAnnotations(@Nonnull AnnotationName name);
}
