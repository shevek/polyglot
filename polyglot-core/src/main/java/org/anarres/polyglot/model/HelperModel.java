/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.Multimap;
import javax.annotation.Nonnull;
import org.anarres.polyglot.ErrorHandler;
import org.anarres.polyglot.node.AHelper;
import org.anarres.polyglot.node.PMatcher;
import org.anarres.polyglot.node.TIdentifier;
import org.anarres.polyglot.output.TemplateProperty;

/**
 *
 * @author shevek
 */
public class HelperModel extends AbstractNamedModel {

    @Nonnull
    public static HelperModel forNode(@Nonnull ErrorHandler errors, @Nonnull AHelper node) {
        HelperModel model = new HelperModel(node.getName(), node.getMatcher(), annotations(errors, node.getAnnotations()));
        model.setJavadocComment(node.getJavadocComment());
        return model;
    }

    public interface Value {
    }

    private final PMatcher matcher;
    // private final AHelper node;
    // NFA?
    public Value value;   // CharSet or NFA.

    public HelperModel(TIdentifier name, PMatcher matcher, Multimap<String, ? extends AnnotationModel> annotations) {
        super(name, annotations);
        this.matcher = matcher;
    }

    @TemplateProperty("html")
    public PMatcher getMatcher() {
        return matcher;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    @Override
    public AHelper toNode() {
        return new AHelper(
                newJavadocCommentToken(),
                toNameToken(),
                matcher.clone(),
                toAnnotations(getAnnotations()));
    }

}
