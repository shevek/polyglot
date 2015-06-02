/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.base.CaseFormat;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import org.anarres.polyglot.output.TemplateProperty;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.node.TJavadocComment;
import org.anarres.polyglot.node.Token;

/**
 * Really, this should only be inherited into AST objects which are generated to Java.
 * However, it gets inherited into AbstractElementModel too.
 *
 * @author shevek
 */
public abstract class AbstractNamedJavaModel extends AbstractNamedModel {

    private TJavadocComment javadocComment;

    public AbstractNamedJavaModel(@Nonnull Token location, @Nonnull String name) {
        super(location, name);
    }

    public AbstractNamedJavaModel(@Nonnull TIdentifier name) {
        super(name);
    }

    /** Typically {@link CaseFormat#UPPER_CAMEL}. */
    @Nonnull
    @TemplateProperty
    public abstract String getJavaTypeName();
    // return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, getName()); }

    /** Typically {@link CaseFormat#LOWER_CAMEL}. */
    @Nonnull
    @TemplateProperty("parser.vm")  // For referring to CST production elements on the stack.
    public String getJavaFieldName() {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, getName());
    }

    /** Typically {@link CaseFormat#UPPER_CAMEL}. */
    @Nonnull
    @TemplateProperty
    public String getJavaMethodName() {
        return getJavaTypeName();
    }

    @CheckForNull
    public TJavadocComment newJavadocCommentToken() {
        if (javadocComment == null)
            return null;
        return javadocComment.clone();
    }

    @CheckForNull
    public String getJavadocComment() {
        if (javadocComment == null)
            return null;
        return javadocComment.getText();
    }

    public void setJavadocComment(TJavadocComment javadocComment) {
        this.javadocComment = javadocComment;
    }
}
