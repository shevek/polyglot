/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.anarres.polyglot.model;

import com.google.common.collect.ImmutableMultimap;
import javax.annotation.Nonnull;
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
    public static HelperModel forNode(@Nonnull AHelper node) {
        return new HelperModel(node.getName(), node.getMatcher());
    }

    public interface Value {
    }

    private final PMatcher matcher;
    // private final AHelper node;
    // NFA?
    public Value value;   // CharSet or NFA.

    public HelperModel(TIdentifier name, PMatcher matcher) {
        super(name, ImmutableMultimap.<String, AnnotationModel>of());
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
        return new AHelper(toNameToken(), matcher.clone());
    }

}
