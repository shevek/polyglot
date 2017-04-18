/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.output;

import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.model.AnnotatedModel;
import org.anarres.polyglot.model.AnnotationModel;

/**
 *
 * @author shevek
 */
public class AbstractHelper {

    @Nonnull
    public boolean hasAnnotations(@Nonnull AnnotatedModel model, @Nonnull String name) {
        return model.getAnnotations().containsKey(name);
    }

    /**
     * Returns the list of values of annotations on the given model with the given name.
     *
     * The returned list may contain nulls for annotations which did not specify a value.
     *
     * @param model The model from which to retrieve annotations.
     * @param name The name of the annotations to retrieve.
     * @return The list of values of annotations on the given model with the given name.
     */
    @Nonnull
    public List<String> getAnnotations(@Nonnull AnnotatedModel model, @Nonnull String name) {
        List<String> out = new ArrayList<>();
        for (AnnotationModel annotation : model.getAnnotations().get(name))
            out.add(annotation.getValue());
        return out;
    }

    @Nonnull
    public boolean hasAnnotation(@Nonnull AnnotatedModel model, @Nonnull String name) {
        return hasAnnotations(model, name);
    }

    /**
     * Returns the value of the unique annotation on the given model with the given name.
     *
     * If the annotation is missing or not unique, an exception is thrown.
     * If the annotation has a null or no value, a null is returned.
     * Note: A null return value does NOT mean that the annotation was missing.
     *
     * @param model The model from which to retrieve annotations.
     * @param name The name of the annotations to retrieve.
     * @return The value of the unique annotation on the given model with the given name.
     */
    @CheckForNull
    public String getAnnotation(@Nonnull AnnotatedModel model, @Nonnull String name) {
        AnnotationModel annotation = Iterables.getOnlyElement(model.getAnnotations().get(name));
        return annotation.getValue();
    }

}
