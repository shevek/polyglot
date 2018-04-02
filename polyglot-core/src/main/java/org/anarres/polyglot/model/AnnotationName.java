/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 *
 * @author shevek
 */
public enum AnnotationName {

    /** Used on a TokenModel or CstProductionModel to indicate the user-visible name of the syntactic construct. */
    Named(AbstractNamedModel.class), // Basically anything.
    /** Used on a TokenModel to set a default text, even if the regex is variable. */
    DefaultText(TokenModel.class),
    /** Used on a TokenModel to force a fixed text, even if the regex is variable. */
    Text(TokenModel.class),
    /** Used on a CstProductionModel to request deliberate early inlining. */
    Inline(CstProductionModel.class, CstAlternativeModel.class),
    /** Indicates that a production may be overwritten, or discarded if unreferenced. */
    Weak(TokenModel.class, ExternalModel.class, CstProductionModel.class, AstProductionModel.class),
    /** (Currently informal) Indicates whether a CST production is for general use, or is a private refactoring. */
    Public(CstProductionModel.class, AstProductionModel.class),
    Private(CstProductionModel.class, AstProductionModel.class),
    /** Used on a TokenModel to indicate that the token is ignored by the lexer. */
    // @Deprecated
    LexerIgnore(TokenModel.class),
    /** Used on a TokenModel to indicate that the token is included by only the named lexer(s). */
    LexerInclude(TokenModel.class),
    /** Used on a TokenModel to indicate that the token is excluded by the named lexer(s). */
    LexerExclude(TokenModel.class),
    /** Used on a TokenModel to indicate that it may be masked, i.e. never match. */
    LexerAllowMasking(TokenModel.class),
    /** Used on a TokenModel to indicate that the token or production is ignored by the parser with the given name(s). */
    // @Deprecated
    ParserIgnore(TokenModel.class, CstProductionModel.class, CstAlternativeModel.class),
    // /** Used on a TokenModel to indicate that the token or production is included by only the parser with the given name(s). */
    // ParserInclude(TokenModel.class, CstProductionModel.class, CstAlternativeModel.class),
    // /** Used on a TokenModel to indicate that the token or production is excluded by the parser with the given name(s). */
    // ParserExclude(TokenModel.class, CstProductionModel.class, CstAlternativeModel.class),
    /** Indicates that this is the start production for a Parser. */
    ParserStart(CstProductionModel.class),
    /** Indicates the relative precedence of a reduction. */
    ParserPrecedence(CstAlternativeModel.class),
    /** Used on an AstAlternativeModel to specify the Java superclass of the alternative. */
    javaExtends(AstProductionModel.class, AstAlternativeModel.class),
    /** Used on an AstProductionModel or AstAlternativeModel to specify a Java interface for the alternative. */
    javaImplements(AstProductionModel.class, AstAlternativeModel.class),
    /** Used on an AstElementModel to indicate that the javaExtends superclass implements the getter and setter. */
    javaProvided(AstElementModel.class),
    /** Used on an AstProductionModel, AstAlternativeModel or ElementModel to indicate an annotation for the class or method. */
    javaClassAnnotation(TokenModel.class, AstProductionModel.class, AstAlternativeModel.class),
    javaGetAnnotation(AstElementModel.class),
    javaSetAnnotation(AstElementModel.class),
    javaFieldAnnotation(AstElementModel.class),;

    private final Class<? extends Model>[] targets;

    @SafeVarargs
    @SuppressWarnings("varargs")
    private AnnotationName(@Nonnull Class<? extends Model>... targets) {
        this.targets = targets;
    }

    @Nonnull
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Class<? extends Model>[] getTargets() {
        return targets;
    }

    public boolean isTarget(@Nonnull Class<? extends Model> type) {
        for (Class<? extends Model> target : targets) {
            if (target.isAssignableFrom(type))
                return true;
        }
        return false;
    }

    @CheckForNull
    public static AnnotationName forText(@Nonnull String text) {
        try {
            return AnnotationName.valueOf(text);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static boolean isKnownAnnotation(@Nonnull String text) {
        try {
            AnnotationName.valueOf(text);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
