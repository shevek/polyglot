/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public enum AnnotationName {

    /** Used on a TokenModel or CstProductionModel to indicate the user-visible name of the syntactic construct. */
    Named,
    /** Used on a TokenModel to force a fixed text, even if the regex is variable. */
    Text,
    /** Used on a CstProductionModel to request deliberate early inlining. */
    Inline,
    /** Used on an AstAlternativeModel to specify the Java superclass of the alternative. */
    javaExtends,
    /** Used on an AstProductionModel or AstAlternativeModel to specify a Java interface for the alternative. */
    javaImplements,
    /** Used on an AstProductionModel, AstAlternativeModel or ElementModel to indicate an annotation for the class or method. */
    javaAnnotation;

    public static boolean isKnownAnnotation(@Nonnull String text) {
        try {
            AnnotationName.valueOf(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
