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
public enum CstElementAssociativity {
    LEFT, RIGHT, NONE, UNSPECIFIED;

    @Nonnull
    public static CstElementAssociativity forModel(@Nonnull AnnotatedModel model) {
        if (model.hasAnnotation(AnnotationName.LeftAssociative))
            return CstElementAssociativity.LEFT;
        else if (model.hasAnnotation(AnnotationName.RightAssociative))
            return CstElementAssociativity.RIGHT;
        else if (model.hasAnnotation(AnnotationName.NonAssociative))
            return CstElementAssociativity.NONE;
        else
            return CstElementAssociativity.UNSPECIFIED;
    }
}
