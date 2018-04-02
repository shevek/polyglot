/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public class AnnotationUtils {

    public static boolean isAnnotated(@Nonnull AnnotatedModel model, @Nonnull AnnotationName name, @CheckForNull String value) {
        // LOG.debug("isIgnored: " + model + "." + model.getName());
        for (AnnotationModel annotation : model.getAnnotations(name)) {
            String annotationValue = annotation.getValue();
            // LOG.info("Actual: " + annotationValue);
            if (annotationValue == null)
                return true;
            if (annotationValue.equals(value))
                return true;
        }
        return false;
    }

    public static boolean isIncluded(@Nonnull AnnotatedModel model, @Nonnull AnnotationName includeName, @Nonnull AnnotationName excludeName, @Nonnull String value) {
        if (model.hasAnnotation(includeName))
            return isAnnotated(model, includeName, value);    // Returns overall false if no match.
        if (model.hasAnnotation(excludeName))
            return !isAnnotated(model, excludeName, value);   // Returns overall true if no match.
        return true;
    }

}
